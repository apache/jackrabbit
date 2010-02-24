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
package org.apache.jackrabbit.spi.commons.nodetype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.commons.iterator.NodeTypeIteratorAdapter;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;

import java.util.ArrayList;

/**
 * <code>AbstractNodeType</code>...
 */
/**
 * <code>AbstractNodeType</code>...
 */
public abstract class AbstractNodeType implements NodeType {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractNodeType.class);

    protected final AbstractNodeTypeManager ntMgr;

    protected final QNodeTypeDefinition ntd;

    protected final NamePathResolver resolver;

    /**
     * Create a new <code>AbstractNodeType</code>.
     *
     * @param ntd      the underlying node type definition.
     * @param ntMgr    the node type manager.
     * @param resolver the name/path resolver of the session that created this
     *                 node type instance.
     */
    public AbstractNodeType(QNodeTypeDefinition ntd,
                            AbstractNodeTypeManager ntMgr,
                            NamePathResolver resolver) {
        this.ntd = ntd;
        this.ntMgr = ntMgr;
        this.resolver = resolver;
    }

    /**
     * Returns the node type definition.
     *
     * @return the internal node type definition.
     */
    public QNodeTypeDefinition getDefinition() {
        return ntd;
    }

    //-----------------------------------------------------------< NodeType >---

    /**
     * {@inheritDoc}
     */
    public String getName() {
        try {
            return resolver.getJCRName(ntd.getName());
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", e);
            return ntd.getName().toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAbstract() {
        return ntd.isAbstract();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMixin() {
        return ntd.isMixin();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryable() {
        return ntd.isQueryable();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDeclaredSupertypeNames() {
        Name[] ntNames = ntd.getSupertypes();
        String[] supertypes = new String[ntNames.length];
        for (int i = 0; i < ntNames.length; i++) {
            try {
                supertypes[i] = resolver.getJCRName(ntNames[i]);
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in node type name", e);
                supertypes[i] = ntNames[i].toString();
            }
        }
        return supertypes;
    }

    /**
     * {@inheritDoc}
     */
    public NodeType[] getDeclaredSupertypes() {
        Name[] ntNames = ntd.getSupertypes();
        NodeType[] supertypes = new NodeType[ntNames.length];
        for (int i = 0; i < ntNames.length; i++) {
            try {
                supertypes[i] = ntMgr.getNodeType(ntNames[i]);
            } catch (NoSuchNodeTypeException e) {
                // should never get here
                log.error("undefined supertype", e);
                return new NodeType[0];
            }
        }
        return supertypes;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#getDeclaredSubtypes()
     */
    public NodeTypeIterator getDeclaredSubtypes() {
        return getSubtypes(true);
    }

    /**
     * @see javax.jcr.nodetype.NodeType#getSubtypes()
     */
    public NodeTypeIterator getSubtypes() {
        return getSubtypes(false);
    }        

    /**
     * {@inheritDoc}
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
        }
        return nodeDefs;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrimaryItemName() {
        // TODO JCR-1947: JSR 283: Node Type Attribute Subtyping Rules
        try {
            Name piName = ntd.getPrimaryItemName();
            if (piName != null) {
                return resolver.getJCRName(piName);
            } else {
                return null;
            }
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in name of primary item", e);
            return ntd.getName().toString();
        }
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
        for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
        }
        return propDefs;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeType(String nodeTypeName) {
        Name ntName;
        try {
            ntName = resolver.getQName(nodeTypeName);
        } catch (NamespaceException e) {
            log.warn("invalid node type name: " + nodeTypeName, e);
            return false;
        } catch (NameException e) {
            log.warn("invalid node type name: " + nodeTypeName, e);
            return false;
        }
        return isNodeType(ntName);
    }

    /**
     * Test if this nodetype equals or is directly or indirectly derived from
     * the node type with the specified <code>nodeTypeName</code>, without
     * checking of a node type of that name really exists.
     *
     * @param nodeTypeName A node type name.
     * @return true if this node type represents the type with the given
     * <code>nodeTypeName</code> or if it is directly or indirectly derived
     * from it; otherwise <code>false</code>. If no node type exists with the
     * specified name this method will also return <code>false</code>.
     */
    public abstract boolean isNodeType(Name nodeTypeName);

    //--------------------------------------------------------------------------

    /**
     * Returns the node types derived from this node type.
     *
     * @param directOnly if <code>true</code> only direct subtypes will be considered
     *
     * @return an <code>NodeTypeIterator</code>.
     * @see NodeType#getSubtypes
     * @see NodeType#getDeclaredSubtypes
     */
    public NodeTypeIterator getSubtypes(boolean directOnly) {
        NodeTypeIterator iter;
        try {
            iter = ntMgr.getAllNodeTypes();
        } catch (RepositoryException e) {
            // should never get here
            log.error("failed to retrieve registered node types", e);
            return NodeTypeIteratorAdapter.EMPTY;
        }

        ArrayList<NodeType> result = new ArrayList<NodeType>();
        String thisName = getName();
        while (iter.hasNext()) {
            NodeType nt = iter.nextNodeType();
            if (!nt.getName().equals(thisName)) {
                if (directOnly) {
                    // direct subtypes only
                    for (String name : nt.getDeclaredSupertypeNames()) {
                        if (name.equals(thisName)) {
                            result.add(nt);
                            break;
                        }
                    }
                } else {
                    // direct and indirect subtypes
                    if (nt.isNodeType(thisName)) {
                        result.add(nt);
                    }
                }
            }
        }
        return new NodeTypeIteratorAdapter(result);
    }
}
