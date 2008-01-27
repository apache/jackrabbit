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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.commons.iterator.NodeTypeIteratorAdapter;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.jcr2spi.util.Dumpable;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.NamespaceException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeDefinition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.io.PrintStream;

/**
 * A <code>NodeTypeManagerImpl</code> implements a session dependant
 * NodeTypeManager.
 */
public class NodeTypeManagerImpl implements NodeTypeManager, NodeTypeRegistryListener, Dumpable {

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
     * The ValueFactory used to convert qualified values to JCR values.
     */
    private final ValueFactory valueFactory;

    /**
     * A cache for <code>NodeType</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map ntCache;

    /**
     * A cache for <code>PropertyDefinition</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map pdCache;

    /**
     * A cache for <code>NodeDefinition</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map ndCache;

    /**
     * Creates a new <code>NodeTypeManagerImpl</code> instance.
     *
     * @param ntReg        node type registry
     * @param mgrProvider  the manager provider
     * @param valueFactory the JCR value factory
     */
    public NodeTypeManagerImpl(NodeTypeRegistry ntReg,
                               ManagerProvider mgrProvider,
                               ValueFactory valueFactory) {
        this.mgrProvider = mgrProvider;
        this.ntReg = ntReg;
        this.ntReg.addListener(this);
        this.valueFactory = valueFactory;

        // setup caches with soft references to node type
        ntCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        pdCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        ndCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    }

    private NamespaceResolver nsResolver() {
        return mgrProvider.getNamespaceResolver();
    }

    private NamePathResolver resolver() {
        return mgrProvider.getNamePathResolver();
    }

    private EffectiveNodeTypeProvider entProvider() {
        return mgrProvider.getEffectiveNodeTypeProvider();
    }
    //--------------------------------------------------------------------------
    /**
     * @param name
     * @return
     * @throws NoSuchNodeTypeException
     */
    public NodeTypeImpl getNodeType(Name name) throws NoSuchNodeTypeException {
        synchronized (ntCache) {
            NodeTypeImpl nt = (NodeTypeImpl) ntCache.get(name);
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
    public NodeDefinition getNodeDefinition(QNodeDefinition def) {
        synchronized (ndCache) {
            NodeDefinition ndi = (NodeDefinition) ndCache.get(def);
            if (ndi == null) {
                ndi = new NodeDefinitionImpl(def, this, resolver());
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
    public PropertyDefinition getPropertyDefinition(QPropertyDefinition def) {
        synchronized (pdCache) {
            PropertyDefinition pdi = (PropertyDefinition) pdCache.get(def);
            if (pdi == null) {
                pdi = new PropertyDefinitionImpl(def, this, resolver(), valueFactory);
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
            String name = resolver().getJCRName(ntName);
            synchronized (pdCache) {
                Iterator iter = pdCache.values().iterator();
                while (iter.hasNext()) {
                    PropertyDefinition pd = (PropertyDefinition) iter.next();
                    if (name.equals(pd.getDeclaringNodeType().getName())) {
                        iter.remove();
                    }
                }
            }
            synchronized (ndCache) {
                Iterator iter = ndCache.values().iterator();
                while (iter.hasNext()) {
                    NodeDefinition nd = (NodeDefinition) iter.next();
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
            String name = resolver().getJCRName(ntName);
            synchronized (pdCache) {
                Iterator iter = pdCache.values().iterator();
                while (iter.hasNext()) {
                    PropertyDefinition pd = (PropertyDefinition) iter.next();
                    if (name.equals(pd.getDeclaringNodeType().getName())) {
                        iter.remove();
                    }
                }
            }
            synchronized (ndCache) {
                Iterator iter = ndCache.values().iterator();
                while (iter.hasNext()) {
                    NodeDefinition nd = (NodeDefinition) iter.next();
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
        ArrayList list = new ArrayList(ntNames.length);
        for (int i = 0; i < ntNames.length; i++) {
            list.add(getNodeType(ntNames[i]));
        }
        return new NodeTypeIteratorAdapter(list);
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        Name[] ntNames = ntReg.getRegisteredNodeTypes();
        ArrayList list = new ArrayList(ntNames.length);
        for (int i = 0; i < ntNames.length; i++) {
            NodeType nt = getNodeType(ntNames[i]);
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
        ArrayList list = new ArrayList(ntNames.length);
        for (int i = 0; i < ntNames.length; i++) {
            NodeType nt = getNodeType(ntNames[i]);
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
            Name qName = resolver().getQName(nodeTypeName);
            return getNodeType(qName);
        } catch (NamespaceException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        } catch (NameException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        }
    }

    //-----------------------------------------------------------< Dumpable >---
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
        ps.println("NodeTypeManager (" + this + ")");
        ps.println();
        ps.println("All NodeTypes:");
        ps.println();
        try {
            NodeTypeIterator iter = this.getAllNodeTypes();
            while (iter.hasNext()) {
                NodeType nt = iter.nextNodeType();
                ps.println(nt.getName());
                NodeType[] supertypes = nt.getSupertypes();
                ps.println("\tSupertypes");
                for (int i = 0; i < supertypes.length; i++) {
                    ps.println("\t\t" + supertypes[i].getName());
                }
                ps.println("\tMixin\t" + nt.isMixin());
                ps.println("\tOrderableChildNodes\t" + nt.hasOrderableChildNodes());
                ps.println("\tPrimaryItemName\t" + (nt.getPrimaryItemName() == null ? "<null>" : nt.getPrimaryItemName()));
                PropertyDefinition[] pd = nt.getPropertyDefinitions();
                for (int i = 0; i < pd.length; i++) {
                    ps.print("\tPropertyDefinition");
                    ps.println(" (declared in " + pd[i].getDeclaringNodeType().getName() + ") ");
                    ps.println("\t\tName\t\t" + (pd[i].getName()));
                    String type = pd[i].getRequiredType() == 0 ? "null" : PropertyType.nameFromValue(pd[i].getRequiredType());
                    ps.println("\t\tRequiredType\t" + type);
                    String[] vca = pd[i].getValueConstraints();
                    StringBuffer constraints = new StringBuffer();
                    if (vca == null) {
                        constraints.append("<null>");
                    } else {
                        for (int n = 0; n < vca.length; n++) {
                            if (constraints.length() > 0) {
                                constraints.append(", ");
                            }
                            constraints.append(vca[n]);
                        }
                    }
                    ps.println("\t\tValueConstraints\t" + constraints.toString());
                    Value[] defVals = pd[i].getDefaultValues();
                    StringBuffer defaultValues = new StringBuffer();
                    if (defVals == null) {
                        defaultValues.append("<null>");
                    } else {
                        for (int n = 0; n < defVals.length; n++) {
                            if (defaultValues.length() > 0) {
                                defaultValues.append(", ");
                            }
                            defaultValues.append(defVals[n].getString());
                        }
                    }
                    ps.println("\t\tDefaultValue\t" + defaultValues.toString());
                    ps.println("\t\tAutoCreated\t" + pd[i].isAutoCreated());
                    ps.println("\t\tMandatory\t" + pd[i].isMandatory());
                    ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(pd[i].getOnParentVersion()));
                    ps.println("\t\tProtected\t" + pd[i].isProtected());
                    ps.println("\t\tMultiple\t" + pd[i].isMultiple());
                }
                NodeDefinition[] nd = nt.getChildNodeDefinitions();
                for (int i = 0; i < nd.length; i++) {
                    ps.print("\tNodeDefinition");
                    ps.println(" (declared in " + nd[i].getDeclaringNodeType() + ") ");
                    ps.println("\t\tName\t\t" + nd[i].getName());
                    NodeType[] reqPrimaryTypes = nd[i].getRequiredPrimaryTypes();
                    if (reqPrimaryTypes != null && reqPrimaryTypes.length > 0) {
                        for (int n = 0; n < reqPrimaryTypes.length; n++) {
                            ps.print("\t\tRequiredPrimaryType\t" + reqPrimaryTypes[n].getName());
                        }
                    }
                    NodeType defPrimaryType = nd[i].getDefaultPrimaryType();
                    if (defPrimaryType != null) {
                        ps.print("\n\t\tDefaultPrimaryType\t" + defPrimaryType.getName());
                    }
                    ps.println("\n\t\tAutoCreated\t" + nd[i].isAutoCreated());
                    ps.println("\t\tMandatory\t" + nd[i].isMandatory());
                    ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(nd[i].getOnParentVersion()));
                    ps.println("\t\tProtected\t" + nd[i].isProtected());
                    ps.println("\t\tAllowsSameNameSiblings\t" + nd[i].allowsSameNameSiblings());
                }
            }
            ps.println();
        } catch (RepositoryException e) {
            e.printStackTrace(ps);
        }
    }
}
