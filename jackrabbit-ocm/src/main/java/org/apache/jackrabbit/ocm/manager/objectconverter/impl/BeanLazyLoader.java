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

import javax.jcr.Session;

import net.sf.cglib.proxy.LazyLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;

public class BeanLazyLoader implements LazyLoader
{

	 private final static Log log = LogFactory.getLog(BeanLazyLoader.class);
	
	 private ObjectConverter objectConverter;
	 private Session session;
	 private Class beanClass;
	 private String path;
	
	
	public BeanLazyLoader(ObjectConverter objectConverter, Session session, Class beanClass, String path)
	{
	     this.objectConverter = objectConverter;
	     this.session = session;
	     this.beanClass = beanClass;
	     this.path = path;
	}

	public Object loadObject()
	{				
		return objectConverter.getObject(session, beanClass, path);		
	}
}
