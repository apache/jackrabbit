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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.commons.iterator.NodeTypeIteratorAdapter;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeTypeManager;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A <code>NodeTypeManagerImpl</code> implements a session dependant
 * NodeTypeManager.
 */
public class NodeTypeManagerImpl extends AbstractNodeTypeManager
        implements JackrabbitNodeTypeManager, NodeTypeRegistryListener {

    /**
     * Component context of the current session.
     */
    private final SessionContext context;

    /**
     * The root node definition.
     */
    private final NodeDefinitionImpl rootNodeDef;

    /**
     * A cache for <code>NodeType</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map<Name, NodeTypeImpl> ntCache;

    /**
     * A cache for <code>PropertyDefinition</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map<QPropertyDefinition, PropertyDefinitionImpl> pdCache;

    /**
     * A cache for <code>NodeDefinition</code> instances created by this
     * <code>NodeTypeManager</code>
     */
    private final Map<QNodeDefinition, NodeDefinitionImpl> ndCache;

    /**
     * Creates a new <code>NodeTypeManagerImpl</code> instance.
     *
     * @param context the session context
     */
    @SuppressWarnings("unchecked")
    public NodeTypeManagerImpl(SessionContext context) {
        this.context = context;

        // setup caches with soft references to node type
        // & item definition instances
        ntCache = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);
        pdCache = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);
        ndCache = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);

        NodeTypeRegistry registry = context.getNodeTypeRegistry();

        rootNodeDef = new NodeDefinitionImpl(
                registry.getRootNodeDef(), this, context);
        ndCache.put(rootNodeDef.unwrap(), rootNodeDef);

        registry.addListener(this);
    }

    /**
     * Disposes this node type manager.
     */
    public void dispose() {
        context.getNodeTypeRegistry().removeListener(this);
    }

    /**
     * @return the root node definition
     */
    public NodeDefinitionImpl getRootNodeDefinition() {
        return rootNodeDef;
    }

    /**
     * @param def the QNodeDefinition
     * @return the node definition
     */
    @Override
    public NodeDefinitionImpl getNodeDefinition(QNodeDefinition def) {
        synchronized (ndCache) {
            NodeDefinitionImpl ndi = ndCache.get(def);
            if (ndi == null) {
                ndi = new NodeDefinitionImpl(def, this, context);
                ndCache.put(def, ndi);
            }
            return ndi;
        }
    }

    /**
     * @param def prop def
     * @return the property definition
     */
    @Override
    public PropertyDefinitionImpl getPropertyDefinition(QPropertyDefinition def) {
        synchronized (pdCache) {
            PropertyDefinitionImpl pdi = pdCache.get(def);
            if (pdi == null) {
                pdi = new PropertyDefinitionImpl(
                        def, this, context, context.getValueFactory());
                pdCache.put(def, pdi);
            }
            return pdi;
        }
    }

    /**
     * @param name node type name
     * @return node type
     * @throws NoSuchNodeTypeException if the nodetype does not exit
     */
    @Override
    public NodeTypeImpl getNodeType(Name name) throws NoSuchNodeTypeException {
        synchronized (ntCache) {
            NodeTypeImpl nt = ntCache.get(name);
            if (nt == null) {
                NodeTypeRegistry registry = context.getNodeTypeRegistry();
                EffectiveNodeType ent = registry.getEffectiveNodeType(name);
                QNodeTypeDefinition def = registry.getNodeTypeDef(name);
                nt = new NodeTypeImpl(
                        ent, def, this, context,
                        context.getValueFactory(), context.getDataStore());
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
        return context;
    }

    /**
     * @return the node type registry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return context.getNodeTypeRegistry();
    }

    /**
     * Registers the node types defined in the given input stream depending
     * on the content type specified for the stream. This will also register
     * any namespaces identified in the input stream if they have not already
     * been registered.
     *
     * @param in node type XML stream
     * @param contentType type of the input stream
     * @param reregisterExisting flag indicating whether node types should be
     *                           reregistered if they already exist
     * @return registered node types
     * @throws IOException if the input stream could not be read or parsed
     * @throws RepositoryException if the node types are invalid or another
     *                             repository error occurs
     */
    public NodeType[] registerNodeTypes(InputStream in, String contentType,
            boolean reregisterExisting)
            throws IOException, RepositoryException {
        
        // make sure the editing session is allowed to register node types.
        context.getAccessManager().checkRepositoryPermission(Permission.NODE_TYPE_DEF_MNGMT);

        try {
            Map<String, String> namespaceMap = new HashMap<String, String>();
            List<QNodeTypeDefinition> nodeTypeDefs = new ArrayList<QNodeTypeDefinition>();

            if (contentType.equalsIgnoreCase(TEXT_XML)
                    || contentType.equalsIgnoreCase(APPLICATION_XML)) {
                try {
                    NodeTypeReader ntr = new NodeTypeReader(in);

                    Properties namespaces = ntr.getNamespaces();
                    if (namespaces != null) {
                        Enumeration<?> prefixes = namespaces.propertyNames();
                        while (prefixes.hasMoreElements()) {
                            String prefix = (String) prefixes.nextElement();
                            String uri = namespaces.getProperty(prefix);
                            namespaceMap.put(prefix, uri);
                        }
                    }

                    QNodeTypeDefinition[] defs = ntr.getNodeTypeDefs();
                    nodeTypeDefs.addAll(Arrays.asList(defs));
                } catch (NameException e) {
                    throw new RepositoryException("Illegal JCR name", e);
                }
            } else if (contentType.equalsIgnoreCase(TEXT_X_JCR_CND)) {
                try {
                    NamespaceMapping mapping = new NamespaceMapping(context.getSessionImpl());

                    CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> reader =
                        new CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(
                            new InputStreamReader(in), "cnd input stream", mapping,
                            new QDefinitionBuilderFactory());

                    namespaceMap.putAll(mapping.getPrefixToURIMapping());
                    for (QNodeTypeDefinition ntDef: reader.getNodeTypeDefinitions()) {
                        nodeTypeDefs.add(ntDef);
                    }
                } catch (ParseException e) {
                    IOException e2 = new IOException(e.getMessage());
                    e2.initCause(e);
                    throw e2;
                }
            } else {
                throw new UnsupportedRepositoryOperationException(
                        "Unsupported content type: " + contentType);
            }

            new NamespaceHelper(context.getSessionImpl()).registerNamespaces(namespaceMap);

            if (reregisterExisting) {
                NodeTypeRegistry registry = context.getNodeTypeRegistry();
                // split the node types into new and already registered node types.
                // this way we can register new node types together with already
                // registered node types which make circular dependencies possible
                List<QNodeTypeDefinition> newNodeTypeDefs = new ArrayList<QNodeTypeDefinition>();
                List<QNodeTypeDefinition> registeredNodeTypeDefs = new ArrayList<QNodeTypeDefinition>();
                for (QNodeTypeDefinition nodeTypeDef: nodeTypeDefs) {
                    if (registry.isRegistered(nodeTypeDef.getName())) {
                        registeredNodeTypeDefs.add(nodeTypeDef);
                    } else {
                        newNodeTypeDefs.add(nodeTypeDef);
                    }
                }

                ArrayList<NodeType> nodeTypes = new ArrayList<NodeType>();

                // register new node types
                nodeTypes.addAll(registerNodeTypes(newNodeTypeDefs));

                // re-register already existing node types
                for (QNodeTypeDefinition nodeTypeDef: registeredNodeTypeDefs) {
                    registry.reregisterNodeType(nodeTypeDef);
                    nodeTypes.add(getNodeType(nodeTypeDef.getName()));
                }
                return nodeTypes.toArray(new NodeType[nodeTypes.size()]);
            } else {
                Collection<NodeType> types = registerNodeTypes(nodeTypeDefs);
                return types.toArray(new NodeType[types.size()]);
            }

        } catch (InvalidNodeTypeDefException e) {
            throw new RepositoryException("Invalid node type definition", e);
        }
    }

    //---------------------------------------------< NodeTypeRegistryListener >
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
        synchronized (pdCache) {
            Iterator<PropertyDefinitionImpl> iter = pdCache.values().iterator();
            while (iter.hasNext()) {
                PropertyDefinitionImpl pd = iter.next();
                if (ntName.equals(pd.unwrap().getDeclaringNodeType())) {
                    iter.remove();
                }
            }
        }
        synchronized (ndCache) {
            Iterator<NodeDefinitionImpl> iter = ndCache.values().iterator();
            while (iter.hasNext()) {
                NodeDefinitionImpl nd = iter.next();
                if (ntName.equals(nd.unwrap().getDeclaringNodeType())) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nodeTypesUnregistered(Collection<Name> names) {
        // flush all affected cache entries
        for (Name name : names) {
            ntCache.remove(name);
        }
        synchronized (pdCache) {
            Iterator<PropertyDefinitionImpl> iter = pdCache.values().iterator();
            while (iter.hasNext()) {
                PropertyDefinitionImpl pd = iter.next();
                if (names.contains(pd.unwrap().getDeclaringNodeType())) {
                    iter.remove();
                }
            }
        }
        synchronized (ndCache) {
            Iterator<NodeDefinitionImpl> iter = ndCache.values().iterator();
            while (iter.hasNext()) {
                NodeDefinitionImpl nd = iter.next();
                if (names.contains(nd.unwrap().getDeclaringNodeType())) {
                    iter.remove();
                }
            }
        }
    }

    //------------------------------------------------------< NodeTypeManager >
    /**
     * {@inheritDoc}
     */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        Name[] ntNames = context.getNodeTypeRegistry().getRegisteredNodeTypes();
        Arrays.sort(ntNames);
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
        Name[] ntNames = context.getNodeTypeRegistry().getRegisteredNodeTypes();
        Arrays.sort(ntNames);
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
        Name[] ntNames = context.getNodeTypeRegistry().getRegisteredNodeTypes();
        Arrays.sort(ntNames);
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
            return getNodeType(context.getQName(nodeTypeName));
        } catch (NameException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        } catch (NamespaceException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        }
    }

    //--------------------------------------------< JackrabbitNodeTypeManager >

    /**
     * Internal helper method for registering a list of node type definitions.
     * Returns a collection containing the registered node types.
     *
     * @param defs a collection of <code>QNodeTypeDefinition<code> objects
     * @return registered node types
     * @throws InvalidNodeTypeDefException if a nodetype is invalid
     * @throws RepositoryException if an error occurs
     */
    private Collection<NodeType> registerNodeTypes(List<QNodeTypeDefinition> defs)
            throws InvalidNodeTypeDefException, RepositoryException {
        context.getNodeTypeRegistry().registerNodeTypes(defs);

        Set<NodeType> types = new HashSet<NodeType>();
        for (QNodeTypeDefinition def : defs) {
            try {
                types.add(getNodeType(def.getName()));
            } catch (NoSuchNodeTypeException e) {
                // ignore
            }
        }
        return types;
    }

    /**
     * Registers the node types defined in the given XML stream.  This
     * is a trivial implementation that just invokes the existing
     * {@link NodeTypeReader} and {@link NodeTypeRegistry} methods and
     * heuristically creates the returned node type array.  It will also
     * register any namespaces defined in the input source that have not
     * already been registered.
     *
     * {@inheritDoc}
     */
    public NodeType[] registerNodeTypes(InputSource in)
            throws SAXException, RepositoryException {
        try {
            return registerNodeTypes(in.getByteStream(), TEXT_XML);
        } catch (IOException e) {
            throw new SAXException("Error reading node type stream", e);
        }
    }

    private static final String APPLICATION_XML = "application/xml";

    /**
     * Registers the node types defined in the given input stream depending
     * on the content type specified for the stream. This will also register
     * any namespaces identified in the input stream if they have not already
     * been registered.
     *
     * {@inheritDoc}
     */
    public NodeType[] registerNodeTypes(InputStream in, String contentType)
            throws IOException, RepositoryException {
        return registerNodeTypes(in, contentType, false);
    }

    /**
     * Checks whether a node type with the given name exists.
     *
     * @param name node type name
     * @return <code>true</code> if the named node type exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException if the name format is invalid
     */
    public boolean hasNodeType(String name) throws RepositoryException {
        try {
            Name qname = context.getQName(name);
            return getNodeTypeRegistry().isRegistered(qname);
        } catch (NamespaceException e) {
            return false;
        } catch (NameException e) {
           throw new RepositoryException("Invalid name: " + name, e);
        }
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * Registers or updates the specified <code>Collection</code> of
     * <code>NodeTypeDefinition</code> objects. This method is used to register
     * or update a set of node types with mutual dependencies. Returns an
     * iterator over the resulting <code>NodeType</code> objects.
     * <p>
     * The effect of the method is "all or nothing"; if an error occurs, no node
     * types are registered or updated.
     * <p>
     * Throws an <code>InvalidNodeTypeDefinitionException</code> if a
     * <code>NodeTypeDefinition</code> within the <code>Collection</code> is
     * invalid or if the <code>Collection</code> contains an object of a type
     * other than <code>NodeTypeDefinition</code>.
     * <p>
     * Throws a <code>NodeTypeExistsException</code> if <code>allowUpdate</code>
     * is <code>false</code> and a <code>NodeTypeDefinition</code> within the
     * <code>Collection</code> specifies a node type name that is already
     * registered.
     * <p>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param definitions a collection of <code>NodeTypeDefinition</code>s
     * @param allowUpdate a boolean
     * @return the registered node types.
     * @throws InvalidNodeTypeDefinitionException if a
     *  <code>NodeTypeDefinition</code> within the <code>Collection</code> is
     *  invalid or if the <code>Collection</code> contains an object of a type
     *  other than <code>NodeTypeDefinition</code>.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *  <code>false</code> and a <code>NodeTypeDefinition</code> within the
     *  <code>Collection</code> specifies a node type name that is already
     *  registered.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public NodeTypeIterator registerNodeTypes(
            NodeTypeDefinition[] definitions, boolean allowUpdate)
            throws InvalidNodeTypeDefinitionException, NodeTypeExistsException,
            UnsupportedRepositoryOperationException, RepositoryException {

        // make sure the editing session is allowed to register node types.
        context.getAccessManager().checkRepositoryPermission(Permission.NODE_TYPE_DEF_MNGMT);

        NodeTypeRegistry registry = context.getNodeTypeRegistry();

        // split the node types into new and already registered node types.
        // this way we can register new node types together with already
        // registered node types which make circular dependencies possible
        List<QNodeTypeDefinition> addedDefs = new ArrayList<QNodeTypeDefinition>();
        List<QNodeTypeDefinition> modifiedDefs = new ArrayList<QNodeTypeDefinition>();
        for (NodeTypeDefinition definition : definitions) {
            // convert to QNodeTypeDefinition
            QNodeTypeDefinition def = toNodeTypeDef(definition);
            if (registry.isRegistered(def.getName())) {
              if (allowUpdate) {
                  modifiedDefs.add(def);
              } else {
                  throw new NodeTypeExistsException(definition.getName());
              }
            } else {
                addedDefs.add(def);
            }
        }

        try {
            ArrayList<NodeType> result = new ArrayList<NodeType>();

            // register new node types
            result.addAll(registerNodeTypes(addedDefs));

            // re-register already existing node types
            for (QNodeTypeDefinition nodeTypeDef: modifiedDefs) {
                registry.reregisterNodeType(nodeTypeDef);
                result.add(getNodeType(nodeTypeDef.getName()));
            }

            return new NodeTypeIteratorAdapter(result);
        } catch (InvalidNodeTypeDefException e) {
            throw new InvalidNodeTypeDefinitionException(e.getMessage(), e);
        }
    }

    /**
     * Unregisters the specified set of node types. Used to unregister a set of node types with mutual dependencies.
     * <p>
     * Throws a <code>NoSuchNodeTypeException</code> if one of the names listed is not a registered node type.
     * <p>
     * Throws an <code>UnsupportedRepositoryOperationException</code>
     * if this implementation does not support node type registration.
     *
     * @param names a <code>String</code> array
     * @throws UnsupportedRepositoryOperationException if this implementation does not support node type registration.
     * @throws NoSuchNodeTypeException if one of the names listed is not a registered node type.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void unregisterNodeTypes(String[] names)
            throws UnsupportedRepositoryOperationException,
            NoSuchNodeTypeException, RepositoryException {

        // make sure the editing session is allowed to un-register node types.
        context.getAccessManager().checkRepositoryPermission(Permission.NODE_TYPE_DEF_MNGMT);

        Set<Name> ntNames = new HashSet<Name>();
        for (String name : names) {
            try {
                ntNames.add(context.getQName(name));
            } catch (NamespaceException e) {
                throw new RepositoryException("Invalid name: " + name, e);
            } catch (NameException e) {
                throw new RepositoryException("Invalid name: " + name, e);
            }
        }
        getNodeTypeRegistry().unregisterNodeTypes(ntNames);
    }

    /**
     * Internal helper method for converting a <code>NodeTypeDefinition</code>
     * (using prefixed JCR names) to a <code>NodeTypeDef</code> (using
     * namespace-qualified names).
     *
     * @param definition the definition
     * @return a <code>NodeTypeDef</code>
     * @throws RepositoryException if a repository error occurs
     */
    private QNodeTypeDefinition toNodeTypeDef(NodeTypeDefinition definition)
            throws RepositoryException {
        return new QNodeTypeDefinitionImpl(definition, context, QValueFactoryImpl.getInstance());
    }

    //--------------------------------------------------------------< Object >

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "NodeTypeManager(" + super.toString() + ")\n"
            + context.getNodeTypeRegistry();
    }
}
