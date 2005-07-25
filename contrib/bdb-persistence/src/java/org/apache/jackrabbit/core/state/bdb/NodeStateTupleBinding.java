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
package org.apache.jackrabbit.core.state.bdb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.name.QName;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class NodeStateTupleBinding extends TupleBinding {

    private Log log = LogFactory.getLog(NodeStateTupleBinding.class);

    private NodeId id;

    public NodeStateTupleBinding(NodeId nodeId) {
        this.id = nodeId;
    }

    public NodeStateTupleBinding() {
    }

    public Object entryToObject(TupleInput in) {

        NodeState state = new NodeState(id.getUUID(), null, null, NodeState.STATUS_NEW, false);

        // check uuid
        String s = in.readString();
        if (!state.getUUID().equals(s)) {
            String msg = "invalid serialized state: uuid mismatch";
            log.debug(msg);
            throw new RuntimeException(msg);
        }

        // deserialize node state

        // primaryType
        s = in.readString();
        state.setNodeTypeName(QName.valueOf(s));
        // parentUUID
        s = in.readString();
        if (s.length() > 0) {
            state.setParentUUID(s);
        }
        // definitionId
        s = in.readString();
        state.setDefinitionId(NodeDefId.valueOf(s));
        // mixin types
        int count = in.readInt(); // count
        Set set = new HashSet(count);
        for (int i = 0; i < count; i++) {
            set.add(QName.valueOf(in.readString())); // name
        }
        if (set.size() > 0) {
            state.setMixinTypeNames(set);
        }
        // properties (names)
        count = in.readInt(); // count
        for (int i = 0; i < count; i++) {
            state.addPropertyName(QName.valueOf(in.readString())); // name
        }
        // child nodes (list of name/uuid pairs)
        count = in.readInt(); // count
        for (int i = 0; i < count; i++) {
            QName name = QName.valueOf(in.readString()); // name
            String s1 = in.readString(); // uuid
            state.addChildNodeEntry(name, s1);
        }

        return state;
    }

    public void objectToEntry(Object o, TupleOutput out) {

        NodeState state = (NodeState) o;

        // uuid
        out.writeString(state.getUUID());
        // primaryType
        out.writeString(state.getNodeTypeName().toString());
        // parentUUID
        out.writeString(state.getParentUUID() == null ? "" : state.getParentUUID());
        // definitionId
        out.writeString(state.getDefinitionId().toString());
        // mixin types
        Collection c = state.getMixinTypeNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            out.writeString(iter.next().toString()); // name
        }
        // properties (names)
        c = state.getPropertyNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            QName propName = (QName) iter.next();
            out.writeString(propName.toString()); // name
        }
        // child nodes (list of name/uuid pairs)
        c = state.getChildNodeEntries();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            out.writeString(entry.getName().toString()); // name
            out.writeString(entry.getUUID()); // uuid
        }

    }

}
