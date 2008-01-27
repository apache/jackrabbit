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
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;


/**
 * ObjectIterator is a wrapper class for JCR NodeIterator, which returns
 * mapped objects. Note, though, that this iterator may not return the same
 * number of objects as the underlying node iterator as not all nodes may
 * successfully be mapped to objects. Any problems mapping nodes to objects are
 * logged at INFO level.
 * <p>
 * This Iterator implementation does not support removing elements, therefore
 * the {@link #remove()} method throws a <code>UnsupportOperationException</code>.
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class ObjectIterator implements Iterator
{

    private static final Log log = LogFactory.getLog(ObjectIterator.class);

	private NodeIterator nodeIterator;

	private Session session;

	private ObjectConverter objectConverter;

    private Object nextResult;
	
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
		
		// get first result
        seek();
	}


	public boolean hasNext() {
        return nextResult != null;
    }

	
    public Object next() {
        if (nextResult == null) {
            throw new NoSuchElementException();
        }

        Object result = nextResult;
        seek();
        return result;
    }


    public void remove() {
        throw new UnsupportedOperationException();
    }


    private void seek() {
        while (nodeIterator.hasNext()) {
            try {
                Node node = nodeIterator.nextNode();
                Object value = objectConverter.getObject(session, node.getPath());
                if (value != null) {
                    nextResult = value;
                    return;
                }
            } catch (RepositoryException re) {
                log.info("Repository access issue trying to map node to an object", re);
            } catch (ObjectContentManagerException ocme) {
                log.info("Mapping Failure", ocme);
            } catch (Throwable t) {
                log.info("Unexpected Problem while trying to map a node to an object", t);
            }
        }

        // no more results
        nextResult = null;
    }
}
