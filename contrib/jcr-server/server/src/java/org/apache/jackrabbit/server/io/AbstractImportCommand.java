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

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;
import java.io.InputStream;

/**
 * This Class implements an abstract import command for a nc-resource.
 */
public abstract class AbstractImportCommand implements Command, JcrConstants {

    /**
     * Executes this command by calling {@link #importResource} if
     * the given context is of the correct class.
     *
     * @param context the (import) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(Context context) throws Exception {
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
        if (importResource(context, parentNode, in)) {
            context.setInputStream(null);
        }
        return false;
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
