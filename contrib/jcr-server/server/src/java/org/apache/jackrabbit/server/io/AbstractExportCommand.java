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

/**
 * This Class implements an abstract export command for a nc-resource. It acts
 * as generic base class that retrieves the resource properties from the
 * exporting node.
 * </p>
 * the following properties are retrieved from the node and set to the context
 * prior of calling {@link #exportNode(ExportContext, javax.jcr.Node)}:
 * <ul>
 * <li>jcr:lastModified
 * <li>jcr:created
 * <li>jcr:mimeType
 * <ul>
 */
public abstract class AbstractExportCommand extends AbstractCommand {

    /**
     * Executes this command by calling {@link #execute(ExportContext)} if
     * the given context is of the correct class.
     *
     * @param context the (export) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(AbstractContext context) throws Exception {
        if (context instanceof ExportContext) {
            return execute((ExportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executs this command using the given export context. It copies the common
     * resource properties to the export context and finally calls
     * {@link #exportNode(ExportContext, javax.jcr.Node)}.
     *
     * @param context the export context
     * @return the return value of {@link #exportNode(ExportContext, javax.jcr.Node)}
     *         or false, if this command does not handle the given node at all.
     * @throws Exception if an error occurrs
     */
    public boolean execute(ExportContext context) throws Exception {
        Node node = context.getNode();
        if (!canHandle(node)) {
            return false;
        }

        Node content = node.isNodeType(NT_FILE)
                ? node.getNode(JCR_CONTENT)
                : node;
        if (content.hasProperty(JCR_LASTMODIFIED)) {
            context.setModificationTime(content.getProperty(JCR_LASTMODIFIED).getLong());
        } else {
            context.setModificationTime(System.currentTimeMillis());
        }
        if (node.hasProperty(JCR_CREATED)) {
            context.setCreationTime(node.getProperty(JCR_CREATED).getValue().getLong());
        }
        if (content.hasProperty(JCR_MIMETYPE)) {
            context.setContentType(content.getProperty(JCR_MIMETYPE).getString());
        } else {
            context.setContentType(getDefaultContentType());
        }
        return exportNode(context, content);
    }

    /**
     * Creates the response content. a successfull export must set the
     * input stream and contentlength of the export context.
     *
     * @param context
     * @param content
     * @return
     * @throws Exception
     */
    public abstract boolean exportNode(ExportContext context, Node content) throws Exception ;

    /**
     * Returns the default content type of this export
     *
     * @return default content type
     */
    public abstract String getDefaultContentType();

    /**
     * Checks if this export command can handle the given node.
     * 
     * @param node
     * @return <code>true</code> if it can handle the export;
     *         <code>false</code> otherwise.
     */
    public abstract boolean canHandle(Node node);
}
