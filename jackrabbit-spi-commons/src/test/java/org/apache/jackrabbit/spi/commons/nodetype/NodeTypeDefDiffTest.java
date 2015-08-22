/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.commons.nodetype;

import junit.framework.TestCase;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import javax.jcr.PropertyType;

public class NodeTypeDefDiffTest extends TestCase {

    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    private static final Name NODE_TYPE1 = FACTORY.create("{}nodeType1");
    private static final Name PROP_NAME = FACTORY.create("{}prop");
    private static final Name CHILD_NAME = FACTORY.create("{}child");

    public void testChangedPropertyDefinition() throws Exception {
        // old node type definition
        QNodeTypeDefinitionBuilder oldDef = new QNodeTypeDefinitionBuilder();
        oldDef.setName(NODE_TYPE1);
        oldDef.setSupertypes(new Name[] { NameConstants.NT_BASE });

        QPropertyDefinitionBuilder oldPropDef = new QPropertyDefinitionBuilder();
        oldPropDef.setDeclaringNodeType(NODE_TYPE1);
        oldPropDef.setName(PROP_NAME);
        oldPropDef.setRequiredType(PropertyType.STRING);
        oldPropDef.setMultiple(false);

        oldDef.setPropertyDefs(new QPropertyDefinition[]{oldPropDef.build()});

        QNodeDefinitionBuilder oldChildDef = new QNodeDefinitionBuilder();
        oldChildDef.setRequiredPrimaryTypes(new Name[]{ NODE_TYPE1, NameConstants.NT_FOLDER });
        oldChildDef.setName(CHILD_NAME);
        oldChildDef.setDeclaringNodeType(oldDef.getName());

        oldDef.setChildNodeDefs(new QNodeDefinition[] { oldChildDef.build() });

        // new node type definition
        QNodeTypeDefinitionBuilder newDef = new QNodeTypeDefinitionBuilder();
        newDef.setName(NODE_TYPE1);
        newDef.setSupertypes(new Name[] { NameConstants.NT_BASE });

        QPropertyDefinitionBuilder newPropDef = new QPropertyDefinitionBuilder();
        newPropDef.setDeclaringNodeType(NODE_TYPE1);
        newPropDef.setName(PROP_NAME);
        newPropDef.setRequiredType(PropertyType.UNDEFINED);
        newPropDef.setMultiple(true);

        newDef.setPropertyDefs(new QPropertyDefinition[]{newPropDef.build()});

        QNodeDefinitionBuilder newChildDef = new QNodeDefinitionBuilder();
        newChildDef.setRequiredPrimaryTypes(new Name[]{ NODE_TYPE1, NameConstants.NT_BASE });
        newChildDef.setName(CHILD_NAME);
        newChildDef.setDeclaringNodeType(oldDef.getName());

        newDef.setChildNodeDefs(new QNodeDefinition[] { newChildDef.build() });

        // change a property def isMultiple from false to true and requiredType STRING to UNDEFINED
        // remove nt:folder from a node def's requiredPrimaryType constraint
        NodeTypeDefDiff nodeTypeDefDiff = NodeTypeDefDiff.create(oldDef.build(), newDef.build());
        assertTrue(nodeTypeDefDiff.isTrivial());

        // change a property def isMultiple from true to false and requiredType UNDEFINED to STRING
        // add nt:folder to a node def's requiredPrimaryType constraint
        nodeTypeDefDiff = NodeTypeDefDiff.create(newDef.build(), oldDef.build());
        assertTrue(nodeTypeDefDiff.isMajor());
    }

    /**
     * If we add a residual property definition of a different type, it should be recognized as
     * a new definition not as a change in definition
     */
    public void testMultipleResidualPropertyDefinitions() throws Exception {
        // old node type definition
        QNodeTypeDefinitionBuilder oldDef = new QNodeTypeDefinitionBuilder();
        oldDef.setName(NODE_TYPE1);
        oldDef.setSupertypes(new Name[] { NameConstants.NT_BASE });

        QPropertyDefinitionBuilder oldPropDef = new QPropertyDefinitionBuilder();
        oldPropDef.setDeclaringNodeType(NODE_TYPE1);
        oldPropDef.setName(PROP_NAME);
        oldPropDef.setRequiredType(PropertyType.STRING);
        oldPropDef.setMultiple(false);

        oldDef.setPropertyDefs(new QPropertyDefinition[]{ oldPropDef.build() });

        // new node type definition
        QNodeTypeDefinitionBuilder newDef = new QNodeTypeDefinitionBuilder();
        newDef.setName(NODE_TYPE1);
        newDef.setSupertypes(new Name[] { NameConstants.NT_BASE });

        QPropertyDefinitionBuilder newPropDef1 = oldPropDef;
        QPropertyDefinitionBuilder newPropDef2 = new QPropertyDefinitionBuilder();
        newPropDef2.setDeclaringNodeType(NODE_TYPE1);
        newPropDef2.setName(PROP_NAME);
        newPropDef2.setRequiredType(PropertyType.BOOLEAN);
        newPropDef2.setMultiple(false);

        newDef.setPropertyDefs(new QPropertyDefinition[]{ newPropDef1.build(), newPropDef2.build() });

        NodeTypeDefDiff nodeTypeDefDiff = NodeTypeDefDiff.create(oldDef.build(), newDef.build());
        assertTrue(nodeTypeDefDiff.isTrivial());
    }

    public void testChangedSameNameChildNodeDefinition() throws Exception {
        // old node type definition
        QNodeTypeDefinitionBuilder oldDef = new QNodeTypeDefinitionBuilder();
        oldDef.setName(NODE_TYPE1);
        oldDef.setSupertypes(new Name[] { NameConstants.NT_BASE });

        QNodeDefinitionBuilder oldChildDef1 = new QNodeDefinitionBuilder();
        oldChildDef1.setRequiredPrimaryTypes(new Name[]{ NameConstants.NT_FOLDER });
        oldChildDef1.setName(CHILD_NAME);
        oldChildDef1.setDeclaringNodeType(oldDef.getName());

        QNodeDefinitionBuilder oldChildDef2 = new QNodeDefinitionBuilder();
        oldChildDef2.setRequiredPrimaryTypes(new Name[]{ NameConstants.NT_FILE });
        oldChildDef2.setName(CHILD_NAME);
        oldChildDef2.setDeclaringNodeType(oldDef.getName());

        oldDef.setChildNodeDefs(new QNodeDefinition[] { oldChildDef1.build(), oldChildDef2.build() });

        // new node type definition
        QNodeTypeDefinitionBuilder newDef = new QNodeTypeDefinitionBuilder();
        newDef.setName(NODE_TYPE1);
        newDef.setSupertypes(new Name[] { NameConstants.NT_BASE });

        QNodeDefinitionBuilder newChildDef1 = new QNodeDefinitionBuilder();
        newChildDef1.setRequiredPrimaryTypes(new Name[]{ NameConstants.NT_FOLDER });
        newChildDef1.setName(CHILD_NAME);
        newChildDef1.setDeclaringNodeType(oldDef.getName());

        QNodeDefinitionBuilder newChildDef2 = new QNodeDefinitionBuilder();
        newChildDef2.setRequiredPrimaryTypes(new Name[]{ NameConstants.NT_FILE });
        newChildDef2.setName(CHILD_NAME);
        newChildDef2.setDeclaringNodeType(oldDef.getName());

        QNodeDefinitionBuilder newChildDef3 = new QNodeDefinitionBuilder();
        newChildDef3.setRequiredPrimaryTypes(new Name[]{ NameConstants.NT_RESOURCE });
        newChildDef3.setName(CHILD_NAME);
        newChildDef3.setDeclaringNodeType(oldDef.getName());

        newDef.setChildNodeDefs(new QNodeDefinition[] { newChildDef1.build(), newChildDef2.build(), newChildDef3.build() });

        // from old to new is trivial
        NodeTypeDefDiff nodeTypeDefDiff = NodeTypeDefDiff.create(oldDef.build(), newDef.build());
        assertTrue(nodeTypeDefDiff.isTrivial());

        // .. but the reverse is not
        nodeTypeDefDiff = NodeTypeDefDiff.create(newDef.build(), oldDef.build());
        assertTrue(nodeTypeDefDiff.isMajor());

    }
}