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

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NamespaceException;
import javax.jcr.Value;

import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.QValueValue;

/**
 * <code>NodeTypeDefinitionFactory</code> can be used to convert the internal
 * SPI node type definitions to JCR {@link NodeTypeDefinition}s.
 */
public class NodeTypeDefinitionFactory {

    private final NodeTypeManager ntMgr;

    private final NamePathResolver resolver;

    /**
     * Creates a new node type definition factory that operates on the given
     * session to create the templates.
     *
     * @param session repository session.
     * @throws RepositoryException if an error occurs.
     */
    public NodeTypeDefinitionFactory(Session session)
            throws RepositoryException {
        this.ntMgr = session.getWorkspace().getNodeTypeManager();
        this.resolver = new DefaultNamePathResolver(session);
    }

    /**
     * Create a list of {@link NodeTypeDefinition JCR node type definitions}
     * from a collection of {@link QNodeTypeDefinition}.
     *
     * @param defs the SPI node type definitions.
     * @return the JCR node type definitions.
     * @throws RepositoryException if an error occurs.
     */
    public List<NodeTypeDefinition> create(Collection<QNodeTypeDefinition> defs)
            throws RepositoryException {
        List<NodeTypeDefinition> list = new ArrayList<NodeTypeDefinition>(defs.size());
        for (QNodeTypeDefinition qNtd: defs) {
            list.add(create(qNtd));
        }
        return list;
    }

    /**
     * Create a new JCR node type definition from the given
     * <code>QNodeTypeDefinition</code>.
     *
     * @param qNtd A SPI node type definition.
     * @return the corresponding JCR node type definition.
     * @throws RepositoryException if an error occurs.
     */
    @SuppressWarnings("unchecked")
    public NodeTypeDefinition create(QNodeTypeDefinition qNtd)
            throws RepositoryException {
        NodeTypeTemplate nt = ntMgr.createNodeTypeTemplate();
        nt.setName(getJCRName(qNtd.getName()));
        nt.setDeclaredSuperTypeNames(getJCRNames(qNtd.getSupertypes()));
        nt.setAbstract(qNtd.isAbstract());
        nt.setMixin(qNtd.isMixin());
        nt.setOrderableChildNodes(qNtd.hasOrderableChildNodes());
        nt.setPrimaryItemName(getJCRName(qNtd.getPrimaryItemName()));
        nt.setQueryable(qNtd.isQueryable());
        List nodeDefs = nt.getNodeDefinitionTemplates();
        for (QNodeDefinition qNd: qNtd.getChildNodeDefs()) {
            nodeDefs.add(create(qNd));
        }
        List propDefs = nt.getPropertyDefinitionTemplates();
        for (QPropertyDefinition qPd: qNtd.getPropertyDefs()) {
            propDefs.add(create(qPd));
        }
        return nt;
    }

    /**
     * Create a new JCR node definition from the given <code>QNodeDefinition</code>.
     *
     * @param qNd A node definition.
     * @return The corresponding JCR node definition.
     * @throws RepositoryException if an error occurs.
     */
    public NodeDefinition create(QNodeDefinition qNd)
            throws RepositoryException {
        NodeDefinitionTemplate nt = ntMgr.createNodeDefinitionTemplate();
        nt.setName(getJCRName(qNd.getName()));
        nt.setAutoCreated(qNd.isAutoCreated());
        nt.setMandatory(qNd.isMandatory());
        nt.setOnParentVersion(qNd.getOnParentVersion());
        nt.setProtected(qNd.isProtected());
        nt.setSameNameSiblings(qNd.allowsSameNameSiblings());
        nt.setDefaultPrimaryTypeName(getJCRName(qNd.getDefaultPrimaryType()));
        nt.setRequiredPrimaryTypeNames(getJCRNames(qNd.getRequiredPrimaryTypes()));
        return nt;
    }

    /**
     * Create a new JCR property definition from the given <code>QPropertyDefinition</code>.
     *
     * @param qPd A SPI property definition.
     * @return the corresponding JCR property definition.
     * @throws RepositoryException if an error occurs.
     */
    public PropertyDefinition create(QPropertyDefinition qPd) throws RepositoryException {
        PropertyDefinitionTemplate pt = ntMgr.createPropertyDefinitionTemplate();
        pt.setName(getJCRName(qPd.getName()));
        pt.setAutoCreated(qPd.isAutoCreated());
        pt.setMandatory(qPd.isMandatory());
        pt.setOnParentVersion(qPd.getOnParentVersion());
        pt.setProtected(qPd.isProtected());
        pt.setRequiredType(qPd.getRequiredType());
        pt.setMultiple(qPd.isMultiple());
        pt.setFullTextSearchable(qPd.isFullTextSearchable());
        pt.setValueConstraints(createValueConstraints(qPd.getRequiredType(), qPd.getValueConstraints()));
        pt.setAvailableQueryOperators(qPd.getAvailableQueryOperators());
        pt.setQueryOrderable(qPd.isQueryOrderable());
        pt.setDefaultValues(createValues(qPd.getDefaultValues()));
        return pt;
    }

    private String[] getJCRNames(Name[] names) throws NamespaceException {
        if (names == null) {
            return null;
        }
        String[] ret = new String[names.length];
        for (int i=0; i<names.length; i++) {
            ret[i] = resolver.getJCRName(names[i]);
        }
        return ret;
    }

    private String getJCRName(Name name) throws NamespaceException {
        if (name == null) {
            return null;
        }
        return resolver.getJCRName(name);
    }

    private String[] createValueConstraints(int type, QValueConstraint[] qv)
            throws RepositoryException {
        String[] ret = new String[qv.length];
        for (int i=0; i<ret.length; i++) {
            try {
                ValueConstraint c = ValueConstraint.create(type, qv[i].getString());
                ret[i] = c.getDefinition(resolver);
            } catch (InvalidConstraintException e) {
                throw new RepositoryException("Internal error while converting value constraints.", e);
            }
        }
        return ret;
    }

    private Value[] createValues(QValue[] qv) {
        if (qv == null){
            return null;
        }
        Value[] ret = new Value[qv.length];
        for (int i=0; i<ret.length; i++) {
            ret[i] = new QValueValue(qv[i], resolver);
        }
        return ret;
    }

}