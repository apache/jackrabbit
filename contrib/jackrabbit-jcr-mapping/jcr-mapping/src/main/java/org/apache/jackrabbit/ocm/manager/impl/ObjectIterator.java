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

package org.apache.jackrabbit.ocm.manager.impl;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;


/**
 * ObjectIterator is a wrapper class for JCR NodeIterator
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class ObjectIterator implements Iterator
{

	private NodeIterator nodeIterator;

	private Class objectClass;

	private Session session;

	private ObjectConverter objectConverter;

	
	/**
	 * Constructor 
	 * 
	 * @param iterator JCR node iterator 
	 * @param objectClass the object class used to instantiate the objects
	 * @param converter The object converter
	 * @param session the JCR session 
	 */
	public ObjectIterator(NodeIterator iterator, Class objectClass, ObjectConverter converter, Session session)
	{
		nodeIterator = iterator;
		this.objectClass = objectClass;
		objectConverter = converter;
		this.session = session;
	}

	/**
	 * 
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext()
	{
		return nodeIterator.hasNext();
	}

	/**
	 * 
	 * @see java.util.Iterator#next()
	 */
	public Object next() 
	{

		try
		{
			Node node = nodeIterator.nextNode();
			return objectConverter.getObject(session, node.getPath());
		}
		catch (Exception e)
		{
           return null;			
		}

	}

	/**
	 * 
	 * @see java.util.Iterator#remove()
	 */
	public void remove()
	{
		nodeIterator.remove();
	}

}
