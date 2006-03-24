/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.state.orm.ojb;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.orm.ORMNodeState;
import org.apache.ojb.broker.util.collections.RemovalAwareList;

/**
 * <p> OJB specific node state. In order to properly track list
 * modifications, we use an OJB specific list implementation.</p>
 */
public class OJBNodeState extends ORMNodeState implements Serializable {

    private List awareChildNodeEntries = new RemovalAwareList();
    private List awarePropertyEntries = new RemovalAwareList();
    private List awareMixinTypeNames = new RemovalAwareList();
    private List awareParentUUIDs = new RemovalAwareList();

    public OJBNodeState() {
    }
    public OJBNodeState(ItemId id) {
        super(id);
    }
    public OJBNodeState(NodeState state) {
        super();
        fromPersistentNodeState(state);
    }
    public Collection getChildNodeEntries() {
        return awareChildNodeEntries;
    }
    public void setChildNodeEntries(Collection childNodeEntries) {
        this.awareChildNodeEntries.clear();
        this.awareChildNodeEntries.addAll(childNodeEntries);
    }

    public List getAwareChildNodeEntries() {
        return awareChildNodeEntries;
    }

    public void setAwareChildNodeEntries(List awareChildNodeEntries) {
        this.awareChildNodeEntries = awareChildNodeEntries;
    }

    public Collection getPropertyEntries() {
        return awarePropertyEntries;
    }

    public Collection getMixinTypeNames() {
        return awareMixinTypeNames;
    }

    public Collection getParentUUIDs() {
        return awareParentUUIDs;
    }

    public void setPropertyEntries(Collection propertyEntries) {
        this.awarePropertyEntries.clear();
        this.awarePropertyEntries.addAll(propertyEntries);
    }

    public void setMixinTypeNames(Collection mixinTypeNames) {
        this.awareMixinTypeNames.clear();
        this.awareMixinTypeNames.addAll(mixinTypeNames);
    }

    public void setParentUUIDs(Collection parentUUIDs) {
        this.awareParentUUIDs.clear();
        this.awareParentUUIDs.addAll(parentUUIDs);
    }

}
