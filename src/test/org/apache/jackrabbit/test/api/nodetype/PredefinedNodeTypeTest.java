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
package org.apache.jackrabbit.test.api.nodetype;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.jdom.input.SAXBuilder;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.filter.Filter;
import org.jdom.filter.ContentFilter;

import javax.jcr.version.OnParentVersionAction;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeTypeIterator;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * <code>PredefinedNodeTypeTest</code> tests if the implemented predefined node
 * types implemented correctly.
 *
 * @test
 * @sources PredefinedNodeTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest
 * @keywords level1
 */
public class PredefinedNodeTypeTest extends AbstractJCRTest {

    private static final String NODETYPE_ELEMENT = "nodeType";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String ISMIXIN_ATTRIBUTE = "isMixin";
    private static final String ORDERABLECHILDNODES_ATTRIBUTE = "hasOrderableChildNodes";
    private static final String PRIMARYITEMNAME_ATTRIBUTE = "primaryItemName";
    private static final String SUPERTYPES_ELEMENT = "supertypes";
    private static final String SUPERTYPE_ELEMENT = "supertype";
    private static final String PROPERTYDEF_ELEMENT = "propertyDef";
    private static final String REQUIREDTYPE_ATTRIBUTE = "requiredType";
    private static final String VALUECONSTRAINTS_ELEMENT = "valueConstraints";
    private static final String VALUECONSTRAINT_ELEMENT = "valueConstraint";
    private static final String DEFAULTVALUES_ELEMENT = "defaultValues";
    private static final String DEFAULTVALUE_ELEMENT = "defaultValue";
    private static final String AUTOCREATE_ATTRIBUTE = "autoCreate";
    private static final String MANDATORY_ATTRIBUTE = "mandatory";
    private static final String PROTECTED_ATTRIBUTE = "protected";
    private static final String MULTIPLE_ATTRIBUTE = "multiple";
    private static final String SAMENAMESIBS_ATTRIBUTE = "sameNameSibs";
    private static final String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";
    private static final String CHILDNODEDEF_ELEMENT = "childNodeDef";
    private static final String REQUIREDPRIMARYTYPES_ELEMENT = "requiredPrimaryTypes";
    private static final String REQUIREDPRIMARYTYPE_ELEMENT = "requiredPrimaryType";
    private static final String DEFAULTPRIMARYTYPE_ATTRIBUTE = "defaultPrimaryType";

    private static final String WILDCARD = "*";

    private static final String PREDEFINED_NODETYPES_RESOURCE_PATH =
            "org/apache/jackrabbit/test/api/nodetype/predefined_nodetypes.xml";

    /**
     * The NodeTypeManager of the session
     */
    private NodeTypeManager manager;

    /**
     * The read-only session for the test
     */
    private Session session;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }


    /**
     * Tests if the mandatory node type <code>nt:base</code> is supported
     */
    public void testNTBaseSupport()
            throws RepositoryException {

        try {
            manager.getNodeType(ntBase);
        } catch (NoSuchNodeTypeException e) {
            fail("Node type nt:base must be supported.");
        }
    }

    /**
     * Tests if all primary node types are subtypes of node type <code>nt:base</code>
     */
    public void testIfPrimaryNodeTypesAreSubtypesOfNTBase()
            throws NoSuchNodeTypeException, RepositoryException {

        NodeTypeIterator types = manager.getPrimaryNodeTypes();

        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            NodeType superTypes[] = type.getSupertypes();
            if (!type.getName().equals(ntBase)) {
                boolean isSubOfNTBase = false;
                for (int i = 0; i < superTypes.length; i++) {
                    if (superTypes[i].getName().equals(ntBase)) {
                        isSubOfNTBase = true;
                    }
                }
                assertTrue("All primary node types must be subtypes of nt:base",
                           isSubOfNTBase);
            }
        }
    }

    /**
     * Read and parse the xml file containing all the predefined node type
     * definitions. If predefined node types are implemented, compare the
     * implemented node types to the predefined.
     */
    public void testPredefinedNodeTypes()
            throws IOException, JDOMException, RepositoryException {

        InputStream in = null;
        in = getClass().getClassLoader().getResourceAsStream(PREDEFINED_NODETYPES_RESOURCE_PATH);
        SAXBuilder builder = new SAXBuilder();
        Element root = null;
        Document doc = builder.build(in);
        root = doc.getRootElement();

        // read definitions
        Iterator iter = root.getChildren(NODETYPE_ELEMENT).iterator();
        while (iter.hasNext()) {
            Element ntElem = (Element) iter.next();
            String sntName = ntElem.getAttributeValue(NAME_ATTRIBUTE);
            try {
                NodeType implType = manager.getNodeType(sntName);
                compareElement(ntElem, implType);
            } catch (NoSuchNodeTypeException e) {
                // the current predefined node type is not implemented: ignore
            }
        }
    }


    //------------------------< private methods >-------------------------------

    /**
     * Parse a single predefined <code>NodeType</code> and compare it to the
     * implementation.
     */
    private void compareElement(Element ntElem, NodeType implType) {

        String sntName = ntElem.getAttributeValue(NAME_ATTRIBUTE);

        // supertypes
        NodeType implSupertypes[] = implType.getDeclaredSupertypes();
        int supertypesCounter = 0;
        Element typesElem = ntElem.getChild(SUPERTYPES_ELEMENT);
        if (typesElem != null) {
            Iterator iter = typesElem.getChildren(SUPERTYPE_ELEMENT).iterator();
            while (iter.hasNext()) {
                Element typeElem = (Element) iter.next();
                Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
                List content = typeElem.getContent(filter);
                if (!content.isEmpty()) {
                    String name = typeElem.getTextTrim();
                    supertypesCounter++;

                    boolean isExist = false;
                    for (int i = 0; i < implSupertypes.length; i++) {
                        if (implSupertypes[i].getName().equals(name)) {
                            isExist = true;
                            break;
                        }
                    }
                    assertTrue("Implementation of node type " + sntName +
                            " requires supertype " + name,
                            isExist);
                }
            }
        }
        assertEquals("Implementation of node type " + sntName + ": " +
                "Supertypes exceed definition: ",
                supertypesCounter,
                implSupertypes.length);


        // isMixin
        String mixin = ntElem.getAttributeValue(ISMIXIN_ATTRIBUTE);
        boolean expectedMixin = false;
        if (mixin != null && mixin.length() > 0) {
            expectedMixin = Boolean.valueOf(mixin).booleanValue();
        }
        assertEquals("Implementation of node type " + sntName + ": " +
                "Wrong mixin value:",
                expectedMixin,
                implType.isMixin());


        // orderableChildNodes
        String orderableChildNodes = ntElem.getAttributeValue(ORDERABLECHILDNODES_ATTRIBUTE);
        boolean expectedOrderableChildNodes = false;
        if (orderableChildNodes != null && orderableChildNodes.length() > 0) {
            expectedOrderableChildNodes = Boolean.valueOf(orderableChildNodes).booleanValue();
        }
        assertEquals("Implementation of node type " + sntName + ": " +
                "Wrong orderable child nodes value:",
                expectedOrderableChildNodes,
                implType.hasOrderableChildNodes());


        // primaryItemName
        String expectedPrimaryItemName = ntElem.getAttributeValue(PRIMARYITEMNAME_ATTRIBUTE);
        if (expectedPrimaryItemName != null && expectedPrimaryItemName.length() == 0) {
            expectedPrimaryItemName = null;
        }
        assertEquals("Implementation of node type " + sntName + ": " +
                "Wrong primary item name:",
                expectedPrimaryItemName,
                implType.getPrimaryItemName());


        // property definitions
        log.println("*** property definitions ***");
        Iterator iter = ntElem.getChildren(PROPERTYDEF_ELEMENT).iterator();
        int propertyDefsCounter = 0;
        PropertyDef implPropertyDefs[] = implType.getDeclaredPropertyDefs();
        StringBuffer residualPropDefIndexes = new StringBuffer(",");
        while (iter.hasNext()) {
            propertyDefsCounter++;

            Element elem = (Element) iter.next();
            String propDefName = elem.getAttributeValue(NAME_ATTRIBUTE);
            log.println("*" + propDefName);

            boolean isResidual = (propDefName.equals(WILDCARD)) ? true : false;
            boolean residualSucceed = false;

            boolean isExist = false;

            for (int i = 0; i < implPropertyDefs.length; i++) {
                if (implPropertyDefs[i].getName().equals(propDefName)) {
                    if (isResidual) {
                        if (residualPropDefIndexes.indexOf(Integer.toString(i)) == -1) {
                            // check if one of the residual property defs is matching
                            // (multiple residual defs are possible)
                            // residualPropDefIndexes is a list holding the indexes of
                            // the implemented property defs already checked (to avoid double checking)
                            PropertyDef implPropertyDef = implPropertyDefs[i];
                            residualSucceed = comparePropertyDef(elem, implPropertyDef, sntName, isResidual);
                            if (residualSucceed) {
                                residualPropDefIndexes.append(i + ",");
                                isExist = true;
                                break;
                            }
                        }
                    } else {
                        PropertyDef implPropertyDef = implPropertyDefs[i];
                        comparePropertyDef(elem, implPropertyDef, sntName, isResidual);
                        isExist = true;
                        break;
                    }
                }
            }
            if (isResidual && !residualSucceed) {
                fail("Implementation of node type " + sntName + ": " +
                        "Residual property def does not match the definitions");
            }
            assertTrue("Implementation of node type " + sntName + ": " +
                    "Property def " + propDefName + " is missing.",
                    isExist);
        }
        assertEquals("Implementation of node type " + sntName + ": " +
                "Property defs exceed definition: ",
                propertyDefsCounter,
                implPropertyDefs.length);


        // child-node definitions
        iter = ntElem.getChildren(CHILDNODEDEF_ELEMENT).iterator();
        int childNodeDefsCounter = 0;
        NodeDef implChildNodeDefs[] = implType.getDeclaredChildNodeDefs();
        StringBuffer residualNodeDefIndexes = new StringBuffer(",");
        while (iter.hasNext()) {
            childNodeDefsCounter++;

            Element elem = (Element) iter.next();
            String nodeDefName = elem.getAttributeValue(NAME_ATTRIBUTE);

            boolean isResidual = (nodeDefName.equals(WILDCARD)) ? true : false;
            boolean residualSucceed = false;

            boolean isExist = false;

            for (int i = 0; i < implChildNodeDefs.length; i++) {
                if (implChildNodeDefs[i].getName().equals(nodeDefName)) {
                    if (isResidual) {
                        if (residualNodeDefIndexes.indexOf(Integer.toString(i)) == -1) {
                            // check if one of the residual child node defs is matching
                            // (multiple residual defs are possible)
                            // residualNodeDefIndexes is a list holding the indexes of
                            // the implemented child node defs already checked (to avoid double checking)
                            NodeDef implChildNodeDef = implChildNodeDefs[i];
                            residualSucceed = compareChildNodeDef(elem, implChildNodeDef, sntName, isResidual);
                            if (residualSucceed) {
                                residualNodeDefIndexes.append(i + ",");
                                isExist = true;
                                break;
                            }
                        }
                    } else {
                        NodeDef implChildNodeDef = implChildNodeDefs[i];
                        compareChildNodeDef(elem, implChildNodeDef, sntName, isResidual);
                        isExist = true;
                        break;
                    }
                }
            }
            if (isResidual && !residualSucceed) {
                fail("Implementation of node type " + sntName + ": " +
                        "Residual child node def does not match the definitions");
            }
            assertTrue("Implementation of node type " + sntName + ": " +
                    "Child node def " + nodeDefName + " is missing.",
                    isExist);
        }
        assertEquals("Implementation of node type " + sntName + ": " +
                "Child node defs exceed definition: ",
                childNodeDefsCounter,
                implChildNodeDefs.length);
    }


    /**
     * Parse a single predefined <code>PropertyDef</code> and compare it to its
     * implementation.
     */
    private boolean comparePropertyDef(Element elem,
                                       PropertyDef implPropertyDef,
                                       String sntName,
                                       boolean isResidual)
            throws IllegalArgumentException {

        String propDefName = implPropertyDef.getName();

        if (isResidual) {
            if (implPropertyDef == null) {
                return false;
            }
        } else {
            assertNotNull("Implementation of node type " + sntName +
                    " requires property def " + propDefName,
                    implPropertyDef);
        }

        // requiredType
        String expectedTypeName = elem.getAttributeValue(REQUIREDTYPE_ATTRIBUTE);
        int expectedType = PropertyType.UNDEFINED;
        if (expectedTypeName != null && expectedTypeName.length() > 0) {
            expectedType = PropertyType.valueFromName(expectedTypeName);
        }
        if (isResidual) {
            if (expectedType != implPropertyDef.getRequiredType()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Wrong required type:",
                    PropertyType.nameFromValue(expectedType),
                    PropertyType.nameFromValue(implPropertyDef.getRequiredType()));
        }

        // valueConstraints
        Element constraintsElem = elem.getChild(VALUECONSTRAINTS_ELEMENT);
        int constraintsCounter = 0;
        String implConstraints[] = implPropertyDef.getValueConstraints();
        if (constraintsElem != null) {
            Iterator iter1 = constraintsElem.getChildren(VALUECONSTRAINT_ELEMENT).iterator();
            while (iter1.hasNext()) {
                Element constraintElem = (Element) iter1.next();
                Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
                List content = constraintElem.getContent(filter);
                if (!content.isEmpty()) {
                    constraintsCounter++;
                    String expectedConstraint = constraintElem.getTextTrim();
                    boolean isExist = false;
                    for (int i = 0; i < implConstraints.length; i++) {
                        if (implConstraints[i].equals(expectedConstraint)) {
                            isExist = true;
                            break;
                        }
                    }
                    if (isResidual) {
                        if (isExist == false) {
                            return false;
                        }
                    } else {
                        assertTrue("Implementation of node type " + sntName + ", " +
                                "property def " + propDefName + ": " +
                                "Missing value constraint.",
                                isExist);
                    }
                }
            }
        }
        if (isResidual) {
            if (constraintsCounter != implConstraints.length) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Value constraints exceed definition: ",
                    constraintsCounter,
                    implConstraints.length);
        }


        // defaultValues
        Element defValuesElem = elem.getChild(DEFAULTVALUES_ELEMENT);
        int defValuesElemCounter = 0;
        Value implDefValues[] = implPropertyDef.getDefaultValues();
        if (defValuesElem != null) {
            Iterator iter1 = defValuesElem.getChildren(DEFAULTVALUE_ELEMENT).iterator();
            while (iter1.hasNext()) {
                Element valueElem = (Element) iter1.next();
                Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
                List content = valueElem.getContent(filter);
                if (!content.isEmpty()) {
                    String defValue = valueElem.getTextTrim();
                    defValuesElemCounter++;
                    boolean isExist = false;
                    for (int i = 0; i < implDefValues.length; i++) {
                        try {
                            if (implDefValues[i].getString().equals(defValue)) {
                                isExist = true;
                                break;
                            }
                        } catch (ValueFormatException e) {
                        } catch (RepositoryException e) {
                        }
                    }
                    if (isResidual) {
                        if (isExist == false) {
                            return false;
                        }
                    } else {
                        assertTrue("Implementation of node type " + sntName + ", " +
                                "property def " + propDefName + ": " +
                                "Missing default value.",
                                isExist);
                    }
                }
            }
        }
        if (isResidual) {
            if (defValuesElemCounter != implDefValues.length) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Default values exceed definition: ",
                    defValuesElemCounter,
                    implDefValues.length);
        }


        // autoCreate
        String autoCreate = elem.getAttributeValue(AUTOCREATE_ATTRIBUTE);
        boolean expectedAutoCreate = false;
        if (autoCreate != null && autoCreate.length() > 0) {
            expectedAutoCreate = Boolean.valueOf(autoCreate).booleanValue();
        }
        if (isResidual) {
            if (expectedAutoCreate != implPropertyDef.isAutoCreate()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Wrong auto create value:",
                    expectedAutoCreate,
                    implPropertyDef.isAutoCreate());
        }

        // mandatory
        String mandatory = elem.getAttributeValue(MANDATORY_ATTRIBUTE);
        boolean expectedMandatory = false;
        if (mandatory != null && mandatory.length() > 0) {
            expectedMandatory = Boolean.valueOf(mandatory).booleanValue();
        }
        if (isResidual) {
            if (expectedMandatory != implPropertyDef.isMandatory()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Wrong mandatory value:",
                    expectedMandatory,
                    implPropertyDef.isMandatory());
        }

        // onParentVersion
        String onVersion = elem.getAttributeValue(ONPARENTVERSION_ATTRIBUTE);
        int expectedOnParentVersion = 0;
        if (onVersion != null && onVersion.length() > 0) {
            expectedOnParentVersion = OnParentVersionAction.valueFromName(onVersion);
        }
        if (isResidual) {
            if (expectedOnParentVersion != implPropertyDef.getOnParentVersion()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Wrong onParentVersion value:",
                    OnParentVersionAction.nameFromValue(expectedOnParentVersion),
                    OnParentVersionAction.nameFromValue(implPropertyDef.getOnParentVersion()));
        }

        // protected
        String writeProtected = elem.getAttributeValue(PROTECTED_ATTRIBUTE);
        boolean expectedProtected = false;
        if (writeProtected != null && writeProtected.length() > 0) {
            expectedProtected = Boolean.valueOf(writeProtected).booleanValue();
        }
        if (isResidual) {
            if (expectedProtected != implPropertyDef.isProtected()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Wrong protected value:",
                    expectedProtected,
                    implPropertyDef.isProtected());
        }

        // multiple
        String multiple = elem.getAttributeValue(MULTIPLE_ATTRIBUTE);
        boolean expectedMultiple = false;
        if (multiple != null && multiple.length() > 0) {
            expectedMultiple = Boolean.valueOf(multiple).booleanValue();
        }
        if (isResidual) {
            if (expectedMultiple != implPropertyDef.isMultiple()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + propDefName + ": " +
                    "Wrong multiple value:",
                    expectedMultiple,
                    implPropertyDef.isMultiple());
        }

        // residual property def passed all tests
        // (respectively any other property def did not fail;
        // in this case the return value has no further meaning)
        return true;
    }


    /**
     * Parse a single predefined <code>ChildNodeDef</code> and compare it to its
     * implementation.
     */
    private boolean compareChildNodeDef(Element elem,
                                        NodeDef implNodeDef,
                                        String sntName,
                                        boolean isResidual)
            throws IllegalArgumentException {

        String nodeDefName = implNodeDef.getName();

        // requiredPrimaryTypes
        Element reqTtypesElem = elem.getChild(REQUIREDPRIMARYTYPES_ELEMENT);
        int reqTypesCounter = 0;
        NodeType implReqTypes[] = implNodeDef.getRequiredPrimaryTypes();
        if (reqTtypesElem != null) {
            Iterator iter1 = reqTtypesElem.getChildren(REQUIREDPRIMARYTYPE_ELEMENT).iterator();
            while (iter1.hasNext()) {
                Element typeElem = (Element) iter1.next();
                Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
                List content = typeElem.getContent(filter);
                if (!content.isEmpty()) {
                    reqTypesCounter++;
                    String expectedName = typeElem.getTextTrim();
                    boolean isExist = false;
                    for (int i = 0; i < implReqTypes.length; i++) {
                        if (implReqTypes[i].getName().equals(expectedName)) {
                            isExist = true;
                            break;
                        }
                    }
                    if (isResidual) {
                        if (isExist == false) {
                            return false;
                        }
                    } else {
                        assertTrue("Implementation of node type " + sntName + ", " +
                                "child node def " + nodeDefName + ": " +
                                "Missing required primary type.",
                                isExist);
                    }
                }
            }
        }
        if (isResidual) {
            if (reqTypesCounter != implReqTypes.length) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + nodeDefName + ": " +
                    "Required primary types exceed definition: ",
                    reqTypesCounter,
                    implReqTypes.length);
        }

        // defaultPrimaryType
        String defaultPrimaryType = elem.getAttributeValue(DEFAULTPRIMARYTYPE_ATTRIBUTE);
        if (defaultPrimaryType == null || defaultPrimaryType.length() == 0) {
            defaultPrimaryType = null;
        }
        NodeType implDefaultPrimaryType = implNodeDef.getDefaultPrimaryType();
        String implDefaultPrimaryTypeName = null;
        if (implDefaultPrimaryType != null) {
            implDefaultPrimaryTypeName = implDefaultPrimaryType.getName();
        }
        if (isResidual) {
            if (implDefaultPrimaryType == null) {
                if (defaultPrimaryType != null) {
                    return false;
                }
            } else {
                if (!implDefaultPrimaryType.getName().equals(defaultPrimaryType)) {
                    return false;
                }
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "child node def " + nodeDefName + ": " +
                    "Wrong default primary type:",
                    defaultPrimaryType,
                    implDefaultPrimaryTypeName);
        }

        // autoCreate
        String autoCreate = elem.getAttributeValue(AUTOCREATE_ATTRIBUTE);
        boolean expectedAutoCreate = false;
        if (autoCreate != null && autoCreate.length() > 0) {
            expectedAutoCreate = Boolean.valueOf(autoCreate).booleanValue();
        }
        if (isResidual) {
            if (expectedAutoCreate != implNodeDef.isAutoCreate()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "child node def " + nodeDefName + ": " +
                    "Wrong auto create value:",
                    expectedAutoCreate,
                    implNodeDef.isAutoCreate());
        }

        // mandatory
        String mandatory = elem.getAttributeValue(MANDATORY_ATTRIBUTE);
        boolean expectedMandatory = false;
        if (mandatory != null && mandatory.length() > 0) {
            expectedMandatory = Boolean.valueOf(mandatory).booleanValue();
        }
        if (isResidual) {
            if (expectedMandatory != implNodeDef.isMandatory()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "child node def " + nodeDefName + ": " +
                    "Wrong mandatory value:",
                    expectedMandatory,
                    implNodeDef.isMandatory());
        }

        // onParentVersion
        String onVersion = elem.getAttributeValue(ONPARENTVERSION_ATTRIBUTE);
        int expectedOnParentVersion = 0;
        if (onVersion != null && onVersion.length() > 0) {
            expectedOnParentVersion = OnParentVersionAction.valueFromName(onVersion);
        }
        if (isResidual) {
            if (expectedOnParentVersion != implNodeDef.getOnParentVersion()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "child node def " + nodeDefName + ": " +
                    "Wrong onParentVersion value:",
                    OnParentVersionAction.nameFromValue(expectedOnParentVersion),
                    OnParentVersionAction.nameFromValue(implNodeDef.getOnParentVersion()));
        }

        // protected
        String writeProtected = elem.getAttributeValue(PROTECTED_ATTRIBUTE);
        boolean expectedProtected = false;
        if (writeProtected != null && writeProtected.length() > 0) {
            expectedProtected = Boolean.valueOf(writeProtected).booleanValue();
        }
        if (isResidual) {
            if (expectedProtected != implNodeDef.isProtected()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "property def " + nodeDefName + ": " +
                    "Wrong protected value:",
                    expectedProtected,
                    implNodeDef.isProtected());
        }

        // sameNameSibs
        String sameNameSibs = elem.getAttributeValue(SAMENAMESIBS_ATTRIBUTE);
        boolean expectedSameNameSibs = false;
        if (sameNameSibs != null && sameNameSibs.length() > 0) {
            expectedSameNameSibs = Boolean.valueOf(sameNameSibs).booleanValue();
        }
        if (isResidual) {
            if (expectedSameNameSibs != implNodeDef.allowSameNameSibs()) {
                return false;
            }
        } else {
            assertEquals("Implementation of node type " + sntName + ", " +
                    "child node def " + nodeDefName + ": " +
                    "Wrong same name sibs value:",
                    expectedSameNameSibs,
                    implNodeDef.allowSameNameSibs());
        }


        // residual node def passed all tests
        // (respectively any other node def did not fail;
        // in this case the return value has no further meaning)
        return true;
    }

}
