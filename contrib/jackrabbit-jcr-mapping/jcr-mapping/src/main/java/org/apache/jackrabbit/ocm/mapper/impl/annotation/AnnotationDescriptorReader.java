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
package org.apache.jackrabbit.ocm.mapper.impl.annotation;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.InitMapperException;
import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ImplementDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;

/**
 * Helper class that reads the xml mapping file and load all class descriptors into memory (object graph)
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class AnnotationDescriptorReader implements DescriptorReader
{
	private static final Log log = LogFactory.getLog(AnnotationDescriptorReader.class);
	
	List<Class> annotatedClassNames;
    public AnnotationDescriptorReader(List<Class> annotatedClassNames)
    {
   	     this.annotatedClassNames = annotatedClassNames;
    }
    
    

    public MappingDescriptor loadClassDescriptors()
	{
		MappingDescriptor mappingDescriptor = new MappingDescriptor();	
		for (Class clazz : annotatedClassNames) {
			
			ClassDescriptor classDescriptor = buildClassDescriptor(mappingDescriptor, clazz);
			mappingDescriptor.addClassDescriptor(classDescriptor);
		}
		return mappingDescriptor;
		
	}
	
	private ClassDescriptor buildClassDescriptor(MappingDescriptor mappingDescriptor, Class clazz)  
	{
		ClassDescriptor classDescriptor = null;
		
		Node nodeAnnotation =  (Node) clazz.getAnnotation(Node.class);
		if (nodeAnnotation != null)
		{
			classDescriptor = createClassDescriptor(clazz, nodeAnnotation);			
			addAttributeDescriptors(mappingDescriptor, classDescriptor, clazz);
			return classDescriptor;
		}
		else
		{
			throw  new InitMapperException("The annotation @Node is not defined for the persistent class " + clazz.getName());
		}
	
		
		
	}

	private ClassDescriptor createClassDescriptor(Class clazz, Node nodeAnnotation) 
	{
		ClassDescriptor classDescriptor = new ClassDescriptor();
		classDescriptor.setClassName(clazz.getName());
		classDescriptor.setJcrType(nodeAnnotation.jcrType());
		if (nodeAnnotation.jcrSuperTypes() != null && ! nodeAnnotation.jcrSuperTypes().equals(""))
		{
		     classDescriptor.setJcrSuperTypes(nodeAnnotation.jcrSuperTypes());
		}
		
		if (nodeAnnotation.jcrMixinTypes() != null && ! nodeAnnotation.jcrMixinTypes().equals(""))
		{		
		     classDescriptor.setJcrMixinTypes(nodeAnnotation.jcrMixinTypes());
		}
		
		if (nodeAnnotation.extend() != null && ! nodeAnnotation.extend().equals(Object.class))
		{
		     classDescriptor.setExtend(nodeAnnotation.extend().getName());
		}
		
		classDescriptor.setAbstract(nodeAnnotation.isAbstract());
		classDescriptor.setInterface(clazz.isInterface());
		return classDescriptor;
	}
	
	private void addImplementDescriptor(ClassDescriptor classDescriptor, Class clazz) 
	{
		Implement implementAnnotation = (Implement) clazz.getAnnotation(Implement.class);
		if (implementAnnotation != null) 
		{
            ImplementDescriptor implementDescriptor =  new ImplementDescriptor();
            implementDescriptor.setInterfaceName(implementAnnotation.interfaceName().getName());
            classDescriptor.addImplementDescriptor(implementDescriptor);
		}
		
	}
	
	/**
	 * Add FieldDescriptors, BeanDescriptors and CollectionDescriptors.
	 * The descriptots can be defined on the getter methods or on the field declation. 
	 * 
	 * @param mappingDescriptor The mapping descriptor
	 * @param classDescriptor the classdescriptor for which the descriptors have to be added
	 * @param clazz The associated class
	 */
	private void addAttributeDescriptors(MappingDescriptor mappingDescriptor, ClassDescriptor classDescriptor,Class clazz) {
 
		addDescriptorsFromFields(mappingDescriptor, classDescriptor, clazz);
		addDescriptorsFromGetters(mappingDescriptor, classDescriptor, clazz);
	}

	private void addDescriptorsFromFields(MappingDescriptor mappingDescriptor, ClassDescriptor classDescriptor, Class clazz) {
	    
		java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
	    for (int index = 0; index < fields.length; index++) 
	    {
			Field fieldAnnotation = fields[index].getAnnotation(Field.class);
			if (fieldAnnotation != null) {
				addFieldDescriptor(classDescriptor, fields[index].getName(), fieldAnnotation);
			}
			
			// Check if there is an Bean annotation
			Bean beanAnnotation = fields[index].getAnnotation(Bean.class);
			if (beanAnnotation != null) {
				addBeanDescriptor(classDescriptor, fields[index].getName(), beanAnnotation);
			}
			
			// Check if there is an Collection annotation
			Collection collectionAnnotation = fields[index].getAnnotation(Collection.class);
			if (collectionAnnotation != null) {
				addCollectionDescriptor(mappingDescriptor, classDescriptor, fields[index].getName(), 
						                fields[index].getName(), collectionAnnotation);
			}
					
		   	
		}
		
	}

	private void addDescriptorsFromGetters(MappingDescriptor mappingDescriptor, ClassDescriptor classDescriptor, Class clazz) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {

			// Check if there is an Field annotation
			Field fieldAnnotation = propertyDescriptor.getReadMethod().getAnnotation(Field.class);
			if (fieldAnnotation != null) {
				addFieldDescriptor(classDescriptor, propertyDescriptor.getName(), fieldAnnotation);
			}
			
			// Check if there is an Bean annotation
			Bean beanAnnotation = propertyDescriptor.getReadMethod().getAnnotation(Bean.class);
			if (beanAnnotation != null) {
				addBeanDescriptor(classDescriptor, propertyDescriptor.getName(), beanAnnotation);
			}
			
			// Check if there is an Collection annotation
			Collection collectionAnnotation = propertyDescriptor.getReadMethod().getAnnotation(Collection.class);
			if (collectionAnnotation != null) {
				addCollectionDescriptor(mappingDescriptor, classDescriptor, propertyDescriptor.getName(), 
						                propertyDescriptor.getReadMethod().getReturnType().getName(), collectionAnnotation);
			}
		}
	}


	private void addCollectionDescriptor(MappingDescriptor mappingDescriptor, ClassDescriptor descriptor,
			                             String fieldName, String collectionClassName, Collection collectionAnnotation) {
		
		Class targetClass = collectionAnnotation.elementClassName();
		CollectionDescriptor collectionDescriptor = new CollectionDescriptor();	
		
		collectionDescriptor.setFieldName(fieldName);
		
		if (collectionAnnotation.jcrName() != null && ! collectionAnnotation.jcrName().equals(""))
		{
		   collectionDescriptor.setJcrName(collectionAnnotation.jcrName());
		}
		else
		{
		   collectionDescriptor.setJcrName(fieldName);
		}
		
		Node annotationNode = (Node) targetClass.getAnnotation(Node.class);
		collectionDescriptor.setProxy(collectionAnnotation.proxy());
		
		collectionDescriptor.setAutoInsert(collectionAnnotation.autoInsert());
		collectionDescriptor.setAutoRetrieve(collectionAnnotation.autoRetrieve());
		collectionDescriptor.setAutoUpdate(collectionAnnotation.autoUpdate());
		collectionDescriptor.setCollectionClassName(collectionClassName);
		if (! collectionAnnotation.elementClassName().equals(Object.class))
		{
			collectionDescriptor.setElementClassName(collectionAnnotation.elementClassName().getName());
		}
		else
		{
		    collectionDescriptor.setElementClassName(targetClass.getName());
		}

		if (! collectionAnnotation.collectionClassName().equals(Object.class))
		{
			collectionDescriptor.setCollectionClassName(collectionAnnotation.collectionClassName().getName());
		}
		
		collectionDescriptor.setCollectionConverter(collectionAnnotation.collectionConverter().getName());
		if (annotationNode != null)
		{
		    collectionDescriptor.setJcrType(annotationNode.jcrType());
		}
		collectionDescriptor.setJcrSameNameSiblings(collectionAnnotation.jcrSameNameSiblings());
		collectionDescriptor.setJcrAutoCreated(collectionAnnotation.jcrAutoCreated());
		collectionDescriptor.setJcrProtected(collectionAnnotation.jcrProtected());
		collectionDescriptor.setJcrOnParentVersion(collectionAnnotation.jcrOnParentVersion());
		collectionDescriptor.setJcrMandatory(collectionAnnotation.jcrMandatory());
		

		descriptor.addCollectionDescriptor(collectionDescriptor);
	}
	
	private void addBeanDescriptor(ClassDescriptor classDescriptor, String fieldName, Bean beanAnnotation) {
		BeanDescriptor beanDescriptor = new BeanDescriptor();
		beanDescriptor.setFieldName(fieldName);
		if (beanAnnotation.jcrName() != null && ! beanAnnotation.jcrName().equals(""))
		{
		   beanDescriptor.setJcrName(beanAnnotation.jcrName());
		}
		else
		{
			beanDescriptor.setJcrName(fieldName);
		}
		
		beanDescriptor.setProxy(beanAnnotation.proxy());				
		beanDescriptor.setConverter(beanAnnotation.converter().getName());
		beanDescriptor.setAutoInsert(beanAnnotation.autoInsert());
		beanDescriptor.setAutoRetrieve(beanAnnotation.autoRetrieve());
		beanDescriptor.setAutoUpdate(beanAnnotation.autoUpdate());
		beanDescriptor.setJcrType(beanAnnotation.jcrType());
		beanDescriptor.setJcrAutoCreated(beanAnnotation.jcrAutoCreated());
		beanDescriptor.setJcrMandatory(beanAnnotation.jcrMandatory());
		beanDescriptor.setJcrOnParentVersion(beanAnnotation.jcrOnParentVersion());
		beanDescriptor.setJcrProtected(beanAnnotation.jcrProtected());			            
		beanDescriptor.setJcrSameNameSiblings(beanAnnotation.jcrSameNameSiblings());				

		classDescriptor.addBeanDescriptor(beanDescriptor);
	}


	private void addFieldDescriptor(ClassDescriptor classDescriptor, String fieldName, Field fieldAnnotation) 
	{
				
		FieldDescriptor fieldDescriptor = new FieldDescriptor();				
		fieldDescriptor.setFieldName(fieldName);
		if ((fieldAnnotation.jcrName() != null) && (!fieldAnnotation.jcrName().equals("")))
		{
			fieldDescriptor.setJcrName(fieldAnnotation.jcrName());	
		}
		else
		{
			fieldDescriptor.setJcrName(fieldName);	
		}
		fieldDescriptor.setId(fieldAnnotation.id());				
		fieldDescriptor.setPath(fieldAnnotation.path());
		fieldDescriptor.setUuid(fieldAnnotation.uuid());
		
		// It is not possible to set a null value into an annotation attribute.
		// If the converter == Object.class, it should be considered as null
		if (! fieldAnnotation.converter().equals(Object.class))
		{
		    fieldDescriptor.setConverter(fieldAnnotation.converter().getName());
		}
		
		// It is not possible to set a null value into an annotation attribute.
		// If the jcrDefaultValue value is an empty string => it should be considered as null
		if ((fieldAnnotation.jcrDefaultValue() != null) && (!fieldAnnotation.jcrDefaultValue().equals("")))
		{
		     fieldDescriptor.setJcrDefaultValue(fieldAnnotation.jcrDefaultValue());
		}
		
		// It is not possible to set a null value into an annotation attribute.
		// If the jcrValueConstraints value is an empty string => it should be considered as null
		if ((fieldAnnotation.jcrValueConstraints() != null) && (!fieldAnnotation.jcrValueConstraints().equals("")))
		{	
		     fieldDescriptor.setJcrValueConstraints(fieldAnnotation.jcrValueConstraints());
		}
		
		// It is not possible to set a null value into an annotation attribute.
		// If the jcrProperty value is an empty string => it should be considered as null
		if ((fieldAnnotation.jcrType() != null) && (!fieldAnnotation.jcrType().equals("")))
		{
		    fieldDescriptor.setJcrType(fieldAnnotation.jcrType());
		}
		
		fieldDescriptor.setJcrAutoCreated(fieldAnnotation.jcrAutoCreated());
		fieldDescriptor.setJcrMandatory(fieldAnnotation.jcrMandatory());
		fieldDescriptor.setJcrOnParentVersion(fieldAnnotation.jcrOnParentVersion());
		fieldDescriptor.setJcrProtected(fieldAnnotation.jcrProtected());
		fieldDescriptor.setJcrMultiple(fieldAnnotation.jcrMultiple());
		
		//fieldDescriptor.setJcrType(value)
		classDescriptor.addFieldDescriptor(fieldDescriptor);
	}    


}
