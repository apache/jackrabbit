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
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.beanconverter.BeanConverter;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;

public class BeanLazyLoader extends AbstractLazyLoader {

	private BeanConverter beanConverter;
	private Session session;
	private Node parentNode;
	private BeanDescriptor beanDescriptor;
	private ClassDescriptor beanClassDescriptor;
	private Class<?> beanClass;
	private Object parent;

	public BeanLazyLoader(BeanConverter beanConverter, Session session, Node parentNode, BeanDescriptor beanDescriptor,
			ClassDescriptor beanClassDescriptor, Class<?> beanClass, Object parent) {
		this.beanConverter = beanConverter;
		this.session = session;
		this.parentNode = parentNode;
		this.beanDescriptor = beanDescriptor;
		this.beanClassDescriptor = beanClassDescriptor;
		this.beanClass = beanClass;
		this.parent = parent;
	}

	@Override
	protected Object fetchTarget() {
		if (isInitialized()) {
			throw new IllegalStateException("Proxy already initialized");
		}

		Object target = beanConverter.getObject(session, parentNode, beanDescriptor, beanClassDescriptor, beanClass, parent);

		clean();
		return target;
	}

	private void clean() {
		 beanConverter = null;
		 session = null;
		 parentNode = null;
		 beanDescriptor = null;
		 parent = null;
	}
}
