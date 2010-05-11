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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.api.jsr283.nodetype.InvalidNodeTypeDefinitionException;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.commons.iterator.NodeTypeIteratorAdapter;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.api.jsr283.nodetype.NodeDefinitionTemplate;
import org.apache.jackrabbit.api.jsr283.nodetype.NodeTypeDefinition;
import org.apache.jackrabbit.api.jsr283.nodetype.NodeTypeExistsException;
import org.apache.jackrabbit.api.jsr283.nodetype.NodeTypeTemplate;
import org.apache.jackrabbit.api.jsr283.nodetype.PropertyDefinitionTemplate;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.Name;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

/**
 * A <code>NodeTypeManagerImpl</code> implements a session dependant
 * NodeTypeManager.
 */
public class NodeTypeManagerImpl implements JackrabbitNodeTypeManager,
        Dumpable, NodeTypeRegistryListener {

    /**
     * The wrapped node type registry.
     */
    private final NodeTypeRegistry ntReg;

    /**
     * Current session.
     */
    private final SessionImpl session;

    /**
     * The value factory obtained from the current session.
     */
    private final ValueFactory valueFactory;

    /**
     * The root node definition.
     */
    private final NodeDefinitionImpl rootNodeDef;

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

    private final DataStore store;

    /**
     * Creates a new <code>NodeTypeManagerImpl</code> instance.
     *
     * @param ntReg      node type registry
     * @param session    current session
     * @throws RepositoryException If an error occurs.
     */
    public NodeTypeManagerImpl(
            NodeTypeRegistry ntReg, SessionImpl session, DataStore store)
            throws RepositoryException {
        this.ntReg = ntReg;
        this.session = session;
        this.valueFactory = session.getValueFactory();
        this.ntReg.addListener(this);
        this.store = store;

        // setup caches with soft references to node type
        // & item definition instances
        ntCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        pdCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        ndCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

        rootNodeDef =
            new NodeDefinitionImpl(ntReg.getRootNodeDef(), this, session);
        ndCache.put(rootNodeDef.unwrap(), rootNodeDef);
    }

    /**
     * @return the root node definition
     */
    public NodeDefinitionImpl getRootNodeDefinition() {
        return rootNodeDef;
    }

    /**
     * @param def internal node definition
     * @return the node definition
     */
    public NodeDefinitionImpl getNodeDefinition(NodeDef def) {
        synchronized (ndCache) {
            NodeDefinitionImpl ndi = (NodeDefinitionImpl) ndCache.get(def);
            if (ndi == null) {
                ndi = new NodeDefinitionImpl(def, this, session);
                ndCache.put(def, ndi);
            }
            return ndi;
        }
    }

    /**
     * @param def internal property definition
     * @return the property definition
     */
    public PropertyDefinitionImpl getPropertyDefinition(PropDef def) {
        synchronized (pdCache) {
            PropertyDefinitionImpl pdi = (PropertyDefinitionImpl) pdCache.get(def);
            if (pdi == null) {
                pdi = new PropertyDefinitionImpl(def, this, session);
                pdCache.put(def, pdi);
            }
            return pdi;
        }
    }

    /**
     * @param name
     * @return
     * @throws NoSuchNodeTypeException
     */
    public NodeTypeImpl getNodeType(Name name) throws NoSuchNodeTypeException {
        synchronized (ntCache) {
            NodeTypeImpl nt = (NodeTypeImpl) ntCache.get(name);
            if (nt == null) {
                EffectiveNodeType ent = ntReg.getEffectiveNodeType(name);
                NodeTypeDef def = ntReg.getNodeTypeDef(name);
                nt = new NodeTypeImpl(ent, def, this, session, valueFactory, store);
                ntCache.put(name, nt);
            }
            return nt;
        }
    }

    /**
     * @return the node type registry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
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

        try {
            Map namespaceMap = new HashMap();
            List nodeTypeDefs = new ArrayList();

            if (contentType.equalsIgnoreCase(TEXT_XML)
                    || contentType.equalsIgnoreCase(APPLICATION_XML)) {
                try {
                    NodeTypeReader ntr = new NodeTypeReader(in);

                    Properties namespaces = ntr.getNamespaces();
                    if (namespaces != null) {
                        Enumeration prefixes = namespaces.propertyNames();
                        while (prefixes.hasMoreElements()) {
                            String prefix = (String) prefixes.nextElement();
                            String uri = namespaces.getProperty(prefix);
                            namespaceMap.put(prefix, uri);
                        }
                    }

                    NodeTypeDef[] defs = ntr.getNodeTypeDefs();
                    nodeTypeDefs.addAll(Arrays.asList(defs));
                } catch (NameException e) {
                    throw new RepositoryException("Illegal JCR name", e);
                }
            } else if (contentType.equalsIgnoreCase(TEXT_X_JCR_CND)) {
                try {
                    NamespaceMapping mapping = new NamespaceMapping(session);
                    CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(
                            new InputStreamReader(in), "cnd input stream", mapping);

                    namespaceMap.putAll(mapping.getPrefixToURIMapping());

                    nodeTypeDefs.addAll(reader.getNodeTypeDefs());
                } catch (ParseException e) {
                    IOException e2 = new IOException(e.getMessage());
                    e2.initCause(e);
                    throw e2;
                }
            } else {
                throw new UnsupportedRepositoryOperationException(
                        "Unsupported content type: " + contentType);
            }

            new NamespaceHelper(session).registerNamespaces(namespaceMap);

            if (reregisterExisting) {
                // split the node types into new and already registered node types.
                // this way we can register new node types together with already
                // registered node types which make circular dependencies possible
                List newNodeTypeDefs = new ArrayList();
                List registeredNodeTypeDefs = new ArrayList();
                for (Iterator iter = nodeTypeDefs.iterator(); iter.hasNext();) {
                    NodeTypeDef nodeTypeDef = (NodeTypeDef) iter.next();
                    if (ntReg.isRegistered(nodeTypeDef.getName())) {
                        registeredNodeTypeDefs.add(nodeTypeDef);
                    } else {
                        newNodeTypeDefs.add(nodeTypeDef);
                    }
                }

                ArrayList nodeTypes = new ArrayList();

                // register new node types
                nodeTypes.addAll(registerNodeTypes(newNodeTypeDefs));

                // reregister already existing node types
                for (Iterator iter = registeredNodeTypeDefs.iterator(); iter.hasNext();) {
                    NodeTypeDef nodeTypeDef = (NodeTypeDef) iter.next();
                    ntReg.reregisterNodeType(nodeTypeDef);
                    nodeTypes.add(getNodeType(nodeTypeDef.getName()));
                }
                return (NodeType[]) nodeTypes.toArray(new NodeType[nodeTypes.size()]);
            } else {
                Collection types = registerNodeTypes(nodeTypeDefs);
                return (NodeType[]) types.toArray(new NodeType[types.size()]);
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
            Iterator iter = pdCache.values().iterator();
            while (iter.hasNext()) {
                PropertyDefinitionImpl pd = (PropertyDefinitionImpl) iter.next();
                if (ntName.equals(pd.unwrap().getDeclaringNodeType())) {
                    iter.remove();
                }
            }
        }
        synchronized (ndCache) {
            Iterator iter = ndCache.values().iterator();
            while (iter.hasNext()) {
                NodeDefinitionImpl nd = (NodeDefinitionImpl) iter.next();
                if (ntName.equals(nd.unwrap().getDeclaringNodeType())) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nodeTypeUnregistered(Name ntName) {
        // flush all affected cache entries
        ntCache.remove(ntName);
        synchronized (pdCache) {
            Iterator iter = pdCache.values().iterator();
            while (iter.hasNext()) {
                PropertyDefinitionImpl pd = (PropertyDefinitionImpl) iter.next();
                if (ntName.equals(pd.unwrap().getDeclaringNodeType())) {
                    iter.remove();
                }
            }
        }
        synchronized (ndCache) {
            Iterator iter = ndCache.values().iterator();
            while (iter.hasNext()) {
                NodeDefinitionImpl nd = (NodeDefinitionImpl) iter.next();
                if (ntName.equals(nd.unwrap().getDeclaringNodeType())) {
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
            return getNodeType(session.getQName(nodeTypeName));
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
     * @param defs a collection of <code>NodeTypeDef<code> objects
     * @returns registered node types
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    private Collection registerNodeTypes(List defs)
            throws InvalidNodeTypeDefException, RepositoryException {
        ntReg.registerNodeTypes(defs);

        Set types = new HashSet();
        Iterator iterator = defs.iterator();
        while (iterator.hasNext()) {
            try {
                NodeTypeDef def = (NodeTypeDef) iterator.next();
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
            Name qname = session.getQName(name);
            return getNodeTypeRegistry().isRegistered(qname);
        } catch (NamespaceException e) {
            return false;
        } catch (NameException e) {
           throw new RepositoryException("Invalid name: " + name, e);
        }
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * Returns an empty <code>NodeTypeTemplate</code> which can then be used to
     * define a node type and passed to
     * <code>NodeTypeManager.registerNodeType</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     * @return A <code>NodeTypeTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public NodeTypeTemplate createNodeTypeTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeTypeTemplateImpl();
    }

    /**
     * Returns a <code>NodeTypeTemplate</code> holding the specified node type
     * definition. This template can then be altered and passed to
     * <code>NodeTypeManager.registerNodeType</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param ntd a <code>NodeTypeDefinition</code>.
     * @return A <code>NodeTypeTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeTypeTemplateImpl(ntd);
    }

    /**
     * Returns an empty <code>NodeDefinitionTemplate</code> which can then be
     * used to create a child node definition and attached to a
     * <code>NodeTypeTemplate</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @return A <code>NodeDefinitionTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public NodeDefinitionTemplate createNodeDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeDefinitionTemplateImpl(this);
    }

    /**
     * Returns an empty <code>PropertyDefinitionTemplate</code> which can then
     * be used to create a property definition and attached to a
     * <code>NodeTypeTemplate</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @return A <code>PropertyDefinitionTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new PropertyDefinitionTemplateImpl();
    }

    /**
     * Registers a new node type or updates an existing node type using the
     * specified definition and returns the resulting <code>NodeType</code>
     * object.
     * <p/>
     * Typically, the object passed to this method will be a
     * <code>NodeTypeTemplate</code> (a subclass of
     * <code>NodeTypeDefinition</code>) acquired from
     * <code>NodeTypeManager.createNodeTypeTemplate</code> and then filled-in
     * with definition information.
     * <p/>
     * Throws an <code>InvalidNodeTypeDefinitionException</code> if the
     * <code>NodeTypeDefinition</code> is invalid.
     * <p/>
     * Throws a <code>NodeTypeExistsException</code> if <code>allowUpdate</code>
     * is <code>false</code> and the <code>NodeTypeDefinition</code> specifies a
     * node type name that is already registered.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param ntd an <code>NodeTypeDefinition</code>.
     * @param allowUpdate a boolean
     * @return the registered node type
     * @throws InvalidNodeTypeDefinitionException if the
     *  <code>NodeTypeDefinition</code> is invalid.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *  <code>false</code> and the <code>NodeTypeDefinition</code> specifies a
     *  node type name that is already registered.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate)
            throws InvalidNodeTypeDefinitionException, NodeTypeExistsException,
            UnsupportedRepositoryOperationException, RepositoryException {
        HashSet defs = new HashSet();
        defs.add(ntd);
        return (NodeType) registerNodeTypes(defs, allowUpdate).next();
    }

    /**
     * Registers or updates the specified <code>Collection</code> of
     * <code>NodeTypeDefinition</code> objects. This method is used to register
     * or update a set of node types with mutual dependencies. Returns an
     * iterator over the resulting <code>NodeType</code> objects.
     * <p/>
     * The effect of the method is "all or nothing"; if an error occurs, no node
     * types are registered or updated.
     * <p/>
     * Throws an <code>InvalidNodeTypeDefinitionException</code> if a
     * <code>NodeTypeDefinition</code> within the <code>Collection</code> is
     * invalid or if the <code>Collection</code> contains an object of a type
     * other than <code>NodeTypeDefinition</code>.
     * <p/>
     * Throws a <code>NodeTypeExistsException</code> if <code>allowUpdate</code>
     * is <code>false</code> and a <code>NodeTypeDefinition</code> within the
     * <code>Collection</code> specifies a node type name that is already
     * registered.
     * <p/>
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
    public NodeTypeIterator registerNodeTypes(Collection definitions,
                                              boolean allowUpdate)
            throws InvalidNodeTypeDefinitionException, NodeTypeExistsException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // split the node types into new and already registered node types.
        // this way we can register new node types together with already
        // registered node types which make circular dependencies possible
        List addedDefs = new ArrayList();
        List modifiedDefs = new ArrayList();
        for (Iterator iter = definitions.iterator(); iter.hasNext();) {
            NodeTypeDefinition definition = (NodeTypeDefinition) iter.next();
            // convert to NodeTypeDef
            NodeTypeDef def = toNodeTypeDef(definition);
            if (ntReg.isRegistered(def.getName())) {
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
            ArrayList result = new ArrayList();

            // register new node types
            result.addAll(registerNodeTypes(addedDefs));

            // reregister already existing node types
            for (Iterator iter = modifiedDefs.iterator(); iter.hasNext();) {
                NodeTypeDef nodeTypeDef = (NodeTypeDef) iter.next();
                ntReg.reregisterNodeType(nodeTypeDef);
                result.add(getNodeType(nodeTypeDef.getName()));
            }

            return new NodeTypeIteratorAdapter(result);
        } catch (InvalidNodeTypeDefException e) {
            throw new InvalidNodeTypeDefinitionException(e.getMessage(), e);
        }
    }

    /**
     * Unregisters the specified node type.
     * <p/>
     * Throws a <code>NoSuchNodeTypeException</code> if no registered node type
     * exists with the specified name.
     *
     * @param name a <code>String</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws NoSuchNodeTypeException if no registered node type exists with
     *  the specified name.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void unregisterNodeType(String name)
            throws UnsupportedRepositoryOperationException,
            NoSuchNodeTypeException, RepositoryException {
        unregisterNodeTypes(new String[] {name});
    }

    /**
     * Unregisters the specified set of node types. Used to unregister a set of node types with mutual dependencies.
     * <p/>
     * Throws a <code>NoSuchNodeTypeException</code> if one of the names listed is not a registered node type.
     * <p/>
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
        HashSet ntNames = new HashSet();
        for (int i = 0; i < names.length; i++) {
            try {
                ntNames.add(session.getQName(names[i]));
            } catch (NamespaceException e) {
                throw new RepositoryException("Invalid name: " + names[i], e);
            } catch (NameException e) {
                throw new RepositoryException("Invalid name: " + names[i], e);
            }
        }
        getNodeTypeRegistry().unregisterNodeTypes(ntNames);
    }

    /**
     * Internal helper method for converting a <code>NodeTypeDefinition</code>
     * (using prefixed JCR names) to a <code>NodeTypeDef</code> (using
     * namespace-qualified names).
     *
     * @param definition
     * @return a <code>NodeTypeDef</code>
     * @throws InvalidNodeTypeDefinitionException
     * @throws RepositoryException
     */
    private NodeTypeDef toNodeTypeDef(NodeTypeDefinition definition)
            throws InvalidNodeTypeDefinitionException, RepositoryException {
        NodeTypeDef def = new NodeTypeDef();

        // name
        String name = definition.getName();
        if (name == null) {
            throw new InvalidNodeTypeDefinitionException("No node type name specified");
        }
        try {
            def.setName(session.getQName(name));
        } catch (NamespaceException e) {
            throw new InvalidNodeTypeDefinitionException("Invalid name: " + name, e);
        } catch (NameException e) {
            throw new InvalidNodeTypeDefinitionException("Invalid name: " + name, e);
        }

        // supertypes
        String[] names = definition.getDeclaredSupertypeNames();
        Name[] qnames = new Name[names.length];
        for (int i = 0; i < names.length; i++) {
            try {
                qnames[i] = session.getQName(names[i]);
            } catch (NamespaceException e) {
                throw new InvalidNodeTypeDefinitionException("Invalid supertype name: " + names[i], e);
            } catch (NameException e) {
                throw new InvalidNodeTypeDefinitionException("Invalid supertype name: " + names[i], e);
            }
        }
        def.setSupertypes(qnames);

        // primary item
        name = definition.getPrimaryItemName();
        if (name != null) {
            try {
                def.setPrimaryItemName(session.getQName(name));
            } catch (NamespaceException e) {
                throw new InvalidNodeTypeDefinitionException("Invalid primary item name: " + name, e);
            } catch (NameException e) {
                throw new InvalidNodeTypeDefinitionException("Invalid primary item name: " + name, e);
            }
        }

        // misc. flags
        def.setMixin(definition.isMixin());
        def.setAbstract(definition.isAbstract());
        def.setOrderableChildNodes(definition.hasOrderableChildNodes());

        // child nodes
        NodeDefinition[] ndefs = definition.getDeclaredChildNodeDefinitions();
        if (ndefs != null) {
            NodeDef[] qndefs = new NodeDef[ndefs.length];
            for (int i = 0; i < ndefs.length; i++) {
                NodeDefImpl qndef = new NodeDefImpl();
                // declaring node type
                qndef.setDeclaringNodeType(def.getName());
                // name
                name = ndefs[i].getName();
                if (name != null) {
                    if (name.equals("*")) {
                        qndef.setName(ItemDef.ANY_NAME);
                    } else {
                        try {
                            qndef.setName(session.getQName(name));
                        } catch (NamespaceException e) {
                            throw new InvalidNodeTypeDefinitionException("Invalid node name: " + name, e);
                        } catch (NameException e) {
                            throw new InvalidNodeTypeDefinitionException("Invalid node name: " + name, e);
                        }
                    }
                }
                // default primary type
                //name = ndefs[i].getDefaultPrimaryTypeName();
                // FIXME when JCR 2.0 API has been finalized
                name = ((NodeDefinitionTemplateImpl) ndefs[i]).getDefaultPrimaryTypeName();
                if (name != null) {
                    try {
                        qndef.setDefaultPrimaryType(session.getQName(name));
                    } catch (NamespaceException e) {
                        throw new InvalidNodeTypeDefinitionException("Invalid default primary type: " + name, e);
                    } catch (NameException e) {
                        throw new InvalidNodeTypeDefinitionException("Invalid default primary type: " + name, e);
                    }
                }
                // required primary types
                //names = ndefs[i].getRequiredPrimaryTypeNames();
                // FIXME when JCR 2.0 API has been finalized
                names = ((NodeDefinitionTemplateImpl) ndefs[i]).getRequiredPrimaryTypeNames();
                qnames = new Name[names.length];
                for (int j = 0; j < names.length; j++) {
                    try {
                        qnames[j] = session.getQName(names[j]);
                    } catch (NamespaceException e) {
                        throw new InvalidNodeTypeDefinitionException("Invalid required primary type: " + names[j], e);
                    } catch (NameException e) {
                        throw new InvalidNodeTypeDefinitionException("Invalid required primary type: " + names[j], e);
                    }
                }
                qndef.setRequiredPrimaryTypes(qnames);

                // misc. flags/attributes
                qndef.setAutoCreated(ndefs[i].isAutoCreated());
                qndef.setMandatory(ndefs[i].isMandatory());
                qndef.setProtected(ndefs[i].isProtected());
                qndef.setOnParentVersion(ndefs[i].getOnParentVersion());
                qndef.setAllowsSameNameSiblings(ndefs[i].allowsSameNameSiblings());

                qndefs[i] = qndef;
            }
            def.setChildNodeDefs(qndefs);
        }

        // properties
        PropertyDefinition[] pdefs = definition.getDeclaredPropertyDefinitions();
        if (pdefs != null) {
            PropDef[] qpdefs = new PropDef[pdefs.length];
            for (int i = 0; i < pdefs.length; i++) {
                PropDefImpl qpdef = new PropDefImpl();
                // declaring node type
                qpdef.setDeclaringNodeType(def.getName());
                // name
                name = pdefs[i].getName();
                if (name != null) {
                    if (name.equals("*")) {
                        qpdef.setName(ItemDef.ANY_NAME);
                    } else {
                        try {
                            qpdef.setName(session.getQName(name));
                        } catch (NamespaceException e) {
                            throw new InvalidNodeTypeDefinitionException("Invalid property name: " + name, e);
                        } catch (NameException e) {
                            throw new InvalidNodeTypeDefinitionException("Invalid property name: " + name, e);
                        }
                    }
                }
                // misc. flags/attributes
                int type = pdefs[i].getRequiredType();
                qpdef.setRequiredType(type);
                qpdef.setAutoCreated(pdefs[i].isAutoCreated());
                qpdef.setMandatory(pdefs[i].isMandatory());
                qpdef.setProtected(pdefs[i].isProtected());
                qpdef.setOnParentVersion(pdefs[i].getOnParentVersion());
                qpdef.setMultiple(pdefs[i].isMultiple());
                // value constraints
                String[] constraints = pdefs[i].getValueConstraints();
                if (constraints != null) {
                    ValueConstraint[] qconstraints = new ValueConstraint[constraints.length];
                    for (int j = 0; j < constraints.length; j++) {
                        try {
                            qconstraints[j] = ValueConstraint.create(type, constraints[j], session);
                        } catch (InvalidConstraintException e) {
                            throw new InvalidNodeTypeDefinitionException(
                                    "Invalid value constraint " + constraints[i], e);
                        }
                    }
                    qpdef.setValueConstraints(qconstraints);
                }
                // default values
                Value[] values = pdefs[i].getDefaultValues();
                if (values != null) {
                    InternalValue[] qvalues = new InternalValue[values.length];
                    for (int j = 0; j < values.length; j++) {
                        try {
                            qvalues[j] = InternalValue.create(values[j], session);
                        } catch (ValueFormatException e) {
                            throw new InvalidNodeTypeDefinitionException(
                                    "Invalid default value format", e);
                        }
                    }
                    qpdef.setDefaultValues(qvalues);
                }

                qpdefs[i] = qpdef;
            }
            def.setPropertyDefs(qpdefs);
        }

        return def;
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
        ps.println("NodeTypeManager (" + this + ")");
        ps.println();
        ntReg.dump(ps);
    }
}
