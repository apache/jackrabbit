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
package org.apache.jackrabbit.jcr.core.version;

import org.apache.jackrabbit.jcr.core.*;
import org.apache.jackrabbit.jcr.core.state.NodeState;
import org.apache.jackrabbit.jcr.util.uuid.UUID;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.version.Version;
import java.util.Calendar;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This Class implements the Version representation of the node.
 *
 * @author Tobias Strasser
 * @version $Revision: 1.12 $, $Date: 2004/09/14 12:49:00 $
 */
public class VersionImpl extends FrozenNode implements Version {

    /**
     * name of the 'jcr:versionLabels' property
     */
    public static final QName PROPNAME_VERSION_LABELS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");

    /**
     * name of the 'jcr:predecessors' property
     */
    public static final QName PROPNAME_PREDECESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "predecessors");

    /**
     * name of the 'jcr:successors' property
     */
    public static final QName PROPNAME_SUCCESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "successors");

    /**
     * name of the 'jcr:isCheckedOut' property
     */
    public static final QName PROPNAME_IS_CHECKED_OUT = new QName(NamespaceRegistryImpl.NS_JCR_URI, "isCheckedOut");

    /**
     * name of the 'jcr:versionHistory' property
     */
    public static final QName PROPNAME_VERSION_HISTORY = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionHistory");

    /**
     * name of the 'jcr:baseVersion' property
     */
    public static final QName PROPNAME_BASE_VERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "baseVersion");

    /** cache for version labels */
    private Set cachedVersionLabels;

    /**
     * Creates a new Version node. This is only called by the ItemManager when
     * creating new node instances.
     *
     * @see org.apache.jackrabbit.jcr.core.ItemManager#createNodeInstance(org.apache.jackrabbit.jcr.core.state.NodeState, javax.jcr.nodetype.NodeDef)
     */
    public VersionImpl(ItemManager itemMgr, SessionImpl session, NodeId id,
		       NodeState state, NodeDef definition,
		       ItemLifeCycleListener[] listeners)
	    throws RepositoryException {
	super(itemMgr, session, id, state, definition, listeners);
    }


    /**
     * @see Version#getCreated()
     */
    public Calendar getCreated() throws RepositoryException {
	// no check for NULL needed since its mandatory
	return getProperty(PROPNAME_CREATED).getDate();
    }

    /**
     * @see Version#getVersionLabels()
     */
    public String[] getVersionLabels() throws RepositoryException {
	initLabelCache();
	return (String[]) cachedVersionLabels.toArray(new String[cachedVersionLabels.size()]);
    }

    /**
     * Checks if this version contains the given version label.
     *
     * @param label the param to check
     * @throws RepositoryException if an error occurrs
     *                             <p/>
     *                             todo: add to spec
     */
    public boolean hasVersionLabel(String label) throws RepositoryException {
	initLabelCache();
	return cachedVersionLabels.contains(label);
    }

    /**
     * @see Version#addVersionLabel(java.lang.String)
     */
    public void addVersionLabel(String label) throws RepositoryException {
	// delegate to version history (will probably change in spec)
	getHistory().addVersionLabel(this, label);
    }

    /**
     * @see Version#removeVersionLabel(java.lang.String)
     */
    public void removeVersionLabel(String label) throws RepositoryException {
	// delegate to version history (will probably change in spec)
	getHistory().removeVersionLabel(label);
    }

    /**
     * @see Version#getSuccessors()
     */
    public Version[] getSuccessors() throws RepositoryException {
	if (hasProperty(PROPNAME_SUCCESSORS)) {
	    Value[] values = getProperty(PROPNAME_SUCCESSORS).getValues();
	    Version[] preds = new Version[values.length];
	    for (int i = 0; i < values.length; i++) {
		preds[i] = (Version) session.getNodeByUUID(values[i].getString());
	    }
	    return preds;
	}
	return new Version[0];
    }

    /**
     * Adds a successor to the jcr:successor list
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param succ
     */
    void internalAddSuccessor(VersionImpl succ) throws RepositoryException {
	Version[] successors = getSuccessors();
	InternalValue[] values = new InternalValue[successors.length + 1];
	for (int i = 0; i < successors.length; i++) {
	    values[i] = InternalValue.create(new UUID(successors[i].getUUID()));
	}
	values[successors.length] = InternalValue.create(new UUID(succ.getUUID()));
	internalSetProperty(PROPNAME_SUCCESSORS, values);
    }

    /**
     * Detaches itself from the version graph.
     *
     * @throws RepositoryException
     */
    void internalDetach() throws RepositoryException {
	// detach this from all successors
	VersionImpl[] succ = (VersionImpl[]) getSuccessors();
	for (int i = 0; i < succ.length; i++) {
	    succ[i].internalDetachPredecessor(this);
	}
	// detach this from all predecessors
	VersionImpl[] pred = (VersionImpl[]) getPredecessors();
	for (int i = 0; i < pred.length; i++) {
	    pred[i].internalDetachSuccessor(this);
	}
	// clear properties
	internalSetProperty(PROPNAME_PREDECESSORS, new InternalValue[0]);
	internalSetProperty(PROPNAME_SUCCESSORS, new InternalValue[0]);
    }

    /**
     * Removes the successor V of this successors list and adds all of Vs
     * successors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachSuccessor(VersionImpl v) throws RepositoryException {
	Version[] vsucc = v.getSuccessors();
	Version[] successors = getSuccessors();
	InternalValue[] values = new InternalValue[successors.length - 1 + vsucc.length];
	int idx = 0;
	// copy successors but ignore 'v'
	for (int i = 0; i < successors.length; i++) {
	    if (!successors[i].isSame(v)) {
		values[idx++] = InternalValue.create(new UUID(successors[i].getUUID()));
	    }
	}
	// attach v's successors
	for (int i = 0; i < vsucc.length; i++) {
	    values[idx++] = InternalValue.create(new UUID(vsucc[i].getUUID()));
	}
	internalSetProperty(PROPNAME_SUCCESSORS, values);
    }

    /**
     * Removes the predecessor V of this predecessor list and adds all of Vs
     * predecessors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachPredecessor(VersionImpl v) throws RepositoryException {
	Version[] vpred = v.getPredecessors();
	Version[] tpred = getPredecessors();
	InternalValue[] values = new InternalValue[tpred.length - 1 + vpred.length];
	int idx = 0;
	// copy predecessors but ignore 'v'
	for (int i = 0; i < tpred.length; i++) {
	    if (!tpred[i].isSame(v)) {
		values[idx++] = InternalValue.create(new UUID(tpred[i].getUUID()));
	    }
	}
	// attach v's predecessors
	for (int i = 0; i < vpred.length; i++) {
	    values[idx++] = InternalValue.create(new UUID(vpred[i].getUUID()));
	}
	internalSetProperty(PROPNAME_PREDECESSORS, values);
    }

    /**
     * @see NodeImpl#internalSetProperty(org.apache.jackrabbit.jcr.core.QName, org.apache.jackrabbit.jcr.core.InternalValue)
     */
    public Property internalSetProperty(QName name,
					InternalValue value)
	    throws ValueFormatException, RepositoryException {
	return super.internalSetProperty(name, value);
    }

    /**
     * @see NodeImpl#internalSetProperty(org.apache.jackrabbit.jcr.core.QName, org.apache.jackrabbit.jcr.core.InternalValue[])
     */
    protected Property internalSetProperty(QName name,
					   InternalValue[] value)
	    throws ValueFormatException, RepositoryException {
	return super.internalSetProperty(name, value);
    }

    /**
     * @see Version#addVersionLabel(java.lang.String)
     */
    protected void internalAddVersionLabel(String label) throws RepositoryException {
	initLabelCache();
	cachedVersionLabels.add(label);
	saveLabelCache();
    }

    /**
     * @see Version#addVersionLabel(java.lang.String)
     */
    protected void internalRemoveVersionLabel(String label) throws RepositoryException {
	initLabelCache();
	cachedVersionLabels.remove(label);
	saveLabelCache();
    }

    /**
     * Initializes / Loads the version label cache
     */
    private void initLabelCache() throws RepositoryException {
	if (cachedVersionLabels==null) {
	    cachedVersionLabels=new HashSet();
	    if (hasProperty(PROPNAME_VERSION_LABELS)) {
		Value[] values = getProperty(PROPNAME_VERSION_LABELS).getValues();
		for (int i = 0; i < values.length; i++) {
		    cachedVersionLabels.add(values[i].getString());
		}
	    }
	}
    }

    /**
     * Saves the current labels in the cache to the 'VersionLabels' property
     * of this node.
     * @throws RepositoryException
     */
    private void saveLabelCache() throws RepositoryException {
	InternalValue[] newValues = new InternalValue[cachedVersionLabels.size()];
	Iterator iter = cachedVersionLabels.iterator();
	for (int i=0; i<newValues.length; i++) {
	    newValues[i] = InternalValue.create((String) iter.next());
	}
	internalSetProperty(PROPNAME_VERSION_LABELS, newValues);
	save();
    }

    /**
     * Returns the version history of this version and not the extended node.
     *
     * @return the version history of this version graph
     * @throws RepositoryException
     */
    private VersionHistoryImpl getHistory() throws RepositoryException {
	return (VersionHistoryImpl) getParent();
    }


    /**
     * Checks if this version is more recent than the given version <code>v</code>.
     * A version is more recent if and only if it is a successor (or a successor
     * of a successor, etc., to any degree of separation) of the compared one.
     *
     * @param v the version to check
     * @return <code>true</code> if the version is more recent;
     *         <code>false</code> otherwise.
     * @throws RepositoryException if an error occurrs.
     */
    public boolean isMoreRecent(Version v) throws RepositoryException {
	VersionIteratorImpl iter = new VersionIteratorImpl(v);
	while (iter.hasNext()) {
	    if (iter.nextVersion().isSame(this)) {
		return true;
	    }
	}
	return false;
    }
}
