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
package org.apache.jackrabbit.core.nodetype;

/**
 * A <code>NodeTypeDefDiff</code> represents the result of the comparison of
 * two node type definitions.
 */
public class NodeTypeDefDiff {

    // type constants
    public static final int TRIVIAL = 0;
    public static final int MINOR = 0;
    public static final int MAJOR = 0;

    private final NodeTypeDef oldDef;
    private final NodeTypeDef newDef;
    private int type;
/*
    private QName name;
    private QName[] supertypes;
    private boolean mixin;
    private boolean orderableChildNodes;
    private PropDef[] propDefs;
    private ChildNodeDef[] nodeDefs;
*/
    /**
     * Constructor
     */
    private NodeTypeDefDiff(NodeTypeDef oldDef, NodeTypeDef newDef) {
	this.oldDef = oldDef;
	this.newDef = newDef;
	init();
    }

    /**
     *
     */
    private void init() {
	// @todo build diff and set type
    }

    /**
     *
     * @param oldDef
     * @param newDef
     * @return
     */
    public static NodeTypeDefDiff create(NodeTypeDef oldDef, NodeTypeDef newDef) {
	if (oldDef == null || newDef == null) {
	    throw new IllegalArgumentException("arguments can not be null");
	}
	if (!oldDef.getName().equals(newDef.getName())) {
	    throw new IllegalArgumentException("node type names must match");
	}
	return new NodeTypeDefDiff(oldDef, newDef);
    }

    /**
     *
     * @return
     */ 
    public boolean isTrivial() {
	return type == TRIVIAL;
    }

    /**
     *
     * @return
     */
    public int getType() {
	return type;
    }
}
