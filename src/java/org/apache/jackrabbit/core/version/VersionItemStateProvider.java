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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.AbstractVISProvider;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * This Class implements a virtual item state provider, in order to expose the
 * versions to the version storage.
 */
public class VersionItemStateProvider extends AbstractVISProvider {
    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionItemStateProvider.class);

    /**
     * the version manager
     */
    private final VersionManager vMgr;

    /**
     * node def id for a version node state
     */
    private NodeDefId NDEF_VERSION;

    /**
     * node def id for a version history node state
     */
    private NodeDefId NDEF_VERSION_HISTORY;

    /**
     * node def id for a version history root node state
     */
    private NodeDefId NDEF_VERSION_HISTORY_ROOT;

    /**
     * node def id for a version labels node state
     */
    private NodeDefId NDEF_VERSION_LABELS;

    /**
     * the parent id
     */
    private final String parentId;

    /**
     * creates a new version item state provide
     *
     * @param vMgr
     * @param rootId
     * @param parentId
     * @throws RepositoryException
     */
    public VersionItemStateProvider(VersionManager vMgr, NodeTypeRegistry ntReg,
                                    String rootId, String parentId)
            throws RepositoryException {
        super(ntReg, new NodeId(rootId));
        this.vMgr = vMgr;
        this.parentId = parentId;
        NDEF_VERSION = ntReg.getEffectiveNodeType(NT_VERSIONHISTORY).getApplicableChildNodeDef(JCR_ROOTVERSION, NT_VERSION).getId();
        NDEF_VERSION_HISTORY =
            ntReg.getEffectiveNodeType(REP_VERSIONSTORAGE).getApplicableChildNodeDef(JCR_ROOTVERSION, NT_VERSIONHISTORY).getId();
        NDEF_VERSION_HISTORY_ROOT =
            ntReg.getEffectiveNodeType(REP_SYSTEM).getApplicableChildNodeDef(JCR_VERSIONSTORAGE, REP_VERSIONSTORAGE).getId();
        NDEF_VERSION_LABELS =
            ntReg.getEffectiveNodeType(NT_VERSIONHISTORY).getApplicableChildNodeDef(JCR_VERSIONLABELS, NT_VERSIONLABELS).getId();
    }

    /**
     * {@inheritDoc}
     */
    protected VirtualNodeState createRootNodeState() throws RepositoryException {
        VirtualNodeState root = new HistoryRootNodeState(this, vMgr, parentId, rootNodeId.getUUID());
        root.setDefinitionId(NDEF_VERSION_HISTORY_ROOT);
        return root;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setNodeReferences(NodeReferences refs) {
        try {
            InternalVersionItem vi = vMgr.getItem(refs.getUUID());
            if (vi != null) {
                vMgr.setItemReferences(vi, refs.getReferences());
                return true;
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {
        try {
            InternalVersionItem vi = vMgr.getItem(id.getUUID());
            if (vi != null) {
                // todo: add caching
                NodeReferences ref = new NodeReferences(id);
                ref.addAllReferences(vMgr.getItemReferences(vi));
                // check for versionstorage internal references
                if (vi instanceof InternalVersion) {
                    InternalVersion v = (InternalVersion) vi;
                    InternalVersion[] suc = v.getSuccessors();
                    for (int i = 0; i < suc.length; i++) {
                        InternalVersion s = suc[i];
                        ref.addReference(new PropertyId(s.getId(), JCR_PREDECESSORS));
                    }
                    InternalVersion[] pred = v.getPredecessors();
                    for (int i = 0; i < pred.length; i++) {
                        InternalVersion p = pred[i];
                        ref.addReference(new PropertyId(p.getId(), JCR_SUCCESSORS));
                    }
                }

                return ref;
            }
        } catch (RepositoryException e) {
            // ignore
        }
        throw new NoSuchItemStateException(id.getUUID());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeReferencesId id) {
        return vMgr.hasItem(id.getUUID());
    }

    /**
     * {@inheritDoc}
     */
    protected boolean internalHasNodeState(NodeId id) {
        return vMgr.hasItem(id.getUUID());
    }

    /**
     * {@inheritDoc}
     */
    protected VirtualNodeState internalGetNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        VirtualNodeState state;
        try {
            InternalVersionItem vi = vMgr.getItem(id.getUUID());
            if (vi instanceof InternalVersionHistory) {
                InternalVersionHistory vh = (InternalVersionHistory) vi;
                state = new VersionHistoryNodeState(this, vh, rootNodeId.getUUID());
                state.setDefinitionId(NDEF_VERSION_HISTORY);
                // add version labels node state
                String uuid = vh.getVersionLabelsUUID();
                state.addChildNodeEntry(JCR_VERSIONLABELS, uuid);

            } else if (vi instanceof InternalVersionLabels) {
                InternalVersionLabels vl = (InternalVersionLabels) vi;
                state = new VersionLabelsNodeState(this, (InternalVersionHistory) vl.getParent(), vl.getParent().getId(), vi.getId());
                state.setDefinitionId(NDEF_VERSION_LABELS);

            } else if (vi instanceof InternalVersion) {
                InternalVersion v = (InternalVersion) vi;
                state = new VersionNodeState(this, v, vi.getParent().getId());
                state.setDefinitionId(NDEF_VERSION);
                state.setPropertyValue(JCR_CREATED, InternalValue.create(v.getCreated()));
                state.setPropertyValues(JCR_PREDECESSORS, PropertyType.REFERENCE, InternalValue.EMPTY_ARRAY);
                state.setPropertyValues(JCR_SUCCESSORS, PropertyType.REFERENCE, InternalValue.EMPTY_ARRAY);

            } else if (vi instanceof InternalFrozenNode) {
                InternalFrozenNode fn = (InternalFrozenNode) vi;
                VirtualNodeState parent = (VirtualNodeState) getItemState(new NodeId(fn.getParent().getId()));
                boolean mimicFrozen = !(parent instanceof VersionNodeState);
                state = createNodeState(parent,
                        JCR_FROZENNODE,
                        id.getUUID(),
                        mimicFrozen ? fn.getFrozenPrimaryType() : NT_FROZENNODE);
                mapFrozenNode(state, fn, mimicFrozen);

            } else if (vi instanceof InternalFrozenVersionHistory) {
                InternalFrozenVersionHistory fn = (InternalFrozenVersionHistory) vi;
                VirtualNodeState parent = (VirtualNodeState) getItemState(new NodeId(fn.getParent().getId()));
                state = createNodeState(parent,
                        fn.getName(),
                        id.getUUID(),
                        NT_VERSIONEDCHILD);
                // IMO, this should be exposed aswell
                // state.setPropertyValue(JCR_BASEVERSION, InternalValue.create(UUID.fromString(fn.getBaseVersionId())));
                state.setPropertyValue(JCR_CHILDVERSIONHISTORY, InternalValue.create(UUID.fromString(fn.getVersionHistoryId())));
            } else {
                // not found, throw
                throw new NoSuchItemStateException(id.toString());
            }
        } catch (RepositoryException e) {
            log.error("Unable to check for item:" + e.toString());
            throw new ItemStateException(e);
        }
        return state;
    }

    //-----------------------------------------------------< internal stuff >---

    /**
     * maps a frozen node
     *
     * @param state
     * @param node
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState mapFrozenNode(VirtualNodeState state,
                                           InternalFrozenNode node,
                                           boolean mimicFrozen)
            throws RepositoryException {

        if (mimicFrozen) {
            if (node.getFrozenUUID() != null) {
                state.setPropertyValue(JCR_UUID, InternalValue.create(node.getFrozenUUID()));
            }
            state.setPropertyValues(JCR_MIXINTYPES, PropertyType.NAME, InternalValue.create(node.getFrozenMixinTypes()));
        } else {
            state.setPropertyValue(JCR_UUID, InternalValue.create(node.getId()));
            if (node.getFrozenUUID() != null) {
                state.setPropertyValue(JCR_FROZENUUID, InternalValue.create(node.getFrozenUUID()));
            }
            state.setPropertyValue(JCR_FROZENPRIMARYTYPE, InternalValue.create(node.getFrozenPrimaryType()));
            state.setPropertyValues(JCR_FROZENMIXINTYPES, PropertyType.NAME, InternalValue.create(node.getFrozenMixinTypes()));
        }

        // map properties
        PropertyState[] props = node.getFrozenProperties();
        for (int i = 0; i < props.length; i++) {
            if (props[i].isMultiValued()) {
                state.setPropertyValues(props[i].getName(), props[i].getType(), props[i].getValues());
            } else {
                state.setPropertyValue(props[i].getName(), props[i].getValues()[0]);
            }
        }
        // map child nodes
        InternalFreeze[] nodes = node.getFrozenChildNodes();
        for (int i = 0; i < nodes.length; i++) {
            state.addChildNodeEntry(nodes[i].getName(), nodes[i].getId());
        }
        return state;
    }

}
