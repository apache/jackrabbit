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
        newChildDef.setRequiredPrimaryTypes(new Name[]{ NameConstants.NT_BASE, NODE_TYPE1, NameConstants.NT_FOLDER });
        newChildDef.setRequiredPrimaryTypes(new Name[]{ NODE_TYPE1, NameConstants.NT_BASE });
        newChildDef.setName(CHILD_NAME);
        newChildDef.setDeclaringNodeType(oldDef.getName());

        newDef.setChildNodeDefs(new QNodeDefinition[] { newChildDef.build() });

        // change a property def isMultiple from false to true and requiredType STRING to UNDEFINED
        // remove nt:folder from a node def's requiredPrimaryType constraint
        NodeTypeDefDiff nodeTypeDefDiff = NodeTypeDefDiff.create(oldDef.build(), newDef.build());
        System.out.println(nodeTypeDefDiff);
        assertTrue(nodeTypeDefDiff.isTrivial());

        // change a property def isMultiple from true to false and requiredType UNDEFINED to STRING
        // add nt:folder to a node def's requiredPrimaryType constraint
        nodeTypeDefDiff = NodeTypeDefDiff.create(newDef.build(), oldDef.build());
        System.out.println(nodeTypeDefDiff);
        assertTrue(nodeTypeDefDiff.isMajor());
    }
}