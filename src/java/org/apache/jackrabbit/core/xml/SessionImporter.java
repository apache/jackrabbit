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
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.util.ValueHelper;
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
import javax.jcr.nodetype.NodeDef;
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
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

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
                                  QName nodeName,
                                  QName nodeTypeName,
                                  QName[] mixinNames,
                                  String uuid)
            throws RepositoryException {
        NodeImpl node;
        // add node
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
        if (uuidBehavior == IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            if (node.isNodeType(Constants.MIX_REFERENCEABLE)) {
                refTracker.mappedUUID(nodeInfo.getUUID(), node.getUUID());
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
            // 'replace' current parent with parent of conflicting
            parent = (NodeImpl) conflicting.getParent();
            // remove conflicting
            conflicting.remove();
            // create new with given uuid at same location as conflicting
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
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

        NodeImpl node = null;
        String uuid = nodeInfo.getUUID();
        QName nodeName = nodeInfo.getName();
        QName ntName = nodeInfo.getNodeTypeName();
        QName[] mixins = nodeInfo.getMixinNames();

        if (parent == null) {
            // parent node was skipped, skip this child node also
            parents.push(null); // push null onto stack for skipped node
            log.debug("skipping node " + nodeName);
            return;
        }
        if (parent.hasNode(nodeName)) {
            // a node with that name already exists...
            NodeImpl existing = parent.getNode(nodeName);
            NodeDef def = existing.getDefinition();
            if (!def.allowSameNameSibs()) {
                // existing doesn't allow same-name siblings,
                // check for potential conflicts
                if (def.isProtected() && existing.isNodeType(ntName)) {
                    // skip protected node
                    parents.push(null); // push null onto stack for skipped node
                    log.debug("skipping protected node " + existing.safeGetJCRPath());
                    return;
                }
                if (def.isAutoCreate() && existing.isNodeType(ntName)) {
                    // this node has already been auto-created, no need to create it
                    node = existing;
                } else {
                    throw new ItemExistsException(existing.safeGetJCRPath());
                }
            }
        }

        if (node == null) {
            // create node
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
        }

        // process properties

        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo pi = (PropInfo) iter.next();
            QName propName = pi.getName();
            InternalValue[] iva = pi.getValues();
            int type = pi.getType();

            // find applicable definition
            EffectiveNodeType ent = node.getEffectiveNodeType();
            PropDef def;
            // multi- or single-valued property?
            if (iva.length == 1) {
                // could be single- or multi-valued (n == 1)
                try {
                    // try single-valued
                    def = ent.getApplicablePropertyDef(propName, type, false);
                } catch (ConstraintViolationException cve) {
                    // try multi-valued
                    def = ent.getApplicablePropertyDef(propName, type, true);
                }
            } else {
                // can only be multi-valued (n == 0 || n > 1)
                def = ent.getApplicablePropertyDef(propName, type, true);
            }

            if (def.isProtected()) {
                // skip protected property
                log.debug("skipping protected property " + propName);
                continue;
            }

            // convert InternalValue objects to Value objects using this
            // session's namespace mappings
            Value[] va = new Value[iva.length];
            // check whether type conversion is required
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != type) {
                // type doesn't match required type,
                // type conversion required
                // FIXME: awkward code
                for (int i = 0; i < iva.length; i++) {
                    // convert InternalValue to Value of required type
                    Value v =
                            ValueHelper.convert(iva[i].toJCRValue(nsContext),
                                    def.getRequiredType());
                    // convert Value to InternalValue using
                    // current namespace context of xml document
                    InternalValue ival = InternalValue.create(v, nsContext);
                    // convert InternalValue back to Value using this
                    // session's namespace mappings
                    va[i] = ival.toJCRValue(session.getNamespaceResolver());
                }
            } else {
                // no type conversion required:
                // convert InternalValue to Value using this
                // session's namespace mappings
                for (int i = 0; i < iva.length; i++) {
                    va[i] = iva[i].toJCRValue(session.getNamespaceResolver());
                }
            }

            // multi- or single-valued property?
            if (va.length == 1) {
                // could be single- or multi-valued (n == 1)
                try {
                    // try setting single-value
                    node.setProperty(propName, va[0]);
                } catch (ValueFormatException vfe) {
                    // try setting value array
                    node.setProperty(propName, va, type);
                } catch (ConstraintViolationException cve) {
                    // try setting value array
                    node.setProperty(propName, va, type);
                }
            } else {
                // can only be multi-valued (n == 0 || n > 1)
                node.setProperty(propName, va, type);
            }
            if (type == PropertyType.REFERENCE) {
                // store reference for later resolution
                refTracker.processedReference(node.getProperty(propName));
            }
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
                    String adjusted = refTracker.getMappedUUID(original);
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
                String adjusted = refTracker.getMappedUUID(original);
                if (adjusted != null) {
                    prop.setValue(session.getNodeByUUID(adjusted));
                }
            }
        }
        refTracker.clear();
    }
}
