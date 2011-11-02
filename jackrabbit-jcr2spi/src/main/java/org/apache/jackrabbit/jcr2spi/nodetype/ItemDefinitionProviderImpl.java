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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public QNodeDefinition getRootNodeDefinition() throws RepositoryException {
        if (rootNodeDefinition == null) {
            IdFactory idFactory = service.getIdFactory();
            PathFactory pf = service.getPathFactory();

            rootNodeDefinition = service.getNodeDefinition(
                    sessionInfo, idFactory.createNodeId((String) null, pf.getRootPath()));
        }
        return rootNodeDefinition;
    }

    public QNodeDefinition getQNodeDefinition(Name[] parentNodeTypeNames,
                                              Name nodeName, Name ntName,
                                              NodeId nodeId) throws RepositoryException {
        if (parentNodeTypeNames == null) {
            return getRootNodeDefinition();
        }
        QNodeDefinition definition;
        try {
            EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentNodeTypeNames);
            EffectiveNodeType entTarget = getEffectiveNodeType(ntName);
            definition = getQNodeDefinition(ent, entTarget, nodeName);
        } catch (RepositoryException e) {
            log.debug("Cannot determine effective node type of {}: {}", nodeId, e);
            definition = getNodeDefinition(service, sessionInfo, nodeId);
        }
        return definition;
    }

   public QNodeDefinition getQNodeDefinition(Name[] parentNodeTypeNames, Name name, Name nodeTypeName)
            throws NoSuchNodeTypeException, ConstraintViolationException {
       EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentNodeTypeNames);
       EffectiveNodeType entTarget = getEffectiveNodeType(nodeTypeName);
       return getQNodeDefinition(ent, entTarget, name);
    }

    public QNodeDefinition getQNodeDefinition(EffectiveNodeType ent, Name name, Name nodeTypeName) throws NoSuchNodeTypeException, ConstraintViolationException {
        EffectiveNodeType entTarget = getEffectiveNodeType(nodeTypeName);
        return getQNodeDefinition(ent, entTarget, name);
    }

    public QPropertyDefinition getQPropertyDefinition(Name[] parentNodeTypeNames,
                                                      Name propertyName,
                                                      int propertyType,
                                                      boolean isMultiValued,
                                                      PropertyId propertyId) throws RepositoryException {
        QPropertyDefinition definition;
        try {
            EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentNodeTypeNames);
            definition = getQPropertyDefinition(ent, propertyName, propertyType, isMultiValued, true);
        } catch (RepositoryException e) {
            log.debug("Cannot determine property definition of {}: {}", propertyId, e);
            definition = getPropertyDefinition(service, sessionInfo, propertyId);
        }
        return definition;
    }

    public QPropertyDefinition getQPropertyDefinition(Name ntName, Name propName,
                                                      int type, boolean multiValued)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        EffectiveNodeType ent = entProvider.getEffectiveNodeType(ntName);
        return getQPropertyDefinition(ent, propName, type, multiValued, false);
    }

    public QPropertyDefinition getQPropertyDefinition(Name[] parentNodeTypeNames,
                                                      Name name, int type,
                                                      boolean multiValued)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentNodeTypeNames);
        return getQPropertyDefinition(ent, name, type, multiValued, false);
    }

    public QPropertyDefinition getQPropertyDefinition(Name[] parentNodeTypeNames,
                                                      Name name, int type)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        EffectiveNodeType ent = entProvider.getEffectiveNodeType(parentNodeTypeNames);
        return getQPropertyDefinition(ent, name, type);
    }

    //--------------------------------------------------------------------------
    private EffectiveNodeType getEffectiveNodeType(Name ntName) throws NoSuchNodeTypeException {
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
                                              Name name)
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
                                                              Name name, int type,
                                                              boolean multiValued, boolean throwWhenAmbiguous)
           throws ConstraintViolationException {
        // try named property definitions first
        QPropertyDefinition[] defs = ent.getNamedQPropertyDefinitions(name);
        QPropertyDefinition match = getMatchingPropDef(defs, type, multiValued, throwWhenAmbiguous);
        if (match != null) {
            return match;
        }

        // no item with that name defined;
        // try residual property definitions
        defs = ent.getUnnamedQPropertyDefinitions();
        match = getMatchingPropDef(defs, type, multiValued, throwWhenAmbiguous);
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
                                                              Name name, int type)
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
                                                   boolean multiValued, boolean throwWhenAmbiguous)
        throws ConstraintViolationException {
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
                            if (match != null && throwWhenAmbiguous) {
                                throw new ConstraintViolationException("ambiguous property definitions found: " + match + " vs " + pd);
                            }

                            // If we already found a match, and that was of PropertyType.STRING,
                            // then do not overwrite it. The whole reason there are multiple
                            // potential matches is that the client did not specify the type,
                            // thus obviously specified a String.
                            if (match == null || match.getRequiredType() != PropertyType.STRING) {
                                // found best possible match
                                match = pd;
                            }
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

    private static QNodeDefinition getNodeDefinition(RepositoryService service, SessionInfo sessionInfo,
            NodeId nodeId) throws RepositoryException {

        try {
            return service.getNodeDefinition(sessionInfo, nodeId);
        }
        catch (RepositoryException e) {
            log.error("Cannot determine node definition of {}: {}", nodeId, e);
            throw e;
        }
    }

    private static QPropertyDefinition getPropertyDefinition(RepositoryService service,
            SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException {

        try {
            return service.getPropertyDefinition(sessionInfo, propertyId);
        }
        catch (RepositoryException e) {
            log.error("Cannot determine property definition of {}: {}", propertyId, e);
            throw e;
        }
    }

}
