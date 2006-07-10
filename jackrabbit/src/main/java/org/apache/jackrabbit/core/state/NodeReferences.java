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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.NodeId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <code>NodeReferences</code> represents the references (i.e. properties of
 * type <code>REFERENCE</code>) to a particular node (denoted by its uuid).
 */
public class NodeReferences implements Serializable {

    /**
     * Serial UID
     */
    static final long serialVersionUID = 7007727035982680717L;

    /**
     * identifier of this <code>NodeReferences</code> instance.
     */
    protected NodeReferencesId id;

    /**
     * list of PropertyId's (i.e. the id's of the properties that refer to
     * the target node denoted by <code>id.getTargetId()</code>).
     * <p/>
     * note that the list can contain duplicate entries because a specific
     * REFERENCE property can contain multiple references (if it's multi-valued)
     * to potentially the same target node.
     */
    protected ArrayList references = new ArrayList();

    /**
     * Package private constructor
     *
     * @param id
     */
    public NodeReferences(NodeReferencesId id) {
        this.id = id;
    }

    /**
     * Returns the identifier of this node references object.
     *
     * @return the id of this node references object.
     */
    public NodeReferencesId getId() {
        return id;
    }

    /**
     * Returns the identifier of the target node.
     *
     * @return the id of the target node
     */
    public NodeId getTargetId() {
        return id.getTargetId();
    }

    /**
     * Returns a flag indicating whether this object holds any references
     *
     * @return <code>true</code> if this object holds references,
     *         <code>false</code> otherwise
     */
    public boolean hasReferences() {
        return !references.isEmpty();
    }

    /**
     * @return the list of references
     */
    public List getReferences() {
        return Collections.unmodifiableList(references);
    }

    /**
     * @param refId
     */
    public void addReference(PropertyId refId) {
        references.add(refId);
    }

    /**
     * @param references
     */
    public void addAllReferences(List references) {
        this.references.addAll(references);
    }

    /**
     * @param refId
     * @return <code>true</code> if the reference was removed;
     *        <code>false</code> otherwise.
     */
    public boolean removeReference(PropertyId refId) {
        return references.remove(refId);
    }

    /**
     *
     */
    public void clearAllReferences() {
        references.clear();
    }
}
