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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This Class implements a virtual node state that represents a nt:versionLabels.
 * node state.
 */
public class VersionLabelsNodeState extends VirtualNodeState implements Constants {

    /**
     * the internal version
     */
    private final InternalVersionHistory vh;

    /**
     * need to cachge the property states
     */
    private final HashMap labelStates = new HashMap();

    /**
     * Creates a new version node state
     *
     * @param vm
     * @param vh
     * @param parentUUID
     * @throws javax.jcr.RepositoryException
     */
    protected VersionLabelsNodeState(VersionItemStateProvider vm, InternalVersionHistory vh,
                                     String parentUUID, String uuid)
            throws RepositoryException {
        super(vm, parentUUID, uuid, NT_VERSIONLABELS, new QName[0]);
        this.vh = vh;
    }

    public synchronized boolean hasPropertyEntry(QName propName) {
        if (super.hasPropertyEntry(propName)) {
            return true;
        }
        return vh.getVersionByLabel(propName) != null;
    }

    public synchronized PropertyEntry getPropertyEntry(QName propName) {
        if (super.hasPropertyEntry(propName)) {
            return super.getPropertyEntry(propName);
        }
        if (hasPropertyEntry(propName)) {
            return createPropertyEntry(propName);
        }
        return null;
    }

    public synchronized List getPropertyEntries() {
        ArrayList list = new ArrayList(super.getPropertyEntries());
        QName[] labels = vh.getVersionLabels();
        for (int i = 0; i < labels.length; i++) {
            list.add(createPropertyEntry(labels[i]));
        }
        return list;
    }

    public VirtualPropertyState[] getProperties() {
        List list = getPropertyEntries();
        VirtualPropertyState[] states = new VirtualPropertyState[list.size()];
        for (int i = 0; i < states.length; i++) {
            try {
                states[i] = getProperty(((PropertyEntry) list.get(i)).getName());
            } catch (NoSuchItemStateException e) {
                // hmmm?
            }
        }
        return states;
    }

    public InternalValue[] getPropertyValues(QName name) throws NoSuchItemStateException {
        return getProperty(name).getValues();
    }

    public InternalValue getPropertyValue(QName name) throws NoSuchItemStateException {
        return getProperty(name).getValues()[0];
    }

    public VirtualPropertyState getProperty(QName name) throws NoSuchItemStateException {
        if (super.hasPropertyEntry(name)) {
            return super.getProperty(name);
        } else {
            InternalVersion v = vh.getVersionByLabel(name);
            if (v != null) {
                try {
                    VirtualPropertyState state = (VirtualPropertyState) labelStates.get(name);
                    if (state == null) {
                        state = stateMgr.createPropertyState(this, name, PropertyType.REFERENCE, false);
                        labelStates.put(name, state);
                    }
                    state.setValues(new InternalValue[]{InternalValue.create(UUID.fromString(v.getId()))});
                    return state;
                } catch (VersionException e) {
                    throw new NoSuchItemStateException(name.toString(), e);
                } catch (RepositoryException e) {
                    throw new NoSuchItemStateException(name.toString(), e);
                }
            }
        }
        throw new NoSuchItemStateException(name.toString());
    }

}
