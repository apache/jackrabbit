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
package org.apache.jackrabbit.ocm.persistence.beanconverter.impl;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.PersistenceException;
import org.apache.jackrabbit.ocm.exception.RepositoryException;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.persistence.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.persistence.beanconverter.BeanConverter;
import org.apache.jackrabbit.ocm.persistence.objectconverter.ObjectConverter;
/**
 * 
 * Default Bean Converter
 * 
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class DefaultBeanConverterImpl extends AbstractBeanConverterImpl  implements BeanConverter {

	private final static Log log = LogFactory.getLog(DefaultBeanConverterImpl.class);
	
	public DefaultBeanConverterImpl(Mapper mapper, ObjectConverter objectConverter, AtomicTypeConverterProvider atomicTypeConverterProvider) 
	{
		super(mapper, objectConverter, atomicTypeConverterProvider);	
	}

	public void insert(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws PersistenceException, RepositoryException, 	JcrMappingException 
	{
		objectConverter.insert(session, parentNode, beanDescriptor.getJcrName(), object);
	}

	public void update(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws PersistenceException, RepositoryException,	JcrMappingException 
	{
		try 
		{
			String jcrNodeName = beanDescriptor.getJcrName(); 
			if (parentNode.hasNode(jcrNodeName))
			{		
			   objectConverter.update(session, parentNode, beanDescriptor.getJcrName() , object);
			}
			else 
			{
			   objectConverter.insert(session, parentNode, beanDescriptor.getJcrName() , object);
			}
		} 
		catch (javax.jcr.RepositoryException e) 
		{
			throw new RepositoryException(e);	
		}
	}

	public Object getObject(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Class beanClass, Object parent)
			throws PersistenceException, RepositoryException,JcrMappingException 
	{
        return objectConverter.getObject(session, beanClass, this.getPath(session, beanDescriptor, parentNode));
		
	}

	public void remove(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
	          throws PersistenceException,	RepositoryException, JcrMappingException 
	{
		try {
			if (parentNode.hasNode(beanDescriptor.getJcrName())) 
			{
				parentNode.getNode(beanDescriptor.getJcrName()).remove();
			}

		} catch (javax.jcr.RepositoryException e) {
			
			throw new RepositoryException(e);
		}
		
	}
	

}
