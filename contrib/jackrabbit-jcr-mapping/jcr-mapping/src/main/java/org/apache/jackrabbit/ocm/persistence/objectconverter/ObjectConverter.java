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
package org.apache.jackrabbit.ocm.persistence.objectconverter;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.exception.PersistenceException;


/**
 * Convert any kind of beans into JCR nodes & properties
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @version $Id: Exp $
 */
public interface ObjectConverter
{
	/**
	 * Insert the object 
	 * 
	 * @param session the JCR session  
	 * @param object the object to insert
	 * @throws PersistenceException when it is not possible to insert the object
	 * 
	 */
    public void insert(Session session, Object object) throws PersistenceException;
    
	/**
	 * Update the object 
	 * 
	 * @param session the JCR session 
	 * @param object the object to update
	 * @throws PersistenceException when it is not possible to update the object
	 */    
    public void update(Session session, Object object) throws PersistenceException;
    
    /**
     * Retrieve an object from the JCR repo
     * 
     * @param session The JCR session 
     * @param clazz The class assigned to the object to retrieve
     * @param path the JCR path
     * @return The object found or null
     * 
     * @throws PersistenceException when it is not possible to retrieve the object
     */
    public Object getObject(Session session, String path) throws PersistenceException;
    
    /**
     * Retrieve an object from the JCR repo
     * 
     * @param session The JCR session 
     * @param clazz The class assigned to the object to retrieve
     * @param path the JCR path
     * @return The object found or null
     * 
     * @throws PersistenceException when it is not possible to retrieve the object
     */
    public Object getObject(Session session, Class clazz, String path) throws PersistenceException;
    
    
    /**
     * Retrieve the specified attribute  for the given persistent object.
     * this attribute is either a bean or a collection. This method is usefull if the corresponding descriptor has an autoRetrieve="false"
     * 
     * @param session The JCR session
     * @param object The persistent object
     * @param attributeName The name of the attribute to retrieve
     */
    public void retrieveMappedAttribute(Session session, Object object, String attributeName);

    
    /**
     * Retrieve all mapped  attributes for the given persistent object.
     * 
     * @param session The JCR session     
     * @param object The persistent object
     */
    public void retrieveAllMappedAttributes(Session session, Object object);
    
	/**
	 * Insert the object 
	 * 
	 * @param session the JCR session 
	 * @param parentNode The parent node used to store the new JCR element (object) 
	 * @param nodeName The node name used to store the object
	 * @param object the object to insert
	 * @throws PersistenceException when it is not possible to insert the object
	 */
    public void insert(Session session, Node parentNode, String nodeName, Object object) throws PersistenceException;
    
	/**
	 * Update the object 
	 * 
	 * @param session the JCR session 
	 * @param parentNode The parent node used to store the new JCR element (object) 
	 * @param nodeName The node name used to store the object
	 * @param object the object to update
	 * @throws PersistenceException when it is not possible to update the object
	 */    
    public void update(Session session, Node parentNode, String nodeName, Object object) throws PersistenceException;
    
   
    /**
     * Get the object JCR path 
     * 
     * @param session the JCR session 
     * @param object the object for which the path has to be retrieve 
     * @return the object JCR path 
     * @throws PersistenceException when it is not possible to retrieve the object path
     */
    public String getPath(Session session , Object object)  throws PersistenceException;

}
