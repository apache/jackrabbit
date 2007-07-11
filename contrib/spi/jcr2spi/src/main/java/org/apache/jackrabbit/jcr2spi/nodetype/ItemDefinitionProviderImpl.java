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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.name.QName;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>ItemDefinitionManagerImpl</code>...
 */
public class ItemDefinitionProviderImpl implements ItemDefinitionProvider {

    private static Logger log = LoggerFactory.getLogger(ItemDefinitionProviderImpl.class);

    private final EffectiveNodeTypeProvider entProvider;
    private final RepositoryService service;
    private final SessionInfo sessionInfo;
    private QNodeDefinition rootNodeDefinition;

    public ItemDefinitionProviderImpl(EffectiveNodeTypeProvider entProvider,
                                      RepositoryService service,
                                      SessionInfo sessionInfo) {
        this.entProvider = entProvider;
        this.service = service;
        this.sessionInfo = sessionInfo;
    }

    /**
     * @inheritDoc
     */
    public QNodeDefinition getRootNodeDefinition() throws RepositoryException {
        if (rootNodeDefinition == null) {
            rootNodeDefinition = service.getNodeDefinition(
                    sessionInfo, service.getRootId(sessionInfo));
        }
        return rootNodeDefinition;
    }

    /**
     * @inheritDoc
     */
    public QNodeDefinition getQNodeDefinition(NodeState nodeState) throws RepositoryException {
        if (nodeState.getHierarchyEntry().getParent() == null) {
            return getRootNodeDefinition();
        }
        QNodeDefinition definition;
        try {
            /*
             Don't use 'getEffectiveNodeType(NodeState) here:
             for NEW-states the definition is always set upon creation.
             for all other states the definion must be retrieved only taking
             the effective nodetypes present on the parent into account
             any kind of transiently added mixins must not have an effect
             on the definition retrieved for an state that has been persisted
             before. The effective NT must be evaluated as if it had been
             evaluated upon creating the workspace state.
             */
            EffectiveNodeType ent = entProvider.getEffectiveNodeType(nodeState.getParent().getNodeTypeNames());
            EffectiveNodeType entTarget = getEffectiveNodeType(nodeState.getNodeTypeName());
            definition = getQNodeDefinition(ent, entTarget, nodeState.getQName());
        } catch (RepositoryException e) {
            definition = service.getNodeDefinition(sessionInfo, nodeState.getNodeEntry().getWorkspaceId());
        } catch (NodeTypeConflictException e) {
            definition = service.getNodeDefinition(sessionInfo, nodeState.getNodeEntry().getWorkspaceId());
        }
        return definition;
    }

   /**
     * @inheritDoc
     */
    public QNodeDefinition getQNodeDefinition(NodeState parentState, QName name, QName nodeTypeName)
            throws NoSuchNodeTypeException, ConstraintViolationException {
       EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentState);
       EffectiveNodeType entTarget = getEffectiveNodeType(nodeTypeName);
       return getQNodeDefinition(ent, entTarget, name);
    }

    /**
     * @inheritDoc
     */
    public QNodeDefinition getQNodeDefinition(EffectiveNodeType ent, QName name, QName nodeTypeName) throws NoSuchNodeTypeException, ConstraintViolationException {
        EffectiveNodeType entTarget = getEffectiveNodeType(nodeTypeName);
        return getQNodeDefinition(ent, entTarget, name);
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition getQPropertyDefinition(PropertyState propertyState) throws RepositoryException {
        QPropertyDefinition definition;
        try {
            /*
             Don't use 'getEffectiveNodeType(NodeState) here:
             for NEW-states the definition is always set upon creation.
             for all other states the definion must be retrieved only taking
             the effective nodetypes present on the parent into account
             any kind of transiently added mixins must not have an effect
             on the definition retrieved for an state that has been persisted
             before. The effective NT must be evaluated as if it had been
             evaluated upon creating the workspace state.
             */
            EffectiveNodeType ent = entProvider.getEffectiveNodeType(propertyState.getParent().getNodeTypeNames());
            definition = getQPropertyDefinition(ent, propertyState.getQName(), propertyState.getType(), propertyState.isMultiValued());
        } catch (RepositoryException e) {
            definition = service.getPropertyDefinition(sessionInfo, ((PropertyEntry) propertyState.getHierarchyEntry()).getWorkspaceId());
        } catch (NodeTypeConflictException e) {
            definition = service.getPropertyDefinition(sessionInfo, ((PropertyEntry) propertyState.getHierarchyEntry()).getWorkspaceId());
        }
        return definition;
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition getQPropertyDefinition(QName ntName, QName propName,
                                                      int type, boolean multiValued)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        EffectiveNodeType ent = entProvider.getEffectiveNodeType(ntName);
        return getQPropertyDefinition(ent, propName, type, multiValued);
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition getQPropertyDefinition(NodeState parentState,
                                                      QName name, int type,
                                                      boolean multiValued)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentState);
        return getQPropertyDefinition(ent, name, type, multiValued);
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition getQPropertyDefinition(NodeState parentState,
                                                      QName name, int type)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentState);
        return getQPropertyDefinition(ent, name, type);
    }

    //--------------------------------------------------------------------------
    private EffectiveNodeType getEffectiveNodeType(QName ntName) throws NoSuchNodeTypeException {
        if (ntName != null) {
            return entProvider.getEffectiveNodeType(ntName);
        } else {
            return null;
        }
    }

    /**
     *
     * @param ent
     * @param entTarget
     * @param name
     * @return
     * @throws ConstraintViolationException
     */
    static QNodeDefinition getQNodeDefinition(EffectiveNodeType ent,
                                              EffectiveNodeType entTarget,
                                              QName name)
            throws ConstraintViolationException {

        // try named node definitions first
        QNodeDefinition[] defs = ent.getNamedQNodeDefinitions(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                QNodeDefinition nd = defs[i];
                // node definition with that name exists
                if (entTarget != null && nd.getRequiredPrimaryTypes() != null) {
                    // check 'required primary types' constraint
                    if (entTarget.includesNodeTypes(nd.getRequiredPrimaryTypes())) {
                        // found named node definition
                        return nd;
                    }
                } else {
                    if (nd.getDefaultPrimaryType() != null) {
                        // found node definition with default node type
                        return nd;
                    }
                }
            }
        }

        // no item with that name defined;
        // try residual node definitions
        QNodeDefinition[] nda = ent.getUnnamedQNodeDefinitions();
        for (int i = 0; i < nda.length; i++) {
            QNodeDefinition nd = nda[i];
            if (entTarget != null && nd.getRequiredPrimaryTypes() != null) {
                // check 'required primary types' constraint
                if (entTarget.includesNodeTypes(nd.getRequiredPrimaryTypes())) {
                    // found residual node definition
                    return nd;
                }
            } else {
                // since no node type has been specified for the new node,
                // it must be determined from the default node type;
                if (nd.getDefaultPrimaryType() != null) {
                    // found residual node definition with default node type
                    return nd;
                }
            }
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching child node definition found for " + name);
    }

   /**
     *
     * @param ent
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws ConstraintViolationException
     */
    private static QPropertyDefinition getQPropertyDefinition(EffectiveNodeType ent,
                                                              QName name, int type,
                                                              boolean multiValued)
           throws ConstraintViolationException {
        // try named property definitions first
        QPropertyDefinition[] defs = ent.getNamedQPropertyDefinitions(name);
        QPropertyDefinition match = getMatchingPropDef(defs, type, multiValued);
        if (match != null) {
            return match;
        }

        // no item with that name defined;
        // try residual property definitions
        defs = ent.getUnnamedQPropertyDefinitions();
        match = getMatchingPropDef(defs, type, multiValued);
        if (match != null) {
            return match;
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching property definition found for " + name);
    }

    /**
     *
     * @param ent
     * @param name
     * @param type
     * @return
     * @throws ConstraintViolationException
     */
    private static QPropertyDefinition getQPropertyDefinition(EffectiveNodeType ent,
                                                              QName name, int type)
            throws ConstraintViolationException {
        // try named property definitions first
        QPropertyDefinition[] defs = ent.getNamedQPropertyDefinitions(name);
        QPropertyDefinition match = getMatchingPropDef(defs, type);
        if (match != null) {
            return match;
        }

        // no item with that name defined;
        // try residual property definitions
        defs = ent.getUnnamedQPropertyDefinitions();
        match = getMatchingPropDef(defs, type);
        if (match != null) {
            return match;
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching property definition found for " + name);
    }

    private static QPropertyDefinition getMatchingPropDef(QPropertyDefinition[] defs, int type) {
        QPropertyDefinition match = null;
        for (int i = 0; i < defs.length; i++) {
            QItemDefinition qDef = defs[i];
            if (!qDef.definesNode()) {
                QPropertyDefinition pd = (QPropertyDefinition) qDef;
                int reqType = pd.getRequiredType();
                // match type
                if (reqType == PropertyType.UNDEFINED
                        || type == PropertyType.UNDEFINED
                        || reqType == type) {
                    if (match == null) {
                        match = pd;
                    } else {
                        // check if this definition is a better match than
                        // the one we've already got
                        if (match.getRequiredType() != pd.getRequiredType()) {
                            if (match.getRequiredType() == PropertyType.UNDEFINED) {
                                // found better match
                                match = pd;
                            }
                        } else {
                            if (match.isMultiple() && !pd.isMultiple()) {
                                // found better match
                                match = pd;
                            }
                        }
                    }
                    if (match.getRequiredType() != PropertyType.UNDEFINED
                            && !match.isMultiple()) {
                        // found best possible match, get outta here
                        return match;
                    }
                }
            }
        }
        return match;
    }

    private static QPropertyDefinition getMatchingPropDef(QPropertyDefinition[] defs, int type,
                                                   boolean multiValued) {
        QPropertyDefinition match = null;
        for (int i = 0; i < defs.length; i++) {
            QItemDefinition qDef = defs[i];
            if (!qDef.definesNode()) {
                QPropertyDefinition pd = (QPropertyDefinition) qDef;
                int reqType = pd.getRequiredType();
                // match type
                if (reqType == PropertyType.UNDEFINED
                        || type == PropertyType.UNDEFINED
                        || reqType == type) {
                    // match multiValued flag
                    if (multiValued == pd.isMultiple()) {
                        // found match
                        if (pd.getRequiredType() != PropertyType.UNDEFINED) {
                            // found best possible match, get outta here
                            return pd;
                        } else {
                            if (match == null) {
                                match = pd;
                            }
                        }
                    }
                }
            }
        }
        return match;
    }
}