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
package org.apache.jackrabbit.commons.cnd;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import static org.apache.jackrabbit.JcrConstants.NT_BASE;

/**
 * Utility class for importing compact node type definitions.
 * @see CompactNodeTypeDefReader
 * @see TemplateBuilderFactory
 */
public final class CndImporter {

    private CndImporter() {
        super();
    }

    /**
     * Shortcut for
     * <pre>
     *   registerNodeTypes(cnd, "cnd input stream", wsp.getNodeTypeManager(),
     *          wsp.getNamespaceRegistry(), session.getValueFactory(), false);
     * </pre>
     * where <code>wsp</code> is the workspace of the <code>session</code> passed.
     * @see #registerNodeTypes(Reader, String, NodeTypeManager, NamespaceRegistry, ValueFactory, boolean)
     * @param cnd
     * @param session  the session to use for registering the node types
     * @return  the registered node types
     *
     * @throws InvalidNodeTypeDefinitionException
     * @throws NodeTypeExistsException
     * @throws UnsupportedRepositoryOperationException
     * @throws ParseException
     * @throws RepositoryException
     * @throws IOException
     */
    public static NodeType[] registerNodeTypes(Reader cnd, Session session)
            throws InvalidNodeTypeDefinitionException, NodeTypeExistsException,
            UnsupportedRepositoryOperationException, ParseException, RepositoryException, IOException {

        Workspace wsp = session.getWorkspace();
        return registerNodeTypes(cnd, "cnd input stream", wsp.getNodeTypeManager(), wsp.getNamespaceRegistry(),
                session.getValueFactory(), false);
    }

    /**
     * Shortcut for
     * <pre>
     *   registerNodeTypes(cnd, "cnd input stream", wsp.getNodeTypeManager(),
     *          wsp.getNamespaceRegistry(), session.getValueFactory(), reregisterExisting);
     * </pre>
     * where <code>wsp</code> is the workspace of the <code>session</code> passed.
     * @see #registerNodeTypes(Reader, String, NodeTypeManager, NamespaceRegistry, ValueFactory, boolean)
     * @param cnd
     * @param session  the session to use for registering the node types
     * @param reregisterExisting  <code>true</code> if existing node types should be re-registered
     *     with those present in the cnd. <code>false</code> otherwise.
     * @return  the registered node types
     *
     * @throws InvalidNodeTypeDefinitionException
     * @throws NodeTypeExistsException
     * @throws UnsupportedRepositoryOperationException
     * @throws ParseException
     * @throws RepositoryException
     * @throws IOException
     */
    public static NodeType[] registerNodeTypes(Reader cnd, Session session, boolean reregisterExisting)
            throws InvalidNodeTypeDefinitionException, NodeTypeExistsException,
            UnsupportedRepositoryOperationException, ParseException, RepositoryException, IOException {

        Workspace wsp = session.getWorkspace();
        return registerNodeTypes(cnd, "cnd input stream", wsp.getNodeTypeManager(), wsp.getNamespaceRegistry(),
                session.getValueFactory(), reregisterExisting);
    }

    /**
     * Registers nodetypes in <code>cnd</code> format.
     * @param cnd  a reader to the cnd. The reader is closed on return.
     * @param systemId  a informative id of the given cnd input.
     * @param nodeTypeManager  the {@link NodeTypeManager} used for creating and registering the
     *     {@link NodeTypeTemplate}s, {@link NodeDefinitionTemplate}s and {@link PropertyDefinitionTemplate}s
     *     defined in the cnd.
     * @param namespaceRegistry  the {@link NamespaceRegistry} used for registering namespaces defined in
     *     the cnd.
     * @param valueFactory  the {@link ValueFactory} used to create
     *     {@link PropertyDefinitionTemplate#setDefaultValues(javax.jcr.Value[]) default value(s)}.
     * @param reregisterExisting  <code>true</code> if existing node types should be re-registered
     *     with those present in the cnd. <code>false</code> otherwise.
     * @return  the registered node types
     *
     * @throws ParseException  if the cnd cannot be parsed
     * @throws InvalidNodeTypeDefinitionException  if a <code>NodeTypeDefinition</code> is invalid.
     * @throws NodeTypeExistsException  if <code>reregisterExisting</code> is <code>false</code> and a
     *     <code>NodeTypeDefinition</code> specifies a node type name that is already registered.
     * @throws UnsupportedRepositoryOperationException  if the <code>NodeTypeManager</code> does not
     *     support node type registration.
     * @throws IOException  if closing the cnd reader fails
     * @throws RepositoryException  if another error occurs.
     */
    public static NodeType[] registerNodeTypes(Reader cnd, String systemId, NodeTypeManager nodeTypeManager,
            NamespaceRegistry namespaceRegistry, ValueFactory valueFactory, boolean reregisterExisting)
        throws ParseException, InvalidNodeTypeDefinitionException, NodeTypeExistsException,
            UnsupportedRepositoryOperationException, RepositoryException, IOException {

        try {
            DefinitionBuilderFactory<NodeTypeTemplate, NamespaceRegistry> factory =
                    new TemplateBuilderFactory(nodeTypeManager, valueFactory, namespaceRegistry);

            CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry> cndReader =
                new CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry>(cnd, systemId, factory);

            Map<String, NodeTypeTemplate> templates = new HashMap<String, NodeTypeTemplate>();
            for (NodeTypeTemplate template : cndReader.getNodeTypeDefinitions()) {
                templates.put(template.getName(), template);
            }

            List<NodeTypeTemplate> toRegister = new ArrayList<NodeTypeTemplate>(templates.size());
            for (NodeTypeTemplate ntt : templates.values()) {
                if (reregisterExisting || !nodeTypeManager.hasNodeType(ntt.getName())) {
                    ensureNtBase(ntt, templates, nodeTypeManager);
                    toRegister.add(ntt);
                }
            }
            NodeTypeIterator registered = nodeTypeManager.registerNodeTypes(
                    toRegister.toArray(new NodeTypeTemplate[toRegister.size()]), true);
            return toArray(registered);
        }
        finally {
            cnd.close();
        }
    }

    private static void ensureNtBase(NodeTypeTemplate ntt, Map<String, NodeTypeTemplate> templates,
            NodeTypeManager nodeTypeManager) throws RepositoryException {
        if (!ntt.isMixin() && !NT_BASE.equals(ntt.getName())) {
            String[] supertypes = ntt.getDeclaredSupertypeNames();
            if (supertypes.length == 0) {
                ntt.setDeclaredSuperTypeNames(new String[] {NT_BASE});
            } else {
                // Check whether we need to add the implicit "nt:base" supertype
                boolean needsNtBase = true;
                for (String name : supertypes) {
                    NodeTypeDefinition std = templates.get(name);
                    if (std == null) {
                        std = nodeTypeManager.getNodeType(name);
                    }
                    if (std != null && !std.isMixin()) {
                        needsNtBase = false;
                    }
                }
                if (needsNtBase) {
                    String[] withNtBase = new String[supertypes.length + 1];
                    withNtBase[0] = NT_BASE;
                    System.arraycopy(supertypes, 0, withNtBase, 1, supertypes.length);
                    ntt.setDeclaredSuperTypeNames(withNtBase);
                }
            }
        }
    }

    // -----------------------------------------------------< private >---

    private static NodeType[] toArray(NodeTypeIterator nodeTypes) {
        ArrayList<NodeType> nts = new ArrayList<NodeType>();

        while (nodeTypes.hasNext()) {
            nts.add(nodeTypes.nextNodeType());
        }

        return nts.toArray(new NodeType[nts.size()]);
    }

}
