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
package org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit;

import java.io.FileInputStream;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import junit.framework.*;

import org.apache.portals.graffito.jcr.mapper.model.BeanDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.ClassDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.CollectionDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.FieldDescriptor;

import org.apache.portals.graffito.jcr.nodemanagement.TestBase;
import org.apache.portals.graffito.jcr.nodemanagement.exception.NamespaceCreationException;
import org.apache.portals.graffito.jcr.nodemanagement.exception.NodeTypeCreationException;

/** JUnit test for NodeTypeManagerImpl.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class NodeTypeManagerImplTest extends TestBase {
    
    /** Class to test.
     */
    private NodeTypeManagerImpl jackrabbitNodeTypeManagerImpl
                = new NodeTypeManagerImpl();
    
    /** Returns testsuite.
     * @return suite
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(NodeTypeManagerImplTest.class);
        return suite;
    }

    /**
     * Test of createNamespace method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     */
    public void testCreateNamespace() throws Exception
    {
        getJackrabbitNodeTypeManagerImpl().createNamespace(session,
                "test", "http://www.test.com/test-uri");
        
        assertEquals(session.getWorkspace().getNamespaceRegistry().getPrefix("http://www.test.com/test-uri"), "test");
        assertEquals(session.getWorkspace().getNamespaceRegistry().getURI("test"), "http://www.test.com/test-uri");
        
        boolean failed = false;
        
        try
        {
            getJackrabbitNodeTypeManagerImpl().createNamespace(session,
                    "test", "http://www.test.com/test-uri");
        } catch (NamespaceCreationException nce) {
            // expected
            failed = true;
        }

        assertTrue(failed);
    }

    /**
     * Test of createNodeTypesFromConfiguration method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateNodeTypesFromConfiguration() throws Exception
    {
        getJackrabbitNodeTypeManagerImpl().createNodeTypesFromConfiguration(session,
                new FileInputStream("./src/config/jackrabbit/nodetypes_test1.xml"));
        
        NodeType test1 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test1");
        assertNotNull(test1);
        assertFalse(test1.isMixin());
        assertFalse(test1.hasOrderableChildNodes());
        assertEquals(test1.getPrimaryItemName(), "test1");
        assertEquals(test1.getSupertypes().length, 1);
        assertEquals(test1.getSupertypes()[0].getName(), "nt:base");
        assertTrue(containsPropertyDefintion(test1.getPropertyDefinitions(), "graffito:testProperty"));
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeType() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.TestClass");
        classDescriptor.setJcrNodeType("graffito:test2");
        classDescriptor.setJcrSuperTypes("nt:base");
        
        FieldDescriptor field1 = new FieldDescriptor();
        field1.setFieldName("a");
        field1.setJcrName("graffito:a");
        field1.setJcrType("String");
        field1.setJcrAutoCreated(true);
        field1.setJcrMandatory(true);
        field1.setJcrMultiple(true);
        classDescriptor.addFieldDescriptor(field1);

        FieldDescriptor field2 = new FieldDescriptor();
        field2.setFieldName("b");
        field2.setJcrName("graffito:b");
        field2.setJcrType("Long");
        field1.setJcrAutoCreated(false);
        field1.setJcrMandatory(true);
        field1.setJcrMultiple(false);        
        classDescriptor.addFieldDescriptor(field2);        

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType testNodeType = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test2");
        assertNotNull(testNodeType);
        assertFalse(testNodeType.isMixin());
        assertEquals(testNodeType.getName(), "graffito:test2");
        assertEquals(testNodeType.getSupertypes().length, 1);
        assertEquals(testNodeType.getSupertypes()[0].getName(), "nt:base");

        // 2 defined in graffito:test2 and 2 inherited from nt:base
        assertEquals(testNodeType.getPropertyDefinitions().length, 4);
        
        assertTrue(containsProperty("graffito:a", testNodeType.getPropertyDefinitions()));
        assertTrue(containsProperty("graffito:b", testNodeType.getPropertyDefinitions()));
        assertTrue(containsProperty("jcr:primaryType", testNodeType.getPropertyDefinitions()));
        assertTrue(containsProperty("jcr:mixinTypes", testNodeType.getPropertyDefinitions()));
        
        PropertyDefinition propDef1 = getPropertyDefinition(testNodeType.getPropertyDefinitions(), "graffito:a");
        System.out.println(getJackrabbitNodeTypeManagerImpl().showPropertyDefinition(propDef1));
        // TODO test all properties
        
        PropertyDefinition propDef2 = getPropertyDefinition(testNodeType.getPropertyDefinitions(), "graffito:b");
        System.out.println(getJackrabbitNodeTypeManagerImpl().showPropertyDefinition(propDef2));
        // TODO test all properties
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeNoNamespace() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test3Class");
        classDescriptor.setJcrNodeType("test3");
        classDescriptor.setJcrSuperTypes("nt:base");

        FieldDescriptor field1 = new FieldDescriptor();
        field1.setFieldName("a");
        field1.setJcrName("a");
        field1.setJcrType("String");
        classDescriptor.addFieldDescriptor(field1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test3 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test3");
        assertNotNull(test3);
        assertFalse(test3.isMixin());
        assertEquals(test3.getName(), "graffito:test3");
        assertEquals(test3.getSupertypes().length, 1);
        assertEquals(test3.getSupertypes()[0].getName(), "nt:base");        
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeNoJcrNodeTypeSet() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test4Class");
        classDescriptor.setJcrSuperTypes("nt:base");

        FieldDescriptor field1 = new FieldDescriptor();
        field1.setFieldName("a");
        field1.setJcrName("a");
        field1.setJcrType("String");
        classDescriptor.addFieldDescriptor(field1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test4 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test.Test4Class");
        assertNotNull(test4);
        assertFalse(test4.isMixin());
        assertEquals(test4.getName(), "graffito:test.Test4Class");
        assertEquals(test4.getSupertypes().length, 1);
        assertEquals(test4.getSupertypes()[0].getName(), "nt:base");        
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeIncompleteFieldDescriptorProperties() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test5Class");
        classDescriptor.setJcrNodeType("graffito:test5");
        classDescriptor.setJcrSuperTypes("graffito:test2");
        
        FieldDescriptor field1 = new FieldDescriptor();
        field1.setFieldName("abc");
        classDescriptor.addFieldDescriptor(field1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test5 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test5");
        assertNotNull(test5);
        assertFalse(test5.isMixin());
        assertEquals(test5.getName(), "graffito:test5");
        // nt:base and graffito:test2
        assertEquals(test5.getSupertypes().length, 2);
        assertTrue(containsSuperType("graffito:test2", test5.getSupertypes()));
        assertTrue(containsSuperType("nt:base", test5.getSupertypes()));
        assertTrue(containsProperty("graffito:abc", test5.getPropertyDefinitions()));
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeNtNamespace() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test6Class");
        classDescriptor.setJcrNodeType("nt:test3");
        classDescriptor.setJcrSuperTypes("nt:base");

        FieldDescriptor field1 = new FieldDescriptor();
        field1.setFieldName("a");
        field1.setJcrName("a");
        field1.setJcrType("String");
        classDescriptor.addFieldDescriptor(field1);
        
        boolean failed = false;
        
        try
        {
            getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        }
        catch (NodeTypeCreationException nce)
        {
            // excepted
            failed = true;
        }
        
        assertTrue(failed);
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeWithPropertyForCollection() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test9Class");
        classDescriptor.setJcrNodeType("graffito:test9");
        classDescriptor.setJcrSuperTypes("nt:base");
        
        CollectionDescriptor collection1 = new CollectionDescriptor();
        collection1.setFieldName("a");
        collection1.setJcrName("a");
        collection1.setJcrType("String");
        classDescriptor.addCollectionDescriptor(collection1);
        
        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test9 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test9");
        assertNotNull(test9);
        // not check node type definition, assuming other tests have done that
        
        // assert property definition a
        PropertyDefinition propDef = getPropertyDefinition(test9.getPropertyDefinitions(), "graffito:a");
        assertNotNull(propDef);
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeWithPropertyForBean() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test10Class");
        classDescriptor.setJcrNodeType("graffito:test10");
        classDescriptor.setJcrSuperTypes("nt:base");
        
        BeanDescriptor bean1 = new BeanDescriptor();
        bean1.setFieldName("a");
        bean1.setJcrName("a");
        bean1.setJcrType("String");
        classDescriptor.addBeanDescriptor(bean1);
        
        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test10 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test10");
        assertNotNull(test10);
        // not check node type definition, assuming other tests have done that
        
        // assert property definition a
        PropertyDefinition propDef = getPropertyDefinition(test10.getPropertyDefinitions(), "graffito:a");
        assertNotNull(propDef);
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);
        
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeWithPropertyForCollectionDefinitionConflict() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test13Class");
        classDescriptor.setJcrNodeType("graffito:test13");
        classDescriptor.setJcrSuperTypes("nt:base");

        CollectionDescriptor collection1 = new CollectionDescriptor();
        collection1.setFieldName("a");
        collection1.setJcrName("a");
        collection1.setJcrType("String");      // should overwrite setJcrNodeType
        collection1.setJcrNodeType("nt:base"); // should be ignored
        classDescriptor.addCollectionDescriptor(collection1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test13 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test13");
        assertNotNull(test13);
        // not check node type definition, assuming other tests have done that
        
        // assert property definition a
        PropertyDefinition propDef = getPropertyDefinition(test13.getPropertyDefinitions(), "graffito:a");
        assertNotNull(propDef);
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeWithPropertyForBeanDefinitionConflict() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test14Class");
        classDescriptor.setJcrNodeType("graffito:test14");
        classDescriptor.setJcrSuperTypes("nt:base");

        BeanDescriptor bean1 = new BeanDescriptor();
        bean1.setFieldName("a");
        bean1.setJcrName("a");
        bean1.setJcrType("String");      // should overwrite setJcrNodeType
        bean1.setJcrNodeType("nt:base"); // should be ignored
        classDescriptor.addBeanDescriptor(bean1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test14 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test14");
        assertNotNull(test14);
        // not check node type definition, assuming other tests have done that
        
        // assert property definition a
        PropertyDefinition propDef = getPropertyDefinition(test14.getPropertyDefinitions(), "graffito:a");
        assertNotNull(propDef);
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);

    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeWithChildNodeForCollection() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test11Class");
        classDescriptor.setJcrNodeType("graffito:test11");
        classDescriptor.setJcrSuperTypes("nt:base");

        CollectionDescriptor collection1 = new CollectionDescriptor();
        collection1.setFieldName("a");
        collection1.setJcrName("b");
        collection1.setJcrNodeType("nt:unstructured");
        classDescriptor.addCollectionDescriptor(collection1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test11 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test11");
        assertNotNull(test11);
        // not check node type definition, assuming other tests have done that
        
        // assert child node definition a
        NodeDefinition nodeDef = getChildNodeDefinition(test11.getChildNodeDefinitions(), "graffito:b");
        assertNotNull(nodeDef);
        assertNotNull(nodeDef.getRequiredPrimaryTypes());
        assertEquals(nodeDef.getRequiredPrimaryTypes().length, 1);
        assertEquals(nodeDef.getRequiredPrimaryTypes()[0].getName(), "nt:unstructured");
    }
    
    /**
     * Test of createSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */
    public void testCreateSingleNodeTypeWithChildNodeForBean() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test12Class");
        classDescriptor.setJcrNodeType("graffito:test12");
        classDescriptor.setJcrSuperTypes("nt:base");

        BeanDescriptor bean1 = new BeanDescriptor();
        bean1.setFieldName("a");
        bean1.setJcrName("b");
        bean1.setJcrNodeType("nt:unstructured");
        classDescriptor.addBeanDescriptor(bean1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test12 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test12");
        assertNotNull(test12);
        // not check node type definition, assuming other tests have done that
        
        // assert property definition a
        NodeDefinition nodeDef = getChildNodeDefinition(test12.getChildNodeDefinitions(), "graffito:b");
        assertNotNull(nodeDef);
        assertNotNull(nodeDef);
        assertNotNull(nodeDef.getRequiredPrimaryTypes());
        assertEquals(nodeDef.getRequiredPrimaryTypes().length, 1);
        assertEquals(nodeDef.getRequiredPrimaryTypes()[0].getName(), "nt:unstructured");
    }
    
    /**
     * Test of createNodeTypes method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */    
    public void testCreateNodeTypes() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test6Class");
        classDescriptor.setJcrNodeType("graffito:test6");
        classDescriptor.setJcrSuperTypes("nt:base");
        
        FieldDescriptor field1 = new FieldDescriptor();
        field1.setFieldName("a");
        field1.setJcrName("graffito:a");
        field1.setJcrType("String");
        classDescriptor.addFieldDescriptor(field1);

        FieldDescriptor field2 = new FieldDescriptor();
        field2.setFieldName("b");
        field2.setJcrName("graffito:b");
        field2.setJcrType("Long");
        classDescriptor.addFieldDescriptor(field2);
        
        ClassDescriptor classDescriptor2 = new ClassDescriptor();
        classDescriptor2.setClassName("test.Test7Class");
        classDescriptor2.setJcrNodeType("graffito:test7");
        classDescriptor2.setJcrSuperTypes("nt:base");
        
        FieldDescriptor field3 = new FieldDescriptor();
        field3.setFieldName("a");
        field3.setJcrName("graffito:a");
        field3.setJcrType("String");
        classDescriptor2.addFieldDescriptor(field3);

        FieldDescriptor field4 = new FieldDescriptor();
        field4.setFieldName("b");
        field4.setJcrName("graffito:b");
        field4.setJcrType("Long");
        classDescriptor2.addFieldDescriptor(field4);
        
        ClassDescriptor[] classDescriptorArray = new ClassDescriptor[2];
        classDescriptorArray[0] = classDescriptor;
        classDescriptorArray[1] = classDescriptor2;
        
        getJackrabbitNodeTypeManagerImpl().createNodeTypes(session, classDescriptorArray);
        
        NodeType test6 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test6");
        assertNotNull(test6);
        
        NodeType test7 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test7");
        assertNotNull(test7);
    }
    
    /**
     * Test of removeSingleNodeType method, of class org.apache.portals.graffito.jcr.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl.
     * @throws java.lang.Exception 
     */    
    public void testRemoveSingleNodeType() throws Exception
    {
        ClassDescriptor classDescriptor = new ClassDescriptor();
        classDescriptor.setClassName("test.Test8Class");
        classDescriptor.setJcrNodeType("graffito:test8");
        classDescriptor.setJcrSuperTypes("nt:base");

        FieldDescriptor field1 = new FieldDescriptor();
        field1.setFieldName("a");
        field1.setJcrName("a");
        field1.setJcrType("String");
        classDescriptor.addFieldDescriptor(field1);

        getJackrabbitNodeTypeManagerImpl().createSingleNodeType(session, classDescriptor);
        
        NodeType test8 = session.getWorkspace().getNodeTypeManager().getNodeType("graffito:test8");
        assertNotNull(test8);
        // not implemented yet in jackrabbit
        // getJackrabbitNodeTypeManagerImpl().removeSingleNodeType(session, "graffito:test8");    
    }    

    /** Returns true if a given property is found in an array of property
     * definitions.
     * 
     * @param propertyName Name of property to find
     * @param propDefs Properties of a node type
     * @return true/false 
     */
    protected boolean containsProperty(String propertyName,
        PropertyDefinition[] propDefs)
    {
        boolean found = false;
        
        for (int i = 0; i < propDefs.length; i++)
        {
            if (propDefs[i].getName().equals(propertyName))
            {
                found = true;
                break;
            }
        }
        
        return found;
    }
    
    /** Returns a property defintion identified by its name.
     * 
     * @param propDefs All property definitions of a node type
     * @param propertyName Name of property definition
     * @return found
     */
    protected PropertyDefinition getPropertyDefinition(PropertyDefinition[] propDefs,
        String propertyName)
    {
        PropertyDefinition found = null;
        
        for (int i = 0; i < propDefs.length; i++)
        {
            if (propDefs[i].getName().equals(propertyName))
            {
                found = propDefs[i];
                break;
            }
        }
        
        return found;
    }
    
    /** Returns true if a given child node is found in an array of child node
     * definitions.
     * 
     * @param childNodeName Name of child node to find
     * @param childNodeDefs Child nodes of a node type
     * @return true/false 
     */
    protected boolean containsChildNode(String childNodeName,
            NodeDefinition[] childNodeDefs)
    {
        boolean found = false;
        
        for (int i = 0; i < childNodeDefs.length; i++)
        {
           if (childNodeDefs[i].getName().equals(childNodeName))
           {
               found = true;
               break;
           }
        }

        return found;
    }
    
    /** Returns a property defintion identified by its name.
     * 
     * @param childNodeDefs Child nodes of a node type
     * @param childNodeName Name of child node to find
     * @return found
     */
    protected NodeDefinition getChildNodeDefinition(NodeDefinition[] childNodeDefs,
            String childNodeName)
    {
        NodeDefinition found = null;
        
        for (int i = 0; i < childNodeDefs.length; i++)
        {
           if (childNodeDefs[i].getName().equals(childNodeName))
           {
               found = childNodeDefs[i];
               break;
           }
        }

        return found;
    }
    
    /** Returns true if a given super type is found in an arry of super types.
     * 
     * @param superType Name of super type to find
     * @param propDefs Properties of a node type
     * @return true/false 
     */
    protected boolean containsSuperType(String superType,
            NodeType[] nodeTypes)
    {
        boolean found = false;
        
        for (int i = 0; i < nodeTypes.length; i++)
        {
           if (nodeTypes[i].getName().equals(superType))
           {
               found = true;
               break;
           }
        }

        return found;
    }    
    
    /** Getter for property jackrabbitNodeTypeManagerImpl.
     * 
     * @return jackrabbitNodeTypeManagerImpl
     */
    public NodeTypeManagerImpl getJackrabbitNodeTypeManagerImpl()
    {
        return jackrabbitNodeTypeManagerImpl;
    }

    /** Setter for property jackrabbitNodeTypeManagerImpl.
     * 
     * @param object jackrabbitNodeTypeManagerImpl
     */
    public void setJackrabbitNodeTypeManagerImpl(NodeTypeManagerImpl object)
    {
        this.jackrabbitNodeTypeManagerImpl = object;
    }
}
