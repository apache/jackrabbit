/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.server.io;

import javax.jcr.Node;
import java.io.InputStream;
import java.util.Calendar;

/**
 * This Class implements an import command that creates a "nt:resource" node or
 * of any other configured nodetype below the current node and adds the resource
 * data as binary property. It further sets the following properties:
 * <ul>
 * <li>jcr:mimeType (from {@link ImportContext#getContentType()})
 * <li>jcr:encoding (from {@link ImportContext#getContentType()})
 * <li>jcr:lastModified (from current time)
 * <li>jcr:data (from {@link ImportContext#getInputStream()})
 * </ul>
 */
public class FileImportCommand extends AbstractImportCommand {

    /**
     * The name of the nodetype for the resource node. Default: nt:resource
     */
    private String resourceNodeType = NT_RESOURCE;

    /**
     * Imports a resource by creating a new nt:resource node.
     *
     * @param ctx the import context
     * @param parentNode the parent node
     * @param in the input stream
     * @return <code>true</code>
     * @throws Exception in an error occurrs
     */
    public boolean importResource(ImportContext ctx, Node parentNode,
                                  InputStream in)
            throws Exception {
        Node content = parentNode.hasNode(JCR_CONTENT)
                ? parentNode.getNode(JCR_CONTENT)
                : parentNode.addNode(JCR_CONTENT, resourceNodeType);
        String contentType = ctx.getContentType();
        content.setProperty(JCR_MIMETYPE, getMimeType(contentType));
        content.setProperty(JCR_ENCODING, getEncoding(contentType));
        content.setProperty(JCR_DATA, in);
        Calendar lastMod = Calendar.getInstance();
        if (ctx.getModificationTime() != 0) {
            lastMod.setTimeInMillis(ctx.getModificationTime());
        }
        content.setProperty(JCR_LASTMODIFIED, lastMod);
        return true;
    }

    /**
     * Sets the node type for the resource node.
     * 
     * @param nodeType nodetype name.
     */
    public void setResourceNodeType(String nodeType) {
        resourceNodeType = nodeType;
    }

    /**
     * Can handle all content type thus returning <code>true</code>.
     * 
     * @param contentType
     * @return <code>true</code>
     */
    public boolean canHandle(String contentType) {
        return true;
    }

    /**
     * Returns the main media type from a MIME Content-Type
     * specification.
     */
    private String getMimeType(String contentType) {
        if (contentType == null) {
            // property will be removed.
            // Note however, that jcr:mimetype is a mandatory property with the
            // built-in nt:file nodetype.
            return contentType;
        }
        // strip any parameters
        int semi = contentType.indexOf(";");
        return (semi > 0) ? contentType.substring(0, semi) : contentType;
    }

    /**
     * Returns the charset parameter of a MIME Content-Type specification, or
     * <code>null</code> if the specified String is <null> or if the charset
     * is not included.
     */
    private String getEncoding(String contentType) {
        // find the charset parameter
        int equal;
        if (contentType == null || (equal = contentType.indexOf("charset=")) == -1) {
            // jcr:encoding property will be removed
            return null;
        }
        String charset = contentType.substring(equal + 8);
        // get rid of any other parameters that might be specified after the charset
        int semi = charset.indexOf(";");
        if (semi != -1) {
            charset = charset.substring(0, semi);
        }
        // strip off enclosing quotes
        if (charset.startsWith("\"") || charset.startsWith("'")) {
            charset = charset.substring(1, charset.length() - 1);
        }
        return charset;
    }
}
