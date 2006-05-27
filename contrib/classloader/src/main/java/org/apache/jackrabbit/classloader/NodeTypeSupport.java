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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;

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
    private static final Log log = LogFactory.getLog(NodeTypeSupport.class);

    /**
     * The name of the class path resource containing the node type definition
     * file used by the {@link #registerNodeType(Workspace)} method to register
     * the required mixin node type (value is "type.cnd").
     */
    private static final String TYPE_FILE = "type.cnd";

    /**
     * The encoding used to read the node type definition file (value is
     * "ISO-8859-1").
     */
    private static final String ENCODING = "ISO-8859-1";

    /**
     * Registers the required node type (<code>rep:jarFile</code>) with the
     * node type manager available from the given <code>workspace</code>.
     * <p>
     * The <code>NodeTypeManager</code> returned by the <code>workspace</code>
     * is expected to be of type
     * <code>org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl</code> for
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

        // Wrap the stream with a reader
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(ins, ENCODING);
        } catch (UnsupportedEncodingException uee) {
            log.warn("Required Encoding " + ENCODING + " not supported, " +
                    "using platform default encoding", uee);

            reader = new InputStreamReader(ins);
        }

        try {
            // Create a CompactNodeTypeDefReader
            CompactNodeTypeDefReader cndReader =
                new CompactNodeTypeDefReader(reader, TYPE_FILE);

            // Get the List of NodeTypeDef objects
            List ntdList = cndReader.getNodeTypeDefs();

            // Get the NodeTypeManager from the Workspace.
            // Note that it must be cast from the generic JCR NodeTypeManager
            // to the Jackrabbit-specific implementation.
            NodeTypeManagerImpl ntmgr =
                (NodeTypeManagerImpl) workspace.getNodeTypeManager();

            // Acquire the NodeTypeRegistry
            NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

            // register the node types from the file in a batch
            ntreg.registerNodeTypes(ntdList);

            // get here and succeed
            return true;

        } catch (ParseException pe) {
            log.error("Unexpected failure to parse compact node defintion " + TYPE_FILE, pe);

        } catch (InvalidNodeTypeDefException ie) {
            log.error("Cannot define required node type", ie);

        } catch (RepositoryException re) {
            log.error("General problem accessing the repository", re);

        } catch (ClassCastException cce) {
            log.error("Unexpected object type encountered", cce);

        } finally {
            // make sure the reader is closed - expect to be non-null here !
            try {
                reader.close();
            } catch (IOException ioe) {
                // ignore
            }
        }

        // fall back to failure
        return false;
    }
}
