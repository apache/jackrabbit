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
package org.apache.jackrabbit.test.api.nodetype;

import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Tests the node type creation functionality of the {@link NodeTypeManager}.
 *
 * @test
 * @sources NodeTypeCreationTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.NodeTypeCreationTest
 * @keywords level2
 */
public class NodeTypeCreationTest extends AbstractJCRTest {

    private static String ns = "http://example.org/jcr-tck/";
    
    private NodeTypeManager ntm;
    
    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        super.setUp();
        ntm = superuser.getWorkspace().getNodeTypeManager();
        super.checkSupportedOption(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED);
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNewNodeTypeTemplate() throws Exception {
        
        String ntname = "{" + ns + "}" + "littlemixin";
        
        NodeTypeTemplate ntt = ntm.createNodeTypeTemplate();
        
        ntt.setName(ntname);
        ntt.setAbstract(false);
        ntt.setDeclaredSuperTypeNames(null);
        ntt.setMixin(true);
        ntt.setOrderableChildNodes(false);
        ntt.setQueryable(false);
        ntt.setPrimaryItemName(null);
        
        List pdefs = ntt.getPropertyDefinitionTemplates();
        pdefs.add(createBooleanPropTemplate());
    }

    public void testPropertyDefinitionTemplate() throws Exception {
        PropertyDefinitionTemplate pdt = createBooleanPropTemplate();
        
        String pref = superuser.getNamespacePrefix(ns);
        
        assertEquals(pref + ":" + "boolean", pdt.getName());
        assertEquals(false, pdt.isAutoCreated());
        assertEquals(false, pdt.isMandatory());
        assertEquals(OnParentVersionAction.IGNORE, pdt.getOnParentVersion());
        assertEquals(false, pdt.isProtected());
        assertEquals(PropertyType.BOOLEAN, pdt.getRequiredType());
        assertEquals(null, pdt.getValueConstraints());
        assertEquals(null, pdt.getDefaultValues());
        assertEquals(false, pdt.isMultiple());
        String[] qo = pdt.getAvailableQueryOperators();
        assertEquals(1, qo.length);
        assertEquals(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qo[0]);
        assertEquals(false, pdt.isFullTextSearchable());
        assertEquals(false, pdt.isQueryOrderable());
    }
    
    
    private PropertyDefinitionTemplate createBooleanPropTemplate() throws RepositoryException {
        String propname = "{" + ns + "}" + "boolean";
        
        PropertyDefinitionTemplate pdt = ntm.createPropertyDefinitionTemplate();
        pdt.setName(propname);
        pdt.setAutoCreated(false);
        pdt.setMandatory(false);
        pdt.setOnParentVersion(OnParentVersionAction.IGNORE);
        pdt.setProtected(false);
        pdt.setRequiredType(PropertyType.BOOLEAN);
        pdt.setValueConstraints(null);
        pdt.setDefaultValues(null);
        pdt.setMultiple(false);
        pdt.setAvailableQueryOperators(new String[] { QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO });
        pdt.setFullTextSearchable(false);
        pdt.setQueryOrderable(false);

        return pdt;
    }
    
}