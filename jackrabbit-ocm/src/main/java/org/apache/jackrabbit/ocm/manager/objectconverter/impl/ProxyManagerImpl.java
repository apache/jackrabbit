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

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.ocm.manager.beanconverter.BeanConverter;
import org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;

public class ProxyManagerImpl implements ProxyManager {

	/**
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager#createBeanProxy(javax.jcr.Session,
	 *      org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter,
	 *      java.lang.Class, java.lang.String)
	 */
	public Object createBeanProxy(BeanConverter beanConverter, String path, Session session, Node parentNode,
			BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Class beanClass, Object parent) {
		try {
			if (path == null || !session.itemExists(path)) {
				return null;
			}
		} catch (RepositoryException e) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to check,if the object exits on " + path, e);
		}

		Callback loader = new BeanLazyLoader(beanConverter, session, parentNode, beanDescriptor, beanClassDescriptor, beanClass, parent);
		return Enhancer.create(beanClass, getInterfaces(beanClass), loader);
	}

	/**
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager#createCollectionProxy(javax.jcr.Session,
	 *      org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter,
	 *      javax.jcr.Node,
	 *      org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor,
	 *      java.lang.Class)
	 */
	public Object createCollectionProxy(Session session, CollectionConverter collectionConverter, Node parentNode,
			CollectionDescriptor collectionDescriptor, Class collectionFieldClass) {

		if (collectionConverter.isNull(session, parentNode, collectionDescriptor, collectionFieldClass)) {
			return null;
		}

		Callback loader = new CollectionLazyLoader(collectionConverter, session, parentNode, collectionDescriptor, collectionFieldClass);
		return Enhancer.create(collectionFieldClass, getInterfaces(collectionFieldClass), loader);
	}
	
	private Class<?>[] getInterfaces(Class<?> collectionFieldClass) {
		
		Class<?>[] interfaces = null;
		if (collectionFieldClass.isInterface()) {
			// if collectionFieldClass is an interface, simply use it
			interfaces = new Class<?>[] { collectionFieldClass };
		} else {
			// else, use all interfaces
			interfaces = collectionFieldClass.getInterfaces();
		}
		return (Class<?>[]) ArrayUtils.add(interfaces, OcmProxy.class);
	}
}
