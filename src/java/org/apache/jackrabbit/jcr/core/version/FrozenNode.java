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
package org.apache.jackrabbit.jcr.core.version;

import org.apache.jackrabbit.jcr.core.*;
import org.apache.jackrabbit.jcr.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.jcr.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.jcr.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr.core.state.NodeState;
import org.apache.jackrabbit.jcr.util.uuid.UUID;

import javax.jcr.*;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.version.OnParentVersionAction;

/**
 * The FrozenNode class represents a node of type 'nt:frozen' and is created
 * as such object in the ItemManager.
 *
 * @author Tobias Strasser
 * @version $Revision: 1.12 $, $Date: 2004/09/14 08:50:07 $
 */
public class FrozenNode extends NodeImpl implements Node {

    /**
     * name of the 'jcr:frozenUUID' property
     */
    public static final QName PROPNAME_FROZEN_UUID =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenUUID");

    /**
     * name of the 'jcr:frozenPrimaryType' property
     */
    public static final QName PROPNAME_FROZEN_PRIMARY_TYPE =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenPrimaryType");

    /**
     * name of the 'jcr:frozenMixinTypes' property
     */
    public static final QName PROPNAME_FROZEN_MIXIN_TYPES =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenMixinTypes");

    /**
     * name of the 'jcr:frozenChildHistories' property
     */
    public static final QName PROPNAME_FROZEN_CHILD_HISTORIES =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenChildHistories");

    /**
     * Creates a new Frozen node. This is only called by the ItemManager when
     * creating new node instances.
     *
     * @see ItemManager#createNodeInstance(NodeState, NodeDef)
     */
    public FrozenNode(ItemManager itemMgr, SessionImpl session, NodeId id,
		      NodeState state, NodeDef definition,
		      ItemLifeCycleListener[] listeners)
	    throws RepositoryException {
	super(itemMgr, session, id, state, definition, listeners);
    }

    /**
     * Initializes the frozen state of a version. i.e. copies the uuid,
     * primary types etc.
     *
     * @param node
     */
    void initFrozenState(NodeImpl node)
	    throws RepositoryException {
	if (isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
	    internalSetProperty(VersionImpl.PROPNAME_FROZEN_UUID,
		    InternalValue.create(node.getUUID()));
	}
	internalSetProperty(VersionImpl.PROPNAME_FROZEN_PRIMARY_TYPE,
		InternalValue.create(node.getProperty(VersionImpl.PROPNAME_PRIMARYTYPE).getValue(),
			PropertyType.NAME, session.getNamespaceResolver()));

	if (node.hasProperty(VersionImpl.PROPNAME_MIXINTYPES)) {
	    Value[] values = node.getProperty(VersionImpl.PROPNAME_MIXINTYPES).getValues();
	    InternalValue[] ivalues = new InternalValue[values.length];
	    for (int i = 0; i < values.length; i++) {
		ivalues[i] = InternalValue.create(values[i], PropertyType.NAME,
			session.getNamespaceResolver());
	    }
	    internalSetProperty(VersionImpl.PROPNAME_FROZEN_MIXIN_TYPES, ivalues);
	}
    }

    /**
     * Creates the frozen state from a node
     *
     * @param node
     * @throws RepositoryException
     */
    void createFrozenState(NodeImpl node) throws RepositoryException {
	// copy over the 'special' properties. please note, that they are not
	// copied in the loop below, since they have a OPV of initialze or compute.
	initFrozenState(node);

	// iterate over the properties
	PropertyIterator piter = node.getProperties();
	while (piter.hasNext()) {
	    PropertyImpl prop = (PropertyImpl) piter.nextProperty();
	    switch (prop.getDefinition().getOnParentVersion()) {
		case OnParentVersionAction.ABORT:
		    throw new RepositoryException("Checkin aborted due to OPV in " + prop.safeGetJCRPath());
		case OnParentVersionAction.COMPUTE:
		case OnParentVersionAction.IGNORE:
		case OnParentVersionAction.INITIALIZE:
		    break;
		case OnParentVersionAction.VERSION:
		case OnParentVersionAction.COPY:
		    internalCopyPropertyFrom(prop);
		    break;
	    }
	}

	// iterate over the nodes
	NodeIterator niter = node.getNodes();
	while (niter.hasNext()) {
	    NodeImpl child = (NodeImpl) niter.nextNode();
	    switch (child.getDefinition().getOnParentVersion()) {
		case OnParentVersionAction.ABORT:
		    throw new RepositoryException("Checkin aborted due to OPV in " + child.safeGetJCRPath());
		case OnParentVersionAction.COMPUTE:
		case OnParentVersionAction.IGNORE:
		case OnParentVersionAction.INITIALIZE:
		    break;
		case OnParentVersionAction.VERSION:
		    if (child.isNodeType(NodeTypeRegistry.MIX_VERSIONABLE)) {
			version(child);
		    }
		    // else ignore
		    break;
		case OnParentVersionAction.COPY:
		    copy(child);
		    break;
	    }
	}
    }


    /**
     * Versions a child node in the version storage
     *
     * @param node
     * @throws RepositoryException
     */
    private void version(NodeImpl node) throws RepositoryException {
	// create nt:frozenVersionableChild (not defined yet in spec)
	NodeTypeImpl nt = session.getNodeTypeManager().getNodeType(NodeTypeRegistry.NT_FROZEN_VERSIONABLE_CHILD);
	QName name = node.getQName();
	NodeDefImpl def = getApplicableChildNodeDef(name, nt.getQName());
	NodeImpl newChild = createChildNode(name, def, nt, null);
	newChild.internalSetProperty(VersionImpl.PROPNAME_VERSION_HISTORY,
		InternalValue.create(new UUID(node.getVersionHistory().getUUID())));
	newChild.internalSetProperty(VersionImpl.PROPNAME_BASE_VERSION,
		InternalValue.create(new UUID(node.getBaseVersion().getUUID())));
    }

    /**
     * Copies a node into the version storage by creating a nt:frozen node.
     * <p/>
     * This behaviour is currently not the desired one.
     *
     * @param node
     * @throws RepositoryException
     */
    private void copy(NodeImpl node) throws RepositoryException {
	NodeTypeImpl nt = session.getNodeTypeManager().getNodeType(NodeTypeRegistry.NT_FROZEN);
	QName name = node.getQName();
	NodeDefImpl def = getApplicableChildNodeDef(name, nt.getQName());
	FrozenNode newChild = (FrozenNode) createChildNode(name, def, nt, null);
	newChild.createFrozenState((NodeImpl) node);
    }
}
