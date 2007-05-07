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
package org.apache.portals.graffito.jcr.persistence.collectionconverter;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.portals.graffito.jcr.exception.PersistenceException;
import org.apache.portals.graffito.jcr.mapper.model.CollectionDescriptor;

/**
 * Convert any kind of {@link ManageableCollection} into severals JCR nodes.
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * 
 */
public interface CollectionConverter
{

	/**
	 * Insert/convert collection elements into some JCR nodes
	 * @param session The JCR session 
	 * @param parentNode the node which will contains the collection element
	 * @param collectionDescriptor The collection descriptor
	 * @param collection the collection to insert
	 * 
	 * @throws PersistenceException when it is not possible to insert the collection
	 * 
	 */
	public void insertCollection(Session session, Node parentNode, 
			                     CollectionDescriptor collectionDescriptor, ManageableCollection collection) throws PersistenceException;

	/**
	 * Update collection elements already present in the JCR repository
	 * @param session The JCR session 
	 * @param parentNode the node which will contains the collection element
	 * @param collectionDescriptor The collection descriptor
	 * @param collection the collection to update
	 * 
	 * @throws PersistenceException when it is not possible to update the collection
	 */
	public void updateCollection(Session session, Node parentNode, 
			                     CollectionDescriptor collectionDescriptor, ManageableCollection collection) throws PersistenceException;

	/**
	 * Get a {@link ManageableCollection} from the JCR repository 
	 * @param session The JCR session 
	 * @param parentNode the node which contains the collection element
	 * @param collectionDescriptor The collection descriptor
	 * @param collectionFieldClass The collection class to used (ArrayList, Vector, ..) 
	 * @return The collection populates with all elements found in the JCR repository
	 * 
	 * @throws PersistenceException when it is not possible to retrieve the collection 
	 */
	public ManageableCollection getCollection(Session session, Node parentNode, 
			                                  CollectionDescriptor collectionDescriptor, Class collectionFieldClass) throws PersistenceException;

	
	/**
	 * Check if the collection is null. This method is mainly used in the Proxy manager to return a null value or a proxy object
	 * Without proxy proxy, this method is never called. 
	 * 
	 * @param session The JCR session 
	 * @param parentNode the node which contains the collection element
	 * @param collectionDescriptor The collection descriptor
	 * @param collectionFieldClass The collection class to used (ArrayList, Vector, ..) 
	 * @return true if the collection contains elements.
	 * 
	 * 
	 * @throws PersistenceException when it is not possible to retrieve the collection 
	 */	
	public boolean isNull(Session session, Node parentNode, 
                                      CollectionDescriptor collectionDescriptor, Class collectionFieldClass) throws PersistenceException;
}
