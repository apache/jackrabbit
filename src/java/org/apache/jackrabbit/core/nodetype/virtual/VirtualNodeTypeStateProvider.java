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
package org.apache.jackrabbit.core.nodetype.virtual;

import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.AbstractVISProvider;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistryListener;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;

/**
 * This Class implements a virtual item state provider that exposes the
 * registered nodetypes.
 */
public class VirtualNodeTypeStateProvider extends AbstractVISProvider implements NodeTypeRegistryListener {

    /**
     * the parent id
     */
    private final String parentId;

    /**
     *
     * @param ntReg
     * @param rootNodeId
     * @param parentId
     */
    public VirtualNodeTypeStateProvider(NodeTypeRegistry ntReg, String rootNodeId, String parentId) {
        super(ntReg, new NodeId(rootNodeId));
        this.parentId = parentId;
        ntReg.addListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * currently we have no dynamic ones, we just recreate the entire nodetypes tree
     */
    protected VirtualNodeState createRootNodeState() throws RepositoryException {
        VirtualNodeState root = new VirtualNodeState(this, parentId, rootNodeId.getUUID(), REP_NODETYPES, null);
        QName[] ntNames = ntReg.getRegisteredNodeTypes();
        for (int i=0; i<ntNames.length; i++) {
            NodeTypeDef ntDef = ntReg.getNodeTypeDef(ntNames[i]);
            VirtualNodeState ntState = createNodeTypeState(root, ntDef);
            root.addChildNodeEntry(ntNames[i], ntState.getUUID());
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

    /**
     * {@inheritDoc}
     */
    public void nodeTypeRegistered(QName ntName) {
        // todo: do more efficient reloading
        try {
            getRootState().discard();
        } catch (ItemStateException e) {
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nodeTypeReRegistered(QName ntName) {
        // todo: do more efficient reloading
        try {
            getRootState().discard();
        } catch (ItemStateException e) {
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nodeTypeUnregistered(QName ntName) {
        // todo: do more efficient reloading
        try {
            getRootState().discard();
        } catch (ItemStateException e) {
            // ignore
        }
    }

    /**
     * Creates a node type state
     * @param parent
     * @param ntDef
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState createNodeTypeState(VirtualNodeState parent, NodeTypeDef ntDef) throws RepositoryException {
        VirtualNodeState ntState = createNodeState(parent, ntDef.getName(), null, NT_NODETYPE);

        // add properties
        ntState.setPropertyValue(JCR_NODETYPENAME, InternalValue.create(ntDef.getName()));
        ntState.setPropertyValues(JCR_SUPERTYPES, PropertyType.NAME, InternalValue.create(ntDef.getSupertypes()));
        ntState.setPropertyValue(JCR_ISMIXIN, InternalValue.create(ntDef.isMixin()));
        ntState.setPropertyValue(JCR_HASORDERABLECHILDNODES, InternalValue.create(ntDef.hasOrderableChildNodes()));
        if (ntDef.getPrimaryItemName() != null) {
            ntState.setPropertyValue(JCR_PRIMARYITEMNAME, InternalValue.create(ntDef.getPrimaryItemName()));
        }

        // add property defs
        PropDef[] propDefs = ntDef.getPropertyDefs();
        for (int i=0; i<propDefs.length; i++) {
            VirtualNodeState pdState = createPropertyDefState(ntState, propDefs[i]);
            ntState.addChildNodeEntry(JCR_PROPERTYDEF, pdState.getUUID());
            // add as hard reference
            ntState.addStateReference(pdState);
        }

        // add child node defs
        ChildNodeDef[] cnDefs = ntDef.getChildNodeDefs();
        for (int i=0; i<cnDefs.length; i++) {
            VirtualNodeState cnState = createChildNodeDefState(ntState, cnDefs[i]);
            ntState.addChildNodeEntry(JCR_CHILDNODEDEF, cnState.getUUID());
            // add as hard reference
            ntState.addStateReference(cnState);
        }

        return ntState;
    }

    /**
     * creates a node state for the given property def
     * @param parent
     * @param propDef
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState createPropertyDefState(VirtualNodeState parent, PropDef propDef) throws RepositoryException {
        VirtualNodeState pState = createNodeState(parent, JCR_PROPERTYDEF, null, NT_PROPERTYDEF);
        // add properties
        pState.setPropertyValue(JCR_NAME, InternalValue.create(propDef.getName()));
        pState.setPropertyValue(JCR_AUTOCREATE, InternalValue.create(propDef.isAutoCreate()));
        pState.setPropertyValue(JCR_MANDATORY, InternalValue.create(propDef.isMandatory()));
        pState.setPropertyValue(JCR_ONPARENTVERSION, InternalValue.create(OnParentVersionAction.nameFromValue(propDef.getOnParentVersion())));
        pState.setPropertyValue(JCR_PROTECTED, InternalValue.create(propDef.isProtected()));
        pState.setPropertyValue(JCR_MULTIPLE, InternalValue.create(propDef.isMultiple()));
        pState.setPropertyValue(JCR_REQUIREDTYPE, InternalValue.create(PropertyType.nameFromValue(propDef.getRequiredType())));
        pState.setPropertyValues(JCR_DEFAULTVALUES, PropertyType.STRING, propDef.getDefaultValues());
        ValueConstraint[] vc = propDef.getValueConstraints();
        InternalValue[] vals = new InternalValue[vc.length];
        for (int i=0; i<vc.length; i++) {
            vals[i] = InternalValue.create(vc[i].getDefinition());
        }
        pState.setPropertyValues(JCR_VALUECONSTRAINTS, PropertyType.STRING, vals);
        return pState;
    }

    /**
     * creates a node state for the given child node def
     * @param parent
     * @param cnDef
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState createChildNodeDefState(VirtualNodeState parent, ChildNodeDef cnDef) throws RepositoryException {
        VirtualNodeState pState = createNodeState(parent, JCR_CHILDNODEDEF, null, NT_CHILDNODEDEF);
        // add properties
        pState.setPropertyValue(JCR_NAME, InternalValue.create(cnDef.getName()));
        pState.setPropertyValue(JCR_AUTOCREATE, InternalValue.create(cnDef.isAutoCreate()));
        pState.setPropertyValue(JCR_MANDATORY, InternalValue.create(cnDef.isMandatory()));
        pState.setPropertyValue(JCR_ONPARENTVERSION, InternalValue.create(OnParentVersionAction.nameFromValue(cnDef.getOnParentVersion())));
        pState.setPropertyValue(JCR_PROTECTED, InternalValue.create(cnDef.isProtected()));
        pState.setPropertyValues(JCR_REQUIREDPRIMARYTYPES, PropertyType.NAME, InternalValue.create(cnDef.getRequiredPrimaryTypes()));
        if (cnDef.getDefaultPrimaryType() != null) {
            pState.setPropertyValue(JCR_DEFAULTPRIMARYTYPE, InternalValue.create(cnDef.getDefaultPrimaryType()));
        }
        pState.setPropertyValue(JCR_SAMENAMESIBS, InternalValue.create(cnDef.allowSameNameSibs()));
        return pState;
    }
}
