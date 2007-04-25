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
package org.apache.portals.graffito.jcr.spring;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.portals.graffito.jcr.exception.JcrMappingException;
import org.apache.portals.graffito.jcr.mapper.Mapper;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.BinaryTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.BooleanTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.ByteArrayTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.CalendarTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.DoubleTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.IntTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.LongTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.StringTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.TimestampTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.UtilDateTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.impl.PersistenceManagerImpl;
import org.apache.portals.graffito.jcr.query.Query;
import org.apache.portals.graffito.jcr.query.QueryManager;
import org.apache.portals.graffito.jcr.query.impl.QueryManagerImpl;
import org.springframework.dao.DataAccessException;
import org.springmodules.jcr.JcrCallback;
import org.springmodules.jcr.JcrSystemException;
import org.springmodules.jcr.JcrTemplate;
import org.springmodules.jcr.SessionFactory;

/**
 * Template whichs adds mapping support for the Java Content Repository.
 * <p/>
 * For PersistenceManagers the template creates internally the set of default converters.
 * 
 * 
 * @see org.apache.portals.graffito.jcr.persistence.PersistenceManager
 * @author Costin Leau
 * 
 */
public class JcrMappingTemplate extends JcrTemplate implements JcrMappingOperations {

    private Mapper mapper;
    

    /**
     * Default constructor for JcrTemplate
     */
    public JcrMappingTemplate() {
        super();

    }

    /**
     * @param sessionFactory
     */
    public JcrMappingTemplate(SessionFactory sessionFactory, Mapper mapper) {
        setSessionFactory(sessionFactory);
        setMapper(mapper);
        
        afterPropertiesSet();
    }

    /**
     * Add rule for checking the mapper.
     * 
     * @see org.springmodules.jcr.JcrAccessor#afterPropertiesSet()
     */
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        if (mapper == null)
            throw new IllegalArgumentException("mapper can NOT be null");
    }

    /**
     * Method for creating a query manager. It's unclear where this entity is stateless or not.
     * 
     * @return
     */
    public  QueryManager createQueryManager() {
    	    try 
    	    {
			Map atomicTypeConverters = this.createDefaultConverters(this.getSession());
            return new QueryManagerImpl (mapper, atomicTypeConverters);
		} 
    	    catch (RepositoryException e) 
    	    {
			throw new JcrSystemException(e);
		}
    }

    /**
     * Creates a persistence manager. It's unclear if this object is stateless/thread-safe or not.
     * However because it depends on session it has to be created per session and it's not per repository.
     * 
     * 
     * @param session
     * @return
     * @throws JcrMappingException
     */
    protected PersistenceManager createPersistenceManager(Session session) throws RepositoryException, JcrMappingException {
        return new PersistenceManagerImpl(mapper,  createQueryManager(), session);
    }

    /**
     * Due to the way the actual jcr-mapping is made we have to create the converters for
     * each session.
     * 
     * @param session
     * @return
     */
    protected Map createDefaultConverters(Session session) throws RepositoryException {
        Map map = new HashMap(14);

        map.put(String.class, new StringTypeConverterImpl());
        map.put(InputStream.class, new BinaryTypeConverterImpl());
        map.put(long.class, new LongTypeConverterImpl());
        map.put(Long.class, new LongTypeConverterImpl());
        map.put(int.class, new IntTypeConverterImpl());
        map.put(Integer.class, new IntTypeConverterImpl());
        map.put(double.class, new DoubleTypeConverterImpl());
        map.put(Double.class, new DoubleTypeConverterImpl());
        map.put(boolean.class, new BooleanTypeConverterImpl());
        map.put(Boolean.class, new BooleanTypeConverterImpl());
        map.put(Calendar.class, new CalendarTypeConverterImpl());
        map.put(Date.class, new UtilDateTypeConverterImpl());
        map.put(byte[].class, new ByteArrayTypeConverterImpl());
        map.put(Timestamp.class, new TimestampTypeConverterImpl());

        return map;
    }

    /**
     * @see org.springmodules.jcr.mapping.JcrMappingOperations#execute(org.springmodules.jcr.mapping.JcrMappingCallback, boolean)
     */
    public Object execute(final JcrMappingCallback action, boolean exposeNativeSession) throws DataAccessException {
        return execute(new JcrCallback() {
            /**
             * @see org.springmodules.jcr.JcrCallback#doInJcr(javax.jcr.Session)
             */
            public Object doInJcr(Session session) throws RepositoryException {
                try {
                    return action.doInJcrMapping(createPersistenceManager(session));
                } catch (JcrMappingException e) {
                    throw convertMappingAccessException(e);
                }
            }

        }, exposeNativeSession);
    }

    /**
     * @see org.springmodules.jcr.mapping.JcrMappingOperations#execute(org.springmodules.jcr.mapping.JcrMappingCallback)
     */
    public Object execute(JcrMappingCallback callback) throws DataAccessException {
        return execute(callback, isExposeNativeSession());
    }

    // ----------------
    // Delegate methods
    // ----------------

    /**
     * @see org.springmodules.jcr.mapping.JcrMappingOperations#insert(java.lang.String, java.lang.Object)
     */
    public void insert(final java.lang.Object object) {
        execute(new JcrMappingCallback() {
            public Object doInJcrMapping(PersistenceManager manager) throws JcrMappingException {
                manager.insert(object);
                return null;
            }
        }, true);
    }

    /**
     * @see org.springmodules.jcr.mapping.JcrMappingOperations#update(java.lang.String, java.lang.Object)
     */
    public void update( final java.lang.Object object) {
        execute(new JcrMappingCallback() {
            public Object doInJcrMapping(PersistenceManager manager) throws JcrMappingException {
                manager.update( object);
                return null;
            }
        }, true);
    }

    /**
     * @see org.springmodules.jcr.mapping.JcrMappingOperations#remove(java.lang.String)
     */
    public void remove(final java.lang.String path) {
        execute(new JcrMappingCallback() {
            public Object doInJcrMapping(PersistenceManager manager) throws JcrMappingException {
                manager.remove(path);
                return null;
            }
        }, true);
    }

    /**
     * @see org.springmodules.jcr.mapping.JcrMappingOperations#getObject(java.lang.Class, java.lang.String)
     */
    public Object getObject(final java.lang.String path) {
        return execute(new JcrMappingCallback() {
            public Object doInJcrMapping(PersistenceManager manager) throws JcrMappingException {
                return manager.getObject(path);
            }
        }, true);
    }

    /**
     * @see org.springmodules.jcr.mapping.JcrMappingOperations#getObjects(org.apache.portals.graffito.jcr.query.Query)
     */
    public Collection getObjects(final Query query) {
        return (Collection) execute(new JcrMappingCallback() {
            public Object doInJcrMapping(PersistenceManager manager) throws JcrMappingException {
                return manager.getObjects(query);
            }
        }, true);
    }

    /**
     * Convert the given MappingException to an appropriate exception from
     * the <code>org.springframework.dao</code> hierarchy.
     * <p>
     * Note that because we have no base specific exception we have to catch
     * the generic Exception and translate it into JcrSystemException.
     * <p>
     * May be overridden in subclasses.
     * 
     * @param ex Exception that occured
     * @return the corresponding DataAccessException instance
     */
    public DataAccessException convertMappingAccessException(Exception ex) {
        // repository exception
        if (ex instanceof RepositoryException)
            return super.convertJcrAccessException((RepositoryException) ex);
        return new JcrSystemException(ex);
    }

    /**
     * @return Returns the mapper.
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * @param mapper The mapper to set.
     */
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    
}
