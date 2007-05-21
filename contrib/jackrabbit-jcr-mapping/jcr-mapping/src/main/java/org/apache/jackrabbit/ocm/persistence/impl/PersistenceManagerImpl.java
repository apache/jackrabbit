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
package org.apache.jackrabbit.ocm.persistence.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionHistory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.IllegalUnlockException;
import org.apache.jackrabbit.ocm.exception.IncorrectPersistentClassException;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.LockedException;
import org.apache.jackrabbit.ocm.exception.PersistenceException;
import org.apache.jackrabbit.ocm.exception.VersionException;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.DigesterMapperImpl;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.persistence.PersistenceManager;
import org.apache.jackrabbit.ocm.persistence.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.persistence.cache.ObjectCache;
import org.apache.jackrabbit.ocm.persistence.cache.impl.RequestObjectCacheImpl;
import org.apache.jackrabbit.ocm.persistence.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.persistence.objectconverter.impl.ObjectConverterImpl;
import org.apache.jackrabbit.ocm.persistence.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.impl.QueryManagerImpl;
import org.apache.jackrabbit.ocm.version.Version;
import org.apache.jackrabbit.ocm.version.VersionIterator;

/**
 *
 * Default implementation for {@link org.apache.jackrabbit.ocm.persistence.PersistenceManager}
 *
 * @author Sandro Boehme
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe</a>
 * @author Martin Koci
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class PersistenceManagerImpl implements PersistenceManager {
    /**
     * Logger.
     */
    private final static Log log = LogFactory.getLog(PersistenceManagerImpl.class);

    /**
     * JCR session.
     */
    protected Session session;

    protected Mapper mapper;

    /**
     * The query manager
     */
    protected QueryManager queryManager;

    /**
     * Object Converter
     */
    protected ObjectConverter objectConverter;
    
    /**
     * Request Cache manager
     */
    protected ObjectCache requestObjectCache; 

    /**
     * Creates a new <code>PersistenceManager</code> that uses the passed in
     * <code>Mapper</code>, <code>QueryManager</code> and a default 
     * <code>ObjectConverter</code>
     *
     * @param mapper the Mapper component
     * @param queryManager the query manager to used
     * @param session The JCR session
     */
    public PersistenceManagerImpl(Mapper mapper,
                                  QueryManager queryManager,
                                  Session session) {
        this.mapper = mapper;
        this.session = session;
        this.requestObjectCache = new RequestObjectCacheImpl();        
        this.objectConverter = new ObjectConverterImpl(mapper, new DefaultAtomicTypeConverterProvider(), new ProxyManagerImpl(), requestObjectCache);
        this.queryManager = queryManager;

    }

    /**
     * Creates a new <code>PersistenceManager</code> based on a JCR session and some xml mapping files. 
     *
     * @param session The JCR session
     * @param xmlMappingFiles the JCR mapping files used mainly to create the <code>Mapper</code> component
     */
    public PersistenceManagerImpl(Session session,String[] xmlMappingFiles ) 
    {
        this.session = session;
		this.mapper = new DigesterMapperImpl(xmlMappingFiles);
		DefaultAtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
        Map atomicTypeConverters = converterProvider.getAtomicTypeConverters();
		this.queryManager = new QueryManagerImpl(mapper, atomicTypeConverters);
        this.requestObjectCache = new RequestObjectCacheImpl();        
        this.objectConverter = new ObjectConverterImpl(mapper, converterProvider, new ProxyManagerImpl(), requestObjectCache);
        
    }
    
    
    /**
     * Full constructor.
     * 
     * @param mapper the Mapper component
     * @param converter the <code>ObjectConverter</code> to be used internally
     * @param queryManager the query manager to used
     * @param session The JCR session
     */
    public PersistenceManagerImpl(Mapper mapper,
                                  ObjectConverter converter,
                                  QueryManager queryManager,
                                  ObjectCache requestObjectCache,
                                  Session session) {
        this.mapper = mapper;
        this.session = session;
        this.objectConverter = converter;
        this.queryManager = queryManager;
        this.requestObjectCache = requestObjectCache;
    }
    
    /**
     * Sets the <code>Mapper</code> used by this persistence manager.
     * 
     * @param mapper mapping solver
     */
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }
    
    /**
     * Sets the <code>ObjectConverter</code> that is used internally by this persistence manager.
     * 
     * @param objectConverter the internal <code>ObjectConverter</code>
     */
    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }
    
    /**
     * Sets the <code>QueryManager</code> used by the persistence manager.
     * 
     * @param queryManager a <code>QueryManager</code>
     */
    public void setQueryManager(QueryManager queryManager) {
        this.queryManager= queryManager;
    }
    
    
    
    public void setRequestObjectCache(ObjectCache requestObjectCache) {
		this.requestObjectCache = requestObjectCache;
	}

	/**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObject(java.lang.Class, java.lang.String)
     * @throws org.apache.jackrabbit.ocm.exception.RepositoryException if the underlying repository
     *  has thrown a javax.jcr.RepositoryException
     * @throws JcrMappingException if the mapping for the class is not correct
     * @throws PersistenceException if the object cannot be retrieved from the path
     */
    public Object getObject( String path) {
        try {
            if (!session.itemExists(path)) {
                return null;
            }
        }         
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object at " + path, e);
        }

        Object object =  objectConverter.getObject(session,  path);
        requestObjectCache.clear();
        return object; 

    }

    
    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObject(java.lang.Class, java.lang.String)
     * @throws org.apache.jackrabbit.ocm.exception.RepositoryException if the underlying repository
     *  has thrown a javax.jcr.RepositoryException
     * @throws JcrMappingException if the mapping for the class is not correct
     * @throws PersistenceException if the object cannot be retrieved from the path
     */
    public Object getObjectByUuid( String uuid) {
        
    	try 
        {
             Node node = session.getNodeByUUID(uuid);
             Object object = objectConverter.getObject(session,  node.getPath());
             requestObjectCache.clear();
             return object; 

        }         
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object with uuid : " + uuid, e);
        }

        

    }    
    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObject(java.lang.Class, java.lang.String, java.lang.String)
     */
    public Object getObject( String path, String versionName) {
        String pathVersion = null;
        try {
            if (!session.itemExists(path)) {
                return null;
            }

            Version version = this.getVersion(path, versionName);
            pathVersion = version.getPath() + "/jcr:frozenNode";

        } 
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object at " + path + " - version :" + versionName,
                    e);
        }

        Object object = objectConverter.getObject(session,  pathVersion);
        requestObjectCache.clear();
        return object;
    }

    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObject(java.lang.Class, java.lang.String)
     * @throws org.apache.jackrabbit.ocm.exception.RepositoryException if the underlying repository
     *  has thrown a javax.jcr.RepositoryException
     * @throws JcrMappingException if the mapping for the class is not correct
     * @throws PersistenceException if the object cannot be retrieved from the path
     */
    public Object getObject(Class objectClass, String path) {
        try {
            if (!session.itemExists(path)) {
                return null;
            }
        }         
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object at " + path, e);
        }

        Object object = objectConverter.getObject(session, objectClass, path);
        requestObjectCache.clear();
        return object; 

    }

    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObject(java.lang.Class, java.lang.String, java.lang.String)
     */
    public Object getObject(Class objectClass, String path, String versionName) {
        String pathVersion = null;
        try {
            if (!session.itemExists(path)) {
                return null;
            }

            Version version = this.getVersion(path, versionName);
            pathVersion = version.getPath() + "/jcr:frozenNode";

        } 
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object at " + path + " - version :" + versionName,
                    e);
        }

        Object object = objectConverter.getObject(session, objectClass, pathVersion);
        requestObjectCache.clear();
        return object;
    }    
    
    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#retrieveAllMappedAttributes(Object)
     */
    public void retrieveAllMappedAttributes(Object object) {
		objectConverter.retrieveAllMappedAttributes(session, object);
      
	}

    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#retrieveMappedAttribute(Object, String)
     */    
	public void retrieveMappedAttribute(Object object, String attributeName) {
		objectConverter.retrieveMappedAttribute(session, object, attributeName);
		
	}

	/**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#insert(java.lang.Object)
     */
    public void insert(Object object) {
        String path = objectConverter.getPath(session, object);

        try {
            if (session.itemExists(path)) {
                Item item = session.getItem(path);
                if (item.isNode()) {
                    if (!((Node) item).getDefinition().allowsSameNameSiblings()) {
                        throw new PersistenceException("Path already exists and it is not supporting the same name sibling : "
                                                       + path);
                    }
                } 
                else {
                    throw new PersistenceException("Path already exists and it is a property : "
                                                   + path);
                }

            }
        } 
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to insert the object at " + path, e);
        }

        objectConverter.insert(session, object);
    }

    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#update(java.lang.Object)
     */
    public void update(Object object) {
        String path = objectConverter.getPath(session, object);
        try {
            if (!session.itemExists(path)) {
                throw new PersistenceException("Path is not existing : " + path);
            } 
            else {
                checkIfNodeLocked(path);
            }
        } 
        catch(javax.jcr.RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to update", e);
        }

        objectConverter.update(session, object);
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#remove(java.lang.String)
     */
    public void remove(String path) {
        try {
            if (!session.itemExists(path)) {
                throw new PersistenceException("Path does not exist : " + path);
            } 
            else {
                checkIfNodeLocked(path);
            }

            Item item = session.getItem(path);
            item.remove();

        } 
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to remove the object at " + path);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#remove(java.lang.Object)
     */
    public void remove(Object object) {
        this.remove(objectConverter.getPath(session, object));
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#remove(org.apache.jackrabbit.ocm.query.Query)
     */
    public void remove(Query query) {
        try {
            String jcrExpression = this.queryManager.buildJCRExpression(query);
            log.debug("Remove Objects with expression : " + jcrExpression);

            javax.jcr.query.Query jcrQuery = session.getWorkspace().getQueryManager()
                .createQuery(jcrExpression, javax.jcr.query.Query.XPATH);
            
            QueryResult queryResult = jcrQuery.execute();
            NodeIterator nodeIterator = queryResult.getNodes();
            List nodes = new ArrayList();

            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                log.debug("Remove node : " + node.getPath());

                // it is not possible to remove nodes from an NodeIterator
                // So, we add the node found in a collection to remove them after
                nodes.add(node);
            }

            // Remove all collection nodes
            for (int i = 0; i < nodes.size(); i++) {
                Node node = (Node) nodes.get(i);
                checkIfNodeLocked(node.getPath());
                try {
                    node.remove();
                }
                catch(javax.jcr.RepositoryException re) {
                    throw new PersistenceException("Cannot remove node at path " 
                            + node.getPath() + " returned from query "
                            + jcrExpression,
                            re);
                }
            }

        } 
        catch(InvalidQueryException iqe) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Invalid query expression", iqe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object collection", e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#objectExists(java.lang.String)
     */
    public boolean objectExists(String path) {
        try {
            //TODO : Check also if it is an object
            return session.itemExists(path);
        } 
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to check if the object exist", e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#isPersistent(java.lang.Class)
     */
    public boolean isPersistent(final Class clazz) {
        
        try 
        {
    	    ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(clazz);
    	    return true;
        }
        catch(IncorrectPersistentClassException e)
        {
        	return false;
        }

    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObject(org.apache.jackrabbit.ocm.query.Query)
     */
    public Object getObject(Query query) {
        try {
            String jcrExpression = this.queryManager.buildJCRExpression(query);
            log.debug("Get Object with expression : " + jcrExpression);

            javax.jcr.query.Query jcrQuery = session.getWorkspace().getQueryManager().createQuery(
                    jcrExpression, javax.jcr.query.Query.XPATH);
            QueryResult queryResult = jcrQuery.execute();
            NodeIterator nodeIterator = queryResult.getNodes();

            if (nodeIterator.getSize() > 1) {
                throw new PersistenceException("Impossible to get the object - the query returns more than one object");
            }

            Object object = null;
            if (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                object = objectConverter.getObject(session, node.getPath());
            }
            requestObjectCache.clear();
            return object;
        } 
        catch(InvalidQueryException iqe) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Invalid query expression", iqe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object collection", e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObjects(org.apache.jackrabbit.ocm.query.Query)
     */
    public Collection getObjects(Query query) {
        try {
            String jcrExpression = this.queryManager.buildJCRExpression(query);
            log.debug("Get Objects with expression : " + jcrExpression);

            javax.jcr.query.Query jcrQuery = session.getWorkspace().getQueryManager()
                .createQuery(jcrExpression, javax.jcr.query.Query.XPATH);
            QueryResult queryResult = jcrQuery.execute();
            NodeIterator nodeIterator = queryResult.getNodes();

            List result = new ArrayList();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                log.debug("Node found : " + node.getPath());
                result.add(objectConverter.getObject(session,  node.getPath()));
            }
            requestObjectCache.clear();
            return result;
        } 
        catch(InvalidQueryException iqe) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Invalid query expression", iqe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object collection", e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getObjectIterator(org.apache.jackrabbit.ocm.query.Query)
     */
    public Iterator getObjectIterator(Query query) {
        try {
            String jcrExpression = this.queryManager.buildJCRExpression(query);
            log.debug("Get Object with expression : " + jcrExpression);

            javax.jcr.query.Query jcrQuery = session.getWorkspace().getQueryManager().createQuery(jcrExpression, javax.jcr.query.Query.XPATH);
            QueryResult queryResult = jcrQuery.execute();
            NodeIterator nodeIterator = queryResult.getNodes();

            return new ObjectIterator(nodeIterator,
                                      query.getFilter().getFilterClass(),
                                      this.objectConverter,
                                      this.session);

        } 
        catch(InvalidQueryException iqe) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Invalid query expression", iqe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object collection", e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#checkin(java.lang.String)
     */
    public void checkin(String path) {
        this.checkin(path, null);
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#checkin(java.lang.String, java.lang.String[])
     */
    public void checkin(String path, String[] versionLabels) {
        try {
            Node node = (Node) session.getItem(path);
            checkIfNodeLocked(node.getPath());
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }
            javax.jcr.version.Version newVersion = node.checkin();

            if (versionLabels != null) {
                VersionHistory versionHistory = node.getVersionHistory();
                for (int i = 0; i < versionLabels.length; i++) {
                    versionHistory.addVersionLabel(newVersion.getName(),
                                                   versionLabels[i], false);
                }
            }
        } 
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(InvalidItemStateException iise) {
            throw new PersistenceException("Cannot checking modified object at path " + path, iise);
        }
        catch(javax.jcr.version.VersionException ve) {
            throw new VersionException("Impossible to checkin the object " + path, ve);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Cannot checkin unversionable node at path " + path, uroe);
        }
        catch(LockException le) {
            throw new VersionException("Cannot checkin locked node at path " + path, le);
        }
        catch (RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to checkin the object " + path, e);
        }

    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#checkout(java.lang.String)
     */
    public void checkout(String path) {
        Node node = null;
        try {
            node = (Node) session.getItem(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }

            node.checkout();
        }         
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Cannot checkout unversionable node at path " + path, uroe);
        }
        catch(LockException le) {
            throw new VersionException("Cannot checkout locked node at path " + path, le);
        }
        catch(javax.jcr.RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to checkout the object " + path, e);
        }

    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#addVersionLabel(java.lang.String, java.lang.String, java.lang.String)
     */
    public void addVersionLabel(String path, String versionName, String versionLabel) {
        try {
            Node node = (Node) session.getItem(path);
            checkIfNodeLocked(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }

            VersionHistory history = node.getVersionHistory();
            history.addVersionLabel(versionName, versionLabel, false);
        } 
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(javax.jcr.version.VersionException ve) {
            throw new VersionException("Impossible to add a new version label to  " + path
                    + " - version name : " + versionName,
                    ve);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Impossible to add a new version label to  " + path
                    + " - version name : " + versionName,
                    uroe);
        }
        catch(javax.jcr.RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getVersion(java.lang.String, java.lang.String)
     */
    public Version getVersion(String path, String versionName) {
        try {
            Node node = (Node) session.getItem(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }

            VersionHistory history = node.getVersionHistory();

            return new Version(history.getVersion(versionName));
        }
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(javax.jcr.version.VersionException ve) {
            throw new VersionException("The version name " + versionName + "does not exist", ve);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Impossible to retrieve versions for path " + path, uroe);
        }
        catch(javax.jcr.RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getVersionLabels(java.lang.String, java.lang.String)
     */
    public String[] getVersionLabels(String path, String versionName) {
        try {
            Node node = (Node) session.getItem(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }

            VersionHistory history = node.getVersionHistory();
            javax.jcr.version.Version version = history.getVersion(versionName);

            return history.getVersionLabels(version);
        } 
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(javax.jcr.version.VersionException ve) {
            throw new VersionException("Impossible to get the version labels : " + path
                    + " - version name : " + versionName,
                    ve);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Impossible to retrieve versions for path " + path, uroe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e);
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getAllVersionLabels(java.lang.String)
     */
    public String[] getAllVersionLabels(String path) {
        try {
            Node node = (Node) session.getItem(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }

            VersionHistory history = node.getVersionHistory();

            return history.getVersionLabels();
        } 
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Impossible to retrieve version history for path " + path, uroe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getAllVersions(java.lang.String)
     */
    public VersionIterator getAllVersions(String path) {
        try {
            Node node = (Node) session.getItem(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path
                                           + "is not versionable");
            }

            VersionHistory history = node.getVersionHistory();

            return new VersionIterator(history.getAllVersions());
        } 
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Impossible to retrieve version history for path " + path, uroe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getRootVersion(java.lang.String)
     */
    public Version getRootVersion(String path) {
        try {
            Node node = (Node) session.getItem(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }

            VersionHistory history = node.getVersionHistory();

            return new Version(history.getRootVersion());
        } 
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Impossible to get the root version  for the object " + path,
                                       uroe);
        }
        catch(RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getBaseVersion(java.lang.String)
     */
    public Version getBaseVersion(String path) {
        try {
            Node node = (Node) session.getItem(path);
            if (!node.isNodeType("mix:versionable")) {
                throw new VersionException("The object " + path + "is not versionable");
            }

            return new Version(node.getBaseVersion());
        } 
        catch(ClassCastException cce) {
            throw new PersistenceException("Cannot retrieve an object from a property path " + path);
        }
        catch(PathNotFoundException pnfe) {
            throw new PersistenceException("Cannot retrieve an object at path " + path, pnfe);
        }
        catch(UnsupportedRepositoryOperationException uroe) {
            throw new VersionException("Impossible to get the base version for the object " + path,
                                        uroe);
        }
        catch(javax.jcr.RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#lock(java.lang.String, java.lang.Object, boolean, boolean)
     */
    public String lock(final String absPath, final boolean isDeep, final boolean isSessionScoped) 
    throws LockedException {
        try {

            // Calling this method will throw exception if node is locked
            // and this operation cant be done (exception translation)
            checkIfNodeLocked(absPath);

            Node node = getNode(absPath);
            Lock lock = node.lock(isDeep, isSessionScoped);

            return lock.getLockToken();
        } 
        catch (LockException e) {
            // Only one case with LockException remains: if node is not mix:lockable, propably error in custom node types definitions
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Node of type is not type mix:lockable", e);
        } 
        catch (RepositoryException e) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e.getMessage(),
                                                                                    e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#unlock(java.lang.String, java.lang.Object, java.lang.String)
     */
    public void unlock(final String absPath, final String lockToken) 
    throws IllegalUnlockException {
        String lockOwner = null;
        try {
            maybeAddLockToken(lockToken);

            Node node = getNode(absPath);
            if (node.isLocked() == false) {
                // Safe - if not locked return
                return;
            }

            Lock lock = node.getLock();
            lockOwner = lock.getLockOwner();

            node.unlock();
        } 
        catch (LockException e) {
            // LockException if this node does not currently hold a lock (see upper code)
            // or holds a lock for which this Session does not have the correct lock token
            log.error("Cannot unlock path: "
                      + absPath
                      + " Jcr user: "
                      + session.getUserID()
                      + " has no lock token to do this. Lock was placed with user: "
                      + lockOwner);
            throw new IllegalUnlockException(lockOwner, absPath);
        } 
        catch (RepositoryException e) {
            // This also catch UnsupportedRepositoryOperationException - we assume that implementation supports it (jackrabbit does)
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(e.getMessage(),
                                                                                    e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#isLocked(java.lang.String)
     */
    public boolean isLocked(final String absPath) {
        try {
            final Node node = getNode(absPath);

            return node.isLocked();
        } 
        catch (RepositoryException e) {
            // node.isLocked() RepositoryException if an error occurs.
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "An exception was thrown while checking the lock at path : " + absPath, e);
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#getQueryManager()
     */
    public QueryManager getQueryManager() {
        return this.queryManager;
    }

    /**
     * Throws {@link LockedException} id node is locked so alter nopde cannot be done
     *
     * @param absPath
     *            abs path to node
     * @throws RepositoryException
     * @throws LockedException
     *             if node is locked
     */
    protected void checkIfNodeLocked(final String absPath) throws RepositoryException, LockedException {
        Node node = getNode(absPath);

        // Node can hold nock or can be locked with precedencor
        if (node.isLocked()) {
            Lock lock = node.getLock();
            String lockOwner = lock.getLockOwner();
            
            if (! session.getUserID().equals(lockOwner))
            {
                final String path = lock.getNode().getPath();
                throw new LockedException(lockOwner, path);
            }
        }
    }

    protected void maybeAddLockToken(final String lockToken) {
        if (lockToken != null) {
            // This user (this instance of PM) potentionally placed lock so
            // session already has lock token
            final String[] lockTokens = getSession().getLockTokens();
            if (lockTokens != null) {
                for(int i= 0; i < lockTokens.length; i++) {
                    if(lockTokens[i].equals(lockToken)) {
                        // we are already holding a lock
                        break;
                    }
                }
            } else {
                getSession().addLockToken(lockToken);
            }
        }
    }

    protected Node getNode(final String absPath) throws PathNotFoundException, RepositoryException {
        if (!getSession().itemExists(absPath)) {
            throw new PersistenceException("No object stored on path: " + absPath);
        }
        Item item = getSession().getItem(absPath);
        if (!item.isNode()) {
            throw new PersistenceException("No object stored on path: " + absPath
                                           + " on absPath is item (leaf)");
        }

        return (Node) item;
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#logout()
     */
    public void logout() {
        try {
            log.debug("Logout. Persisting current session changes.");
            this.session.save();
            this.session.logout();
            log.debug("Session closed");
        }
        catch(NoSuchNodeTypeException nsnte) {
            throw new JcrMappingException(
                    "Cannot persist current session changes. An unknown node type was used.", nsnte);
        }
        catch(javax.jcr.version.VersionException ve) {
            throw new VersionException(
                    "Cannot persist current session changes. Attempt to overwrite checked-in node", ve);
        }
        catch(LockException le) {
            throw new PersistenceException(
                    "Cannot persist current session changes. Violation of a lock detected", le);
        }
        catch(javax.jcr.RepositoryException e) {
            throw new PersistenceException(
                    "Cannot persist current session changes.", e);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#save()
     */
    public void save() {
        try {
            this.session.save();
        }
        catch(NoSuchNodeTypeException nsnte) {
            throw new JcrMappingException(
                    "Cannot persist current session changes. An unknown node type was used.", nsnte);
        }
        catch(javax.jcr.version.VersionException ve) {
            throw new VersionException(
                    "Cannot persist current session changes. Attempt to overwrite checked-in node", ve);
        }
        catch(LockException le) {
            throw new PersistenceException(
                    "Cannot persist current session changes. Violation of a lock detected", le);
        }
        catch(RepositoryException e) {
            throw new PersistenceException(
                    "Cannot persist current session changes.", e);
        }
    }

    /**
     * @return The JCR Session
     */
    public Session getSession() {       
    	return this.session;
    }

	public void refresh(boolean keepChanges) {
		try 
		{
		    session.refresh(keepChanges);
		}
        catch(RepositoryException e) {
            throw new PersistenceException("Cannot refresh current session ", e);
        }
	}
    
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#move(java.lang.String, java.lang.String)
	 */
    public void move(String srcPath, String destPath){
        //Workspace workspace = session.getWorkspace();
        try {
            
        	session.move(srcPath,destPath);
            
        }catch(javax.jcr.nodetype.ConstraintViolationException cve){
            throw new PersistenceException(
                    "Cannot move the object from " + srcPath + " to " + destPath + "." + " Violation of a nodetype or attempt to move under a property detected", cve);
            
        }catch(javax.jcr.version.VersionException ve){
            throw new VersionException(
                    "Cannot move the object from " + srcPath + " to " + destPath + "." + " Parent node of source or destination is versionable and checked in ", ve);
            
        }catch(javax.jcr.AccessDeniedException ade){
            throw new PersistenceException(
                    "Cannot move the object from " + srcPath + " to " + destPath + "." + " Session does not have access permissions", ade);
            
        }catch(javax.jcr.PathNotFoundException pnf){
            throw new PersistenceException(
                    "Cannot move the object from " + srcPath + " to " + destPath + "." + " Node at source or destination does not exist ", pnf);
            
        }catch(javax.jcr.ItemExistsException ie){
            throw new PersistenceException(
                    "Cannot move the object from " + srcPath + " to " + destPath + "." + " It might already exist at destination path.", ie);
            
        }catch(javax.jcr.lock.LockException le){
            throw new PersistenceException(
                    "Cannot move the object from " + srcPath + " to " + destPath + "." + "Violation of a lock detected", le);
            
        }catch(javax.jcr.RepositoryException re){
            throw new PersistenceException(
                    "Cannot move the object from " + srcPath + " to " + destPath + "." , re);
        }   
    }

    /**
     * 
     * @see org.apache.jackrabbit.ocm.persistence.PersistenceManager#copy(java.lang.String, java.lang.String)
     */
    public void copy(String srcPath, String destPath){
        Workspace workspace = session.getWorkspace();
        try{
            workspace.copy(srcPath,destPath);
            
        }catch(javax.jcr.nodetype.ConstraintViolationException cve){
            throw new PersistenceException(
                    "Cannot copy the object from " + srcPath + " to " + destPath + "." + "Violation of a nodetype or attempt to copy under property detected ", cve);
            
        }catch(javax.jcr.version.VersionException ve){
            throw new VersionException(
                    "Cannot copy the object from " + srcPath + " to " + destPath + "." + "Parent node of source or destination is versionable and checked in ", ve);
            
        }catch(javax.jcr.AccessDeniedException ade){
            throw new PersistenceException(
                    "Cannot copy the object from " + srcPath + " to " + destPath + "." + " Session does not have access permissions", ade);
            
        }catch(javax.jcr.PathNotFoundException pnf){
            throw new PersistenceException(
                    "Cannot copy the object from " + srcPath + " to " + destPath + "." + "Node at source or destination does not exist ", pnf);
            
        }catch(javax.jcr.ItemExistsException ie){
            throw new PersistenceException(
                    "Cannot copy the object from " + srcPath + " to " + destPath + "." + "It might already exist at destination path.", ie);
            
        }catch(javax.jcr.lock.LockException le){
            throw new PersistenceException(
                    "Cannot copy the object from " + srcPath + " to " + destPath + "." + "Violation of a lock detected", le);
            
        }catch(javax.jcr.RepositoryException re){
            throw new PersistenceException(
                    "Cannot copy the node from " + srcPath + " to " + destPath + "." , re);
        }
    }    
}
