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

import org.apache.jackrabbit.core.QName;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * <code>PersistentNodeState</code> represents the persistent state of a
 * <code>Node</code>.
 */
public class PersistentNodeState extends NodeState implements PersistableItemState {

    static final long serialVersionUID = -371249062564922125L;

    protected final transient PersistenceManager persistMgr;

    /**
     * Public constructor
     *
     * @param uuid       the UUID of the this node
     * @param persistMgr the persistence manager
     */
    public PersistentNodeState(String uuid, PersistenceManager persistMgr) {
        super(uuid, null, null, STATUS_NEW);
        this.persistMgr = persistMgr;
    }

    /**
     * Constructor used for overlay mechanism.
     *
     * @param overlayedState other node state to overlay
     * @param initialStatus  initial status
     * @param persistMgr     persistence manager
     */
    protected PersistentNodeState(PersistentNodeState overlayedState,
                                  int initialStatus,
                                  PersistenceManager persistMgr) {

        super(overlayedState, initialStatus);

        this.persistMgr = persistMgr;
    }

    /**
     * Set the node type name. Needed for deserialization and should therefore
     * not change the internal status.
     *
     * @param nodeTypeName node type name
     */
    public void setNodeTypeName(QName nodeTypeName) {
        this.nodeTypeName = nodeTypeName;
    }

    //-------------------------------------------------< PersistableItemState >

    /**
     * @see PersistableItemState#reload
     */
    public synchronized void reload() throws ItemStateException {
        status = STATUS_UNDEFINED;
        getPersistenceManager().load(this);
        // reset status
        status = STATUS_EXISTING;
    }

    /**
     * @see PersistableItemState#store
     */
    public synchronized void store() throws ItemStateException {
        getPersistenceManager().store(this);
        // notify listeners
        if (status == STATUS_NEW) {
            notifyStateCreated();
        } else {
            notifyStateUpdated();
        }
        // reset status
        status = STATUS_EXISTING;
    }

    /**
     * @see PersistableItemState#destroy
     */
    public synchronized void destroy() throws ItemStateException {
        getPersistenceManager().destroy(this);
        // notify listeners
        notifyStateDestroyed();
        // reset status
        status = STATUS_UNDEFINED;
    }

    /**
     * Return the persistence manager to use for loading and storing data. May
     * be overridden by subclasses.
     *
     * @return persistence manager
     */
    protected PersistenceManager getPersistenceManager() {
        return persistMgr;
    }

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
