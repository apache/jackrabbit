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

/**
 * This Class implements...
 *
 * @author tripod
 * @version $Revision:$, $Date:$
 */
public class SetContentTypeCommand implements Command {

    /**
     * Executes this command by calling {@link #execute(ImportContext)} if
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
     * Executes this command. It resolves the content type in the import context
     * if it is not alerady set.
     *
     * @param context the import context
     * @return false
     * @throws Exception if an error occurrs
     */
    public boolean execute(ImportContext context) throws Exception {
        if (context.getContentType() == null) {
            String name = context.getSystemId();
            // todo: add extensible list
            if (name.endsWith(".xml")) {
                context.setContentType("text/xml");
            } else if (name.endsWith(".zip")) {
                context.setContentType("application/zip");
            } else {
                context.setContentType("application/octet-stream");
            }
        }
        return false;
    }
}
