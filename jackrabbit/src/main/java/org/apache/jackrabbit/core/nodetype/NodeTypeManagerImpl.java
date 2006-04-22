/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.util.IteratorHelper;
import org.apache.jackrabbit.util.name.NamespaceMapping;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.util.Dumpable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
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
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(NodeTypeManagerImpl.class);

    /**
     * The wrapped node type registry.
     */
    private final NodeTypeRegistry ntReg;

    /**
     * The persistent namespace registry where any new namespaces are
     * automatically registered when new node type definition files are
     * read.
     */
    private final NamespaceRegistry nsReg;

    /**
     * The root node definition.
     */
    private final NodeDefinitionImpl rootNodeDef;

    /**
     * The namespace resolver used to translate qualified names to JCR names.
     */
    private final NamespaceResolver nsResolver;

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
     * @param ntReg      node type registry
     * @param nsResolver namespace resolver
     */
    public NodeTypeManagerImpl(
            NodeTypeRegistry ntReg, NamespaceRegistry nsReg,
            NamespaceResolver nsResolver) {
        this.nsResolver = nsResolver;
        this.ntReg = ntReg;
        this.nsReg = nsReg;
        this.ntReg.addListener(this);

        // setup caches with soft references to node type
        // & item definition instances
        ntCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        pdCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        ndCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

        rootNodeDef = new NodeDefinitionImpl(ntReg.getRootNodeDef(), this,
                nsResolver);
        ndCache.put(rootNodeDef.unwrap().getId(), rootNodeDef);
    }

    /**
     * @return
     */
    public NodeDefinitionImpl getRootNodeDefinition() {
        return rootNodeDef;
    }

    /**
     * @param id
     * @return
     */
    public NodeDefinitionImpl getNodeDefinition(NodeDefId id) {
        synchronized (ndCache) {
            NodeDefinitionImpl ndi = (NodeDefinitionImpl) ndCache.get(id);
            if (ndi == null) {
                NodeDef nd = ntReg.getNodeDef(id);
                if (nd != null) {
                    ndi = new NodeDefinitionImpl(nd, this, nsResolver);
                    ndCache.put(id, ndi);
                }
            }
            return ndi;
        }
    }

    /**
     * @param id
     * @return
     */
    public PropertyDefinitionImpl getPropertyDefinition(PropDefId id) {
        synchronized (pdCache) {
            PropertyDefinitionImpl pdi = (PropertyDefinitionImpl) pdCache.get(id);
            if (pdi == null) {
                PropDef pd = ntReg.getPropDef(id);
                if (pd != null) {
                    pdi = new PropertyDefinitionImpl(pd, this, nsResolver);
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
    public NodeTypeImpl getNodeType(QName name) throws NoSuchNodeTypeException {
        synchronized (ntCache) {
            NodeTypeImpl nt = (NodeTypeImpl) ntCache.get(name);
            if (nt == null) {
                EffectiveNodeType ent = ntReg.getEffectiveNodeType(name);
                NodeTypeDef def = ntReg.getNodeTypeDef(name);
                nt = new NodeTypeImpl(ent, def, this, nsResolver);
                ntCache.put(name, nt);
            }
            return nt;
        }
    }

    /**
     * @return
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    //---------------------------------------------< NodeTypeRegistryListener >
    /**
     * {@inheritDoc}
     */
    public void nodeTypeRegistered(QName ntName) {
        // not interested, ignore
    }

    /**
     * {@inheritDoc}
     */
    public void nodeTypeReRegistered(QName ntName) {
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
    public void nodeTypeUnregistered(QName ntName) {
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
        QName[] ntNames = ntReg.getRegisteredNodeTypes();
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public NodeType getNodeType(String nodeTypeName)
            throws NoSuchNodeTypeException {
        try {
            return getNodeType(QName.fromJCRName(nodeTypeName, nsResolver));
        } catch (UnknownPrefixException upe) {
            throw new NoSuchNodeTypeException(nodeTypeName, upe);
        } catch (IllegalNameException ine) {
            throw new NoSuchNodeTypeException(nodeTypeName, ine);
        }
    }

    //--------------------------------------------< JackrabbitNodeTypeManager >

    /**
     * Internal helper method for registering a list of node type definitions.
     * Returns an array containing the registered node types.
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
     * Registers a single namespace bypassing registration if the namespace
     * is already registered. A unique prefix is automatically genenerated
     * if the given prefix is already mapped to another namespace.
     *
     * @param prefix The namespace prefix
     * @param uri The namespace URI
     * @throws RepositoryException if a repository error occurs
     */
    protected void registerNamespace(String prefix, String uri)
            throws RepositoryException {
        try {
            // Check if the uri is already registered
            nsReg.getPrefix(uri);
        } catch (NamespaceException e1) {
            // The uri is not in the registry. The prefix may be with another
            // uri or it may not be (the ideal scenario).  In either case,
            // attempt to register it and add a incrementing sequence number
            // to the prefix until it no longer conflicts with any existing
            // prefix.
            String original = prefix;
            for (int i = 2; true; i++) {
                try {
                    // Attempt to register the prefix and uri... if the prefix
                    // is already registered an exception will be thrown due
                    // to an attempt to remap.
                    nsReg.registerNamespace(prefix, uri);
                    return;
                } catch (NamespaceException e2) {
                    prefix = original + i;
                }
            }
        }
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
            NodeTypeReader ntr = new NodeTypeReader(in.getByteStream());

            Properties namespaces = ntr.getNamespaces();
            if (namespaces != null) {
                Enumeration prefixes = namespaces.propertyNames();
                while (prefixes.hasMoreElements()) {
                    String prefix = (String) prefixes.nextElement();
                    registerNamespace(prefix, namespaces.getProperty(prefix));
                }
            }

            NodeTypeDef[] defs = ntr.getNodeTypeDefs();
            return registerNodeTypes(Arrays.asList(defs));
        } catch (IllegalNameException e) {
            throw new RepositoryException("Illegal JCR name syntax", e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException("Unknown namespace prefix", e);
        } catch (InvalidNodeTypeDefException e) {
            throw new RepositoryException("Invalid node type definition", e);
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
        try {
          if (contentType.equalsIgnoreCase(TEXT_XML)
                  || contentType.equalsIgnoreCase(APPLICATION_XML)) {
              return registerNodeTypes(new InputSource(in));
          } else if (contentType.equalsIgnoreCase(TEXT_X_JCR_CND)) {
              NamespaceMapping mapping = new NamespaceMapping(nsResolver);
              CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(
                      new InputStreamReader(in), "cnd input stream", mapping);

              Map nsMap = mapping.getPrefixToURIMapping();
              Iterator iterator = nsMap.entrySet().iterator();
              while (iterator.hasNext()) {
                  Map.Entry entry = (Map.Entry) iterator.next();
                  registerNamespace(
                          (String) entry.getKey(), (String) entry.getValue());
              }

              return registerNodeTypes(reader.getNodeTypeDefs());
          } else {
              throw new UnsupportedRepositoryOperationException(
                      "Unsupported content type: " + contentType);
          }
        } catch (InvalidNodeTypeDefException e) {
            throw new RepositoryException("Invalid node type definition", e);
        } catch (SAXException e) {
            throw new IOException(e.getMessage());
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
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
