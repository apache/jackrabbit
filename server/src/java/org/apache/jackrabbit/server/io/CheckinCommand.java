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
 * This Class implements a import command that saves the current node.
 */
public class CheckinCommand extends AbstractCommand {

    /**
     * Executes this command by delegating to {@link #execute(ImportContext)} if
     * the context has the correct class.
     *
     * @param context the (import) context.
     * @return <code>false</code>.
     * @throws Exception if an error occurrs.
     */
    public boolean execute(AbstractContext context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Adds the mixin nodetype to the current import node.
     * 
     * @param context the import context.
     * @return <code>false</code>
     * @throws Exception if an error occurrs.
     */
    public boolean execute(ImportContext context) throws Exception {
        Node node = context.getNode();
        if (node.isNodeType(MIX_VERSIONABLE)) {
            node.checkin();
        }
        return false;
    }
}
