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
import javax.jcr.Session;

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
 * Bean converter used to access to the parent object. 
 * the mixin type referenceable is not mandatory for the node matching to the parent object. 
 * 
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class ParentBeanConverterImpl extends AbstractBeanConverterImpl  implements BeanConverter {

	private final static Log log = LogFactory.getLog(ParentBeanConverterImpl.class);
	
	public ParentBeanConverterImpl(Mapper mapper, ObjectConverter objectConverter, AtomicTypeConverterProvider atomicTypeConverterProvider) 
	{
		super(mapper, objectConverter, atomicTypeConverterProvider);	
	}

	public void insert(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws PersistenceException, RepositoryException, 	JcrMappingException {
	}

	public void update(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws PersistenceException, RepositoryException,	JcrMappingException {
	}

	public Object getObject(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Class beanClass, Object parent)
			throws PersistenceException, RepositoryException,JcrMappingException {
        try 
        {			
			Node grandParentNode = parentNode.getParent();
			if (grandParentNode.getPath().equals("/"))
			{
				return null;
			}
			return objectConverter.getObject(session, grandParentNode.getPath());
			
		} 
        catch (javax.jcr.RepositoryException e) 
		{
			throw new RepositoryException(e);
		} 
		
	}

	public void remove(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
	          throws PersistenceException,	RepositoryException, JcrMappingException {

	}
	
	/**
	 * 
	 * Default implementation for many BeanConverter. This method can be overridden in specific BeanConverter
	 * 
	 */
    public String getPath(Session session, BeanDescriptor beanDescriptor, Node parentNode)
                throws PersistenceException
    {		
		 try 
		 {
			if (parentNode != null)
		    {
				
				 return parentNode.getParent().getPath();
			}
			else
			{
			    return null; 
			}
		} 
		catch (javax.jcr.RepositoryException e) 
		{
			throw new RepositoryException(e);
		}
	}	

}
