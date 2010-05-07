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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.ReferenceValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * <code>SessionImporter</code> ...
 */
public class SessionImporter implements Importer {

    private static Logger log = LoggerFactory.getLogger(SessionImporter.class);

    private final SessionImpl session;
    private final NodeImpl importTargetNode;
    private final int uuidBehavior;

    private Stack parents;

    /**
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

    /**
     * Creates a new <code>SessionImporter</code> instance.
     *
     * @param importTargetNode
     * @param session
     * @param uuidBehavior     any of the constants declared by
     *                         {@link javax.jcr.ImportUUIDBehavior}
     */
    public SessionImporter(NodeImpl importTargetNode,
                           SessionImpl session,
                           int uuidBehavior) {
        this.importTargetNode = importTargetNode;
        this.session = session;
        this.uuidBehavior = uuidBehavior;

        refTracker = new ReferenceChangeTracker();

        parents = new Stack();
        parents.push(importTargetNode);
    }

    protected NodeImpl createNode(NodeImpl parent,
                                  Name nodeName,
                                  Name nodeTypeName,
                                  Name[] mixinNames,
                                  NodeId id)
            throws RepositoryException {
        NodeImpl node;

        // add node
        UUID uuid = (id == null) ? null : id.getUUID();
        node = parent.addNode(nodeName, nodeTypeName, uuid);
        // add mixins
        if (mixinNames != null) {
            for (int i = 0; i < mixinNames.length; i++) {
                node.addMixin(mixinNames[i]);
            }
        }
        return node;
    }

    protected NodeImpl resolveUUIDConflict(NodeImpl parent,
                                           NodeImpl conflicting,
                                           NodeInfo nodeInfo)
            throws RepositoryException {
        NodeImpl node;
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            if (node.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
                refTracker.mappedUUID(nodeInfo.getId().getUUID(), node.getNodeId().getUUID());
            }
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW) {
            // if conflicting node is shareable, then clone it
            if (conflicting.isNodeType(NameConstants.MIX_SHAREABLE)) {
                parent.clone(conflicting, nodeInfo.getName());
                return null;
            }
            String msg = "a node with uuid " + nodeInfo.getId() + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTargetNode or an ancestor thereof
            if (importTargetNode.getPath().startsWith(conflicting.getPath())) {
                String msg = "cannot remove ancestor node";
                log.debug(msg);
                throw new ConstraintViolationException (msg);
            }
            // remove conflicting
            conflicting.remove();
            // create new with given uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getId());
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            if (conflicting.getDepth() == 0) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // 'replace' current parent with parent of conflicting
            parent = (NodeImpl) conflicting.getParent();

            // replace child node
            node = parent.replaceChildNode(nodeInfo.getId(), nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames());
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return node;
    }

    //-------------------------------------------------------------< Importer >
    /**
     * {@inheritDoc}
     */
    public void start() throws RepositoryException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    public void startNode(NodeInfo nodeInfo, List propInfos)
            throws RepositoryException {
        NodeImpl parent = (NodeImpl) parents.peek();

        // process node

        NodeImpl node = null;
        NodeId id = nodeInfo.getId();
        Name nodeName = nodeInfo.getName();
        Name ntName = nodeInfo.getNodeTypeName();
        Name[] mixins = nodeInfo.getMixinNames();

        if (parent == null) {
            // parent node was skipped, skip this child node too
            parents.push(null); // push null onto stack for skipped node
            log.debug("Skipping node: " + nodeName);
            return;
        }

        // make sure the editing session is allowed create nodes with a
        // specified node type (and ev. mixins)
        if (!session.getAccessManager().isGranted(session.getQPath(parent.getPath()), nodeName, Permission.NODE_TYPE_MNGMT)) {
            throw new AccessDeniedException("Insufficient permission.");
        }

        if (parent.hasNode(nodeName)) {
            // a node with that name already exists...
            NodeImpl existing = parent.getNode(nodeName);
            NodeDefinition def = existing.getDefinition();
            if (!def.allowsSameNameSiblings()) {
                // existing doesn't allow same-name siblings,
                // check for potential conflicts
                if (def.isProtected() && existing.isNodeType(ntName)) {
                    // skip protected node
                    parents.push(null); // push null onto stack for skipped node
                    log.debug("Skipping protected node: " + existing);
                    return;
                }
                if (def.isAutoCreated() && existing.isNodeType(ntName)) {
                    // this node has already been auto-created, no need to create it
                    node = existing;
                } else {
                    // edge case: colliding node does have same uuid
                    // (see http://issues.apache.org/jira/browse/JCR-1128)
                    if (!(existing.getId().equals(id)
                            && (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
                            || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING))) {
                        throw new ItemExistsException(
                                "Node with the same UUID exists:" + existing);
                    }
                    // fall through
                }
            }
        }

        if (node == null) {
            // create node
            if (id == null) {
                // no potential uuid conflict, always add new node
                node = createNode(parent, nodeName, ntName, mixins, null);
            } else {
                // potential uuid conflict
                NodeImpl conflicting;
                try {
                    conflicting = session.getNodeById(id);
                } catch (ItemNotFoundException infe) {
                    conflicting = null;
                }
                if (conflicting != null) {
                    // resolve uuid conflict
                    node = resolveUUIDConflict(parent, conflicting, nodeInfo);
                    if (node == null) {
                        // no new node has been created, so skip this node
                        parents.push(null); // push null onto stack for skipped node
                        log.debug("skipping existing node " + nodeInfo.getName());
                        return;
                    }
                } else {
                    // create new with given uuid
                    node = createNode(parent, nodeName, ntName, mixins, id);
                }
            }
        }

        // process properties

        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo pi = (PropInfo) iter.next();
            pi.apply(node, session, refTracker);
        }

        parents.push(node);
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        parents.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        /**
         * adjust references that refer to uuid's which have been mapped to
         * newly generated uuid's on import
         */
        Iterator iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) iter.next();
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE) {
                continue;
            }
            if (prop.isMultiple()) {
                Value[] values = prop.getValues();
                Value[] newVals = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    Value val = values[i];
                    UUID original = UUID.fromString(val.getString());
                    UUID adjusted = refTracker.getMappedUUID(original);
                    if (adjusted != null) {
                        newVals[i] = new ReferenceValue(session.getNodeByUUID(adjusted));
                    } else {
                        // reference doesn't need adjusting, just copy old value
                        newVals[i] = val;
                    }
                }
                prop.setValue(newVals);
            } else {
                Value val = prop.getValue();
                UUID original = UUID.fromString(val.getString());
                UUID adjusted = refTracker.getMappedUUID(original);
                if (adjusted != null) {
                    prop.setValue(session.getNodeByUUID(adjusted));
                }
            }
        }
        refTracker.clear();
    }
}
