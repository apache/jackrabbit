/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.IllegalNameException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.UnknownPrefixException;
import org.apache.jackrabbit.core.util.IteratorHelper;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.*;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * A <code>NodeTypeManagerImpl</code> ...
 */
public class NodeTypeManagerImpl implements NodeTypeManager, NodeTypeRegistryListener {

    private static Logger log = Logger.getLogger(NodeTypeManagerImpl.class);

    private final NodeTypeRegistry ntReg;

    private final NodeDefImpl rootNodeDef;

    // namespace resolver used to translate qualified names to JCR names
    private final NamespaceResolver nsResolver;

    /**
     * A cache for <code>NodeType</code> instances created by this <code>NodeTypeManager</code>
     */
    private final Map ntCache;

    /**
     * Constructor.
     */
    public NodeTypeManagerImpl(NodeTypeRegistry ntReg, NamespaceResolver nsResolver) {
        this.nsResolver = nsResolver;
        this.ntReg = ntReg;
        this.ntReg.addListener(this);

        // setup item cache with soft references to node type instances
        ntCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

        rootNodeDef = new RootNodeDefinition(ntReg.getRootNodeDef(), this, nsResolver);
    }

    /**
     * @return
     */
    public NodeDefImpl getRootNodeDefinition() {
        return rootNodeDef;
    }

    /**
     * @param id
     * @return
     */
    public NodeDefImpl getNodeDef(NodeDefId id) {
        ChildNodeDef cnd = ntReg.getNodeDef(id);
        if (cnd == null) {
            return null;
        }
        return new NodeDefImpl(cnd, this, nsResolver);
    }

    /**
     * @param id
     * @return
     */
    public PropertyDefImpl getPropDef(PropDefId id) {
        PropDef pd = ntReg.getPropDef(id);
        if (pd == null) {
            return null;
        }
        return new PropertyDefImpl(pd, this, nsResolver);
    }

    /**
     * @param name
     * @return
     * @throws NoSuchNodeTypeException
     */
    public synchronized NodeTypeImpl getNodeType(QName name) throws NoSuchNodeTypeException {
        NodeTypeImpl nt = (NodeTypeImpl) ntCache.get(name);
        if (nt != null) {
            return nt;
        }

        EffectiveNodeType ent = ntReg.getEffectiveNodeType(name);
        NodeTypeDef def = ntReg.getNodeTypeDef(name);
        nt = new NodeTypeImpl(ent, def, this, nsResolver);
        ntCache.put(name, nt);

        return nt;
    }

    /**
     * @return
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    //---------------------------------------------< NodeTypeRegistryListener >
    /**
     * @see NodeTypeRegistryListener#nodeTypeRegistered(QName)
     */
    public void nodeTypeRegistered(QName ntName) {
        // ignore
    }

    /**
     * @see NodeTypeRegistryListener#nodeTypeReRegistered(QName)
     */
    public void nodeTypeReRegistered(QName ntName) {
        // flush cache
        ntCache.remove(ntName);
    }

    /**
     * @see NodeTypeRegistryListener#nodeTypeUnregistered(QName)
     */
    public void nodeTypeUnregistered(QName ntName) {
        // sync cache
        ntCache.remove(ntName);
    }

    //------------------------------------------------------< NodeTypeManager >
    /**
     * @see NodeTypeManager#getAllNodeTypes
     */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        QName[] ntNames = ntReg.getRegisteredNodeTypes();
        ArrayList list = new ArrayList(ntNames.length);
        for (int i = 0; i < ntNames.length; i++) {
            list.add(getNodeType(ntNames[i]));
        }
        return new IteratorHelper(Collections.unmodifiableCollection(list));
    }

    /**
     * @see NodeTypeManager#getPrimaryNodeTypes
     */
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        QName[] ntNames = ntReg.getRegisteredNodeTypes();
        ArrayList list = new ArrayList(ntNames.length);
        for (int i = 0; i < ntNames.length; i++) {
            NodeType nt = getNodeType(ntNames[i]);
            if (!nt.isMixin()) {
                list.add(nt);
            }
        }
        return new IteratorHelper(Collections.unmodifiableCollection(list));
    }

    /**
     * @see NodeTypeManager#getMixinNodeTypes
     */
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        QName[] ntNames = ntReg.getRegisteredNodeTypes();
        ArrayList list = new ArrayList(ntNames.length);
        for (int i = 0; i < ntNames.length; i++) {
            NodeType nt = getNodeType(ntNames[i]);
            if (nt.isMixin()) {
                list.add(nt);
            }
        }
        return new IteratorHelper(Collections.unmodifiableCollection(list));
    }

    /**
     * @see NodeTypeManager#getNodeType
     */
    public NodeType getNodeType(String nodeTypeName) throws NoSuchNodeTypeException {
        try {
            return getNodeType(QName.fromJCRName(nodeTypeName, nsResolver));
        } catch (UnknownPrefixException upe) {
            throw new NoSuchNodeTypeException(nodeTypeName, upe);
        } catch (IllegalNameException ine) {
            throw new NoSuchNodeTypeException(nodeTypeName, ine);
        }
    }

    //----------------------------------------------------------< diagnostics >
    /**
     * Dumps the state of this <code>NodeTypeManager</code> instance.
     *
     * @param ps
     * @throws RepositoryException
     */
    public void dump(PrintStream ps) throws RepositoryException {
        ps.println("NodeTypeManager (" + this + ")");
        ps.println();
        ntReg.dump(ps);
    }

    //--------------------------------------------------------< inner classes >
    /**
     * The <code>RootNodeDefinition</code> defines the characteristics of
     * the root node.
     */
    private static class RootNodeDefinition extends NodeDefImpl {

        /**
         * Creates a new <code>RootNodeDefinition</code>.
         */
        RootNodeDefinition(ChildNodeDef def, NodeTypeManagerImpl ntMgr, NamespaceResolver nsResolver) {
            super(def, ntMgr, nsResolver);
        }

        /**
         * @see NodeDef#getName
         */
        public String getName() {
            // not applicable
            return "";
        }

        /**
         * @see NodeDef#getDeclaringNodeType
         */
        public NodeType getDeclaringNodeType() {
            // not applicable
            return null;
        }
    }
}

