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

package org.apache.jackrabbit.ocm.persistence.objectconverter.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.CollectionConverter;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollectionUtil;
import org.apache.jackrabbit.ocm.persistence.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.persistence.objectconverter.ProxyManager;

public class ProxyManagerImpl implements ProxyManager 
{

	private final static Log log = LogFactory.getLog(ProxyManagerImpl.class);
	
	 
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.persistence.objectconverter.impl.ProxyManager#createBeanProxy(javax.jcr.Session, org.apache.portals.graffito.jcr.persistence.objectconverter.ObjectConverter, java.lang.Class, java.lang.String)
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

	
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.persistence.objectconverter.impl.ProxyManager#createCollectionProxy(javax.jcr.Session, org.apache.portals.graffito.jcr.persistence.collectionconverter.CollectionConverter, javax.jcr.Node, org.apache.portals.graffito.jcr.mapper.model.CollectionDescriptor, java.lang.Class)
	 */
	public  Object createCollectionProxy(Session session, CollectionConverter collectionConverter, Node parentNode,  CollectionDescriptor collectionDescriptor, Class collectionFieldClass) 
	{	
		
		if (collectionConverter.isNull(session, parentNode, collectionDescriptor, collectionFieldClass)) 	{
			return null;
		}
		
		ManageableCollection manageableCollection = ManageableCollectionUtil.getManageableCollection(collectionFieldClass);
		
		LazyLoader loader = new CollectionLazyLoader(collectionConverter, session, parentNode, collectionDescriptor, collectionFieldClass);
		return  Enhancer.create(manageableCollection.getClass(), loader);
	}	
}
