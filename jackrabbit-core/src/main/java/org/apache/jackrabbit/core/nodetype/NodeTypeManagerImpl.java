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
import org.apache.jackrabbit.conversion.NameException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.conversion.NamePathResolver;
import org.apache.jackrabbit.util.IteratorHelper;
import org.apache.jackrabbit.namespace.NamespaceMapping;
import org.apache.jackrabbit.namespace.NamespaceResolver;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.util.Dumpable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.NamespaceException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
     * The persistent namespace registry where any new namespaces are
     * automatically registered when new node type definition files are
     * read.
     */
    private final NamespaceRegistryImpl nsReg;

    /**
     * The root node definition.
     */
    private final NodeDefinitionImpl rootNodeDef;

    /**
     * The namespace resolver
     */
    private final NamespaceResolver nsResolver;

    /**
     * The resolver used to translate qualified names to JCR names.
     */
    private final NamePathResolver resolver;

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
     * @param nsReg      namespace registry
     * @param resolver
     */
    public NodeTypeManagerImpl(
            NodeTypeRegistry ntReg, NamespaceRegistryImpl nsReg,
            NamespaceResolver nsResolver, NamePathResolver resolver, DataStore store) {
        this.nsResolver = nsResolver;
        this.resolver = resolver;
        this.ntReg = ntReg;
        this.nsReg = nsReg;
        this.ntReg.addListener(this);
        this.store = store;

        // setup caches with soft references to node type
        // & item definition instances
        ntCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        pdCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        ndCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

        rootNodeDef = new NodeDefinitionImpl(ntReg.getRootNodeDef(), this,
                resolver);
        ndCache.put(rootNodeDef.unwrap().getId(), rootNodeDef);
    }

    /**
     * @return the root node definition
     */
    public NodeDefinitionImpl getRootNodeDefinition() {
        return rootNodeDef;
    }

    /**
     * @param id
     * @return the node definition
     */
    public NodeDefinitionImpl getNodeDefinition(NodeDefId id) {
        synchronized (ndCache) {
            NodeDefinitionImpl ndi = (NodeDefinitionImpl) ndCache.get(id);
            if (ndi == null) {
                NodeDef nd = ntReg.getNodeDef(id);
                if (nd != null) {
                    ndi = new NodeDefinitionImpl(nd, this, resolver);
                    ndCache.put(id, ndi);
                }
            }
            return ndi;
        }
    }

    /**
     * @param id
     * @return the property definition
     */
    public PropertyDefinitionImpl getPropertyDefinition(PropDefId id) {
        synchronized (pdCache) {
            PropertyDefinitionImpl pdi = (PropertyDefinitionImpl) pdCache.get(id);
            if (pdi == null) {
                PropDef pd = ntReg.getPropDef(id);
                if (pd != null) {
                    pdi = new PropertyDefinitionImpl(pd, this, resolver);
                    pdCache.put(id, pdi);
                }
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
                nt = new NodeTypeImpl(ent, def, this, resolver, store);
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
                    NamespaceMapping mapping = new NamespaceMapping(nsResolver);
                    CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(
                            new InputStreamReader(in), "cnd input stream", mapping);

                    namespaceMap.putAll(mapping.getPrefixToURIMapping());

                    nodeTypeDefs.addAll(reader.getNodeTypeDefs());
                } catch (ParseException e) {
                    throw new IOException(e.getMessage());
                }
            } else {
                throw new UnsupportedRepositoryOperationException(
                        "Unsupported content type: " + contentType);
            }

            Iterator iterator = namespaceMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                nsReg.safeRegisterNamespace((String) entry.getKey(),
                        (String) entry.getValue());
            }

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
                nodeTypes.addAll(Arrays.asList(registerNodeTypes(newNodeTypeDefs)));

                // reregister already existing node types
                for (Iterator iter = registeredNodeTypeDefs.iterator(); iter.hasNext();) {
                    NodeTypeDef nodeTypeDef = (NodeTypeDef) iter.next();
                    ntReg.reregisterNodeType(nodeTypeDef);
                    nodeTypes.add(getNodeType(nodeTypeDef.getName()));
                }
                return (NodeType[]) nodeTypes.toArray(new NodeType[nodeTypes.size()]);
            } else {
                return registerNodeTypes(nodeTypeDefs);
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
        return new IteratorHelper(Collections.unmodifiableCollection(list));
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
        return new IteratorHelper(Collections.unmodifiableCollection(list));
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
        return new IteratorHelper(Collections.unmodifiableCollection(list));
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getNodeType(String nodeTypeName)
            throws NoSuchNodeTypeException {
        try {
            return getNodeType(resolver.getQName(nodeTypeName));
        } catch (NameException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        } catch (NamespaceException e) {
            throw new NoSuchNodeTypeException(nodeTypeName, e);
        }
    }

    //--------------------------------------------< JackrabbitNodeTypeManager >

    /**
     * Internal helper method for registering a list of node type definitions.
     * Returns an array containing the registered node types.
     *
     * @param defs a collection of <code>NodeTypeDef<code> objects
     * @returns registered node types
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    private NodeType[] registerNodeTypes(List defs)
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
        return (NodeType[]) types.toArray(new NodeType[types.size()]);
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
            Name qname = resolver.getQName(name);
            return getNodeTypeRegistry().isRegistered(qname);
        } catch (NamespaceException e) {
            return false;
        } catch (NameException e) {
           throw new RepositoryException("Invalid name: " + name, e);
        }
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
