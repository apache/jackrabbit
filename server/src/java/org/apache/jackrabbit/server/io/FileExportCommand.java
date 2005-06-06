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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * This Class implements a export command that returns the data included in the
 * jcr:data property of a nt:resource node.
 */
public class FileExportCommand extends AbstractExportCommand {

    /**
     * Exports the node by returning the content of the jcr:data property of
     * the content node.
     *
     * @param context the export context
     * @param content the content node
     * @return <code>true</code>
     * @throws Exception if an error occurrs
     */
    public boolean exportNode(ExportContext context, Node content) throws Exception {
        if (content.hasProperty(JCR_ENCODING)) {
            String encoding = content.getProperty(JCR_ENCODING).getString();
            // ignore "" encodings (although this is avoided during import)
            if (!"".equals(encoding)) {
                context.setContentType(context.getContentType() + "; charset=\"" + encoding + "\"");
            }
        }
        if (content.hasProperty(JCR_DATA)) {
            Property p = content.getProperty(JCR_DATA);
            context.setContentLength(p.getLength());
            context.setInputStream(p.getStream());
        } else {
            context.setContentLength(0);
            context.setInputStream(null);
        }
        return true;
    }

    /**
     * Returns the default content type
     *
     * @return "application/octet-stream".
     */
    public String getDefaultContentType() {
        return "application/octet-stream";
    }

    /**
     * Checks if the given node can be handled by this export command. This is
     * the case, if the node contains a 'jcr:content' node which is of node type
     * 'nt:resource'.
     * 
     * @param node the node to be exported
     * @return <code>true</code> if the correct node is passed;
     *         <code>false</code> otherwise.
     */
    public boolean canHandle(Node node) {
        try {
            return node.isNodeType(NT_RESOURCE) || node.hasNode(JCR_CONTENT) && node.getNode(JCR_CONTENT).isNodeType(NT_RESOURCE);
        } catch (RepositoryException e) {
            return false;
        }
    }

}
