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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * Implements a <code>InternalFrozenNode</code>
 */
class InternalFrozenNodeImpl extends InternalFreezeImpl
        implements InternalFrozenNode {

    /**
     * the list of frozen properties
     */
    private PropertyState[] frozenProperties;

    /**
     * the frozen id of the original node
     */
    private NodeId frozenUUID = null;

    /**
     * the frozen primary type of the original node
     */
    private Name frozenPrimaryType = null;

    /**
     * the frozen list of mixin types of the original node
     */
    private Set<Name> frozenMixinTypes = null;

    /**
     * Creates a new frozen node based on the given persistence node.
     *
     * @param vMgr version manager
     * @param node underlying node
     * @param parent parent item
     * @throws RepositoryException if an error occurs
     */
    public InternalFrozenNodeImpl(InternalVersionManagerBase vMgr, NodeStateEx node,
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
        List<PropertyState> propList = new ArrayList<PropertyState>();

        Set<Name> mixins = new HashSet<Name>();
        for (PropertyState prop : props) {
            if (prop.getName().equals(NameConstants.JCR_FROZENUUID)) {
                // special property
                InternalValue value =
                    node.getPropertyValue(NameConstants.JCR_FROZENUUID);
                // JCR-1803: The value should be a STRING, but older Jackrabbit
                // versions (< 1.1, see JCR-487) used REFERENCE values. Since
                // we do not automatically upgrade old content, we need to be
                // ready to handle both types of values here.
                if (value.getType() == PropertyType.STRING) {
                    frozenUUID = new NodeId(value.getString());
                } else {
                    frozenUUID = value.getNodeId();
                }
            } else if (prop.getName().equals(NameConstants.JCR_FROZENPRIMARYTYPE)) {
                // special property
                frozenPrimaryType = node.getPropertyValue(NameConstants.JCR_FROZENPRIMARYTYPE).getName();
            } else if (prop.getName().equals(NameConstants.JCR_FROZENMIXINTYPES)) {
                // special property
                InternalValue[] values = node.getPropertyValues(NameConstants.JCR_FROZENMIXINTYPES);
                if (values != null) {
                    for (InternalValue value : values) {
                        mixins.add(value.getName());
                    }
                }
            } else if (!prop.getName().equals(NameConstants.JCR_PRIMARYTYPE)
                    && !prop.getName().equals(NameConstants.JCR_UUID)) {
                propList.add(prop);
            }
        }
        frozenProperties = propList.toArray(new PropertyState[propList.size()]);
        frozenMixinTypes = Collections.unmodifiableSet(mixins);

        // do some checks
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
    @Override
    public NodeId getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public List<ChildNodeEntry> getFrozenChildNodes()
            throws VersionException {
        return node.getState().getChildNodeEntries();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasFrozenChildNode(Name name, int idx) {
        return node.getState().hasChildNodeEntry(name, idx);
    }

    /**
     * {@inheritDoc}
     */
    public InternalFreeze getFrozenChildNode(Name name, int idx) 
            throws RepositoryException {
        ChildNodeEntry e = node.getState().getChildNodeEntry(name, idx);
        return e == null
                ? null
                : (InternalFreeze) vMgr.getItem(e.getId());
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
    public NodeId getFrozenId() {
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
    public Set<Name> getFrozenMixinTypes() {
        return frozenMixinTypes;
    }

    /**
     * Checks-in a <code>src</code> node. It creates a new child node of
     * <code>parent</code> with the given <code>name</code> and adds the
     * source nodes properties according to their OPV value to the
     * list of frozen properties. It creates frozen child nodes for each child
     * node of <code>src</code> according to its OPV value.
     *
     * @param parent destination parent
     * @param name new node name
     * @param src source node state
     * @return the node node state
     * @throws RepositoryException if an error occurs
     */
    protected static NodeStateEx checkin(NodeStateEx parent, Name name,
                                         NodeStateEx src)
            throws RepositoryException {
        try {
            return checkin(parent, name, src, false);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Checks-in a <code>src</code> node. It creates a new child node of
     * <code>parent</code> with the given <code>name</code> and adds the
     * source nodes properties according to their OPV value to the
     * list of frozen properties. It creates frozen child nodes for each child
     * node of <code>src</code> according to its OPV value.
     *
     * @param parent destination parent
     * @param name new node name
     * @param src source node state
     * @param forceCopy if <code>true</code> the OPV is ignored and a COPY is performed
     * @return the nde node state
     * @throws RepositoryException if an error occurs
     * @throws ItemStateException if an error during reading the items occurs
     */
    private static NodeStateEx checkin(NodeStateEx parent, Name name,
                                       NodeStateEx src, boolean forceCopy)
            throws RepositoryException, ItemStateException {

        // create new node
        NodeStateEx node = parent.addNode(name, NameConstants.NT_FROZENNODE, null, true);

        // initialize the internal properties
        node.setPropertyValue(NameConstants.JCR_FROZENUUID,
                InternalValue.create(src.getNodeId().toString()));
        node.setPropertyValue(NameConstants.JCR_FROZENPRIMARYTYPE,
                InternalValue.create(src.getState().getNodeTypeName()));
        if (src.hasProperty(NameConstants.JCR_MIXINTYPES)) {
            node.setPropertyValues(NameConstants.JCR_FROZENMIXINTYPES,
                    PropertyType.NAME, src.getPropertyValues(NameConstants.JCR_MIXINTYPES));
        }

        // add the properties
        for (PropertyState prop: src.getProperties()) {
            int opv;
            if (forceCopy) {
                opv = OnParentVersionAction.COPY;
            } else {
                opv = src.getDefinition(prop).getOnParentVersion();
            }

            Name propName = prop.getName();
            if (opv == OnParentVersionAction.ABORT) {
                parent.reload();
                throw new VersionException("Checkin aborted due to OPV abort in " + propName);
            } else if (opv == OnParentVersionAction.VERSION
                    || opv == OnParentVersionAction.COPY) {
                // ignore frozen properties
                if (!propName.equals(NameConstants.JCR_PRIMARYTYPE)
                        && !propName.equals(NameConstants.JCR_MIXINTYPES)
                        && !propName.equals(NameConstants.JCR_UUID)
                        // JCR-3635: should never occur in normal content...
                        && !propName.equals(NameConstants.JCR_FROZENPRIMARYTYPE)
                        && !propName.equals(NameConstants.JCR_FROZENMIXINTYPES)
                        && !propName.equals(NameConstants.JCR_FROZENUUID)) {
                    node.copyFrom(prop);
                }
            }
        }

        // add the frozen children and histories
        boolean isFull = src.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE);
        for (NodeStateEx child: src.getChildNodes()) {
            int opv;
            if (forceCopy) {
                opv = OnParentVersionAction.COPY;
            } else {
                opv = child.getDefinition().getOnParentVersion();
            }

            if (opv == OnParentVersionAction.ABORT) {
                throw new VersionException("Checkin aborted due to OPV in " + child);
            } else if (opv == OnParentVersionAction.VERSION) {
                if (isFull && child.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                    // create frozen versionable child
                    NodeId histId = child.getPropertyValue(NameConstants.JCR_VERSIONHISTORY).getNodeId();
                    NodeStateEx newChild = node.addNode(child.getName(), NameConstants.NT_VERSIONEDCHILD, null, false);
                    newChild.setPropertyValue(
                            NameConstants.JCR_CHILDVERSIONHISTORY,
                            InternalValue.create(histId));
                } else {
                    // else copy
                    checkin(node, child.getName(), child, true);
                }
            } else if (opv == OnParentVersionAction.COPY) {
                checkin(node, child.getName(), child, true);
            }
        }
        return node;
    }

}
