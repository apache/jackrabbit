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

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;


/**
 * ObjectIterator is a wrapper class for JCR NodeIterator, which returns
 * mapped objects.
 * <p>
 * This Iterator implementation does not support removing elements, therefore
 * the {@link #remove()} method throws a <code>UnsupportOperationException</code>.
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class ObjectIterator implements RangeIterator
{
	private NodeIterator nodeIterator;

	private Session session;

	private ObjectConverter objectConverter;

	/**
	 * Constructor
	 *
	 * @param iterator JCR node iterator
	 * @param converter The object converter
	 * @param session the JCR session
	 */
	public ObjectIterator(NodeIterator iterator, ObjectConverter converter, Session session)
	{
		nodeIterator = iterator;
		objectConverter = converter;
		this.session = session;
	}

    /**
     * @see java.util.Iterator#hasNext()
     */
	public boolean hasNext() {
        return nodeIterator.hasNext();
    }

	
    /**
     * @see java.util.Iterator#next() 
     */
    public Object next() {
        try {
            Node node = nodeIterator.nextNode();
            return objectConverter.getObject(session, node.getPath());
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Repository access issue trying to map node to an object", re);
        }
    }

    /**
     * This Iterator implementation does not support removing elements, therefore
     * this method always throws a <code>UnsupportOperationException</code>.
     *
     * @see java.util.Iterator#next()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }


    /**
     * @see javax.jcr.RangeIterator#skip(long)
     */
    public void skip(long l) {
        nodeIterator.skip(l);
    }

    /**
     * @see javax.jcr.RangeIterator#getSize()
     */
    public long getSize() {
        return nodeIterator.getSize();
    }

    /**
     * @see javax.jcr.RangeIterator#getPosition()
     */
    public long getPosition() {
        return nodeIterator.getPosition();
    }
}
