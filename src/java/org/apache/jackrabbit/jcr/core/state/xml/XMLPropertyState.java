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
package org.apache.jackrabbit.jcr.core.state.xml;

import org.apache.jackrabbit.jcr.core.QName;
import org.apache.jackrabbit.jcr.core.state.ItemStateException;
import org.apache.jackrabbit.jcr.core.state.PersistableItemState;
import org.apache.jackrabbit.jcr.core.state.PersistenceManager;
import org.apache.jackrabbit.jcr.core.state.PersistentPropertyState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * <code>PersistentPropertyState</code> represents the persistent state of a
 * <code>Property</code>.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.7 $, $Date: 2004/08/02 16:19:50 $
 */
class XMLPropertyState extends PersistentPropertyState {

    static final long serialVersionUID = 2828529090195836265L;

    /**
     * Package private constructor
     *
     * @param name       name of the property
     * @param parentUUID the uuid of the parent node
     * @param persistMgr the persistence manager
     */
    public XMLPropertyState(QName name, String parentUUID, PersistenceManager persistMgr) {
	super(name, parentUUID, persistMgr);
    }

    //-------------------------------------------------< PersistableItemState >
    /**
     * @see PersistableItemState#reload
     */
    public synchronized void reload() throws ItemStateException {
	persistMgr.reload(this);
	// reset status
	status = STATUS_EXISTING;
    }

    /**
     * @see PersistableItemState#store
     */
    public synchronized void store() throws ItemStateException {
	persistMgr.store(this);
	// notify listeners
	if (status == STATUS_NEW) {
	    notifyStateCreated();
	} else {
	    notifyStateModified();
	}
	// reset status
	status = STATUS_EXISTING;
    }

    /**
     * @see PersistableItemState#destroy
     */
    public synchronized void destroy() throws ItemStateException {
	persistMgr.destroy(this);
	// notify listeners
	notifyStateDestroyed();
	// reset status
	status = STATUS_UNDEFINED;
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
