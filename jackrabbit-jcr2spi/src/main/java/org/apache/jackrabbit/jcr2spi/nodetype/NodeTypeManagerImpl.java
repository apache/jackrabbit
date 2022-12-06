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
package org.apache.jackrabbit.jcr2spi.nodetype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.commons.iterator.NodeTypeIteratorAdapter;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeTypeManager;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>NodeTypeManagerImpl</code> implements a session dependant
 * NodeTypeManager.
 */
public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements NodeTypeDefinitionProvider, NodeTypeRegistryListener {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(NodeTypeManagerImpl.class);

    /**
     * The ManagerProvider
     */
    private final ManagerProvider mgrProvider;

    /**
     * The wrapped node type registry.
     */
    private final NodeTypeRegistry ntReg;

    /**
     * The ValueFactory used to build property definitions.
     */
    private final ValueFactory valueFactory;

    /**
     * A cache for <code>NodeType</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map<Name, NodeTypeImpl> ntCache;

    /**
     * A cache for <code>PropertyDefinition</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map<QPropertyDefinition, PropertyDefinition> pdCache;

    /**
     * A cache for <code>NodeDefinition</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map<QNodeDefinition, NodeDefinition> ndCache;

    /**
     * Creates a new <code>NodeTypeManagerImpl</code> instance.
     *
     * @param ntReg        node type registry
     * @param mgrProvider  the manager provider
     * @throws RepositoryException If an error occurs.
     */
    public NodeTypeManagerImpl(NodeTypeRegistry ntReg,
                               ManagerProvider mgrProvider) throws RepositoryException {
        this.mgrProvider = mgrProvider;
        this.ntReg = ntReg;
        this.ntReg.addListener(this);
        this.valueFactory = mgrProvider.getJcrValueFactory();

        // setup caches with soft references to node type
        ntCache = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);
        pdCache = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);
        ndCache = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);
    }

    private EffectiveNodeTypeProvider entProvider() {
        return mgrProvider.getEffectiveNodeTypeProvider();
    }

    //--------------------------------------------------------------------------
    /**
     * @see AbstractNodeTypeManager#getNodeType(org.apache.jackrabbit.spi.Name)
     */
    @Override
    public NodeTypeImpl getNodeType(Name name) throws NoSuchNodeTypeException {
        synchronized (ntCache) {
            NodeTypeImpl nt = ntCache.get(name);
            if (nt == null) {
                EffectiveNodeType ent = entProvider().getEffectiveNodeType(name);
                QNodeTypeDefinition def = ntReg.getNodeTypeDefinition(name);
                nt = new NodeTypeImpl(ent, def, this, mgrProvider);
                ntCache.put(name, nt);
            }
            return nt;
        }
    }

    /**
     * @see org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeTypeManager#getNamePathResolver()
     */
    @Override
    public NamePathResolver getNamePathResolver() {
        return mgrProvider.getNamePathResolver();
    }

    /**
     *
     * @param nodeTypeName
     * @return
     */
    public boolean hasNodeType(Name nodeTypeName) {
        boolean isRegistered = ntCache.containsKey(nodeTypeName);
        if (!isRegistered) {
            isRegistered = ntReg.isRegistered(nodeTypeName);
        }
        return isRegistered;
    }

    /**
     * Retrieve the <code>NodeDefinition</code> for the given
     * <code>QNodeDefinition</code>.
     *
     * @param def
     * @return
     */
    @Override
    public NodeDefinition getNodeDefinition(QNodeDefinition def) {
        synchronized (ndCache) {
            NodeDefinition ndi = ndCache.get(def);
            if (ndi == null) {
                ndi = new NodeDefinitionImpl(def, this, getNamePathResolver());
                ndCache.put(def, ndi);
            }
            return ndi;
        }
    }

    /**
     * Retrieve the <code>PropertyDefinition</code> for the given
     * <code>QPropertyDefinition</code>.
     *
     * @param def
     * @return
     */
    @Override
    public PropertyDefinition getPropertyDefinition(QPropertyDefinition def) {
        synchronized (pdCache) {
            PropertyDefinition pdi = pdCache.get(def);
            if (pdi == null) {
                pdi = new PropertyDefinitionImpl(def, this, getNamePathResolver(), valueFactory);
                pdCache.put(def, pdi);
            }
            return pdi;
        }
    }

    /**
     * @return the NodeTypeRegistry
     */
    NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    //-----------------------------------------< NodeTypeDefinitionProvider >---
    /**
     * @see NodeTypeDefinitionProvider#getNodeTypeDefinition(org.apache.jackrabbit.spi.Name)
     */
    public QNodeTypeDefinition getNodeTypeDefinition(Name ntName) throws NoSuchNodeTypeException, RepositoryException {
        NodeTypeImpl nt = getNodeType(ntName);
        return nt.getDefinition();
    }

    //-------------------------------------------< NodeTypeRegistryListener >---
    /**
     * {@inheritDoc}
     */
    public void nodeTypeRegistered(Name ntName) {
        // not interested, ignore
    }

    /**
     * {@inheritDoc}
     */
    public void nodeTypeReRegistered(Name ntName) {
        // flush all affected cache entries
        ntCache.remove(ntName);
        try {
            String name = getNamePathResolver().getJCRName(ntName);
            synchronized (pdCache) {
                Iterator<PropertyDefinition> iter = pdCache.values().iterator();
                while (iter.hasNext()) {
                    PropertyDefinition pd = iter.next();
                    if (name.equals(pd.getDeclaringNodeType().getName())) {
                        iter.remove();
                    }
                }
            }
            synchronized (ndCache) {
                Iterator<NodeDefinition> iter = ndCache.values().iterator();
                while (iter.hasNext()) {
                    NodeDefinition nd = iter.next();
                    if (name.equals(nd.getDeclaringNodeType().getName())) {
                        iter.remove();
                    }
                }
            }
        } catch (NamespaceException e) {
            log.warn(e.getMessage() + " -> clear definition cache." );
            synchronized (pdCache) {
                pdCache.clear();
            }
            synchronized (ndCache) {
                ndCache.clear();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nodeTypeUnregistered(Name ntName) {
        // flush all affected cache entries
        ntCache.remove(ntName);
        try {
            String name = getNamePathResolver().getJCRName(ntName);
            synchronized (pdCache) {
                Iterator<PropertyDefinition> iter = pdCache.values().iterator();
                while (iter.hasNext()) {
                    PropertyDefinition pd = iter.next();
                    if (name.equals(pd.getDeclaringNodeType().getName())) {
                        iter.remove();
                    }
                }
            }
            synchronized (ndCache) {
                Iterator<NodeDefinition> iter = ndCache.values().iterator();
                while (iter.hasNext()) {
                    NodeDefinition nd = iter.next();
                    if (name.equals(nd.getDeclaringNodeType().getName())) {
                        iter.remove();
                    }
                }
            }
        } catch (NamespaceException e) {
            log.warn(e.getMessage() + " -> clear definition cache." );
            synchronized (pdCache) {
                pdCache.clear();
            }
            synchronized (ndCache) {
                ndCache.clear();
            }
        }
    }

    //----------------------------------------------------< NodeTypeManager >---
    /**
     * {@inheritDoc}
     */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        Name[] ntNames = ntReg.getRegisteredNodeTypes();
        ArrayList<NodeType> list = new ArrayList<NodeType>(ntNames.length);
        for (Name ntName : ntNames) {
            list.add(getNodeType(ntName));
        }
        return new NodeTypeIteratorAdapter(list);
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        Name[] ntNames = ntReg.getRegisteredNodeTypes();
        ArrayList<NodeType> list = new ArrayList<NodeType>(ntNames.length);
        for (Name ntName : ntNames) {
            NodeType nt = getNodeType(ntName);
            if (!nt.isMixin()) {
                list.add(nt);
            }
        }
        return new NodeTypeIteratorAdapter(list);
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        Name[] ntNames = ntReg.getRegisteredNodeTypes();
        ArrayList<NodeType> list = new ArrayList<NodeType>(ntNames.length);
        for (Name ntName : ntNames) {
            NodeType nt = getNodeType(ntName);
            if (nt.isMixin()) {
                list.add(nt);
            }
        }
        return new NodeTypeIteratorAdapter(list);
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getNodeType(String nodeTypeName)
            throws NoSuchNodeTypeException {
        try {
            Name qName = getNamePathResolver().getQName(nodeTypeName);
            return getNodeType(qName);
        } catch (NamespaceException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        } catch (NameException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        }
    }

    /**
     * @see NodeTypeManager#hasNodeType(String)
     */
    public boolean hasNodeType(String name) throws RepositoryException {
        try {
            Name qName = getNamePathResolver().getQName(name);
            return hasNodeType(qName);
        } catch (NamespaceException e) {
            return false;
        } catch (NameException e) {
            return false;
        }
    }

    /**
     * @see NodeTypeManager#registerNodeTypes(javax.jcr.nodetype.NodeTypeDefinition[], boolean)
     */
    public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate)
            throws RepositoryException {
        List<QNodeTypeDefinition> defs = new ArrayList<QNodeTypeDefinition>(ntds.length);
        for (NodeTypeDefinition definition : ntds) {
            QNodeTypeDefinition qdef = new QNodeTypeDefinitionImpl(definition, getNamePathResolver(), mgrProvider.getQValueFactory());
            if (!allowUpdate && hasNodeType(qdef.getName())) {
                throw new NodeTypeExistsException("NodeType " + definition.getName() + " already exists.");
            }
            defs.add(qdef);
        }

        getNodeTypeRegistry().registerNodeTypes(defs, allowUpdate);

        List<NodeType> nts = new ArrayList<NodeType>();
        for (QNodeTypeDefinition def : defs) {
            nts.add(getNodeType(def.getName()));
        }
        return new NodeTypeIteratorAdapter(nts);

    }

    /**
     * @see NodeTypeManager#unregisterNodeTypes(String[])
     */
    public void unregisterNodeTypes(String[] names) throws RepositoryException {
        HashSet<Name> ntNames = new HashSet<Name>();
        for (String name : names) {
            ntNames.add(getNamePathResolver().getQName(name));
        }
        getNodeTypeRegistry().unregisterNodeTypes(ntNames);
    }

    //-------------------------------------------------------------< Object >---

    /**
     * Returns the the state of this instance in a human readable format.
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NodeTypeManager (" + super.toString() + ")\n");
        builder.append("All NodeTypes:\n");
        try {
            NodeTypeIterator iter = this.getAllNodeTypes();
            while (iter.hasNext()) {
                NodeType nt = iter.nextNodeType();
                builder.append(nt.getName());
                builder.append("\n\tSupertypes");
                for (NodeType supertype : nt.getSupertypes()) {
                    builder.append("\n\t\t" + supertype.getName());
                }
                builder.append("\n\tMixin\t" + nt.isMixin());
                builder.append("\n\tOrderableChildNodes\t" + nt.hasOrderableChildNodes());
                builder.append("\n\tPrimaryItemName\t" + (nt.getPrimaryItemName() == null ? "<null>" : nt.getPrimaryItemName()));
                for (PropertyDefinition aPd : nt.getPropertyDefinitions()) {
                    builder.append("\n\tPropertyDefinition");
                    builder.append(" (declared in " + aPd.getDeclaringNodeType().getName() + ") ");
                    builder.append("\n\t\tName\t\t" + (aPd.getName()));
                    String type = aPd.getRequiredType() == 0 ? "null" : PropertyType.nameFromValue(aPd.getRequiredType());
                    builder.append("\n\t\tRequiredType\t" + type);
                    String[] vca = aPd.getValueConstraints();
                    StringBuffer constraints = new StringBuffer();
                    if (vca == null) {
                        constraints.append("<null>");
                    } else {
                        for (String aVca : vca) {
                            if (constraints.length() > 0) {
                                constraints.append(", ");
                            }
                            constraints.append(aVca);
                        }
                    }
                    builder.append("\n\t\tValueConstraints\t" + constraints.toString());
                    Value[] defVals = aPd.getDefaultValues();
                    StringBuffer defaultValues = new StringBuffer();
                    if (defVals == null) {
                        defaultValues.append("<null>");
                    } else {
                        for (Value defVal : defVals) {
                            if (defaultValues.length() > 0) {
                                defaultValues.append(", ");
                            }
                            defaultValues.append(defVal.getString());
                        }
                    }
                    builder.append("\n\t\tDefaultValue\t" + defaultValues.toString());
                    builder.append("\n\t\tAutoCreated\t" + aPd.isAutoCreated());
                    builder.append("\n\t\tMandatory\t" + aPd.isMandatory());
                    builder.append("\n\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(aPd.getOnParentVersion()));
                    builder.append("\n\t\tProtected\t" + aPd.isProtected());
                    builder.append("\n\t\tMultiple\t" + aPd.isMultiple());
                }
                for (NodeDefinition aNd : nt.getChildNodeDefinitions()) {
                    builder.append("\n\tNodeDefinition");
                    builder.append(" (declared in " + aNd.getDeclaringNodeType() + ") ");
                    builder.append("\n\t\tName\t\t" + aNd.getName());
                    NodeType[] reqPrimaryTypes = aNd.getRequiredPrimaryTypes();
                    if (reqPrimaryTypes != null && reqPrimaryTypes.length > 0) {
                        for (NodeType reqPrimaryType : reqPrimaryTypes) {
                            builder.append("\n\t\tRequiredPrimaryType\t" + reqPrimaryType.getName());
                        }
                    }
                    NodeType defPrimaryType = aNd.getDefaultPrimaryType();
                    if (defPrimaryType != null) {
                        builder.append("\n\t\tDefaultPrimaryType\t" + defPrimaryType.getName());
                    }
                    builder.append("\n\t\tAutoCreated\t" + aNd.isAutoCreated());
                    builder.append("\n\t\tMandatory\t" + aNd.isMandatory());
                    builder.append("\n\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(aNd.getOnParentVersion()));
                    builder.append("\n\t\tProtected\t" + aNd.isProtected());
                    builder.append("\n\t\tAllowsSameNameSiblings\t" + aNd.allowsSameNameSiblings());
                }
            }
        } catch (RepositoryException e) {
            builder.append(e.getMessage());
        }
        return builder.toString();
    }
}
