/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.nodetype.compact;

import junit.framework.TestCase;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeDefDiff;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;

import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

public class CompactNodeTypeDefTest extends TestCase {
    private static final String TEST_FILE = "cnd-reader-test-input.cnd";
    private static final String NS_PREFIX = "ex";
    private static final String NS_URI = "http://example.org/jackrabbit/example";
    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    private static final Name NODE_TYPE_NAME = FACTORY.create(NS_URI, "NodeType");
    private static final Name PARENT_NODE_TYPE_1 = FACTORY.create(NS_URI, "ParentNodeType1");
    private static final Name PARENT_NODE_TYPE_2 = FACTORY.create(NS_URI, "ParentNodeType2");
    private static final Name[] SUPERTYPES = new Name[]{PARENT_NODE_TYPE_1, PARENT_NODE_TYPE_2};

    private static final Name PROPERTY_NAME = FACTORY.create(NS_URI, "property");
    private static final long DEFAULT_VALUE_1 = 1;
    private static final long DEFAULT_VALUE_2 = 2;
    private static final String VALUE_CONSTRAINT = "[1,10]";

    private static final Name CHILD_NODE_NAME = FACTORY.create(NS_URI, "node");
    private static final Name REQUIRED_NODE_TYPE_1 = FACTORY.create(NS_URI, "RequiredNodeType1");
    private static final Name REQUIRED_NODE_TYPE_2 = FACTORY.create(NS_URI, "RequiredNodeType2");
    private static final Name[] REQUIRED_NODE_TYPES = new Name[]{REQUIRED_NODE_TYPE_1, REQUIRED_NODE_TYPE_2};

    private NodeTypeDef modelNodeTypeDef;

    protected void setUp() throws Exception {
        NamespaceMapping namespaceMapping = new NamespaceMapping();
        namespaceMapping.setMapping(NS_PREFIX, NS_URI);
        InternalValue dv1 = InternalValue.create(DEFAULT_VALUE_1);
        InternalValue dv2 = InternalValue.create(DEFAULT_VALUE_2);

        NamePathResolver resolver = new DefaultNamePathResolver(namespaceMapping);
        ValueConstraint vc = ValueConstraint.create(PropertyType.LONG, VALUE_CONSTRAINT, resolver);
        InternalValue[] defaultValues = new InternalValue[]{dv1, dv2};
        ValueConstraint[] valueConstraints = new ValueConstraint[]{vc};

        PropDefImpl pd = new PropDefImpl();
        pd.setName(PROPERTY_NAME);
        pd.setRequiredType(PropertyType.LONG);
        pd.setMandatory(true);
        pd.setAutoCreated(true);
        pd.setProtected(true);
        pd.setMultiple(true);
        pd.setOnParentVersion(OnParentVersionAction.VERSION);
        pd.setDefaultValues(defaultValues);
        pd.setValueConstraints(valueConstraints);
        pd.setDeclaringNodeType(NODE_TYPE_NAME);

        NodeDefImpl nd = new NodeDefImpl();
        nd.setName(CHILD_NODE_NAME);
        nd.setRequiredPrimaryTypes(REQUIRED_NODE_TYPES);
        nd.setDefaultPrimaryType(REQUIRED_NODE_TYPE_1);
        nd.setMandatory(true);
        nd.setAutoCreated(true);
        nd.setProtected(true);
        nd.setAllowsSameNameSiblings(true);
        nd.setOnParentVersion(OnParentVersionAction.VERSION);
        nd.setDeclaringNodeType(NODE_TYPE_NAME);

        modelNodeTypeDef = new NodeTypeDef();
        modelNodeTypeDef.setName(NODE_TYPE_NAME);
        modelNodeTypeDef.setSupertypes(SUPERTYPES);
        modelNodeTypeDef.setOrderableChildNodes(true);
        modelNodeTypeDef.setMixin(true);
        modelNodeTypeDef.setPrimaryItemName(PROPERTY_NAME);
        modelNodeTypeDef.setPropertyDefs(new PropDef[]{pd});
        modelNodeTypeDef.setChildNodeDefs(new NodeDef[]{nd});
    }

    public void testCompactNodeTypeDef() throws Exception {

        // Read in node type def from test file
        Reader reader = new InputStreamReader(
            getClass().getClassLoader().getResourceAsStream(TEST_FILE));
        CompactNodeTypeDefReader cndReader =
            new CompactNodeTypeDefReader(reader, TEST_FILE);
        List ntdList = cndReader.getNodeTypeDefs();
        NamespaceMapping nsm = cndReader.getNamespaceMapping();
        NodeTypeDef ntd = (NodeTypeDef)ntdList.get(0);

        // Test CND Reader by comparing imported NTD with model NTD.
        NodeTypeDefDiff diff = NodeTypeDefDiff.create(modelNodeTypeDef, ntd);
        if (diff.isModified()){
            fail("Imported node type definition is not identical to model definition");
        }
    }

    public void testCompactNodeTypeDefs() throws Exception {

        // Read in node type def from test file
        Reader reader = new InputStreamReader(
            getClass().getClassLoader().getResourceAsStream(TEST_FILE));
        CompactNodeTypeDefReader cndReader =
            new CompactNodeTypeDefReader(reader, TEST_FILE);
        List ntdList1 = cndReader.getNodeTypeDefs();
        NamespaceMapping nsm = cndReader.getNamespaceMapping();
        NamePathResolver resolver = new DefaultNamePathResolver(nsm);

        // Put imported node type def back into CND form with CND writer
        StringWriter sw = new StringWriter();
        CompactNodeTypeDefWriter.write(ntdList1, nsm, resolver, sw);

        // Rerun the reader on the product of the writer
        cndReader = new CompactNodeTypeDefReader(new StringReader(sw.toString()), TEST_FILE);
        List ntdList2 = cndReader.getNodeTypeDefs();

        if (ntdList1.size() != ntdList2.size())
            fail("Exported node type definition was not successfully read back in");
        else
            for(int k = 0; k < ntdList1.size(); k++) {
                NodeTypeDef ntd1 = (NodeTypeDef) ntdList1.get(k);
                NodeTypeDef ntd2 = (NodeTypeDef) ntdList2.get(k);

                NodeTypeDefDiff diff = NodeTypeDefDiff.create(ntd1, ntd2);
                if (diff.isModified() && !diff.isTrivial()){
                    fail("Exported node type definition was not successfully read back in. "
                            + ntd2.getName() + "differs from original");
                }
            }


    }

}
