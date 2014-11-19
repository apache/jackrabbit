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
package org.apache.jackrabbit.jcr2spi.operation;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.json.JsonUtil;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

public class SetPolicy extends TransientOperation {
    private Name nodeName;
    private Name ntName;
    private String uuid;
    protected NodeState parentState;
    private final NodeId parentId;
    private final NodeState aclNode;
    private final NamePathResolver resolver;

    private final List<ItemState> addedStates = new ArrayList<ItemState>();
    
    /**
     * Options that must not be violated for a successful set policy operation.
     */
    private final static int SET_POLICY_OPTIONS = ItemStateValidator.CHECK_ACCESS | ItemStateValidator.CHECK_LOCK |
                                                  ItemStateValidator.CHECK_VERSIONING;

    private SetPolicy(NodeState parentState, NodeState aclNode, NamePathResolver npResolver) throws RepositoryException {
        super(SET_POLICY_OPTIONS);
        
        this.parentState = parentState;
        this.aclNode = aclNode;
        this.nodeName = aclNode.getName();
        this.ntName = aclNode.getNodeTypeName();
        this.uuid = aclNode.getUniqueID();
        
        parentId = parentState.getNodeId();
        resolver = npResolver;
        
        // the parent node affected by this operation
        addAffectedItemState(parentState);
    }
    
    /**
     * Writes the JSON string representation of the acl node using the specified StringWriter.
     * Note: When this operation is visited by the OperationVisitorImpl, this method will be called to output
     * the complete JSON string of the policy node which can then be added to the Batch.
     * @param writer        the string writer object.
     * @throws RepositoryException
     */
    public void writeJson(StringWriter writer) throws RepositoryException {
       
        writer.write('{');
        writer.write(getJsonKey(JcrConstants.JCR_PRIMARYTYPE));
        writer.write(JsonUtil.getJsonString(resolver.getJCRName(ntName)));
        if (uuid != null) {
            writer.write(',');
            writer.write(getJsonKey(JcrConstants.JCR_UUID));
            writer.write(JsonUtil.getJsonString(uuid));
        }
        Iterator<NodeEntry> entries = ((NodeEntry) aclNode.getHierarchyEntry()).getNodeEntries();
        while (entries.hasNext()) {
            NodeEntry ne = entries.next();
            writeJson(ne.getNodeState(), writer);
        }
        writer.write('}');
    }
    
    private void writeJson(NodeState nodeState, StringWriter writer) throws RepositoryException {
        String entryName = resolver.getJCRName(nodeState.getName());
        Name ntName = nodeState.getNodeTypeName();
        String id = nodeState.getUniqueID();

        writer.write(',');
        writer.write(getJsonKey(entryName));
        writer.write('{');
        writer.write(getJsonKey(JcrConstants.JCR_PRIMARYTYPE));
        writer.write(JsonUtil.getJsonString(resolver.getJCRName(ntName)));
        if (id != null) {
            writer.write(',');
            writer.write(getJsonKey(JcrConstants.JCR_UUID));
            writer.write(JsonUtil.getJsonString(id));
        }
        
        // write all the properties
        Iterator<PropertyEntry> props = ((NodeEntry) nodeState.getHierarchyEntry()).getPropertyEntries();
        while (props.hasNext()) {
            PropertyEntry pe = props.next();
            writer.write(',');
            writeJson(pe.getPropertyState(), writer);
        }
        writer.write('}');
    }
    
    private void writeJson(PropertyState pState, StringWriter writer) throws RepositoryException {
        String propName = resolver.getJCRName(pState.getName());
        if (pState.getDefinition().isMultiple()) {
            QValue[] values = pState.getValues();
            writeJson(propName, values, writer);
        } else {
            QValue value = pState.getValue();
            writeJson(propName, value, writer);
        }
        
    }

    private void writeJson(String key, QValue value, StringWriter writer) throws RepositoryException {
        writer.write(getJsonKey(key));
        writer.write(getJsonString(value));
    }

    /**
     * Note: Properties of type Name is represented in JSON using
     * the jcr name of the property value. the actual value object
     * will be created by the ContentHandler during the import
     * this json.
     * @param key
     * @param values
     * @param writer
     * @throws RepositoryException
     */
    private void writeJson(String key, QValue[] values, StringWriter writer) throws RepositoryException {
        writer.write(getJsonKey(key));
        
        StringBuilder sb = null;
        int index = 0;        
        writer.write('[');        
        for (QValue value : values) {           
            String sv = resolver.getJCRName(value.getName());
            sb = new StringBuilder();
            String delim = (index++ == 0) ? "" : ",";
            sb.append(delim).append('"').append(sv).append('"');
            writer.write(sb.toString());
        }
        writer.write(']');
    }
    //-----------------------------------------------------------------< Operation >---
    /**
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Throws UnsupportedOperationException
     *
     * @see Operation#persisted()
     */
    @Override
    public void persisted() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        parentState.getHierarchyEntry().complete(this);
    }

    /**
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_UNDO;
        parentState.getHierarchyEntry().complete(this);
    }

    //--------------------------------------< Access Operation Parameters >--
    public NodeId getParentId() {
        return parentId;
    }
    
    public NodeState getParentState() {
        return parentState;
    }
    
    public Name getNodeName() {
        return nodeName;
    }
    
    public void addedState(List<ItemState> newStates) {
        addedStates.addAll(newStates);
    }
    
    public List<ItemState> getAddedStates() {
        return addedStates;
    }
    
    String getJsonKey(String str) {
        return JsonUtil.getJsonString(str) + " : ";
    }
    
    public Name getNodeTypeName() {
        return ntName;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    /**
     * This method is called to get all the transient itemstates added by this policy operation.
     * It is called during operation completion on the affected parent item.
     * @return
     * @throws RepositoryException
     */
    public List<ItemState> getItemStates() throws RepositoryException {
        List<ItemState> states = new ArrayList<ItemState>();
        states.addAll(addedStates);
        states.add(aclNode);
        Iterator<NodeEntry> it = aclNode.getNodeEntry().getNodeEntries();
        while (it.hasNext()) {
            NodeEntry ne = it.next();
            states.add(ne.getNodeState());
            Iterator<PropertyEntry> pit = ne.getPropertyEntries();
            while (pit.hasNext()) {
                PropertyEntry pe = pit.next();
                states.add(pe.getPropertyState());
            }
        }
        
        return states;
    }
    
    // copied from RepositoryServiceImpl#Bacth -> move to jcr-commons JsonUtils?
    private String getJsonString(QValue value) throws RepositoryException {
        String str;
        switch (value.getType()) {
            case PropertyType.STRING:
                str = JsonUtil.getJsonString(value.getString());
                break;
            case PropertyType.BOOLEAN:
            case PropertyType.LONG:
                str = value.getString();
                break;
            case PropertyType.DOUBLE:
                double d = value.getDouble();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                // JSON cannot specifically handle this property type...
                    str = null;
                } else {
                    str = value.getString();
                    if (str.indexOf('.') == -1) {
                        str += ".0";
                    }
                }
                break;
            default:
                // JSON cannot specifically handle this property type...
                str = null;
        }
        return str;
    }

    //--------------------------------------------------------< factory >---
    public static SetPolicy create(NodeState parentState, NodeState aclNode, NamePathResolver resolver) throws RepositoryException {
        SetPolicy sp = new SetPolicy(parentState, aclNode, resolver);
        // load child entries.
        assertChildNodeEntries(parentState);
        return sp;
    }
}


