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
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JCRJarURLConnection</code> extends the
 * {@link org.apache.jackrabbit.net.JCRURLConnection} class to support accessing
 * archive files stored in a JCR Repository.
 * <p>
 * Just like the base class, this class requires the URL to resolve, either
 * directly or through primary item chain, to a repository <code>Property</code>.
 * <p>
 * Access to this connections property and archive entry content is perpared
 * with the {@link #connect()}, which after calling the base class implementation
 * to find the property tries to find the archive entry and set the connection's
 * fields according to the entry. This implementation's {@link #connect()}
 * method fails if the named entry does not exist in the archive.
 * <p>
 * The {@link #getInputStream()} method either returns an stream on the archive
 * entry or on the archive depending on whether an entry path is specified
 * in the URL or not. Like the base class implementation, this implementation
 * returns a new <code>InputStream</code> on each invocation.
 * <p>
 * If an entry path is defined on the URL, the header fields are set from the
 * archive entry:
 * <table border="0" cellspacing="0" cellpadding="3">
 *  <tr><td><code>Content-Type</code><td>Guessed from the entry name or
 *      <code>application/octet-stream</code> if the type cannot be guessed
 *      from the name</tr>
 *  <tr><td><code>Content-Encoding</code><td><code>null</code></tr>
 *  <tr><td><code>Content-Length</code><td>The size of the entry</tr>
 *  <tr><td><code>Last-Modified</code><td>The last modification time of the
 *      entry</tr>
 * </table>
 * <p>
 * If no entry path is defined on the URL, the header fields are set from the
 * property by the base class implementation with the exception of the
 * content type, which is set to <code>application/java-archive</code> by
 * the {@link #connect()} method.
 * <p>
 * <em>Note that this implementation does only support archives stored in the
 * JCR Repository, no other contained storage such as </em>file<em> or
 * </em>http<em> is supported.</em>
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 *
 * @author Felix Meschberger
 */
public class JCRJarURLConnection extends JCRURLConnection {

    /** default log category */
    private static final Logger log =
        LoggerFactory.getLogger(JCRJarURLConnection.class);

    /**
     * The name of the MIME content type for this connection's content if
     * no entry path is defined on the URL (value is "application/java-archive").
     */
    protected static final String APPLICATION_JAR = "application/java-archive";

    /**
     * Creates an instance of this class for the given <code>url</code>
     * supported by the <code>handler</code>.
     *
     * @param url The URL to base the connection on.
     * @param handler The URL handler supporting the given URL.
     */
    JCRJarURLConnection(URL url, JCRJarURLHandler handler) {
        super(url, handler);
    }

    /**
     * Returns the path to the entry contained in the archive or
     * <code>null</code> if the URL contains no entry specification in the
     * path.
     */
    String getEntryPath() {
        return getFileParts().getEntryPath();
    }

    /**
     * Connects to the URL setting the header fields and preparing for the
     * {@link #getProperty()} and {@link #getInputStream()} methods.
     * <p>
     * After calling the base class implemenation to get the basic connection,
     * the entry is looked for in the archive to set the content type, content
     * length and last modification time header fields according to the named
     * entry. If no entry is defined on the URL, only the content type header
     * field is set to <code>application/java-archive</code>.
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

        if (!connected) {

            // have the base class connect to get the jar property
            super.connect();

            // we assume the connection is now (temporarily) connected,
            // thus calling the getters will not result in a recursive loop
            Property property = getProperty();
            String contentType = getContentType();
            String contentEncoding = getContentEncoding();
            int contentLength = getContentLength();
            long lastModified = getLastModified();

            // mark as not connected to not get false positives if the
            // following code fails
            connected = false;

            // Get hold of the data
            try {

                JarInputStream jins = null;
                try {

                    // try to get the jar input stream, fail if no jar
                    jins = new JarInputStream(property.getStream());

                    String entryPath = getEntryPath();
                    if (entryPath != null) {

                        JarEntry entry = findEntry(jins, entryPath);

                        if (entry != null) {

                            contentType = guessContentTypeFromName(entryPath);
                            if (contentType == null) {
                                contentType = APPLICATION_OCTET;
                            }

                            contentLength = (int) entry.getSize();
                            lastModified = entry.getTime();

                        } else {

                            throw failure("connect", entryPath +
                                " not contained in jar archive", null);

                        }

                    } else {

                        // replaces the base class defined content type
                        contentType = APPLICATION_JAR;

                    }

                } finally {
                    if (jins != null) {
                        try {
                            jins.close();
                        } catch (IOException ignore) {
                        }
                    }
                }

                log.debug("connect: Using atom '" + property.getPath()
                    + "' with content type '" + contentType + "' for "
                    + String.valueOf(contentLength) + " bytes");

                // set the fields
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
     * Returns an input stream that reads from this open connection. If not
     * entry path is specified in the URL, this method returns the input stream
     * providing access to the archive as a whole. Otherwise the input stream
     * returned is a <code>JarInputStream</code> positioned at the start of
     * the named entry.
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
     * @return The <code>InputStream</code> on the archive or the entry if
     *      specified.
     *
     * @throws IOException if an error occurrs opening the connection through
     *      {@link #connect()} or creating the <code>InputStream</code> on the
     *      repository <code>Property</code>.
     */
    public InputStream getInputStream() throws IOException {

        // get the input stream on the archive itself - also enforces connect()
        InputStream ins = super.getInputStream();

        // access the entry in the archive if defined
        String entryPath = getEntryPath();
        if (entryPath != null) {
            // open the jar input stream
            JarInputStream jins = new JarInputStream(ins);

            // position at the correct entry
            findEntry(jins, entryPath);

            // return the input stream
            return jins;
        }

        // otherwise just return the stream on the archive
        return ins;
    }

    //----------- internal helper to find the entry ------------------------

    /**
     * Returns the <code>JarEntry</code> for the path from the
     * <code>JarInputStream</code> or <code>null</code> if the path cannot
     * be found in the archive.
     *
     * @param zins The <code>JarInputStream</code> to search in.
     * @param path The path of the <code>JarEntry</code> to return.
     *
     * @return The <code>JarEntry</code> for the path or <code>null</code>
     *      if no such entry can be found.
     *
     * @throws IOException if a problem occurrs reading from the stream.
     */
    static JarEntry findEntry(JarInputStream zins, String path)
        throws IOException {

        JarEntry entry = zins.getNextJarEntry();
        while (entry != null) {
            if (path.equals(entry.getName())) {
                return entry;
            }

            entry = zins.getNextJarEntry();
        }
        // invariant : nothing found in the zip matching the path

        return null;
    }
}
