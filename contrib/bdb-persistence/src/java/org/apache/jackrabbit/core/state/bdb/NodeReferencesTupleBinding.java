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
import java.util.Iterator;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class NodeReferencesTupleBinding extends TupleBinding {

    private NodeReferencesId id;

    public NodeReferencesTupleBinding(NodeReferencesId id) {
        this.id = id;
    }

    public NodeReferencesTupleBinding() {
    }

    public Object entryToObject(TupleInput in) {

        NodeReferences refs = new NodeReferences(id);
        refs.clearAllReferences();

        // references
        int count = in.readInt(); // count
        for (int i = 0; i < count; i++) {
            refs.addReference(PropertyId.valueOf(in.readString())); // propertyId
        }

        return refs;
    }

    public void objectToEntry(Object o, TupleOutput out) {

        NodeReferences refs = (NodeReferences) o;

        // references
        Collection c = refs.getReferences();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            PropertyId propId = (PropertyId) iter.next();
            out.writeString(propId.toString()); // propertyId
        }

    }

}
