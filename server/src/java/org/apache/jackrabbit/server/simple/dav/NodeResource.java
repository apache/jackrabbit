/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.server.simple.dav;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.util.Text;

import javax.jcr.*;
import java.util.Date;
import java.util.Locale;
import java.io.*;
import java.text.SimpleDateFormat;

/**
 * The <code>NodeResource</code> class wraps a jcr item in order to respond
 * to 'GET', 'HEAD', 'PROPFIND' or 'PROPPATCH' requests. If the item is a
 * {@link javax.jcr.Node} its primary property is determined. The value of the
 * primary property can be accessed by {@link #getStream()}. If possible other
 * required information (last modification date, content type...) is retrieved
 * from the property siblings.<br>
 * If the requested item is a {@link javax.jcr.Property} it is treated accordingly.
 */
public class NodeResource {

    /** the default logger */
    private static final Logger log = Logger.getLogger(NodeResource.class);

    /**
     * modificationDate date format per RFC 1123
     */
    public static SimpleDateFormat modificationDateFormat =
	new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    /**
     * Simple date format for the creation date ISO representation (partial).
     */
    public static SimpleDateFormat creationDateFormat =
	new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final String PROP_MIMETYPE = "jcr:mimeType";
    private static final String PROP_ENCODING = "jcr:encoding";
    private static final String PROP_LASTMODIFIED = "jcr:lastModified";
    private static final String PROP_CREATED = "jcr:created";

    private long creationTime = 0;
    private long modificationTime = new Date().getTime();
    private long contentLength = 0;
    private String contentType = null;
    private InputStream in = null;

    /**
     * Create a new <code>NodeResource</code> that wraps a JSR170 item.
     *
     * @throws ItemNotFoundException
     * @throws RepositoryException
     * @throws IllegalArgumentException if the given item is <code>null</code>
     */
    public NodeResource(DavResourceImpl davResource, Node node) throws ItemNotFoundException, RepositoryException {
	try {
	    if (davResource.isCollection()) {
		createDirListingContent(node);
	    } else {
		if (node.hasProperty(PROP_CREATED)) {
		    creationTime = node.getProperty(PROP_CREATED).getValue().getLong();
		}
		Node content = node.getPrimaryNodeType().getName().equals("nt:file")
			? node.getNode("jcr:content")
			: node;
		if (content.getPrimaryNodeType().getName().equals("nt:resource")) {
		    createPlainFileContent(content);
		} else {
		    createDocViewContent(content);
		}
	    }
	} catch (IOException e) {
	    // ignore
	}
    }

    private void createPlainFileContent(Node content) throws IOException, RepositoryException {
	if (content.hasProperty(PROP_LASTMODIFIED)) {
	    modificationTime = content.getProperty(PROP_LASTMODIFIED).getLong();
	}
	if (content.hasProperty(PROP_MIMETYPE)) {
	    contentType = content.getProperty(PROP_MIMETYPE).getString();
	}
	if (content.hasProperty(PROP_ENCODING)) {
	    String encoding = content.getProperty(PROP_ENCODING).getString();
	    if (!encoding.equals("")) {
		contentType+="; charset=\"" + encoding + "\"";
	    }
	}
	if (content.hasProperty("jcr:data")) {
	    Property p = content.getProperty("jcr:data");
	    contentLength = p.getLength();
	    in = p.getStream();
	} else {
	    contentLength = 0;
	}
    }

    private void createDocViewContent(Node node) throws IOException, RepositoryException {
	File tmpfile = File.createTempFile("__webdav", ".xml");
	FileOutputStream out = new FileOutputStream(tmpfile);
	node.getSession().exportDocView(node.getPath(), out, true, false);
	out.close();
	in = new FileInputStream(tmpfile);
	contentLength = tmpfile.length();
	modificationTime = tmpfile.lastModified();
	contentType = "text/xml";
	tmpfile.deleteOnExit();
    }

    private void createSysViewContent(Node node) throws IOException, RepositoryException {
	File tmpfile = File.createTempFile("__webdav", ".xml");
	FileOutputStream out = new FileOutputStream(tmpfile);
	node.getSession().exportSysView(node.getPath(), out, true, false);
	out.close();
	in = new FileInputStream(tmpfile);
	contentLength = tmpfile.length();
	modificationTime = tmpfile.lastModified();
	contentType = "text/xml";
	tmpfile.deleteOnExit();
    }

    private void createDirListingContent(Node node) throws IOException, RepositoryException {
	File tmpfile = File.createTempFile("__webdav", ".xml");
	FileOutputStream out = new FileOutputStream(tmpfile);

	String repName = node.getSession().getRepository().getDescriptor(Repository.REP_NAME_DESC);
	String repURL = node.getSession().getRepository().getDescriptor(Repository.REP_VENDOR_URL_DESC);
	String repVersion = node.getSession().getRepository().getDescriptor(Repository.REP_VERSION_DESC);
	PrintWriter writer = new PrintWriter(out);
	writer.print("<html><head><title>");
	writer.print(repName);
	writer.print(" ");
	writer.print(repVersion);
	writer.print(" ");
	writer.print(node.getPath());
	writer.print("</title></head>");
	writer.print("<body><h2>");
	writer.print(node.getPath());
	writer.print("</h2><ul>");
	writer.print("<li><a href=\"..\">..</a></li>");
	NodeIterator iter = node.getNodes();
	while (iter.hasNext()) {
	    Node child = iter.nextNode();
	    String label = Text.getLabel(child.getPath());
	    writer.print("<li><a href=\"");
	    writer.print(Text.escape(label));
	    if (!child.getPrimaryNodeType().getName().equals("nt:file")) {
		writer.print("/");
	    }
	    writer.print("\">");
	    writer.print(label);
	    writer.print("</a></li>");
	}
	writer.print("</ul><hr size=\"1\"><em>Powered by <a href=\"");
	writer.print(repURL);
	writer.print("\">");
	writer.print(repName);
	writer.print("</a> version ");
	writer.print(repVersion);
	writer.print("</em></body></html>");

	writer.close();
	out.close();
	in = new FileInputStream(tmpfile);
	contentLength = tmpfile.length();
	modificationTime = tmpfile.lastModified();
	contentType = "text/html";
	tmpfile.deleteOnExit();
    }

    /**
     * Return the content length or '0'.
     * @return content Length or '0' if it could not be determined.
     */
    public long getContentLength() {
	return contentLength;
    }

    /**
     * Return the creation time or '0'.
     *
     * @return creation time or '0' if it could not be determined.
     */
    public long getCreationTime() {
	return creationTime;
    }

    /**
     * Return the last modification time. By default it is set to the current
     * time.
     *
     * @return time of last modification or the current time, if it could not
     * be determined.
     */
    public long getModificationTime() {
	return modificationTime;
    }

    /**
     * Return the last modification time as formatted string.
     *
     * @return last modification time as string.
     * @see NodeResource#modificationDateFormat
     */
    public String getLastModified() {
	if (modificationTime >= 0) {
	    return modificationDateFormat.format(new Date(modificationTime));
	} else {
	    return null;
	}
    }

    /**
     * Return the creation time as formatted string.
     *
     * @return creation time as string.
     * @see NodeResource#creationDateFormat
     */
    public String getCreationDate() {
	if (creationTime >= 0) {
	    return creationDateFormat.format(new Date(creationTime));
	} else {
	    return null;
	}
    }

    /**
     * Return the weak ETag
     *
     * @return weak ETag
     */
    public String getETag() {
	return "W/\"" + this.contentLength + "-" + this.modificationTime + "\"";
    }

    /**
     * Return the strong ETag or empty string if it cannot be determined.
     *
     * @return strong ETag
     */
    public String getStrongETag() {
	return "";
    }

    /**
     * Return the content type or <code>null</code> if it could not be determined.
     *
     * @return content type
     */
    public String getContentType() {
	return contentType;
    }

    /**
     * Return a stream to the resource value.
     *
     * @return
     */
    public InputStream getStream() {
	return in;
    }
}
