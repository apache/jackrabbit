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
package org.apache.jackrabbit.jcr.core.nodetype;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.NamespaceResolver;
import org.apache.jackrabbit.jcr.core.QName;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;

/**
 * A <code>NodeDefImpl</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.10 $, $Date: 2004/09/09 15:23:43 $
 */
public class NodeDefImpl extends ItemDefImpl implements NodeDef {

    private static Logger log = Logger.getLogger(NodeDefImpl.class);

    private final ChildNodeDef nodeDef;


    /**
     * Package private constructor
     *
     * @param nodeDef    child node definition
     * @param ntMgr      node type manager
     * @param nsResolver namespace resolver
     */
    NodeDefImpl(ChildNodeDef nodeDef, NodeTypeManagerImpl ntMgr, NamespaceResolver nsResolver) {
	super(nodeDef, ntMgr, nsResolver);
	this.nodeDef = nodeDef;
    }

    public ChildNodeDef unwrap() {
	return nodeDef;
    }

    //--------------------------------------------------------------< NodeDef >
    /**
     * @see NodeDef#getDefaultPrimaryType
     */
    public NodeType getDefaultPrimaryType() {
	QName ntName = nodeDef.getDefaultPrimaryType();
	try {
	    if (ntName == null) {
		// return "nt:unstructured"
		return ntMgr.getNodeType(NodeTypeRegistry.NT_UNSTRUCTURED);
	    } else {
		return ntMgr.getNodeType(ntName);
	    }
	} catch (NoSuchNodeTypeException e) {
	    // should never get here
	    log.error("default node type does not exist", e);
	    return null;
	}
    }

    /**
     * @see NodeDef#getRequiredPrimaryTypes
     */
    public NodeType[] getRequiredPrimaryTypes() {
	QName[] ntNames = nodeDef.getRequiredPrimaryTypes();
	try {
	    if (ntNames == null || ntNames.length == 0) {
		// return "nt:base"
		return new NodeType[]{ntMgr.getNodeType(NodeTypeRegistry.NT_BASE)};
	    } else {
		NodeType[] nodeTypes = new NodeType[ntNames.length];
		for (int i = 0; i < ntNames.length; i++) {
		    nodeTypes[i] = ntMgr.getNodeType(ntNames[i]);
		}
		return nodeTypes;
	    }
	} catch (NoSuchNodeTypeException e) {
	    // should never get here
	    log.error("required node type does not exist", e);
	    return new NodeType[0];
	}
    }

    /**
     * @see NodeDef#allowSameNameSibs
     */
    public boolean allowSameNameSibs() {
	return nodeDef.allowSameNameSibs();
    }
}

