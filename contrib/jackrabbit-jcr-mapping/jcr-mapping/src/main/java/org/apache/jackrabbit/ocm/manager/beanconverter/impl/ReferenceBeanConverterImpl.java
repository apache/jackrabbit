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
package org.apache.jackrabbit.ocm.manager.beanconverter.impl;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.exception.RepositoryException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.beanconverter.BeanConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
/**
 * 
 * Map a bean attribute into a reference jcr property. It is not possible to update direclty the referenced bean. 
 * Only the corresponding uuid can be updated in the main object. The modifications on the referenced bean attributes are ignored
 * 
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class ReferenceBeanConverterImpl extends AbstractBeanConverterImpl  implements BeanConverter {

	private final static Log log = LogFactory.getLog(ReferenceBeanConverterImpl.class);
	
	public ReferenceBeanConverterImpl(Mapper mapper, ObjectConverter objectConverter, AtomicTypeConverterProvider atomicTypeConverterProvider) 
	{
		super(mapper, objectConverter, atomicTypeConverterProvider);	
	}

	public void insert(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws ObjectContentManagerException, RepositoryException, 	JcrMappingException 
	{
		updateReferenceProperty(parentNode, beanDescriptor, beanClassDescriptor, object); 
		
	}

	public void update(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws ObjectContentManagerException, RepositoryException,	JcrMappingException 
	{
		updateReferenceProperty(parentNode, beanDescriptor, beanClassDescriptor, object);
	}

	public Object getObject(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Class beanClass, Object parent)
			throws ObjectContentManagerException, RepositoryException,JcrMappingException 
	{
        try {
			String uuid = parentNode.getProperty(beanDescriptor.getJcrName()).getString();
			String path = session.getNodeByUUID(uuid).getPath();
			
			return objectConverter.getObject(session, path);
		} catch (Exception e) {
			return null;		
			
		}
		
	}

	public void remove(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
	          throws ObjectContentManagerException,	RepositoryException, JcrMappingException 
	{
		updateReferenceProperty(parentNode, beanDescriptor, beanClassDescriptor, null);	
	}
	
	private void updateReferenceProperty(Node parentNode, BeanDescriptor beanDescriptor,ClassDescriptor beanClassDescriptor, Object object) {
		try {
			if (object == null)
			{
				parentNode.setProperty(beanDescriptor.getJcrName(), (Value) null);
			}
			
			FieldDescriptor fieldDescriptor = beanClassDescriptor.getUuidFieldDescriptor();
			if (fieldDescriptor == null)
			{
				throw new JcrMappingException("The bean doesn't have an uuid - classdescriptor : " + beanClassDescriptor.getClassName());
			}
			
			String uuid = (String) ReflectionUtils.getNestedProperty(object, fieldDescriptor.getFieldName());
			parentNode.setProperty(beanDescriptor.getJcrName(), uuid, PropertyType.REFERENCE);
		} catch (Exception e) {
			throw new ObjectContentManagerException("Impossible to insert the bean attribute into the repository", e);
		}
	}

}
