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
package org.apache.jackrabbit.ocm.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.B;
import org.apache.jackrabbit.ocm.testmodel.C;
import org.apache.jackrabbit.ocm.testmodel.PropertyTest;
import org.apache.jackrabbit.ocm.testmodel.inheritance.Ancestor;
import org.apache.jackrabbit.ocm.testmodel.inheritance.AnotherDescendant;
import org.apache.jackrabbit.ocm.testmodel.inheritance.Descendant;
import org.apache.jackrabbit.ocm.testmodel.inheritance.SubDescendant;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.CmsObjectImpl;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.DocumentImpl;
import org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject;
import org.apache.jackrabbit.ocm.testmodel.interfaces.Document;
import org.apache.jackrabbit.ocm.testmodel.interfaces.Interface;
import org.apache.jackrabbit.ocm.testmodel.proxy.Main;

/**
 * Test Annotation Mapper
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class AnnotationMapperImplTest extends TestCase {
	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public AnnotationMapperImplTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new TestSuite(DigesterMapperImplTest.class);
	}

	/**
	 * Simple test mapper
	 *
	 */
	public void testMapper() {
		try {

			Mapper mapper = getMapper();
					
			assertNotNull("Mapper is null", mapper);

			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(A.class);
			assertNotNull("ClassDescriptor is null", classDescriptor);
			assertTrue("Invalid classname", classDescriptor.getClassName().equals(A.class.getName()));
			assertTrue("Invalid path field", classDescriptor.getPathFieldDescriptor().getFieldName().equals("path"));
			assertEquals("Invalid mixins", "mix:lockable", classDescriptor.getJcrMixinTypes()[0]);

			FieldDescriptor fieldDescriptor = classDescriptor	.getFieldDescriptor("a1");
			assertNotNull("FieldDescriptor is null", fieldDescriptor);
			assertTrue("Invalid jcrName for field a1", fieldDescriptor.getJcrName().equals("a1"));

			BeanDescriptor beanDescriptor = classDescriptor.getBeanDescriptor("b");
			assertNotNull("BeanDescriptor is null", beanDescriptor);
			assertTrue("Invalid jcrName for field b", beanDescriptor	.getJcrName().equals("b"));			
			assertNotNull("Invalid bean default converter", beanDescriptor.getConverter());
			

			CollectionDescriptor collectionDescriptor = classDescriptor.getCollectionDescriptor("collection");
			assertNotNull("CollectionDescriptor is null", collectionDescriptor);
			assertTrue("Invalid jcrName for field collection",collectionDescriptor.getJcrName().equals("collection"));
		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}


	/**
	 * Simple test mapper
	 *
	 */
	public void testUuid() {
		try {

			Mapper mapper = getMapper();
					
			assertNotNull("Mapper is null", mapper);

			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(org.apache.jackrabbit.ocm.testmodel.uuid.A.class);
			assertNotNull("ClassDescriptor is null", classDescriptor);
			assertTrue("Invalid uuid field", classDescriptor.getUuidFieldDescriptor().getFieldName().equals("uuid"));

		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}

	/**
	 * Simple test mapper
	 *
	 */
	public void testDiscriminatorSetting() {
		try {

			Mapper mapper = getMapper();

			assertNotNull("Mapper is null", mapper);

			ClassDescriptor classDescriptor = mapper.getClassDescriptorByNodeType("ocm:C");
			//ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(C.class);
			assertNotNull("ClassDescriptor is null", classDescriptor);
			assertTrue("Invalid classname", classDescriptor.getClassName().equals(C.class.getName()));			
		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}

	/**
	 * Test optional mapping properties
	 *
	 */
	public void testMapperOptionalProperties() {
		try {

			Mapper mapper = getMapper();			
			
			assertNotNull("Mapper is null", mapper);

			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(B.class);
			assertNotNull("ClassDescriptor is null", classDescriptor);
			assertTrue("Invalid classname", classDescriptor.getClassName()
					.equals(B.class.getName()));
			assertEquals(classDescriptor.getJcrSuperTypes(), "nt:base");

			FieldDescriptor b1Field = classDescriptor.getFieldDescriptor("b1");
			assertNotNull("FieldDescriptor is null", b1Field);
			assertEquals(b1Field.getFieldName(), "b1");
			assertEquals(b1Field.getJcrType(), "String");
			assertFalse(b1Field.isJcrAutoCreated());
			assertFalse(b1Field.isJcrMandatory());
			assertFalse(b1Field.isJcrProtected());
			assertFalse(b1Field.isJcrMultiple());
			assertEquals(b1Field.getJcrOnParentVersion(), "IGNORE");

			FieldDescriptor b2Field = classDescriptor.getFieldDescriptor("b2");
			assertNotNull("FieldDescriptor is null", b2Field);
			assertEquals(b2Field.getFieldName(), "b2");
			assertEquals(b2Field.getJcrType(), "String");
			assertFalse(b2Field.isJcrAutoCreated());
			assertFalse(b2Field.isJcrMandatory());
			assertFalse(b2Field.isJcrProtected());
			assertFalse(b2Field.isJcrMultiple());
			assertEquals(b2Field.getJcrOnParentVersion(), "IGNORE");

			ClassDescriptor classDescriptor2 = mapper
					.getClassDescriptorByClass(A.class);
			assertNotNull("ClassDescriptor is null", classDescriptor2);
			assertTrue("Invalid classname", classDescriptor2.getClassName()
					.equals(A.class.getName()));

			BeanDescriptor beanDescriptor = classDescriptor2
					.getBeanDescriptor("b");
			assertNotNull(beanDescriptor);
			assertEquals(beanDescriptor.getFieldName(), "b");
			assertEquals(beanDescriptor.getJcrType(), "nt:unstructured");
			assertFalse(beanDescriptor.isJcrAutoCreated());
			assertFalse(beanDescriptor.isJcrMandatory());
			assertFalse(beanDescriptor.isJcrProtected());
			assertFalse(beanDescriptor.isJcrSameNameSiblings());
			assertEquals(beanDescriptor.getJcrOnParentVersion(), "IGNORE");

			CollectionDescriptor collectionDescriptor = classDescriptor2
					.getCollectionDescriptor("collection");
			assertNotNull(collectionDescriptor);
			assertEquals(collectionDescriptor.getJcrType(), "ocm:C");
			assertFalse(collectionDescriptor.isJcrAutoCreated());
			assertFalse(collectionDescriptor.isJcrMandatory());
			assertFalse(collectionDescriptor.isJcrProtected());
			assertFalse(collectionDescriptor.isJcrSameNameSiblings());
			assertEquals(collectionDescriptor.getJcrOnParentVersion(), "IGNORE");
			
			classDescriptor = mapper.getClassDescriptorByClass(PropertyTest.class);
			assertNotNull(classDescriptor);
			FieldDescriptor fieldDescriptor = classDescriptor.getFieldDescriptor("requiredWithConstraintsProp");
			assertNotNull(fieldDescriptor.getJcrValueConstraints());
			assertTrue("Invalid constaint", fieldDescriptor.getJcrValueConstraints()[0].equals("abc") );
			assertTrue("Invalid constaint", fieldDescriptor.getJcrValueConstraints()[1].equals("def") );
			assertTrue("Invalid constaint", fieldDescriptor.getJcrValueConstraints()[2].equals("ghi") );
			
			fieldDescriptor = classDescriptor.getFieldDescriptor("autoCreatedProp");
			assertNotNull(fieldDescriptor.getJcrDefaultValue());
			assertTrue("Invalid default value", fieldDescriptor.getJcrDefaultValue().equals("aaa") );
			
		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}

	/**
	 *
	 * Test Node Type per hierarchy setting
	 */
	public void testMapperNtHierarchy() {
		try {
			Mapper mapper = getMapper();
			
			assertNotNull("Mapper is null", mapper);

			ClassDescriptor classDescriptor = mapper
					.getClassDescriptorByClass(Ancestor.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertEquals("Incorrect path field", classDescriptor
					.getPathFieldDescriptor().getFieldName(), "path");
			assertTrue("The ancestor class has no discriminator",
					classDescriptor.hasDiscriminator());
			assertTrue("The ancestor class is not abstract", classDescriptor
					.isAbstract());
			assertNull("The ancestor class has an ancestor", classDescriptor
					.getSuperClassDescriptor());
			assertTrue(
					"Ancestor class doesn't have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertFalse(
					"Ancestor class  have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerConcreteClassStrategy());

			Collection descendandDescriptors = classDescriptor
					.getDescendantClassDescriptors();
			assertEquals("Invalid number of descendants", descendandDescriptors
					.size(), 2);

			classDescriptor = mapper.getClassDescriptorByClass(Descendant.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertEquals("Incorrect path field", classDescriptor
					.getPathFieldDescriptor().getFieldName(), "path");
			assertTrue("The descendant  class has no discriminator",
					classDescriptor.hasDiscriminator());
			assertNotNull("ancerstorField is null in the descendant class",
					classDescriptor.getFieldDescriptor("ancestorField"));
			assertFalse("The descendant class is abstract", classDescriptor
					.isAbstract());
			assertNotNull("The descendant class has not an ancestor",
					classDescriptor.getSuperClassDescriptor());
			assertEquals("Invalid ancestor class for the descendant class",
					classDescriptor.getSuperClassDescriptor().getClassName(),
					"org.apache.jackrabbit.ocm.testmodel.inheritance.Ancestor");
			descendandDescriptors = classDescriptor
					.getDescendantClassDescriptors();
			assertEquals("Invalid number of descendants", descendandDescriptors
					.size(), 1);
			assertTrue(
					"Descendant  class doesn't have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertFalse(
					"Descendant class  have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerConcreteClassStrategy());

			classDescriptor = mapper.getClassDescriptorByClass(SubDescendant.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertEquals("Incorrect path field", classDescriptor
					.getPathFieldDescriptor().getFieldName(), "path");
			assertTrue("The subdescendant  class has no discriminator",
					classDescriptor.hasDiscriminator());
			assertNotNull("ancestorField is null in the descendant class",
					classDescriptor.getFieldDescriptor("ancestorField"));
			assertFalse("The subdescendant class is abstract", classDescriptor
					.isAbstract());
			assertNotNull("The subdescendant class has not an ancestor",
					classDescriptor.getSuperClassDescriptor());
			assertEquals("Invalid ancestor class for the descendant class",
					classDescriptor.getSuperClassDescriptor().getClassName(),
					"org.apache.jackrabbit.ocm.testmodel.inheritance.Descendant");
			descendandDescriptors = classDescriptor
					.getDescendantClassDescriptors();
			assertEquals("Invalid number of descendants", descendandDescriptors
					.size(), 0);
			assertTrue(
					"SubDescendant  class doesn't have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertFalse(
					"SubDescendant class  have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerConcreteClassStrategy());

		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}

	/**
	 *
	 * Test Node Type per concrete class  setting
	 */	
	public void testMapperNtConcreteClass() {
		try {
			Mapper mapper = getMapper();
			
			assertNotNull("Mapper is null", mapper);

			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(CmsObjectImpl.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertEquals("Incorrect path field", classDescriptor
					.getPathFieldDescriptor().getFieldName(), "path");
			assertFalse("The cms object class  has discriminator",
					classDescriptor.hasDiscriminator());
			assertTrue("The cmsobject class is not abstract", classDescriptor
					.isAbstract());
			assertNull("The cmsobject class has an ancestor", classDescriptor
					.getSuperClassDescriptor());
			assertFalse(
					"The cmsobject class  have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertTrue(
					"The cmsobject class  have not a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerConcreteClassStrategy());
			assertTrue("The cmsobject class has no descendant ",
					classDescriptor.hasDescendants());
			assertEquals("Invalid number of descendants", classDescriptor
					.getDescendantClassDescriptors().size(), 2);

			classDescriptor = mapper.getClassDescriptorByClass(DocumentImpl.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertEquals("Incorrect path field", classDescriptor
					.getPathFieldDescriptor().getFieldName(), "path");
			assertFalse("The document class  has discriminator",
					classDescriptor.hasDiscriminator());
			assertFalse("The document class is abstract", classDescriptor
					.isAbstract());
			assertNotNull("The document class has not  an ancestor",
					classDescriptor.getSuperClassDescriptor());
			assertEquals("The document class has an invalid ancestor ancestor",
					classDescriptor.getSuperClassDescriptor().getClassName(),
					"org.apache.jackrabbit.ocm.testmodel.inheritance.impl.ContentImpl");
			assertFalse(
					"The document class  have a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertTrue(
					"The document class  have not a node type per hierarchy strategy",
					classDescriptor.usesNodeTypePerConcreteClassStrategy());
			assertFalse("The document class has no descendant ",
					classDescriptor.hasDescendants());
			assertEquals("Invalid number of descendants", classDescriptor
					.getDescendantClassDescriptors().size(), 0);

		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}

	/**
	 * Test interface setting
	 */
	public void testInterfaceWithDiscriminator() {
		try {
			Mapper mapper = getMapper();
			assertNotNull("Mapper is null", mapper);
			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(Interface.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertTrue("Interface is not an interface", classDescriptor.isInterface());
			assertTrue("Interface  has not a discriminator", classDescriptor.hasDiscriminator());
			String[] mixinTypes = classDescriptor.getJcrMixinTypes();
			assertEquals("Invalid mixin type for the interface",mixinTypes.length , 0);
			assertNull("The interface has an ancestor", classDescriptor.getSuperClassDescriptor());
			assertTrue("The interface has not implementation/descendant", classDescriptor.hasDescendants());
			Collection descendants = classDescriptor.getDescendantClassDescriptors();
			assertEquals("Invalid number of implementation/descendants", descendants.size(), 1);
			assertEquals("Invalid interface implementation",( (ClassDescriptor) descendants.iterator().next()).getClassName(), "org.apache.jackrabbit.ocm.testmodel.inheritance.AnotherDescendant");
			assertTrue("Invalid extend strategy", classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertFalse("Incalid extend strategy", classDescriptor.usesNodeTypePerConcreteClassStrategy());
			
			classDescriptor = mapper.getClassDescriptorByClass(AnotherDescendant.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertFalse("Interface is  an interface", classDescriptor.isInterface());
			assertTrue("AnotherDescendant  has not a discriminator", classDescriptor.hasDiscriminator());
			assertEquals("Invalid number of implemented interface", classDescriptor.getImplements().size(), 1);
			assertEquals("Invalid  interface name", classDescriptor.getImplements().iterator().next(), "org.apache.jackrabbit.ocm.testmodel.interfaces.Interface");
			assertTrue("Invalid extend strategy", classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertFalse("Invalid extend strategy", classDescriptor.usesNodeTypePerConcreteClassStrategy());
			

		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}
	
	/**
	 * Test interface setting
	 */
	public void testInterfaceWithoutDiscriminator()
	{
		try {
			Mapper mapper = getMapper();
			assertNotNull("Mapper is null", mapper);
			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(CmsObject.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertTrue("CmsObject is not an interface", classDescriptor.isInterface());
			assertFalse("Interface  has a discriminator", classDescriptor.hasDiscriminator());
			String[] mixinTypes = classDescriptor.getJcrMixinTypes();
			assertEquals("Invalid mixin type for the interface",mixinTypes.length , 0);
			assertNull("The interface has an ancestor", classDescriptor.getSuperClassDescriptor());
			assertTrue("The interface has not implementation/descendant", classDescriptor.hasDescendants());
			Collection descendants = classDescriptor.getDescendantClassDescriptors();
			assertEquals("Invalid number of implementation/descendants", descendants.size(),3);			
			assertFalse("Invalid extend strategy", classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertTrue("Invalid extend strategy", classDescriptor.usesNodeTypePerConcreteClassStrategy());
			
			
			classDescriptor = mapper.getClassDescriptorByClass(Document.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertTrue("Document is not  an interface", classDescriptor.isInterface());
			assertFalse("Document  has a discriminator", classDescriptor.hasDiscriminator());
			assertEquals("Invalid number of implemented interface", classDescriptor.getImplements().size(), 0);			
			assertFalse("Invalid extend strategy", classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertTrue("Invalid extend strategy", classDescriptor.usesNodeTypePerConcreteClassStrategy());
			descendants = classDescriptor.getDescendantClassDescriptors();			
			assertEquals("Invalid number of implementation/descendants", descendants.size(),1);
		

			classDescriptor = mapper.getClassDescriptorByClass(DocumentImpl.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertFalse("DocumentImpl is  an interface", classDescriptor.isInterface());
			assertFalse("DocumentImpl  has a discriminator", classDescriptor.hasDiscriminator());
			assertTrue("DocumentImpl has not interface", classDescriptor.hasInterfaces());	
			assertEquals("Invalid number of implemented interface", classDescriptor.getImplements().size(), 1);				
			assertFalse("Invalid extend strategy", classDescriptor.usesNodeTypePerHierarchyStrategy());
			assertTrue("Invalid extend strategy", classDescriptor.usesNodeTypePerConcreteClassStrategy());

			
		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}
	
	public void testInheritanceWithMixin()
	{
		try {
			Mapper mapper = getMapper();
			assertNotNull("Mapper is null", mapper);
			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(org.apache.jackrabbit.ocm.testmodel.inheritance.withmixin.Ancestor.class);
			assertNotNull("Classdescriptor is null", classDescriptor);
			assertEquals("No mixin defined for Ancestor", 1, classDescriptor.getJcrMixinTypes().length);
			String[] mixinTypes = classDescriptor.getJcrMixinTypes();
			assertEquals("Invalid mixin defined for Ancestor", "mix:referenceable", mixinTypes[0]);
			
			classDescriptor = mapper.getClassDescriptorByClass(org.apache.jackrabbit.ocm.testmodel.inheritance.withmixin.Descendant.class);
			assertEquals("No mixin defined for Descendant", 1, classDescriptor.getJcrMixinTypes().length);
			mixinTypes = classDescriptor.getJcrMixinTypes();
			assertEquals("Invalid mixin defined for Ancestor", "mix:referenceable", mixinTypes[0]);
			
			classDescriptor = mapper.getClassDescriptorByClass(org.apache.jackrabbit.ocm.testmodel.inheritance.withmixin.SubDescendant.class);
			assertEquals("No mixin defined for SubDescendant", 1, classDescriptor.getJcrMixinTypes().length);
			mixinTypes = classDescriptor.getJcrMixinTypes();
			assertEquals("Invalid mixin defined for Ancestor", "mix:referenceable", mixinTypes[0]);

			
		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}

	/**
	 *
	 * Test Node Type per concrete class setting
	 */
	public void testProxy() {
		try {
			Mapper mapper = getMapper();
			assertNotNull("Mapper is null", mapper);

			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(Main.class);
			assertNotNull("ClassDescriptor is null", classDescriptor);
			assertTrue("Invalid proxy setting for bean field proxyDetail ", classDescriptor.getBeanDescriptor("proxyDetail").isProxy());
			assertFalse("Invalid proxy setting for bean field detail  ", classDescriptor.getBeanDescriptor("detail").isProxy());
			assertTrue("Invalid proxy setting for collection field proxyDetail ", classDescriptor.getCollectionDescriptor("proxyCollection").isProxy());
			
		} catch (JcrMappingException e) {
			e.printStackTrace();
			fail("Impossible to retrieve the converter " + e);
		}
	}
	
	private Mapper getMapper()
	{
		List<Class> classes = new ArrayList<Class>();
		classes.add( org.apache.jackrabbit.ocm.testmodel.A.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.B.class);
		classes.add( org.apache.jackrabbit.ocm.testmodel.C.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.PropertyTest.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.Ancestor.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.AnotherDescendant.class);
		classes.add( org.apache.jackrabbit.ocm.testmodel.inheritance.Descendant.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.SubDescendant.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.withmixin.Ancestor.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.withmixin.AnotherDescendant.class);
		classes.add( org.apache.jackrabbit.ocm.testmodel.inheritance.withmixin.Descendant.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.withmixin.SubDescendant.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.impl.CmsObjectImpl.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.inheritance.impl.DocumentImpl.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.interfaces.Document.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.interfaces.Interface.class);
		classes.add(org.apache.jackrabbit.ocm.testmodel.proxy.Main.class);
		
		Mapper mapper = new AnnotationMapperImpl(classes);
		return mapper;
	}
	
}