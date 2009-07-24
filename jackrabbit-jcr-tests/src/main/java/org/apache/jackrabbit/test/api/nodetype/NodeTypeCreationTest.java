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
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.ConstraintViolationException;
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

    private String expandedPropName;
    private String jcrPropName;

    private NodeTypeManager ntm;
    
    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        super.setUp();
        ntm = superuser.getWorkspace().getNodeTypeManager();
        super.checkSupportedOption(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED);

        expandedPropName = "{" + NS_JCR_URI + "}" + "boolean";
        jcrPropName = superuser.getNamespacePrefix(NS_JCR_URI) + ":boolean";
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testEmptyNodeTypeTemplate() throws Exception {

        NodeTypeTemplate ntt = ntm.createNodeTypeTemplate();
        assertNull(ntt.getName());

        assertFalse(ntt.isMixin());
        assertFalse(ntt.isAbstract());
        assertFalse(ntt.hasOrderableChildNodes());
        // TODO: see https://jsr-283.dev.java.net/issues/show_bug.cgi?id=798
        assertTrue(ntt.isQueryable());

        // TODO see https://jsr-283.dev.java.net/issues/show_bug.cgi?id=797        
        assertNotNull(ntt.getDeclaredSupertypeNames());
        assertEquals(0, ntt.getDeclaredSupertypeNames().length);

        assertNull(ntt.getPrimaryItemName());

        assertNull(ntt.getDeclaredChildNodeDefinitions());
        assertNull(ntt.getDeclaredPropertyDefinitions());

        assertNotNull(ntt.getNodeDefinitionTemplates());
        assertTrue(ntt.getNodeDefinitionTemplates().isEmpty());

        assertNotNull(ntt.getPropertyDefinitionTemplates());
        assertTrue(ntt.getPropertyDefinitionTemplates().isEmpty());
    }
    
    public void testNewNodeTypeTemplate() throws Exception {
        
        String expandedName = "{" + NS_MIX_URI + "}" + "littlemixin";
        String jcrName = superuser.getNamespacePrefix(NS_MIX_URI) + ":littlemixin";
        
        NodeTypeTemplate ntt = ntm.createNodeTypeTemplate();

        ntt.setName(expandedName);
        assertEquals(jcrName, ntt.getName());
        ntt.setName(jcrName);
        assertEquals(jcrName, ntt.getName());

        ntt.setAbstract(false);
        assertFalse(ntt.isAbstract());

        try {
            ntt.setDeclaredSuperTypeNames(null);
            fail("null isn't a valid array of jcr name");
        } catch (ConstraintViolationException e) {
            // success
        }
        assertNotNull(ntt.getDeclaredSupertypeNames());
        assertEquals(0, ntt.getDeclaredSupertypeNames().length);

        ntt.setDeclaredSuperTypeNames(new String[] {mixReferenceable});
        assertNotNull(ntt.getDeclaredSupertypeNames());
        assertEquals(1, ntt.getDeclaredSupertypeNames().length);
        assertEquals(mixReferenceable, ntt.getDeclaredSupertypeNames()[0]);

        ntt.setMixin(true);
        assertTrue(ntt.isMixin());

        ntt.setOrderableChildNodes(true);
        assertTrue(ntt.hasOrderableChildNodes());

        ntt.setQueryable(false);
        assertFalse(ntt.isQueryable());

        ntt.setPrimaryItemName(null);
        assertNull(ntt.getPrimaryItemName());

        ntt.setPrimaryItemName(jcrPrimaryType);
        assertEquals(jcrPrimaryType, ntt.getPrimaryItemName());

        PropertyDefinitionTemplate pdTemplate = createBooleanPropTemplate();

        List pdefs = ntt.getPropertyDefinitionTemplates();
        pdefs.add(pdTemplate);

        assertNotNull(ntt.getDeclaredPropertyDefinitions());
        assertEquals(1, ntt.getDeclaredPropertyDefinitions().length);
        assertEquals(pdTemplate, ntt.getDeclaredPropertyDefinitions()[0]);

        pdefs = ntt.getPropertyDefinitionTemplates();
        assertEquals(1, pdefs.size());
        assertEquals(pdTemplate, pdefs.get(0));

        NodeDefinitionTemplate ndTemplate = ntm.createNodeDefinitionTemplate();

        List ndefs = ntt.getNodeDefinitionTemplates();
        ndefs.add(ndTemplate);

        assertNotNull(ntt.getDeclaredChildNodeDefinitions());
        assertEquals(1, ntt.getDeclaredChildNodeDefinitions().length);
        assertEquals(ndTemplate, ntt.getDeclaredChildNodeDefinitions()[0]);

        ndefs = ntt.getNodeDefinitionTemplates();
        assertEquals(1, ndefs.size());
        assertEquals(ndTemplate, ndefs.get(0));
    }

    public void testEmptyPropertyDefinitionTemplate() throws Exception {
        PropertyDefinitionTemplate pdt = ntm.createPropertyDefinitionTemplate();

        assertNull(pdt.getName());
        assertFalse(pdt.isAutoCreated());
        assertFalse(pdt.isMandatory());
        assertFalse(pdt.isProtected());
        assertEquals(OnParentVersionAction.COPY, pdt.getOnParentVersion());
        assertNull(pdt.getDeclaringNodeType());

        assertEquals(PropertyType.STRING, pdt.getRequiredType());
        assertFalse(pdt.isMultiple());
        assertNull(pdt.getValueConstraints());
        assertNull(pdt.getDefaultValues());

        // TODO: add tests for default values of (missing definition in API)
        // getAvailableQueryOperators
        // isFullTextSearchable
        // isQueryOrderable

    }

    public void testPropertyDefinitionTemplate() throws Exception {
        PropertyDefinitionTemplate pdt = createBooleanPropTemplate();

        assertEquals(jcrPropName, pdt.getName());
        try {
            pdt.setName(null);
            fail("null isn't a valid JCR name");
        } catch (ConstraintViolationException e) {
            // success
        }


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

    public void testSetDefaultValues() throws Exception {

        PropertyDefinitionTemplate pdt = ntm.createPropertyDefinitionTemplate();
        pdt.setRequiredType(PropertyType.LONG);

        pdt.setDefaultValues(null);
        assertNull(pdt.getDefaultValues());

        pdt.setDefaultValues(new Value[0]);
        assertNotNull(pdt.getDefaultValues());
        assertEquals(0, pdt.getDefaultValues().length);

        pdt.setDefaultValues(new Value[] { superuser.getValueFactory().createValue(24)});
        assertNotNull(pdt.getDefaultValues());
        assertEquals(1, pdt.getDefaultValues().length);
        assertEquals(24, pdt.getDefaultValues()[0].getLong());
        assertEquals(PropertyType.LONG, pdt.getDefaultValues()[0].getType());
    }

    public void testSetValueConstraints() throws Exception {

        // TODO: PropertyDefinitionTemplate: test setting value constraints
        // TODO: PropertyDefinitionTemplate: test setting name/path constraints
        // see  https://jsr-283.dev.java.net/issues/show_bug.cgi?id=794
    }

    public void testEmptyNodeDefinitionTemplate() throws Exception {
        NodeDefinitionTemplate ndt = ntm.createNodeDefinitionTemplate();

        assertNull(ndt.getName());
        assertFalse(ndt.isAutoCreated());
        assertFalse(ndt.isMandatory());
        assertFalse(ndt.isProtected());
        assertEquals(OnParentVersionAction.COPY, ndt.getOnParentVersion());
        assertNull(ndt.getDeclaringNodeType());

        assertNull(ndt.getRequiredPrimaryTypes());
        assertNull(ndt.getRequiredPrimaryTypeNames());
        assertNull(ndt.getDefaultPrimaryType());
        assertNull(ndt.getDefaultPrimaryTypeName());
        assertFalse(ndt.allowsSameNameSiblings());
    }

    public void testNodeDefinitionTemplate() throws Exception {
        NodeDefinitionTemplate ndt = ntm.createNodeDefinitionTemplate();

        try {
            ndt.setName(null);
            fail("null isn't a valid JCR name");
        } catch (ConstraintViolationException e) {
            // success
        }

        String expandedName = "{" + NS_JCR_URI + "}" + "content";
        String jcrName = superuser.getNamespacePrefix(NS_JCR_URI) + ":content";
        ndt.setName(expandedName);
        assertEquals(jcrName, ndt.getName());
        ndt.setName(jcrName);
        assertEquals(jcrName, ndt.getName());

        ndt.setSameNameSiblings(true);
        assertTrue(ndt.allowsSameNameSiblings());

        ndt.setAutoCreated(true);
        assertTrue(ndt.isAutoCreated());

        ndt.setMandatory(true);
        assertTrue(ndt.isMandatory());

        ndt.setProtected(true);
        assertTrue(ndt.isProtected());

        ndt.setOnParentVersion(OnParentVersionAction.VERSION);
        assertEquals(OnParentVersionAction.VERSION, ndt.getOnParentVersion());

        expandedName = "{" + NS_NT_URI + "}" + "folder";
        jcrName = superuser.getNamespacePrefix(NS_NT_URI) + ":folder";
        ndt.setDefaultPrimaryTypeName(expandedName);
        assertEquals(jcrName, ndt.getDefaultPrimaryTypeName());

        try {
            ndt.setDefaultPrimaryTypeName(null);
            fail("null isn't a valid jcr name");
        } catch (ConstraintViolationException e) {
            // success
        }

        ndt.setRequiredPrimaryTypeNames(new String[] {expandedName});
        assertNotNull(ndt.getRequiredPrimaryTypeNames());
        assertEquals(1, ndt.getRequiredPrimaryTypeNames().length);
        assertEquals(jcrName, ndt.getRequiredPrimaryTypeNames()[0]);

        try {
            ndt.setRequiredPrimaryTypeNames(null);
            fail("null isn't a valid array of jcr name");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    public void testResidualNames() throws Exception {
        String residualName = "*";

        NodeDefinitionTemplate ndt = ntm.createNodeDefinitionTemplate();
        ndt.setName(residualName);
        assertEquals(residualName, ndt.getName());

        PropertyDefinitionTemplate pdt = ntm.createPropertyDefinitionTemplate();
        pdt.setName(residualName);
        assertEquals(residualName, pdt.getName());
    }

    public void testInvalidJCRNames() throws Exception {
        String invalidName = ":ab[2]";

        // invalid name(s) passed to NT-template methods
        NodeTypeTemplate ntt = ntm.createNodeTypeTemplate();
        try {
            ntt.setName(invalidName);
            fail("ConstraintViolationException expected. Nt-name is invalid");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            ntt.setDeclaredSuperTypeNames(new String[] {"{" + NS_MIX_URI + "}" + "littlemixin", invalidName});
            fail("ConstraintViolationException expected. One of the super type names is invalid");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            ntt.setPrimaryItemName(invalidName);
            fail("ConstraintViolationException expected. Primary item name is invalid");
        } catch (ConstraintViolationException e) {
            // success
        }

        // invalid name(s) passed to NodeDefinitionTemplate
        NodeDefinitionTemplate ndt = ntm.createNodeDefinitionTemplate();
        try {
            ndt.setName(invalidName);
            fail("ConstraintViolationException expected. Name is invalid");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            ndt.setRequiredPrimaryTypeNames(new String[] {"{" + NS_MIX_URI + "}" + "littlemixin", invalidName});
            fail("ConstraintViolationException expected. One of the required primary type names is invalid");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            ndt.setDefaultPrimaryTypeName(invalidName);
            fail("ConstraintViolationException expected. Default primary type name is invalid");
        } catch (ConstraintViolationException e) {
            // success
        }

        // invalid name(s) passed to PropertyDefinitionTemplate
        PropertyDefinitionTemplate pdt = ntm.createPropertyDefinitionTemplate();
        try {
            pdt.setName(invalidName);
            fail("ConstraintViolationException expected. Name is invalid");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    private PropertyDefinitionTemplate createBooleanPropTemplate() throws RepositoryException {
        PropertyDefinitionTemplate pdt = ntm.createPropertyDefinitionTemplate();
        pdt.setName(expandedPropName);
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