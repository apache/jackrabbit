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
package org.apache.jackrabbit.ocm.manager;

import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Session;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.ocm.exception.IllegalUnlockException;
import org.apache.jackrabbit.ocm.exception.LockedException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.lock.Lock;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.version.Version;
import org.apache.jackrabbit.ocm.version.VersionIterator;

/**
 * The object content manager encapsulates a JCR session. 
 * This is the main component used to manage objects into the JCR repository.
 * 
 * @author Sandro Boehme 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * 
 */
public interface ObjectContentManager
{
    
    /**
     * Check if an object exists
     * @param path the object path 
     * @return true if the item exists
     * @throws ObjectContentManagerException when it is not possible to check if the item exist
     */
    public boolean objectExists(String path) throws ObjectContentManagerException;

    
    /**
     * Can this object content manager insert, update, delete, ... that type?
     * 
     * @param clazz class for question
     * @return <code>true</code> if the class is persistence
     */
     boolean isPersistent(Class clazz);
     
     
    /**
     * Insert an object into the JCR repository
     * 
     * @param object the object to add    
     * @throws ObjectContentManagerException when it is not possible to insert the object 
     */
    public void insert(Object object) throws ObjectContentManagerException;

    /**
     * Update an object 
     *
     * @param object the object to update 
     * @throws ObjectContentManagerException when it is not possible to update the object
     */
    public void update(Object object) throws ObjectContentManagerException;

    /**
     * Get an object from the JCR repository 
     * @param path the object path
     * @return the object found or null
     * 
     * @throws ObjectContentManagerException when it is not possible to retrieve the object 
     */
    public Object getObject( String path) throws ObjectContentManagerException;

    /**
     * Get an object from the JCR repository 
     * @param the object uuid
     * @return the object found or null
     * 
     * @throws ObjectContentManagerException when it is not possible to retrieve the object 
     */
    public Object getObjectByUuid( String uuid) throws ObjectContentManagerException;

    /**
     * Get an object from the JCR repository 
     * @param path the object path
     * @param versionNumber The desired object version number
     * @return the object found or null
     * 
     * @throws ObjectContentManagerException when it is not possible to retrieve the object 
     */
    public Object getObject(String path, String versionNumber) throws ObjectContentManagerException;
    
    /**
     * Get an object from the JCR repository 
     * @param objectClass the object class
     * @param path the object path
     * @return the object found or null
     * 
     * @throws ObjectContentManagerException when it is not possible to retrieve the object 
     */
    public Object getObject(Class objectClass, String path) throws ObjectContentManagerException;

    /**
     * Get an object from the JCR repository 
     * @param objectClass the object class
     * @param path the object path
     * @param versionNumber The desired object version number
     * @return the object found or null
     * 
     * @throws ObjectContentManagerException when it is not possible to retrieve the object 
     */
     public Object getObject(Class objectClass, String path, String versionNumber) throws ObjectContentManagerException;
    
    
    /**
     * Retrieve the specified attribute  for the given persistent object.
     * this attribute is either a bean or a collection. This method is usefull if the corresponding descriptor has an autoRetrieve="false"
     * 
     * @param object The persistent object
     * @param attributeName The name of the attribute to retrieve
     */
    public void retrieveMappedAttribute(Object object, String attributeName);

    
    /**
     * Retrieve all mapped  attributes for the given persistent object.
     * @param object The persistent object
     */
    public void retrieveAllMappedAttributes(Object object);
    
    
    /**
     * Remove an object from a JCR repository
     * @param path the object path
     * @throws ObjectContentManagerException when it is not possible to remove the object 
     * 
     */
    public void remove(String path) throws ObjectContentManagerException;
    
    
    /**
     * Remove an object from a JCR repository
     * @param object the object to remove
     * @throws ObjectContentManagerException when it is not possible to remove the object 
     * 
     */
    public void remove(Object object) throws ObjectContentManagerException;
    
    /**
     * Remove all objects matching to a query
     * @param query The query used to find the objects to remove
     * @throws ObjectContentManagerException when it is not possible to remove all objects 
     * 
     */
    public void remove(Query query) throws ObjectContentManagerException;

    
    /**
     * Retrieve an object matching to a query     
     * @param query The Query object used to seach the object
     * @return The object found or null
     * @throws ObjectContentManagerException when it is not possible to retrieve the object 
     * 
     */
    public Object getObject(Query query) throws ObjectContentManagerException;

    
    /**
     * Retrieve some objects matching to a query     
     * @param query The query used to seach the objects
     * @return a collection of objects found
     * @throws ObjectContentManagerException when it is not possible to retrieve the objects 
     * 
     */
    public Collection getObjects(Query query) throws ObjectContentManagerException;
    
    
    /**
     * Retrieve some objects matching to a query. 
     *  
     * @param query The query used to seach the objects
     * @return an iterator of objects found
     * @throws ObjectContentManagerException when it is not possible to retrieve the objects 
     */
    public Iterator getObjectIterator (Query query) throws ObjectContentManagerException;
     
    
    /**
     * Checkout - Create a new version
     * This is only possible if the object is based on mix:versionable node type
     *  
     * @param path The object path
     * @throws VersionException when it is not possible to create a new version 
     */
    public void checkout(String path) throws VersionException;
    
    /**
     * Checkin an object
     * @param path the object path 
     * @throws VersionException when it is not possible to checkin
     */
    public void checkin(String path) throws VersionException;
    
    /**
     * Checkin an object and apply some labels to this new version 
     * Within a particular object path, a given label may appear a maximum of once
     * @param path The object path 
     * @param versionLabels the version labels to apply to the new version 
     * @throws VersionException when it is possible to checkin
     */
    public void checkin(String path, String[] versionLabels) throws VersionException;
    
    
    /**
     * Get all version labels assigned to a particular object version 
     * @param path the object path
     * @param versionName the object version name (1.0, ...) 
     * @return a array of string (version labels) 
     * @throws VersionException when it is not to get all version labels
     */
    public String[] getVersionLabels(String path, String versionName) throws VersionException;
    
    
    /**
     * Get all version labels assigned to all versions 
     * @param path the object path     
     * @return a array of string (version labels) 
     * @throws VersionException when it is not to get all version labels
     */
    public String[] getAllVersionLabels(String path) throws VersionException;    
    
    /**
     * Add  a new label to a particular version  
     * @param path the object path
     * @param versionName the object versio name (1.0, 1.1, ...) 
     * @param versionLabel The new label to apply
     * @throws VersionException when it is not possible to add a new version label to this version
     */
    public void addVersionLabel(String path, String versionName, String versionLabel) throws VersionException;   
    
        
    /**
     * Get all object versions 
     * @param path the object path
     * @return a version iterator
     * @throws VersionException when it is not possible to retrieve all versions
     */
    public VersionIterator getAllVersions(String path) throws VersionException;
    
    /**
     * Get the first object version 
     * @param path the object path
     * @return the first version found
     * @throws VersionException when it is not possible to get the root version 
     */
    public Version getRootVersion(String path) throws VersionException;
    
    /**
     * Get the lastest object version 
     * @param path the object path
     * @return the last version found 
     * @throws VersionException when it is not possible to get the last version 
     */
    public Version getBaseVersion(String path) throws VersionException;
    /**
     * Get a particular version
     * @param path the object path
     * @param versionName the version name
     * @return the version found or null 
     * @throws VersionException when it is not possible to retrieve this particular version 
     */
    public Version getVersion(String path, String versionName) throws VersionException;
    

    /**
     * Save all modifications made by the object content manager
     *
     * @throws ObjectContentManagerException when it is not possible to save all pending operation into the JCR repo 
     */
    public void save() throws ObjectContentManagerException;  
    
    /**
     * Close the session    
     * @throws ObjectContentManagerException when it is not possible to logout
     */
    public void logout() throws ObjectContentManagerException;
    
    /**
     * Lock object saved on {@param path }.
     * 
     * @param path
     *            path to saved object.
     * @param isDeep
     *            is lock deep? See JCR spec: 8.4.3 Shallow and Deep Locks
     * @param isSessionScoped
     *            is lock session scoped? See JCR spec: Session-scoped and Open-scoped Locks
     * @return lock - Wrapper object for a JCR lock
     * 
     * @throws LockedException
     *             if path is locked (cannot lock same path again)
     */
    public Lock lock(String path, boolean isDeep, boolean isSessionScoped) throws LockedException;
    
    /**
     * Unlock object stored on {@param path }.
     *  
     * @param path path to stored object
     * 
     * 
     * @param lockToken
     *            see JCR spec: 8.4.6 Lock Token; can be <code>null</code>
     * 
     * @throws IllegalUnlockException
     *             throws if the current operation does not own the current lock
     */
    public void unlock(String path, String lockToken) throws IllegalUnlockException;
    
    /**
     * Is that path locked?
     * 
     * @param absPath
     * @return <code>true</code> if path locked
     */
    public boolean isLocked(String absPath);
    
    /**
     * 
     * @return The query manager reference
     */
    public QueryManager getQueryManager();
    
    /**
     * Refresh the underlying jcr session (see the jcr spec)
     * @param keepChanges
     */
    public void refresh(boolean keepChanges);
    
    /**
     *  Move an object
     *   
     * @param srcPath path of the object to move
     * @param destPath destination path
     * 
     * @throws ObjectContentManagerException
     */
    public void move(String srcPath, String destPath) throws ObjectContentManagerException;
    
    /**
     * Copy an object 
     * 
     * @param srcPath path of the object to copy
     * @param destPath destination path
     * 
     * @throws ObjectContentManagerException
     */
    public void copy(String srcPath, String destPath) throws ObjectContentManagerException; 
        
    /**
     * This method returns the JCR session. The JCR session could be used to make some JCR specific calls.
     * @return the associated JCR session 
     */
    public Session getSession();
}
