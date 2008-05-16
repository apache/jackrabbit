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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.NamespaceException;

/**
 * This class implements the <code>NodeDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link NodeDef},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(NodeDefinitionImpl.class);

    /**
     * Package private constructor.
     *
     * @param nodeDef    child node definition
     * @param ntMgr      node type manager
     * @param resolver   name resolver
     */
    NodeDefinitionImpl(NodeDef nodeDef, NodeTypeManagerImpl ntMgr,
                       NamePathResolver resolver) {
        super(nodeDef, ntMgr, resolver);
    }

    /**
     * Returns the wrapped node definition.
     *
     * @return the wrapped node definition.
     */
    public NodeDef unwrap() {
        return (NodeDef) itemDef;
    }

    //-------------------------------------------------------< NodeDefinition >
    /**
     * {@inheritDoc}
     */
    public NodeType getDefaultPrimaryType() {
        Name ntName = ((NodeDef) itemDef).getDefaultPrimaryType();
        if (ntName == null) {
            return null;
        }
        if (ntMgr == null) {
            return null;
        }
        try {
            return ntMgr.getNodeType(ntName);
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("invalid default node type " + ntName, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeType[] getRequiredPrimaryTypes() {
        if (ntMgr == null) {
            return null;
        }
        Name[] ntNames = ((NodeDef) itemDef).getRequiredPrimaryTypes();
        try {
            if (ntNames == null || ntNames.length == 0) {
                // return "nt:base"
                return new NodeType[] {ntMgr.getNodeType(NameConstants.NT_BASE)};
            } else {
                NodeType[] nodeTypes = new NodeType[ntNames.length];
                for (int i = 0; i < ntNames.length; i++) {
                    nodeTypes[i] = ntMgr.getNodeType(ntNames[i]);
                }
                return nodeTypes;
            }
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("required node type does not exist", e);
            return new NodeType[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return ((NodeDef) itemDef).allowsSameNameSiblings();
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * Returns the names of the required primary node types.
     * <p/>
     * If this <code>NodeDefinition</code> is acquired from a live
     * <code>NodeType</code> this list will reflect the node types returned by
     * <code>getRequiredPrimaryTypes</code>, above.
     * <p/>
     * If this <code>NodeDefinition</code> is actually a
     * <code>NodeDefinitionTemplate</code> that is not part of a registered node
     * type, then this method will return the required primary types as set in
     * that template. If that template is a newly-created empty one, then this
     * method will return an array containing a single string indicating the
     * node type <code>nt:base</code>.
     *
     * @return a String array
     * @since JCR 2.0
     */
    public String[] getRequiredPrimaryTypeNames() {
        Name[] ntNames = ((NodeDef) itemDef).getRequiredPrimaryTypes();
        try {
            if (ntNames == null || ntNames.length == 0) {
                // return "nt:base"
                return new String[] {resolver.getJCRName(NameConstants.NT_BASE)};
            } else {
                String[] names = new String[ntNames.length];
                for (int i = 0; i < ntNames.length; i++) {
                    names[i] = resolver.getJCRName(ntNames[i]);
                }
                return names;
            }
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", e);
            return new String[0];
        }
    }

    /**
     * Returns the name of the default primary node type.
     * <p/>
     * If this <code>NodeDefinition</code> is acquired from a live
     * <code>NodeType</code> this list will reflect the NodeType returned by
     * getDefaultPrimaryType, above.
     * <p/>
     * If this <code>NodeDefinition</code> is actually a
     * <code>NodeDefinitionTemplate</code> that is not part of a registered node
     * type, then this method will return the required primary types as set in
     * that template. If that template is a newly-created empty one, then this
     * method will return <code>null</code>.
     *
     * @return a String
     * @since JCR 2.0
     */
    public String getDefaultPrimaryTypeName() {
        Name ntName = ((NodeDef) itemDef).getDefaultPrimaryType();
        if (ntName == null) {
            return null;
        }

        try {
            return resolver.getJCRName(ntName);
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", e);
            // not correct, but an acceptable fallback
            return ntName.toString();
        }
    }
}

