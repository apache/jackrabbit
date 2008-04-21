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

package org.apache.jackrabbit.ocm.manager.objectconverter.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjectsUtil;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjects;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

public class ProxyManagerImpl implements ProxyManager
{

	private final static Log log = LogFactory.getLog(ProxyManagerImpl.class);



	/**
	 *
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager#createBeanProxy(javax.jcr.Session, org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter, java.lang.Class, java.lang.String)
	 */
	public  Object createBeanProxy(Session session, ObjectConverter objectConverter, Class beanClass, String path)
	{

       try {
			if (!session.itemExists(path)) {
				return null;
			}
		} catch (RepositoryException e) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to check,if the object exits on " + path, e);
		}

		LazyLoader loader = new BeanLazyLoader(objectConverter, session, beanClass, path) ;
		return  Enhancer.create(beanClass, loader);
	}

	/**
	 *
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager#createCollectionProxy(javax.jcr.Session, org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, java.lang.Class)
	 */
	public  Object createCollectionProxy(Session session, CollectionConverter collectionConverter, Node parentNode,  CollectionDescriptor collectionDescriptor, Class collectionFieldClass)
	{

		if (collectionConverter.isNull(session, parentNode, collectionDescriptor, collectionFieldClass)) 	{
			return null;
		}

		//ManageableObjects manageableCollection = ManageableObjectsUtil.getManageableObjects(collectionFieldClass);

		LazyLoader loader = new CollectionLazyLoader(collectionConverter, session, parentNode, collectionDescriptor, collectionFieldClass);
		return  Enhancer.create(collectionFieldClass, loader);
	}
}
