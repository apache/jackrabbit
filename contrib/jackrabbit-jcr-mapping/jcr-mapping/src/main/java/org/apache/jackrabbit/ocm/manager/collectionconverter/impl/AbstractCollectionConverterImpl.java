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
package org.apache.jackrabbit.ocm.manager.collectionconverter.impl;

import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.PersistenceException;
import org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;

/** 
 * Abstract class used for all CollectionConverter
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public abstract class AbstractCollectionConverterImpl implements CollectionConverter {
	protected Map atomicTypeConverters;
	protected ObjectConverter objectConverter;
	protected Mapper mapper;

	/**
	 * Constructor
	 * 
	 * @param atomicTypeConverters The atomic type converter to used
	 * @param objectConverter The object converter to used
	 * @param mapper The mapper to used
	 */
	public AbstractCollectionConverterImpl(Map atomicTypeConverters, ObjectConverter objectConverter, Mapper mapper) {
		this.atomicTypeConverters = atomicTypeConverters;
		this.objectConverter = objectConverter;
		this.mapper = mapper;
	}

	protected abstract void doInsertCollection(Session session, Node parentNode, CollectionDescriptor descriptor,
			ManageableCollection collection) throws RepositoryException;

	protected abstract void doUpdateCollection(Session session, Node parentNode, CollectionDescriptor descriptor,
			ManageableCollection collection) throws RepositoryException;

	protected abstract ManageableCollection doGetCollection(Session session, Node parentNode,
			CollectionDescriptor collectionDescriptor, Class collectionFieldClass) throws RepositoryException;

	protected abstract boolean doIsNull(Session session, Node parentNode, CollectionDescriptor collectionDescriptor,
			Class collectionFieldClass) throws RepositoryException;

	/**
	 * @see org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter#insertCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection)
	 */
	public void insertCollection(Session session, Node parentNode, CollectionDescriptor collectionDescriptor,
			ManageableCollection collection) {
		try {
			doInsertCollection(session, parentNode, collectionDescriptor, collection);
		} catch (ItemExistsException iee) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName() + ". An item already exists.", iee);
		} catch (PathNotFoundException pnfe) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName(), pnfe);
		} catch (VersionException ve) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName(), ve);
		} catch (ConstraintViolationException cve) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName() + ". Constraint violation.", cve);
		} catch (LockException le) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName() + " on locked parent.", le);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Cannot insert collection field : "
					+ collectionDescriptor.getFieldName() + " of class "
					+ collectionDescriptor.getClassDescriptor().getClassName(), re);
		}
	}

	/**
	 *
	 * @see org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter#updateCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection)
	 */
	public void updateCollection(Session session, Node parentNode, CollectionDescriptor collectionDescriptor,
			ManageableCollection collection) {
		try {

				doUpdateCollection(session, parentNode, collectionDescriptor, collection);
		} catch (VersionException ve) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName(), ve);
		} catch (LockException le) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName() + " on locked node", le);
		} catch (ConstraintViolationException cve) {
			throw new PersistenceException("Cannot insert collection field : " + collectionDescriptor.getFieldName()
					+ " of class " + collectionDescriptor.getClassDescriptor().getClassName() + " Constraint violation.", cve);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Cannot insert collection field : "
					+ collectionDescriptor.getFieldName() + " of class "
					+ collectionDescriptor.getClassDescriptor().getClassName(), re);
		}
	}

	/**
	 * @see org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter#getCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, java.lang.Class)
	 */
	public ManageableCollection getCollection(Session session, Node parentNode, CollectionDescriptor collectionDescriptor,
			Class collectionFieldClass) {
		try {
			return doGetCollection(session, parentNode, collectionDescriptor, collectionFieldClass);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Cannot get collection field : "
					+ collectionDescriptor.getFieldName() + "for " + collectionDescriptor.getClassDescriptor().getClassName(), re);
		}
	}

	/**
	 * @see org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter#isNull(Session, Node, CollectionDescriptor, Class)
	 */
	public boolean isNull(Session session, Node parentNode, CollectionDescriptor collectionDescriptor,
			Class collectionFieldClass) {
		try {
			return doIsNull(session, parentNode, collectionDescriptor, collectionFieldClass);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Cannot  check if the collections has elements : "
					+ collectionDescriptor.getFieldName() + "for " + collectionDescriptor.getClassDescriptor().getClassName(), re);
		}
	}
	protected String getCollectionJcrName(CollectionDescriptor descriptor) {
		String jcrName = descriptor.getJcrName();

		if (null == jcrName) {
			throw new JcrMappingException("The JcrName attribute is not defined for the CollectionDescriptor : "
					+ descriptor.getFieldName());
		}

		return jcrName;
	}
}
