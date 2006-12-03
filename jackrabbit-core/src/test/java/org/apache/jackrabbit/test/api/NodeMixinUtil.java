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
package org.apache.jackrabbit.test.api;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;

/**
 * Utility class to locate mixins in the NodeTyeManager.
 */
public class NodeMixinUtil {

    /**
     * @return the name of a mixin node type that can be added by the requested
     *         <code>node</code>
     */
    public static String getAddableMixinName(Session session, Node node)
            throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator mixins = manager.getMixinNodeTypes();

        while (mixins.hasNext()) {
            String name = mixins.nextNodeType().getName();
            if (node.canAddMixin(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * @return a string that is not the name of a mixin type
     */
    public static String getNonExistingMixinName(Session session)
            throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator mixins = manager.getMixinNodeTypes();
        StringBuffer s = new StringBuffer("X");
        while (mixins.hasNext()) {
            s.append(mixins.nextNodeType().getName());
        }
        return s.toString().replaceAll(":", "");
    }


}
