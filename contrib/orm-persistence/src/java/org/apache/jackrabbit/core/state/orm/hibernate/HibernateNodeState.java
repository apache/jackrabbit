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
package org.apache.jackrabbit.core.state.orm.hibernate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.orm.ORMNodeState;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

/**
 * <p> Hibernate-specific node state class. This is necessary because
 * in this implementation we use a set to represent lists.</p>
 */
public class HibernateNodeState extends ORMNodeState {

    // might be a bug here because it seems that these entries should be
    // ordered
    private List listChildNodeEntries = new ArrayList();
    private Set setParentUUIDs = new HashSet();
    private Set setMixinTypeNames = new HashSet();
    private Set setPropertyEntries = new HashSet();

    public HibernateNodeState() {
    }
    public HibernateNodeState(ItemId id) {
        super(id);
    }
    public HibernateNodeState(NodeState state) {
        super();
        fromPersistentNodeState(state);
    }
    public Collection getChildNodeEntries() {
        return listChildNodeEntries;
    }
    public void setChildNodeEntries(Collection childNodeEntries) {
        this.listChildNodeEntries.clear();
        this.listChildNodeEntries.addAll(childNodeEntries);
    }

    public Collection getPropertyEntries() {
        return setPropertyEntries;
    }

    public Collection getMixinTypeNames() {
        return setMixinTypeNames;
    }

    public Collection getParentUUIDs() {
        return setParentUUIDs;
    }

    public void setPropertyEntries(Collection propertyEntries) {
        this.setPropertyEntries.clear();
        this.setPropertyEntries.addAll(propertyEntries);
    }

    public void setMixinTypeNames(Collection mixinTypeNames) {
        this.setMixinTypeNames.clear();
        this.setMixinTypeNames.addAll(mixinTypeNames);
    }

    public void setParentUUIDs(Collection parentUUIDs) {
        this.setParentUUIDs.clear();
        this.setParentUUIDs.addAll(parentUUIDs);
    }

    public List getListChildNodeEntries() {
        return listChildNodeEntries;
    }

    public void setListChildNodeEntries(List listChildNodeEntries) {
        this.listChildNodeEntries = listChildNodeEntries;
    }

    public Set getSetPropertyEntries() {
        return setPropertyEntries;
    }

    public void setSetPropertyEntries(Set setPropertyEntries) {
        this.setPropertyEntries = setPropertyEntries;
    }

    public Set getSetMixinTypeNames() {
        return setMixinTypeNames;
    }

    public void setSetMixinTypeNames(Set setMixinTypeNames) {
        this.setMixinTypeNames = setMixinTypeNames;
    }

    public Set getSetParentUUIDs() {
        return setParentUUIDs;
    }

    public void setSetParentUUIDs(Set setParentUUIDs) {
        this.setParentUUIDs = setParentUUIDs;
    }

}
