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
package org.apache.portals.graffito.jcr.persistence.beanconverter.impl;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.portals.graffito.jcr.exception.JcrMappingException;
import org.apache.portals.graffito.jcr.exception.PersistenceException;
import org.apache.portals.graffito.jcr.exception.RepositoryException;
import org.apache.portals.graffito.jcr.mapper.Mapper;
import org.apache.portals.graffito.jcr.mapper.model.BeanDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.ClassDescriptor;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.portals.graffito.jcr.persistence.beanconverter.BeanConverter;
import org.apache.portals.graffito.jcr.persistence.objectconverter.ObjectConverter;
import org.apache.portals.graffito.jcr.persistence.objectconverter.impl.SimpleFieldsHelper;
/**
 * 
 * Bean converter used to map some node properties into one nested bean field.
 * The corresponding bean field is not associated to a subnode.
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class InlineBeanConverterImpl extends AbstractBeanConverterImpl  implements BeanConverter {

	SimpleFieldsHelper simpleFieldsHelper; 
	
	public InlineBeanConverterImpl(Mapper mapper, ObjectConverter objectConverter, AtomicTypeConverterProvider atomicTypeConverterProvider) 
	{
		super(mapper, objectConverter, atomicTypeConverterProvider);
		this.simpleFieldsHelper = new SimpleFieldsHelper(atomicTypeConverterProvider);
	}

	public void insert(Session session, Node parentNode,  BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws PersistenceException, RepositoryException, 	JcrMappingException {
		
		simpleFieldsHelper.storeSimpleFields(session, object, beanClassDescriptor, parentNode);
	}

	public void update(Session session, Node parentNode,  BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
			throws PersistenceException, RepositoryException, JcrMappingException {
		simpleFieldsHelper.storeSimpleFields(session, object, beanClassDescriptor, parentNode);
	}

	public Object getObject(Session session, Node parentNode,  BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Class beanClass, Object bean)
			throws PersistenceException, RepositoryException,JcrMappingException {
		
		return simpleFieldsHelper.retrieveSimpleFields(session, beanClassDescriptor, parentNode, bean);
 
	}

	public void remove(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object object, ClassDescriptor parentClassDescriptor, Object parent)
	          throws PersistenceException,	RepositoryException, JcrMappingException {
				
		simpleFieldsHelper.storeSimpleFields(session, object, beanClassDescriptor, parentNode);
	}
	
	

}
