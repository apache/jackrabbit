/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core.state;

import org.apache.jackrabbit.jcr.core.QName;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * <code>PersistentNodeState</code> represents the persistent state of a
 * <code>Node</code>.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.9 $, $Date: 2004/08/02 16:19:48 $
 */
public abstract class PersistentNodeState extends NodeState implements PersistableItemState {

    static final long serialVersionUID = -371249062564922125L;

    transient protected final PersistenceManager persistMgr;

    /**
     * Package private constructor
     *
     * @param uuid         the UUID of the this node
     * @param nodeTypeName node type of this node
     * @param parentUUID   the UUID of the parent node
     * @param persistMgr   the persistence manager
     */
    protected PersistentNodeState(String uuid, QName nodeTypeName, String parentUUID, PersistenceManager persistMgr) {
	super(uuid, nodeTypeName, parentUUID, STATUS_NEW);
	this.persistMgr = persistMgr;
    }

    //-------------------------------------------------< PersistableItemState >
    /**
     * @see PersistableItemState#reload
     */
    public abstract void reload() throws ItemStateException;

    /**
     * @see PersistableItemState#store
     */
    public abstract void store() throws ItemStateException;

    /**
     * @see PersistableItemState#destroy
     */
    public abstract void destroy() throws ItemStateException;

    //-------------------------------------------------< Serializable support >
    private void writeObject(ObjectOutputStream out) throws IOException {
	// delegate to default implementation
	out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
	// delegate to default implementation
	in.defaultReadObject();
    }
}
