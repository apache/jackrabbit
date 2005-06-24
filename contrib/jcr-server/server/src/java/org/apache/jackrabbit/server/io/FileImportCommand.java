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
        content.setProperty(JCR_MIMETYPE, ctx.getContentType());
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
}
