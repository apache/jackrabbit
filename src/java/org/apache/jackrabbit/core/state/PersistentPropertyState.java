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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.QName;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * <code>PersistentPropertyState</code> represents the persistent state of a
 * <code>Property</code>.
 */
public class PersistentPropertyState extends PropertyState implements PersistableItemState {

    static final long serialVersionUID = -8919019313955817383L;

    protected final transient PersistenceManager persistMgr;

    /**
     * Public constructor
     *
     * @param name       name of the property
     * @param parentUUID the uuid of the parent node
     * @param persistMgr the persistence manager
     */
    public PersistentPropertyState(QName name, String parentUUID, PersistenceManager persistMgr) {
        super(name, parentUUID, STATUS_NEW);
        this.persistMgr = persistMgr;
    }

    /**
     * Constructor used for overlay mechanism.
     *
     * @param overlayedState other node state to overlay
     * @param initialStatus  initial status
     * @param persistMgr     persistence manager
     */
    protected PersistentPropertyState(PersistentPropertyState overlayedState,
                                      int initialStatus,
                                      PersistenceManager persistMgr) {

        super(overlayedState, initialStatus);

        this.persistMgr = persistMgr;
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
