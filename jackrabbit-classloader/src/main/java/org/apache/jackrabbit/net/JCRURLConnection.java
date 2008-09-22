/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.classloader.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>JCRURLConnection</code> is the <code>URLConnection</code>
 * implementation to access the data addressed by a JCR Repository URL.
 * <p>
 * As the primary use of a <code>URLConnection</code> and thus the
 * <code>JCRURLConnection</code> is to provide access to the content of a
 * resource identified by the URL, it is the primary task of this class to
 * identify and access a repository <code>Property</code> based on the URL. This
 * main task is executed in the {@link #connect()} method.
 * <p>
 * Basically the guideposts to access content from a JCR Repository URL are
 * the following:
 * <ul>
 * <li>The URL must ultimately resolve to a repository property to provide
 *      content.
 * <li>If the URL itself is the path to a property, that property is used to
 *      provide the content.
 * <li>If the URL is a path to a node, either the
 *      <code>jcr:content/jcr:data</code> or <code>jcr:data</code> property is
 *      used or the primary item chain starting with this node is followed until
 *      no further primary items exist. If the final item is a property, that
 *      property is used to provide the content.
 * <li>If neither of the above methods resolve to a property, the
 *      {@link #connect()} fails and access to the content is not possible.
 * </ul>
 * <p>
 * After having connected the property is available through the
 * {@link #getProperty()} method. Other methods exist to retrieve repository
 * related information defined when creating the URL: {@link #getSession()} to
 * retrieve the session of the URL, {@link #getPath()} to retrieve the path
 * with which the URL was created and {@link #getItem()} to retrieve the item
 * with which the URL was created. The results of calling {@link #getProperty()}
 * and {@link #getItem()} will be the same if the URL directly addressed the
 * property. If the URL addressed the node whose primary item chain ultimately
 * resolved to the property, the {@link #getItem()} will return the node and
 * {@link #getProperty()} will return the resolved property.
 * <p>
 * A note on the <code>InputStream</code> available from
 * {@link #getInputStream()}: Unlike other implementations - for example
 * for <code>file:</code> or <code>http:</code> URLs - which return the same
 * stream on each call, this implementation returns a new stream on each
 * invocation.
 * <p>
 * The following header fields are implemented by this class:
 * <dl>
 * <dt><code>Content-Length</code>
 * <dd>The size of the content is filled from the <code>Property.getLength()</code>
 *      method, which returns the size in bytes of the property's value for
 *      binary values and the number of characters used for the string
 *      representation of the value for all other value types.
 *
 * <dt><code>Content-Type</code>
 * <dd>The content type is retrieved from the <code>jcr:mimeType</code>
 *      property of the property's parent node if existing. Otherwise the
 *      <code>guessContentTypeFromName</code> method is called on the
 *      {@link #getPath() path}. If this does not yield a content type, it is
 *      set to <code>application/octet-stream</code> for binary properties and
 *      to <code>text/plain</code> for other types.
 *
 * <dt><code>Content-Enconding</code>
 * <dd>The content encoding is retrieved from the <code>jcr:econding</code>
 *      property of the property's parent node if existing. Otherwise this
 *      header field remains undefined (aka <code>null</code>).
 *
 * <dt><code>Last-Modified</code>
 * <dd>The last modified type is retrieved from the <code>jcr:lastModified</code>
 *      property of the property's parent node if existing. Otherwise the last
 *      modification time is set to zero.
 * </dl>
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 */
public class JCRURLConnection extends URLConnection {

    /** Default logging */
    private static final Logger log =
        LoggerFactory.getLogger(JCRURLConnection.class);

    /**
     * The name of the header containing the content size (value is
     * "content-length").
     */
    protected static final String CONTENT_LENGTH = "content-length";

    /**
     * The name of the header containing the MIME type of the content (value is
     * "content-type").
     */
    protected static final String CONTENT_TYPE = "content-type";

    /**
     * The name of the header containing the content encoding (value is
     * "content-encoding").
     */
    protected static final String CONTENT_ENCODING = "content-encoding";

    /**
     * The name of the header containing the last modification time stamp of
     * the content (value is "last-modified").
     */
    protected static final String LAST_MODIFIED = "last-modified";

    /**
     * The default content type name for binary properties accessed by this
     * connection (value is "application/octet-stream").
     * @see #connect()
     */
    protected static final String APPLICATION_OCTET = "application/octet-stream";

    /**
     * The default content type name for non-binary properties accessed by this
     * connection (value is "text/plain").
     * @see #connect()
     */
    protected static final String TEXT_PLAIN = "text/plain";

    /**
     * The handler associated with the URL of this connection. This handler
     * provides the connection with access to the repository and the item
     * underlying the URL.
     */
    private final JCRURLHandler handler;

    /**
     * The {@link FileParts} encapsulating the repository name, workspace name,
     * item path and optional archive entry path contained in the file part
     * of the URL. This field is set on-demand by the {@link #getFileParts()}
     * method.
     *
     * @see #getFileParts()
     */
    private FileParts fileParts;

    /**
     * The <code>Item</code> addressed by the path of this connection's URL.
     * This field is set on-demand by the {@link #getItem()} method.
     *
     * @see #getItem()
     */
    private Item item;

    /**
     * The <code>Property</code> associated with the URLConnection. The field
     * is only set after the connection has been successfully opened.
     *
     * @see #getProperty()
     * @see #connect()
     */
    private Property property;

    /**
     * The (guessed) content type of the data. Currently the content type is
     * guessed based on the path name of the page or the binary attribute of the
     * atom.
     * <p>
     * Implementations are free to decide, how to define the content type. But
     * they are required to set the type in the {@link #connect(Ticket)}method.
     *
     * @see #getContentType()
     * @see #connect()
     */
    private String contentType;

    /**
     * The (guessed) content encoding of the data. Currently the content type is
     * guessed based on the path name of the page or the binary attribute of the
     * atom.
     * <p>
     * Implementations are free to decide, how to define the content type. But
     * they are required to set the type in the {@link #connect(Ticket)}method.
     *
     * @see #getContentEncoding()
     * @see #connect()
     */
    private String contentEncoding;

    /**
     * The content lentgh of the data, which is the size field of the atom
     * status information of the base atom.
     * <p>
     * Implementations are free to decide, how to define the content length. But
     * they are required to set the type in the {@link #connect(Ticket)}method.
     *
     * @see #getContentLength()
     * @see #connect()
     */
    private int contentLength;

    /**
     * The last modification time in milliseconds since the epoch (1970/01/01)
     * <p>
     * Implementations are free to decide, how to define the last modification
     * time. But they are required to set the type in the
     * {@link #connect(Ticket)}method.
     *
     * @see #getLastModified()
     * @see #connect()
     */
    private long lastModified;

    /**
     * Creates an instance of this class for the given <code>url</code>
     * supported by the <code>handler</code>.
     *
     * @param url The URL to base the connection on.
     * @param handler The URL handler supporting the given URL.
     */
    JCRURLConnection(URL url, JCRURLHandler handler) {
        super(url);
        this.handler = handler;
    }

    /**
     * Returns the current session of URL.
     * <p>
     * Calling this method does not require this connection being connected.
     */
    public Session getSession() {
        return handler.getSession();
    }

    /**
     * Returns the path to the repository item underlying the URL of this
     * connection.
     * <p>
     * Calling this method does not require this connection being connected.
     */
    public String getPath() {
        return getFileParts().getPath();
    }

    /**
     * Returns the repository item underlying the URL of this connection
     * retrieved through the path set on the URL.
     * <p>
     * Calling this method does not require this connection being connected.
     *
     * @throws IOException If the item has to be retrieved from the repository
     *      <code>Session</code> of this connection and an error occurrs. The
     *      cause of the exception will refer to the exception thrown from the
     *      repository. If the path addresses a non-existing item, the cause
     *      will be a <code>PathNotFoundException</code>.
     */
    public Item getItem() throws IOException {
        if (item == null) {
            try {
                item = getSession().getItem(getPath());
            } catch (RepositoryException re) {
                throw failure("getItem", re.toString(), re);
            }
        }

        return item;
    }

    /**
     * Returns the repository <code>Property</code> providing the contents of
     * this connection.
     * <p>
     * Calling this method forces the connection to be opened by calling the
     * {@link #connect()} method.
     *
     * @throws IOException May be thrown by the {@link #connect()} method called
     *      by this method.
     *
     * @see #connect()
     */
    public Property getProperty() throws IOException {
        // connect to set the property value
        connect();

        return property;
    }

    //---------- URLConnection overwrites -------------------------------------

    /**
     * Connects to the URL setting the header fields and preparing for the
     * {@link #getProperty()} and {@link #getInputStream()} methods.
     * <p>
     * The following algorithm is applied:
     * <ol>
     * <li>The repository item is retrieved from the URL's
     *      <code>URLHandler</code>.
     * <li>If the item is a node, the <code>getPrimaryItem</code> method is
     *      called on that node. If the node has no primary item, the connection
     *      fails.
     * <li>If the item - either from the handler or after calling
     *      <code>getPrimaryItem</code> is still a node, this method fails
     *      because a <code>Property</code> is required for a successfull
     *      connection.
     * <li>If the property found above is a multi-valued property, connection
     *      fails, because multi-valued properties are not currently supported.
     * <li>The content length header field is set from the property length
     *      (<code>Property.getLength())</code>).
     * <li>The header fields for the content type, content encoding and last
     *      modification time are set from the <code>jcr:mimeType</code>,
     *      <code>jcr:encoding</code>, and <code>jcr:lastModification</code>
     *      properties of the property's parent node if existing. Otherwise the
     *      content encoding field is set to <code>null</code> and the last
     *      modification time is set to zero. The content type field is guessed
     *      from the name of the URL item. If the content type cannot be
     *      guessed, it is set to <code>application/octet-stream</code> if the
     *      property is of binary type or <code>text/plain</code> otherwise.
     * </ol>
     * <p>
     * When this method successfully returns, this connection is considered
     * connected. In case of an exception thrown, the connection is not
     * connected.
     *
     * @throws IOException if an error occurrs retrieving the data property or
     *      any of the header field value properties or if any other errors
     *      occurrs. Any cuasing exception is set as the cause of this
     *      exception.
     */
    public synchronized void connect() throws IOException {
        // todo: The ContentBus URL must also contain version information on
        if (!connected) {

            // Get hold of the data
            try {
                // resolve the URLs item to a property
                Property property = Util.getProperty(getItem());
                if (property == null) {
                    throw failure("connect",
                        "Multivalue property not supported", null);
                }

                // values to set later
                String contentType;
                String contentEncoding = null; // no defined content encoding
                int contentLength = (int) property.getLength();
                long lastModified;

                Node parent = property.getParent();
                if (parent.hasProperty("jcr:lastModified")) {
                    lastModified = parent.getProperty("jcr:lastModified").getLong();
                } else {
                    lastModified = 0;
                }
                
                if (parent.hasProperty("jcr:mimeType")) {
                    contentType = parent.getProperty("jcr:mimeType").getString();
                } else {
                    contentType = guessContentTypeFromName(getItem().getName());
                    if (contentType == null) {
                        contentType = (property.getType() == PropertyType.BINARY)
                                ? APPLICATION_OCTET
                                : TEXT_PLAIN;
                    }
                }
                
                if (parent.hasProperty("jcr:encoding")) {
                    contentEncoding = parent.getProperty("jcr:encoding").getString();
                } else {
                    contentEncoding = null;
                }

                log.debug(
                    "connect: Using property '{}' with content type '{}' for {} bytes",
                    new Object[] { property.getPath(), contentType,
                        new Integer(contentLength) });

                // set the fields
                setProperty(property);
                setContentType(contentType);
                setContentEncoding(contentEncoding);
                setContentLength(contentLength);
                setLastModified(lastModified);

                // mark connection open
                connected = true;

            } catch (RepositoryException re) {
                throw failure("connect", re.toString(), re);
            }
        }
    }

    /**
     * Returns an input stream that reads from this open connection.
     * <p>
     * <b>NOTES:</b>
     * <ul>
     * <li>Each call to this method returns a new <code>InputStream</code>.
     * <li>Do not forget to close the return stream when not used anymore for
     *      the system to be able to free resources.
     * </ul>
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @throws IOException if an error occurrs opening the connection through
     *      {@link #connect()} or creating the <code>InputStream</code> on the
     *      repository <code>Property</code>.
     *
     * @see #connect()
     */
    public InputStream getInputStream() throws IOException {
        try {
            return getProperty().getStream();
        } catch (RepositoryException re) {
            throw failure("getInputStream", re.toString(), re);
        }
    }

    /**
     * Gets the named header field. This implementation only supports the
     * Content-Type, Content-Encoding, Content-Length and Last-Modified header
     * fields. All other names return <code>null</code>.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @param s The name of the header field value to return.
     *
     * @return The corresponding value or <code>null</code> if not one of the
     *      supported fields or the named field's value cannot be retrieved
     *      from the data source.
     *
     * @see #connect()
     */
    public String getHeaderField(String s) {
        try {
            connect();
            if (CONTENT_LENGTH.equalsIgnoreCase(s)) {
                return String.valueOf(contentLength);
            } else if (CONTENT_TYPE.equalsIgnoreCase(s)) {
                return contentType;
            } else if (LAST_MODIFIED.equalsIgnoreCase(s)) {
                return String.valueOf(lastModified);
            } else if (CONTENT_ENCODING.equalsIgnoreCase(s)) {
                return contentEncoding;
            }
        } catch (IOException ioe) {
            log.info("getHeaderField: Problem connecting: " + ioe.toString());
            log.debug("dump", ioe);
        }

        return null;
    }

    /**
     * Get the header field with the given index. As with
     * {@link #getHeaderField(String)} only Content-Length, Content-Type,
     * Content-Encoding, and Last-Modified are supported. All indexes other
     * than 0, 1, 2 or 3 will return <code>null</code>.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @param i The index of the header field value to return.
     *
     * @return The corresponding value or <code>null</code> if not one of the
     *      supported fields or the known field's value cannot be retrieved
     *      from the data source.
     *
     * @see #connect()
     */
    public String getHeaderField(int i) {
        try {
            connect();
            if (i == 0) {
                return String.valueOf(contentLength);
            } else if (i == 1) {
                return contentType;
            } else if (i == 2) {
                return String.valueOf(lastModified);
            } else if (i == 3) {
                return contentEncoding;
            }
        } catch (IOException ioe) {
            log.info("getHeaderField: Problem connecting: " + ioe.toString());
            log.debug("dump", ioe);
        }

        return null;
    }

    /**
     * Get the name of the header field with the given index. As with
     * {@link #getHeaderField(String)} only Content-Length, Content-Type,
     * Content-Encoding and Last-Modified are supported. All indexes other than
     * 0, 1, 2 or 3 will return <code>null</code>.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @param i The index of the header field name to return.
     * @return The corresponding name or <code>null</code> if not one of the
     *         supported fields.
     *
     * @see #connect()
     */
    public String getHeaderFieldKey(int i) {
        try {
            connect();
            if (i == 0) {
                return CONTENT_LENGTH;
            } else if (i == 1) {
                return CONTENT_TYPE;
            } else if (i == 2) {
                return LAST_MODIFIED;
            } else if (i == 3) {
                return CONTENT_ENCODING;
            }
        } catch (IOException ioe) {
            log
                .info("getHeaderFieldKey: Problem connecting: "
                    + ioe.toString());
            log.debug("dump", ioe);
        }
        return null;
    }

    /**
     * Returns an unmodifiable map of all header fields. Each entry is indexed
     * with a string key naming the field. The entry's value is an unmodifiable
     * list of the string values of the respective header field.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @return An unmodifiable map of header fields and their values. The map
     *      will be empty if an error occurrs connecting through
     *      {@link #connect()}.
     *
     * @see #connect()
     */
    public Map getHeaderFields() {
        Map fieldMap = new HashMap();

        try {
            connect();
            fieldMap.put(CONTENT_LENGTH, toList(String.valueOf(contentLength)));
            fieldMap.put(CONTENT_TYPE, toList(contentType));
            fieldMap.put(LAST_MODIFIED, toList(String.valueOf(lastModified)));

            // only include if not null))
            if (contentEncoding != null) {
                fieldMap.put(CONTENT_ENCODING, toList(contentEncoding));
            }
        } catch (IOException ioe) {
            log.info("getHeaderFields: Problem connecting: " + ioe.toString());
            log.debug("dump", ioe);
        }

        return Collections.unmodifiableMap(fieldMap);
    }

    /**
     * Returns the content type of the data as a string. This is just a
     * perfomance convenience overwrite of the base class implementation.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @return The content length of the data or <code>null</code> if the
     *      content type cannot be derived from the data source.
     *
     * @see #connect()
     */
    public String getContentType() {
        try {
            connect();
            return contentType;
        } catch (IOException ioe) {
            log.info("getContentType: Problem connecting: " + ioe.toString());
            log.debug("dump", ioe);
        }

        return null;
    }

    /**
     * Returns the content encoding of the data as a string. This is just a
     * perfomance convenience overwrite of the base class implementation.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @return The content encoding of the data or <code>null</code> if the
     *      content encoding cannot be derived from the data source.
     *
     * @see #connect()
     */
    public String getContentEncoding() {
        try {
            connect();
            return contentEncoding;
        } catch (IOException ioe) {
            log.info("getContentEncoding: Problem connecting: " + ioe.toString());
            log.debug("dump", ioe);
        }

        return null;
    }

    /**
     * Returns the content length of the data as an number. This is just a
     * perfomance convenience overwrite of the base class implementation.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @return The content length of the data or -1 if the content length cannot
     *         be derived from the data source.
     *
     * @see #connect()
     */
    public int getContentLength() {
        try {
            connect();
            return contentLength;
        } catch (IOException ioe) {
            log.info("getContentLength: Problem connecting: " + ioe.toString());
            log.debug("dump", ioe);
        }
        return -1;
    }

    /**
     * Returns the value of the <code>last-modified</code> header field. The
     * result is the number of milliseconds since January 1, 1970 GMT.
     * <p>
     * Calling this method implicitly calls {@link #connect()} to ensure the
     * connection is open.
     *
     * @return the date the resource referenced by this
     *         <code>URLConnection</code> was last modified, or -1 if not
     *         known.
     *
     * @see #connect()
     */
    public long getLastModified() {
        try {
            connect();
            return lastModified;
        } catch (IOException ioe) {
            log.info("getLastModified: Problem connecting: " + ioe.toString());
            log.debug("dump", ioe);
        }
        return -1;
    }

    //---------- implementation helpers ----------------------------------------

    /**
     * Returns the URL handler of the URL of this connection.
     */
    protected JCRURLHandler getHandler() {
        return handler;
    }

    /**
     * Returns the {@link FileParts} object which contains the decomposed file
     * part of this connection's URL.
     */
    FileParts getFileParts() {
        if (fileParts == null) {
            fileParts = new FileParts(getURL().getFile());
        }

        return fileParts;
    }

    /**
     * @param contentEncoding The contentEncoding to set.
     */
    protected void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    /**
     * @param contentLength The contentLength to set.
     */
    protected void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @param contentType The contentType to set.
     */
    protected void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @param lastModified The lastModified to set.
     */
    protected void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @param property The property to set.
     */
    protected void setProperty(Property property) {
        this.property = property;
    }

    //---------- internal -----------------------------------------------------

    /**
     * Logs the message and returns an IOException to be thrown by the caller.
     * The log message contains the caller name, the external URL form and the
     * message while the IOException is only based on the external URL form and
     * the message given.
     *
     * @param method The method in which the error occurred. This is used for
     *            logging.
     * @param message The message to log and set in the exception
     * @param cause The cause of failure. May be <code>null</code>.
     *
     * @return The IOException the caller may throw.
     */
    protected IOException failure(String method, String message, Throwable cause) {
        log.info(method + ": URL: " + url.toExternalForm() + ", Reason: "
            + message);

        if (cause != null) {
            log.debug("dump", cause);
        }

        IOException ioe = new IOException(url.toExternalForm() + ": " + message);
        ioe.initCause(cause);
        return ioe;
    }

    /**
     * Returns an unmodifiable list containing just the given string value.
     */
    private List toList(String value) {
        String[] values = { value };
        List valueList = Arrays.asList(values);
        return Collections.unmodifiableList(valueList);
    }
}
