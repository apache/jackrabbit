/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;

import java.util.*;
import java.io.Serializable;

/**
 * <code>NodeReferences</code> represents the references (i.e. properties of
 * type <code>REFERENCE</code>) to a particular node (denoted by its uuid).
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.1 $, $Date: 2004/09/06 07:29:10 $
 */
public class NodeReferences implements Serializable {

    static final long serialVersionUID = 7007727035982680717L;

    /**
     * id of the target node
     */
    protected NodeId targetId;

    /**
     * list of PropertyId's (i.e. the id's of the properties that refer to
     * the target node denoted by <code>targetId</code>).
     * <p/>
     * note that the list can contain duplicate entries because a specific
     * REFERENCE property can contain multiple references (if it's multi-valued)
     * to potentially the same target node.
     */
    protected List references;

    /**
     * Package private constructor
     *
     * @param targetId
     */
    public NodeReferences(NodeId targetId) {
	this.targetId = targetId;
	references = new ArrayList();
    }

    /**
     *
     * @return
     */
    public NodeId getTargetId() {
	return targetId;
    }

    /**
     *
     * @return
     */
    public boolean hasReferences() {
	return !references.isEmpty();
    }

    /**
     *
     * @return
     */
    public Collection getReferences() {
	return Collections.unmodifiableCollection(references);
    }

    /**
     *
     * @param refId
     */
    public void addReference(PropertyId refId) {
	references.add(refId);
    }

    /**
     *
     * @param references
     */
    public void addAllReferences(Set references) {
	references.addAll(references);
    }

    /**
     *
     * @param refId
     * @return
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
