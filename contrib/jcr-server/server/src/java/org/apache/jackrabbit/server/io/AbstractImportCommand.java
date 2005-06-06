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

import org.apache.jackrabbit.util.Text;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStream;

/**
 * This Class implements an abstract import command for a nc-resource.
 */
public abstract class AbstractImportCommand extends AbstractCommand {

    /**
     * the nodetype for the node
     */
    private String nodeType = "nt:file";

    /**
     * Executes this command by calling {@link #importResource} if
     * the given context is of the correct class.
     *
     * @param context the (import) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(AbstractContext context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executes this command. It checks if this command can handle the content
     * type and delegates it to {@link #importResource}. If the import is
     * successfull, the input stream of the importcontext is cleared.
     *
     * @param context the import context
     * @return false
     * @throws Exception if an error occurrs
     */
    public boolean execute(ImportContext context) throws Exception {
        Node parentNode = context.getNode();
        InputStream in = context.getInputStream();
        if (in == null) {
            // assume already consumed
            return false;
        }
        if (!canHandle(context.getContentType())) {
            // ignore imports
            return false;
        }
        // check 'file' node
        Node fileNode = parentNode.hasNode(context.getSystemId())
            ? parentNode.getNode(context.getSystemId())
            : parentNode.addNode(context.getSystemId(), nodeType);

        if (importResource(context, fileNode, in)) {
            context.setInputStream(null);
        }
        // set current node
        context.setNode(fileNode);
        return false;
    }

    /**
     * Creates collection recursively.
     *
     * @param root
     * @param relPath
     * @return
     * @throws RepositoryException
     */
    static public Node mkDirs(ImportContext context, Node root, String relPath)
            throws RepositoryException {
        String[] seg = Text.explode(relPath, '/');
        for (int i=0; i< seg.length; i++) {
            if (!root.hasNode(seg[i])) {
                // not quite correct
                ImportContext subctx = context.createSubContext(root);
                subctx.setSystemId(seg[i]);
                try {
                    ImportCollectionChain.getChain().execute(subctx);
                } catch (Exception e) {
                    throw new RepositoryException(e);
                }
            }
            root = root.getNode(seg[i]);
        }
        return root;
    }

    /**
     * Sets the node type
     * @param nodeType
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Imports the resource contained in the import context.
     *
     * @param ctx
     * @param parentNode
     * @param in
     * @return
     * @throws Exception
     */
    public abstract boolean importResource(ImportContext ctx, Node parentNode, InputStream in)
            throws Exception;

    /**
     * Returns true, if this command handles the given content type.
     * 
     * @param contentType
     * @return <code>true</code> if this command handles the given content type;
     *         <code>false</code> otherwise.
     */
    public abstract boolean canHandle(String contentType);
}
