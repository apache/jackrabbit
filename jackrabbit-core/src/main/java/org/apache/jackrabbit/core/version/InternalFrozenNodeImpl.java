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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implements a <code>InternalFrozenNode</code>
 */
class InternalFrozenNodeImpl extends InternalFreezeImpl
        implements InternalFrozenNode {

    /**
     * checkin mode version.
     */
    private static final int MODE_VERSION = 0;

    /**
     * checkin mode copy. specifies, that the items are always copied.
     */
    private static final int MODE_COPY = 1;

    /**
     * mode flag specifies, that the mode should be recursed. otherwise i
     * will be redetermined by the opv.
     */
    private static final int MODE_COPY_RECURSIVE = 3;

    /**
     * the list of frozen properties
     */
    private PropertyState[] frozenProperties;

    /**
     * the frozen child nodes
     */
    private InternalFreeze[] frozenNodes = null;

    /**
     * the frozen uuid of the original node
     */
    private UUID frozenUUID = null;

    /**
     * the frozen primary type of the orginal node
     */
    private Name frozenPrimaryType = null;

    /**
     * the frozen list of mixin types of the original node
     */
    private Name[] frozenMixinTypes = null;

    /**
     * Creates a new frozen node based on the given persistance node.
     *
     * @param node
     * @throws javax.jcr.RepositoryException
     */
    public InternalFrozenNodeImpl(AbstractVersionManager vMgr, NodeStateEx node,
                                  InternalVersionItem parent)
            throws RepositoryException {
        super(vMgr, node, parent);

        // init the frozen properties
        PropertyState[] props;
        try {
            props = node.getProperties();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
        List propList = new ArrayList();

        for (int i = 0; i < props.length; i++) {
            PropertyState prop = props[i];
            if (prop.getName().equals(NameConstants.JCR_FROZENUUID)) {
                // special property
                InternalValue value =
                    node.getPropertyValue(NameConstants.JCR_FROZENUUID);
                // JCR-1803: The value should be a STRING, but older Jackrabbit
                // versions (< 1.1, see JCR-487) used REFERENCE values. Since
                // we do not automatically upgrade old content, we need to be
                // ready to handle both types of values here.
                if (value.getType() == PropertyType.STRING) {
                    frozenUUID = UUID.fromString(value.getString());
                } else {
                    frozenUUID = value.getUUID();
                }
            } else if (prop.getName().equals(NameConstants.JCR_FROZENPRIMARYTYPE)) {
                // special property
                frozenPrimaryType = node.getPropertyValue(NameConstants.JCR_FROZENPRIMARYTYPE).getQName();
            } else if (prop.getName().equals(NameConstants.JCR_FROZENMIXINTYPES)) {
                // special property
                InternalValue[] values = node.getPropertyValues(NameConstants.JCR_FROZENMIXINTYPES);
                if (values == null) {
                    frozenMixinTypes = new Name[0];
                } else {
                    frozenMixinTypes = new Name[values.length];
                    for (int j = 0; j < values.length; j++) {
                        frozenMixinTypes[j] = values[j].getQName();
                    }
                }
            } else if (prop.getName().equals(NameConstants.JCR_PRIMARYTYPE)) {
                // ignore
            } else if (prop.getName().equals(NameConstants.JCR_UUID)) {
                // ignore
            } else {
                propList.add(prop);
            }
        }
        frozenProperties = (PropertyState[]) propList.toArray(new PropertyState[propList.size()]);

        // do some checks
        if (frozenMixinTypes == null) {
            frozenMixinTypes = new Name[0];
        }
        if (frozenPrimaryType == null) {
            throw new RepositoryException("Illegal frozen node. Must have 'frozenPrimaryType'");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return node.getName();
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized InternalFreeze[] getFrozenChildNodes()
            throws VersionException {
        if (frozenNodes == null) {
            try {
                // maybe add iterator?
                List entries = node.getState().getChildNodeEntries();
                frozenNodes = new InternalFreeze[entries.size()];
                Iterator iter = entries.iterator();
                int i = 0;
                while (iter.hasNext()) {
                    ChildNodeEntry entry =
                            (ChildNodeEntry) iter.next();
                    frozenNodes[i++] = (InternalFreeze) vMgr.getItem(entry.getId());
                }
            } catch (RepositoryException e) {
                throw new VersionException("Unable to retrieve frozen child nodes", e);
            }
        }
        return frozenNodes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasFrozenHistory(UUID uuid) {
        try {
            NodeId id = new NodeId(uuid);
            InternalFreeze[] frozen = getFrozenChildNodes();
            for (int i = 0; i < frozen.length; i++) {
                if (frozen[i] instanceof InternalFrozenVersionHistory
                        && ((InternalFrozenVersionHistory) frozen[i])
                            .getVersionHistoryId().equals(id)) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState[] getFrozenProperties() {
        return frozenProperties;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getFrozenUUID() {
        return frozenUUID;
    }

    /**
     * {@inheritDoc}
     */
    public Name getFrozenPrimaryType() {
        return frozenPrimaryType;
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getFrozenMixinTypes() {
        return frozenMixinTypes;
    }

    /**
     * Checks-in a <code>src</code> node. It creates a new child node of
     * <code>parent</code> with the given <code>name</code> and adds the
     * source nodes properties according to their OPV value to the
     * list of frozen properties. It creates frozen child nodes for each child
     * node of <code>src</code> according to its OPV value.
     *
     * @param parent
     * @param name
     * @param src
     * @return
     * @throws RepositoryException
     */
    protected static NodeStateEx checkin(NodeStateEx parent, Name name,
                                         NodeImpl src)
            throws RepositoryException {
        return checkin(parent, name, src, MODE_VERSION);
    }

    /**
     * Checks-in a <code>src</code> node. It creates a new child node of
     * <code>parent</code> with the given <code>name</code> and adds the
     * source nodes properties according to their OPV value to the
     * list of frozen properties. It creates frozen child nodes for each child
     * node of <code>src</code> according to its OPV value.
     *
     * @param parent
     * @param name
     * @param src
     * @return
     * @throws RepositoryException
     */
    private static NodeStateEx checkin(NodeStateEx parent, Name name,
                                       NodeImpl src, int mode)
            throws RepositoryException {

        // create new node
        NodeStateEx node = parent.addNode(name, NameConstants.NT_FROZENNODE, null, true);

        // initialize the internal properties
        node.setPropertyValue(NameConstants.JCR_FROZENUUID,
                InternalValue.create(src.internalGetUUID().toString()));
        node.setPropertyValue(NameConstants.JCR_FROZENPRIMARYTYPE,
                InternalValue.create(((NodeTypeImpl) src.getPrimaryNodeType()).getQName()));
        if (src.hasProperty(NameConstants.JCR_MIXINTYPES)) {
            NodeType[] mixins = src.getMixinNodeTypes();
            InternalValue[] ivalues = new InternalValue[mixins.length];
            for (int i = 0; i < mixins.length; i++) {
                ivalues[i] = InternalValue.create(((NodeTypeImpl) mixins[i]).getQName());
            }
            node.setPropertyValues(NameConstants.JCR_FROZENMIXINTYPES, PropertyType.NAME, ivalues);
        }

        // add the properties
        PropertyIterator piter = src.getProperties();
        while (piter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) piter.nextProperty();
            int opv;
            if ((mode & MODE_COPY) > 0) {
                opv = OnParentVersionAction.COPY;
            } else {
                opv = prop.getDefinition().getOnParentVersion();
            }

            if (opv == OnParentVersionAction.ABORT) {
                parent.reload();
                throw new VersionException("Checkin aborted due to OPV in " + prop);
            } else if (opv == OnParentVersionAction.VERSION
                    || opv == OnParentVersionAction.COPY) {
                // ignore frozen properties
                if (!prop.getQName().equals(NameConstants.JCR_PRIMARYTYPE)
                        && !prop.getQName().equals(NameConstants.JCR_MIXINTYPES)
                        && !prop.getQName().equals(NameConstants.JCR_UUID)) {
                    node.copyFrom(prop);
                }
            }
        }

        // add the frozen children and histories
        NodeIterator niter = src.getNodes();
        while (niter.hasNext()) {
            NodeImpl child = (NodeImpl) niter.nextNode();
            int opv;
            if ((mode & MODE_COPY_RECURSIVE) > 0) {
                opv = OnParentVersionAction.COPY;
            } else {
                opv = child.getDefinition().getOnParentVersion();
            }

            if (opv == OnParentVersionAction.ABORT) {
                throw new VersionException("Checkin aborted due to OPV in " + child);
            } else if (opv == OnParentVersionAction.VERSION) {
                if (child.isNodeType(NameConstants.MIX_SIMPLE_VERSIONABLE)) {
                    // create frozen versionable child
                    NodeStateEx newChild = node.addNode(child.getQName(), NameConstants.NT_VERSIONEDCHILD, null, false);
                    newChild.setPropertyValue(NameConstants.JCR_CHILDVERSIONHISTORY,
                            InternalValue.create(new UUID(child.getVersionHistory().getUUID())));
                    /*
                        newChild.setPropertyValue(JCR_BASEVERSION,
                                InternalValue.create(child.getBaseVersion().getUUID()));
                     */
                } else {
                    // else copy but do not recurse
                    checkin(node, child.getQName(), child, MODE_COPY);
                }
            } else if (opv == OnParentVersionAction.COPY) {
                checkin(node, child.getQName(), child, MODE_COPY_RECURSIVE);
            }
        }
        return node;
    }

}
