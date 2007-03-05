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
package org.apache.jackrabbit.classloader;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>NodeTypeSupport</code> contains a single utility method
 * {@link #registerNodeType(Workspace)} to register the required mixin node
 * type <code>rep:jarFile</code> with the repository.
 * <p>
 * If the class loader is not used on a Jackrabbit based repository, loading
 * this class or calling the {@link #registerNodeType(Workspace)} methods may
 * fail with link errors.
 *
 * @author Felix Meschberger
 */
/* package */ class NodeTypeSupport {

    /** Default log */
    private static final Logger log =
        LoggerFactory.getLogger(NodeTypeSupport.class);

    /**
     * The name of the class path resource containing the node type definition
     * file used by the {@link #registerNodeType(Workspace)} method to register
     * the required mixin node type (value is "type.cnd").
     */
    private static final String TYPE_FILE = "type.cnd";

    /**
     * Registers the required node type (<code>rep:jarFile</code>) with the
     * node type manager available from the given <code>workspace</code>.
     * <p>
     * The <code>NodeTypeManager</code> returned by the <code>workspace</code>
     * is expected to be of type
     * <code>org.apache.jackrabbit.api.JackrabbitNodeTypeManager</code> for
     * the node type registration to succeed.
     * <p>
     * This method is not synchronized. It is up to the calling method to
     * prevent paralell execution.
     *
     * @param workspace The <code>Workspace</code> providing the node type
     *      manager through which the node type is to be registered.
     *
     * @return <code>true</code> if this class can be used to handle archive
     *      class path entries. See above for a description of the test used.
     */
    /* package */ static boolean registerNodeType(Workspace workspace) {

        // Access the node type definition file, "fail" if not available
        InputStream ins = NodeTypeSupport.class.getResourceAsStream(TYPE_FILE);
        if (ins == null) {
            log.error("Node type definition file " + TYPE_FILE +
                " not in class path. Cannot define required node type");
            return false;
        }

        try {
            NodeTypeManager ntm = workspace.getNodeTypeManager();
            if (ntm instanceof JackrabbitNodeTypeManager) {
                log.debug("Using Jackrabbit to import node types from "
                    + TYPE_FILE);
                JackrabbitNodeTypeManager jntm = (JackrabbitNodeTypeManager) ntm;
                jntm.registerNodeTypes(ins,
                    JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
                return true;
            }
        } catch (IOException ioe) {
            log.error("Cannot register node types from " + TYPE_FILE, ioe);
        } catch (RepositoryException re) {
            log.error("Cannot register node types from " + TYPE_FILE, re);
        }

        // fall back to failure
        log.warn("Repository is not a Jackrabbit, cannot import node types");
        return false;
    }
}
