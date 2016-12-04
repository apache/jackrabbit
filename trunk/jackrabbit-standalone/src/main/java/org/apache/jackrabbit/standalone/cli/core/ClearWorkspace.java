/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.standalone.cli.core;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Remove all the content from the current working <code>Workspace</code>
 */
public class ClearWorkspace implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(ClearWorkspace.class);

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        Session s = CommandHelper.getSession(ctx);

        if (log.isDebugEnabled()) {
            log.debug("removing all content from workspace "
                    + s.getWorkspace().getName());
        }

        // Set current node to root
        CommandHelper.setCurrentNode(ctx, s.getRootNode());
        NodeIterator iter = s.getRootNode().getNodes();
        while (iter.hasNext()) {
            Node n = (Node) iter.next();
            if (!n.getName().equals(JcrConstants.JCR_SYSTEM)) {
                n.remove();
            }
        }
        PropertyIterator pIter = s.getRootNode().getProperties();
        while (pIter.hasNext()) {
            Property p = pIter.nextProperty();
            if (!p.getName().equals(JcrConstants.JCR_PRIMARYTYPE)) {
                p.remove();
            }
        }
        return false;
    }
}
