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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * <code>WorkspaceImporter</code> ...
 */
public class WorkspaceImporter implements Importer {

    private static Logger log = Logger.getLogger(WorkspaceImporter.class);

    private final NodeState importTarget;
    private final WorkspaceImpl wsp;
    private final HierarchyManager hierMgr;
    private final UpdatableItemStateManager stateMgr;

    private final int uuidBehavior;

    private boolean aborted;
    private Stack parents;

    /**
     * mapping <original uuid> to <new uuid> of mix:referenceable nodes
     */
    private final HashMap uuidMap;
    /**
     * list of imported reference properties that might need correcting
     */
    private final ArrayList references;

    public WorkspaceImporter(NodeState importTarget,
                             WorkspaceImpl wsp,
                             int uuidBehavior) {
        this.importTarget = importTarget;
        this.wsp = wsp;

        hierMgr = wsp.getHierarchyManager();
        stateMgr = wsp.getItemStateManager();

        this.uuidBehavior = uuidBehavior;

        aborted = false;

        uuidMap = new HashMap();
        references = new ArrayList();

        parents = new Stack();
        parents.push(importTarget);
    }

    protected NodeState createNode(NodeState parent,
                                   QName nodeName,
                                   QName nodeTypeName,
                                   QName[] mixinNames,
                                   String uuid)
            throws RepositoryException {
        NodeState node = stateMgr.createNew(uuid, nodeTypeName, parent.getUUID());
        node.setMixinTypeNames(new HashSet(Arrays.asList(mixinNames)));
        ChildNodeDef nodeDef =
                wsp.findApplicableDefinition(nodeName, nodeTypeName, parent);
        node.setDefinitionId(new NodeDefId(nodeDef));

        // now add new child node entry to parent
        parent.addChildNodeEntry(nodeName, node.getUUID());

        return node;
    }

    protected NodeState resolveUUIDConflict(NodeState parent,
                                            NodeState conflicting,
                                            NodeInfo nodeInfo)
            throws RepositoryException {
        NodeState node = null;
/*
        if (uuidBehavior == IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            if (node.isNodeType(Constants.MIX_REFERENCEABLE)) {
                uuidMap.put(nodeInfo.getUUID(), node.getUUID());
            }
        } else if (uuidBehavior == IMPORT_UUID_COLLISION_THROW) {
            String msg = "a node with uuid " + nodeInfo.getUUID() + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTargetNode or an ancestor thereof
            if (importTargetNode.getPath().startsWith(conflicting.getPath())) {
                String msg = "cannot remove ancestor node";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // remove conflicting
            conflicting.remove();
            // create new with given uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
        } else if (uuidBehavior == IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            if (conflicting.getDepth() == 0) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // @todo implement IMPORT_UUID_COLLISION_REPLACE_EXISTING behavior
            throw new RepositoryException("uuidBehavior IMPORT_UUID_COLLISION_REPLACE_EXISTING is not yet implemented");
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
*/
        return node;
    }

    //-------------------------------------------------------------< Importer >
    /**
     * {@inheritDoc}
     */
    public void start() throws RepositoryException {
        try {
            // start update operation
            stateMgr.edit();
        } catch (ItemStateException ise) {
            aborted = true;
            String msg = "internal error: failed to start update operation";
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startNode(NodeInfo nodeInfo, List propInfos,
                          NamespaceResolver nsContext)
            throws RepositoryException {
        if (aborted) {
            // the import has been aborted, get outta here...
            return;
        }

        boolean succeeded = false;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            NodeState parent = (NodeState) parents.peek();

            // process node

            NodeState node;
            String uuid = nodeInfo.getUUID();
            QName nodeName = nodeInfo.getName();
            QName ntName = nodeInfo.getNodeTypeName();
            QName[] mixins = nodeInfo.getMixinNames();
/*
            if (uuid == null) {
                // no potential uuid conflict, always add new node
                node = createNode(parent, nodeName, ntName, mixins, null);
            } else {
                // potential uuid conflict
                NodeState conflicting;
                try {
                    conflicting = (NodeImpl) session.getNodeByUUID(uuid);
                } catch (ItemNotFoundException infe) {
                    conflicting = null;
                }
                if (conflicting != null) {
                    // resolve uuid conflict
                    node = resolveUUIDConflict(parent, conflicting, nodeInfo);
                } else {
                    // create new with given uuid
                    node = createNode(parent, nodeName, ntName, mixins, uuid);
                }
            }
*/
/*
            // store state
            stateMgr.store(state);
            succeeded = true;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to persist state of " + parentAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
*/
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                stateMgr.cancel();
            }
        }

/*

        // process node

        NodeImpl node;
        String uuid = nodeInfo.getUUID();
        QName nodeName = nodeInfo.getName();
        QName ntName = nodeInfo.getNodeTypeName();
        QName[] mixins = nodeInfo.getMixinNames();
        if (uuid == null) {
            // no potential uuid conflict, always add new node
            node = createNode(parent, nodeName, ntName, mixins, null);
        } else {
            // potential uuid conflict
            NodeImpl conflicting;
            try {
                conflicting = (NodeImpl) session.getNodeByUUID(uuid);
            } catch (ItemNotFoundException infe) {
                conflicting = null;
            }
            if (conflicting != null) {
                // resolve uuid conflict
                node = resolveUUIDConflict(parent, conflicting, nodeInfo);
            } else {
                // create new with given uuid
                node = createNode(parent, nodeName, ntName, mixins, uuid);
            }
        }

        // process properties

        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo pi = (PropInfo) iter.next();
            QName propName = pi.getName();
            Value[] vals = pi.getValues();
            int type = pi.getType();
            if (node.hasProperty(propName)) {
                PropertyDef def = node.getProperty(propName).getDefinition();
                if (def.isProtected()) {
                    // skip protected property
                    log.debug("skipping protected property " + propName);
                    continue;
                }
            }
            // multi- or single-valued property?
            if (vals.length == 1) {
                // could be single- or multi-valued (n == 1)
                try {
                    // try setting single-value
                    node.setProperty(propName, vals[0]);
                } catch (ValueFormatException vfe) {
                    // try setting value array
                    node.setProperty(propName, vals, type);
                } catch (ConstraintViolationException vfe) {
                    // try setting value array
                    node.setProperty(propName, vals, type);
                }
            } else {
                // can only be multi-valued (n == 0 || n > 1)
                node.setProperty(propName, vals, type);
            }
            if (type == PropertyType.REFERENCE) {
                // store reference for later resolution
                references.add(node.getProperty(propName));
            }
        }

        parents.push(node);
*/
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        if (aborted) {
            // the import has been aborted, get outta here...
            return;
        }
        NodeState node = (NodeState) parents.pop();
        boolean succeeded = false;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            // we're done with that node, now store its state
            stateMgr.store(node);
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                stateMgr.cancel();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        if (aborted) {
            // the import has been aborted, get outta here...
            return;
        }

        boolean succeeded = false;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            /**
             * adjust references that refer to uuid's which have been mapped to
             * newly gererated uuid's on import
             */
/*
            Iterator iter = references.iterator();
            while (iter.hasNext()) {
                PropertyState prop = (PropertyState) iter.next();
                // being paranoid...
                if (prop.getType() != PropertyType.REFERENCE) {
                    continue;
                }
                if (prop.getDefinition().isMultiple()) {
                    Value[] values = prop.getValues();
                    Value[] newVals = new Value[values.length];
                    for (int i = 0; i < values.length; i++) {
                        Value val = values[i];
                        String original = val.getString();
                        String adjusted = (String) uuidMap.get(original);
                        if (adjusted != null) {
                            newVals[i] = new ReferenceValue(wsp.getSession().getNodeByUUID(adjusted));
                        } else {
                            // reference doesn't need adjusting, just copy old value
                            newVals[i] = val;
                        }
                    }
                    prop.setValue(newVals);
                } else {
                    Value val = prop.getValue();
                    String original = val.getString();
                    String adjusted = (String) uuidMap.get(original);
                    if (adjusted != null) {
                        prop.setValue(session.getNodeByUUID(adjusted));
                    }
                }
            }
*/
            // finally store the state of the import target
            // (the parent of the imported subtree)
            stateMgr.store(importTarget);
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                stateMgr.cancel();
            }
        }

        if (!aborted) {
            try {
                // finish update
                stateMgr.update();
            } catch (ItemStateException ise) {
                aborted = true;
                String msg = "internal error: failed to finish update operation";
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
        }
    }
}
