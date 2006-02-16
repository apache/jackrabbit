/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.util.IteratorHelper;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
    private static Logger log = Logger.getLogger(NodeTypeManagerImpl.class);

    /**
     * The wrapped node type registry.
     */
    private final NodeTypeRegistry ntReg;

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
    public NodeTypeManagerImpl(NodeTypeRegistry ntReg,
                               NamespaceResolver nsResolver) {
        this.nsResolver = nsResolver;
        this.ntReg = ntReg;
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
     * Registers the node types defined in the given XML stream. This is a
     * trivial implementation that just invokes the existing
     * {@link NodeTypeReader} and {@link NodeTypeRegistry} methods and
     * heuristically creates the returned node type array.
     *
     * {@inheritDoc}
     */
    public NodeType[] registerNodeTypes(InputSource in)
            throws SAXException, RepositoryException {
        try {
            NodeTypeDef[] defs = NodeTypeReader.read(in.getByteStream());
            ntReg.registerNodeTypes(Arrays.asList(defs));

            Set types = new HashSet();
            for (int i = 0; i < defs.length; i++) {
                try {
                    types.add(getNodeType(defs[i].getName()));
                } catch (NoSuchNodeTypeException e) {
                    // ignore
                }
            }
            return (NodeType[]) types.toArray(new NodeType[types.size()]);
        } catch (InvalidNodeTypeDefException e) {
            throw new RepositoryException("Invalid node type definition", e);
        } catch (IOException e) {
            throw new SAXException("Error reading node type stream", e);
        }
    }

    private static final String APPLICATION_XML = "application/xml";

    /** {@inheritDoc} */
    public NodeType[] registerNodeTypes(InputStream in, String contentType)
            throws IOException, RepositoryException {
        if (contentType.equalsIgnoreCase(JackrabbitNodeTypeManager.TEXT_XML)
            || contentType.equalsIgnoreCase(APPLICATION_XML)) {
            try {
                return registerNodeTypes(new InputSource(in));
            } catch (SAXException e) {
                throw new IOException(e.getMessage());
            }
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported content type: " + contentType);
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
