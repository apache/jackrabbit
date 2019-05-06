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
package org.apache.jackrabbit.core.nodetype.virtual;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.AbstractVISProvider;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * This Class implements a virtual item state provider that exposes the
 * registered nodetypes.
 */
public class VirtualNodeTypeStateProvider extends AbstractVISProvider {

    /**
     * the parent id
     */
    private final NodeId parentId;

    /**
     * @param ntReg
     * @param rootNodeId
     * @param parentId
     */
    public VirtualNodeTypeStateProvider(NodeTypeRegistry ntReg,
                                        NodeId rootNodeId, NodeId parentId) {
        super(ntReg, rootNodeId);
        this.parentId = parentId;
        try {
            getRootState();
        } catch (ItemStateException e) {
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * currently we have no dynamic ones, we just recreate the entire nodetypes tree
     */
    protected VirtualNodeState createRootNodeState() throws RepositoryException {
        VirtualNodeState root = new VirtualNodeState(this, parentId, rootNodeId, NameConstants.REP_NODETYPES, null);
        Name[] ntNames = ntReg.getRegisteredNodeTypes();
        for (int i = 0; i < ntNames.length; i++) {
            QNodeTypeDefinition ntDef = ntReg.getNodeTypeDef(ntNames[i]);
            VirtualNodeState ntState = createNodeTypeState(root, ntDef);
            root.addChildNodeEntry(ntNames[i], ntState.getNodeId());
            // add as hard reference
            root.addStateReference(ntState);
        }
        return root;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean internalHasNodeState(NodeId id) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    protected VirtualNodeState internalGetNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException {
        return null;
    }

    public void onNodeTypeAdded(Name ntName) {
        discardAll(); // TODO: More efficient reloading
    }

    public void onNodeTypeModified(Name ntName) {
        discardAll(); // TODO: More efficient reloading
    }

    public void onNodeTypesRemoved(Collection<Name> names) {
        discardAll(); // TODO: More efficient reloading
    }

    /**
     * Creates a node type state
     *
     * @param parent
     * @param ntDef
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState createNodeTypeState(VirtualNodeState parent,
                                                 QNodeTypeDefinition ntDef)
            throws RepositoryException {
        NodeId id = calculateStableId(ntDef.getName().toString());
        VirtualNodeState ntState = createNodeState(parent, ntDef.getName(), id, NameConstants.NT_NODETYPE);

        // add properties
        ntState.setPropertyValue(NameConstants.JCR_NODETYPENAME, InternalValue.create(ntDef.getName()));
        ntState.setPropertyValues(NameConstants.JCR_SUPERTYPES, PropertyType.NAME, InternalValue.create(ntDef.getSupertypes()));
        ntState.setPropertyValue(NameConstants.JCR_ISMIXIN, InternalValue.create(ntDef.isMixin()));
        ntState.setPropertyValue(NameConstants.JCR_HASORDERABLECHILDNODES, InternalValue.create(ntDef.hasOrderableChildNodes()));
        if (ntDef.getPrimaryItemName() != null) {
            ntState.setPropertyValue(NameConstants.JCR_PRIMARYITEMNAME, InternalValue.create(ntDef.getPrimaryItemName()));
        }

        // add property defs
        QPropertyDefinition[] propDefs = ntDef.getPropertyDefs();
        for (int i = 0; i < propDefs.length; i++) {
            VirtualNodeState pdState = createPropertyDefState(ntState, propDefs[i], ntDef, i);
            ntState.addChildNodeEntry(NameConstants.JCR_PROPERTYDEFINITION, pdState.getNodeId());
            // add as hard reference
            ntState.addStateReference(pdState);
        }

        // add child node defs
        QNodeDefinition[] cnDefs = ntDef.getChildNodeDefs();
        for (int i = 0; i < cnDefs.length; i++) {
            VirtualNodeState cnState = createChildNodeDefState(ntState, cnDefs[i], ntDef, i);
            ntState.addChildNodeEntry(NameConstants.JCR_CHILDNODEDEFINITION, cnState.getNodeId());
            // add as hard reference
            ntState.addStateReference(cnState);
        }

        return ntState;
    }

    /**
     * creates a node state for the given property def
     *
     * @param parent
     * @param propDef
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState createPropertyDefState(VirtualNodeState parent,
                                                    QPropertyDefinition propDef,
                                                    QNodeTypeDefinition ntDef, int n)
            throws RepositoryException {
        NodeId id = calculateStableId(
                ntDef.getName().toString() + "/" + NameConstants.JCR_PROPERTYDEFINITION.toString() + "/" + n);
        VirtualNodeState pState = createNodeState(
                parent, NameConstants.JCR_PROPERTYDEFINITION, id,
                NameConstants.NT_PROPERTYDEFINITION);
        // add properties
        if (!propDef.definesResidual()) {
            pState.setPropertyValue(NameConstants.JCR_NAME, InternalValue.create(propDef.getName()));
        }
        pState.setPropertyValue(NameConstants.JCR_AUTOCREATED, InternalValue.create(propDef.isAutoCreated()));
        pState.setPropertyValue(NameConstants.JCR_MANDATORY, InternalValue.create(propDef.isMandatory()));
        pState.setPropertyValue(NameConstants.JCR_ONPARENTVERSION,
                InternalValue.create(OnParentVersionAction.nameFromValue(propDef.getOnParentVersion())));
        pState.setPropertyValue(NameConstants.JCR_PROTECTED, InternalValue.create(propDef.isProtected()));
        pState.setPropertyValue(NameConstants.JCR_MULTIPLE, InternalValue.create(propDef.isMultiple()));
        pState.setPropertyValue(
                NameConstants.JCR_REQUIREDTYPE,
                InternalValue.create(PropertyType.nameFromValue(propDef.getRequiredType()).toUpperCase()));
        InternalValue[] defVals = InternalValue.create(propDef.getDefaultValues());
        // retrieve the property type from the first default value present with
        // the property definition. in case no default values are defined,
        // fallback to PropertyType.STRING in order to avoid creating a property
        // with type UNDEFINED which is illegal.
        int defValsType = PropertyType.STRING;
        if (defVals != null && defVals.length > 0) {
            defValsType = defVals[0].getType();
        }
        if (defVals != null) {
            pState.setPropertyValues(NameConstants.JCR_DEFAULTVALUES, defValsType, defVals);
        }
        QValueConstraint[] vc = propDef.getValueConstraints();
        InternalValue[] vals = new InternalValue[vc.length];
        for (int i = 0; i < vc.length; i++) {
            vals[i] = InternalValue.create(vc[i].getString());
        }
        pState.setPropertyValues(NameConstants.JCR_VALUECONSTRAINTS, PropertyType.STRING, vals);
        return pState;
    }

    /**
     * creates a node state for the given child node def
     *
     * @param parent
     * @param cnDef
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState createChildNodeDefState(VirtualNodeState parent,
                                                     QNodeDefinition cnDef,
                                                     QNodeTypeDefinition ntDef, int n)
            throws RepositoryException {
        NodeId id = calculateStableId(
                ntDef.getName().toString() + "/" + NameConstants.JCR_CHILDNODEDEFINITION.toString() + "/" + n);
        VirtualNodeState pState = createNodeState(
                parent, NameConstants.JCR_CHILDNODEDEFINITION, id, NameConstants.NT_CHILDNODEDEFINITION);
        // add properties
        if (!cnDef.definesResidual()) {
            pState.setPropertyValue(NameConstants.JCR_NAME, InternalValue.create(cnDef.getName()));
        }
        pState.setPropertyValue(NameConstants.JCR_AUTOCREATED, InternalValue.create(cnDef.isAutoCreated()));
        pState.setPropertyValue(NameConstants.JCR_MANDATORY, InternalValue.create(cnDef.isMandatory()));
        pState.setPropertyValue(NameConstants.JCR_ONPARENTVERSION,
                InternalValue.create(OnParentVersionAction.nameFromValue(cnDef.getOnParentVersion())));
        pState.setPropertyValue(NameConstants.JCR_PROTECTED, InternalValue.create(cnDef.isProtected()));
        pState.setPropertyValues(NameConstants.JCR_REQUIREDPRIMARYTYPES,
                PropertyType.NAME, InternalValue.create(cnDef.getRequiredPrimaryTypes()));
        if (cnDef.getDefaultPrimaryType() != null) {
            pState.setPropertyValue(NameConstants.JCR_DEFAULTPRIMARYTYPE, InternalValue.create(cnDef.getDefaultPrimaryType()));
        }
        pState.setPropertyValue(NameConstants.JCR_SAMENAMESIBLINGS, InternalValue.create(cnDef.allowsSameNameSiblings()));
        return pState;
    }

    /**
     * Calculates a stable identifier out of the given string. The algorithm
     * does a MD5 digest from the string an converts it into the UUID format.
     *
     * @param name
     * @return
     * @throws RepositoryException
     */
    private static NodeId calculateStableId(String name) throws RepositoryException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(name.getBytes(StandardCharsets.UTF_8));
            return new NodeId(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean setNodeReferences(ChangeLog references) {
        return false;
    }
}
