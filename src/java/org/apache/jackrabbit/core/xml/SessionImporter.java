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

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.log4j.Logger;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.ReferenceValue;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * <code>SessionImporter</code> ...
 */
public class SessionImporter implements Importer {

    private static Logger log = Logger.getLogger(SessionImporter.class);

    private final SessionImpl session;
    private final NodeImpl importTargetNode;
    private final int uuidBehavior;

    private Stack parents;

    /**
     * mapping <original uuid> to <new uuid> of mix:referenceable nodes
     */
    private final HashMap uuidMap;
    /**
     * list of imported reference properties that might need correcting
     */
    private final ArrayList references;

    public SessionImporter(NodeImpl importTargetNode,
                           SessionImpl session,
                           int uuidBehavior) {
        this.importTargetNode = importTargetNode;
        this.session = session;
        this.uuidBehavior = uuidBehavior;

        uuidMap = new HashMap();
        references = new ArrayList();

        parents = new Stack();
        parents.push(importTargetNode);
    }

    protected NodeImpl createNode(NodeImpl parent,
                                  QName nodeName,
                                  QName nodeTypeName,
                                  QName[] mixinNames,
                                  String uuid)
            throws RepositoryException {
        NodeImpl node;
        // add node
        node = (NodeImpl) parent.addNode(nodeName, nodeTypeName, uuid);
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
    public void startNode(NodeInfo nodeInfo, List propInfos,
                          NamespaceResolver nsContext)
            throws RepositoryException {
        NodeImpl parent = (NodeImpl) parents.peek();

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
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo node) throws RepositoryException {
        parents.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        /**
         * adjust references that refer to uuid's which have been mapped to
         * newly gererated uuid's on import
         */
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            Property prop = (Property) iter.next();
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
                        newVals[i] = new ReferenceValue(session.getNodeByUUID(adjusted));
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
    }
}
