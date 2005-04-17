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
package org.apache.jackrabbit.webdav.simple;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.ExportCollectionChain;
import org.apache.jackrabbit.server.io.ExportResourceChain;
import org.apache.commons.chain.Command;

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

    /**
     * the default logger
     */
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
        ExportContext ctx = new ExportContext(node);
        Command exportChain = davResource.isCollection()
                ? ExportCollectionChain.getChain()
                : ExportResourceChain.getChain();
        try {
            exportChain.execute(ctx);
        } catch (Exception e) {
            log.error("Error while executing export chain: " + e.toString());
            throw new RepositoryException(e);
        }
        this.contentLength = ctx.getContentLength();
        this.contentType = ctx.getContentType();
        this.in = ctx.getInputStream();
        this.creationTime = ctx.getCreationTime();
        this.modificationTime = ctx.getModificationTime();
    }

    /**
     * Return the content length or '0'.
     *
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
