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

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Helper class that reads the xml mapping file and load all class descriptors into memory (object graph)
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class AnnotationDescriptorReader implements DescriptorReader
{
	List<String> annotatedClassNames;
    public AnnotationDescriptorReader(List<String> annotatedClassNames)
    {
   	     this.annotatedClassNames = annotatedClassNames;
    }
    
    

    public MappingDescriptor loadClassDescriptors()
	{
		MappingDescriptor mappingDescriptor = new MappingDescriptor();	
		for (String className : annotatedClassNames) {
			Class clazz = ReflectionUtils.forName(className);
			ClassDescriptor classDescriptor = buildClassDescriptor(mappingDescriptor, clazz);
			mappingDescriptor.addClassDescriptor(classDescriptor);
		}
		return mappingDescriptor;
		
	}
	
	private ClassDescriptor buildClassDescriptor(MappingDescriptor mappingDescriptor, Class clazz)  
	{
		Node annotationNode =  (Node) clazz.getAnnotation(Node.class);
		ClassDescriptor descriptor = new ClassDescriptor();
		descriptor.setClassName(clazz.getName());
		descriptor.setJcrType(annotationNode.jcrType());
		descriptor.setJcrSuperTypes(annotationNode.jcrSuperTypes());		
		descriptor.setJcrMixinTypes(annotationNode.jcrMixinTypes());
		descriptor.setExtend(annotationNode.extend());		
		descriptor.setAbstract(annotationNode.isAbstract());
		descriptor.setInterface(clazz.isInterface());
		
		addFieldDescriptors(descriptor, clazz);
		addBeanDescriptors(descriptor, clazz);
		addCollectionDescriptors(mappingDescriptor, descriptor, clazz);
		return descriptor;
	}

	private void addCollectionDescriptors(MappingDescriptor mappingDescriptor, ClassDescriptor descriptor,Class clazz) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Collection jcrChildNode = propertyDescriptor.getReadMethod().getAnnotation(Collection.class);
			if (jcrChildNode != null) {
				Class targetClass = jcrChildNode.type();
				CollectionDescriptor collectionDescriptor = new CollectionDescriptor();
				ClassDescriptor classDescriptor = mappingDescriptor.getClassDescriptorByName(targetClass.getName());

				if (classDescriptor == null)
					throw new RuntimeException(
							"Unable to reference class "
									+ targetClass.getName()
									+ " as a child node since it has not been registered, ordering perhaps?");

				if (jcrChildNode.jcrName() != null && ! jcrChildNode.jcrName().equals(""))
				{
				   collectionDescriptor.setJcrName(jcrChildNode.jcrName());
				}
				else
				{
				   collectionDescriptor.setJcrName(propertyDescriptor.getName());
				}
				
				Node annotationNode = (Node) targetClass.getAnnotation(Node.class);
				collectionDescriptor.setDefaultPrimaryType(annotationNode.jcrType());
				collectionDescriptor.setJcrSameNameSiblings(jcrChildNode.sameNameSiblings());
				collectionDescriptor.setJcrAutoCreated(jcrChildNode.autoCreate());
				collectionDescriptor.setJcrProtected(jcrChildNode.protect());
				collectionDescriptor.setJcrOnParentVersion(jcrChildNode.onParentVersion());
				collectionDescriptor.setJcrMandatory(jcrChildNode.mandatory());
				collectionDescriptor.setAutoInsert(jcrChildNode.autoInsert());
				collectionDescriptor.setAutoRetrieve(jcrChildNode.autoRetrieve());
				collectionDescriptor.setAutoUpdate(jcrChildNode.autoUpdate());
				collectionDescriptor.setCollectionClassName(propertyDescriptor.getReadMethod().getReturnType().getName());
				collectionDescriptor.setElementClassName(targetClass.getName());
				collectionDescriptor.setCollectionConverter(jcrChildNode.converter().getName());
				collectionDescriptor.setFieldName(propertyDescriptor.getName());

				collectionDescriptor.setJcrType(annotationNode.jcrType());
				collectionDescriptor.setJcrSameNameSiblings(jcrChildNode.sameNameSiblings());
				collectionDescriptor.setProxy(jcrChildNode.proxy());

				descriptor.addCollectionDescriptor(collectionDescriptor);
			}
		}

	}

	
	private void addBeanDescriptors(ClassDescriptor descriptor,Class clazz) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Bean jcrChildNode = propertyDescriptor.getReadMethod().getAnnotation(Bean.class);
			if (jcrChildNode != null) {
				
				BeanDescriptor beanDescriptor = new BeanDescriptor();
				beanDescriptor.setFieldName(propertyDescriptor.getName());
				if (jcrChildNode.jcrName() != null && ! jcrChildNode.jcrName().equals(""))
				{
				   beanDescriptor.setJcrName(jcrChildNode.jcrName());
				}
				else
				{
					beanDescriptor.setJcrName(propertyDescriptor.getName());
				}
				
				beanDescriptor.setProxy(jcrChildNode.proxy());				
				beanDescriptor.setConverter(jcrChildNode.converter().getName());
				beanDescriptor.setAutoInsert(jcrChildNode.autoInsert());
				beanDescriptor.setAutoRetrieve(jcrChildNode.autoRetrieve());
				beanDescriptor.setAutoUpdate(jcrChildNode.autoUpdate());
				beanDescriptor.setJcrType(jcrChildNode.jcrType());
				beanDescriptor.setJcrAutoCreated(jcrChildNode.jcrAutoCreated());
				beanDescriptor.setJcrMandatory(jcrChildNode.jcrMandatory());
				beanDescriptor.setJcrOnParentVersion(jcrChildNode.jcrOnParentVersion());
				beanDescriptor.setJcrProtected(jcrChildNode.jcrProtected());			            
				beanDescriptor.setJcrSameNameSiblings(jcrChildNode.jcrSameNameSiblings());				

				descriptor.addBeanDescriptor(beanDescriptor);
			}
		}

	}
	
	private void addFieldDescriptors(ClassDescriptor descriptor, Class clazz) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Field jcrProperty = propertyDescriptor.getReadMethod().getAnnotation(Field.class);
			if (jcrProperty != null) {
				FieldDescriptor fieldDescriptor = new FieldDescriptor();				
				fieldDescriptor.setFieldName(propertyDescriptor.getName());
				if ((jcrProperty.jcrName() != null) && (!jcrProperty.jcrName().equals("")))
				{
					fieldDescriptor.setJcrName(jcrProperty.jcrName());	
				}
				else
				{
					fieldDescriptor.setJcrName(propertyDescriptor.getName());	
				}
				fieldDescriptor.setId(jcrProperty.id());				
				fieldDescriptor.setPath(jcrProperty.path());
				fieldDescriptor.setUuid(jcrProperty.uuid());
				
				// It is not possible to set a null value into a annotation attribute.
				// If the converter == Object.class, it shoudl be considered as null
				if (! jcrProperty.converter().equals(Object.class))
				{
				    fieldDescriptor.setConverter(jcrProperty.converter().getName());
				}
				fieldDescriptor.setJcrDefaultValue(jcrProperty.jcrDefaultValue());			
				fieldDescriptor.setJcrValueConstraints(jcrProperty.jcrDefaultValue());
				fieldDescriptor.setJcrType(jcrProperty.jcrType());
				
				fieldDescriptor.setJcrAutoCreated(jcrProperty.jcrAutoCreated());
				fieldDescriptor.setJcrMandatory(jcrProperty.jcrMandatory());
				fieldDescriptor.setJcrOnParentVersion(jcrProperty.jcrOnParentVersion());
				fieldDescriptor.setJcrProtected(jcrProperty.jcrProtected());
				fieldDescriptor.setJcrMultiple(jcrProperty.jcrMultiple());
				
				//fieldDescriptor.setJcrType(value)
				descriptor.addFieldDescriptor(fieldDescriptor);
			}
		}

	}    


}
