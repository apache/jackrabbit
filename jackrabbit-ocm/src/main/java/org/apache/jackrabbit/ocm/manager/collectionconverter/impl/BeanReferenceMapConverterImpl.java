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

package org.apache.jackrabbit.ocm.manager.collectionconverter.impl;


import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableMap;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjects;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjectsUtil;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import java.util.Iterator;
import java.util.Map;

/**
 * Map converter used to map reference/uuid property by key into a java.util.Map.
 * <p/>
 * This implementation takes for granted that the keys to the map are of type java.lang.String.
 * <p/>
 * Further development will be required to fully support Map<Object, Object>.
 *
 * @author <a href="mailto:vincent.giguere@gmail.com">Vincent Giguere</a>
 */
public class BeanReferenceMapConverterImpl extends AbstractCollectionConverterImpl {


    /**
     * Constructor
     *
     * @param atomicTypeConverters
     * @param objectConverter
     * @param mapper
     */
    public BeanReferenceMapConverterImpl(Map atomicTypeConverters,
                                         ObjectConverter objectConverter,
                                         Mapper mapper) {
        super(atomicTypeConverters, objectConverter, mapper);
    }

    protected void doInsertCollection(Session session,
                                      Node parentNode,
                                      CollectionDescriptor collectionDescriptor,
                                      ManageableObjects objects) throws RepositoryException {

        // For maps of bean references, only Maps are supported
        if (!(objects instanceof ManageableMap)) {

            throw new JcrMappingException("Impossible to retrieve the attribute "
                    + collectionDescriptor.getFieldName() + " in the class "
                    + collectionDescriptor.getClassDescriptor().getClassName()
                    + " because it is not a map");
        }


        addUuidProperties(session, parentNode, collectionDescriptor, (ManageableMap) objects);
    }


    protected void doUpdateCollection(Session session,
                                      Node parentNode,
                                      CollectionDescriptor collectionDescriptor,
                                      ManageableObjects objects) throws RepositoryException {

    	// For maps of bean references, only Maps are supported
        if (!(objects instanceof ManageableMap)) {

            throw new JcrMappingException("Impossible to retrieve the attribute "
                    + collectionDescriptor.getFieldName() + " in the class "
                    + collectionDescriptor.getClassDescriptor().getClassName()
                    + " because it is not a map");
        }
        String jcrName = getCollectionJcrName(collectionDescriptor);

        // Delete existing values
        if (parentNode.hasProperty(jcrName)) {
            parentNode.setProperty(jcrName, (Value[]) null);
        }

        if (objects == null) {
            return;
        }

        addUuidProperties(session, parentNode, collectionDescriptor, (ManageableMap) objects);

    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.impl.AbstractCollectionConverterImpl#doGetCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, Class)
     */
    protected ManageableObjects doGetCollection(Session session,
                                                Node parentNode,
                                                CollectionDescriptor collectionDescriptor,
                                                Class collectionFieldClass) throws RepositoryException {


        try {
            String jcrName = getCollectionJcrName(collectionDescriptor);

            if (!parentNode.hasProperty(jcrName)) {
                return null;
            }

            Property property = parentNode.getProperty(jcrName);
            Value[] values = property.getValues();

            ManageableObjects objects = ManageableObjectsUtil.getManageableObjects(collectionFieldClass);

            // For maps of bean references, only Maps are supported
            if (!(objects instanceof ManageableMap)) {

                throw new JcrMappingException("Impossible to retrieve the attribute "
                        + collectionDescriptor.getFieldName() + " in the class "
                        + collectionDescriptor.getClassDescriptor().getClassName()
                        + " because it is not a map");
            }

            for (int i = 0; i < values.length; i++) {

                String encoded = values[i].getString();
                String key = MapReferenceValueEncoder.decodeKey(encoded);
                String uuid = MapReferenceValueEncoder.decodeReference(encoded);


                String path = session.getNodeByUUID(uuid).getPath();
                Object object = objectConverter.getObject(session, path);
                ((ManageableMap) objects).addObject(key, object);
            }

            return objects;
        }
        catch (Exception e) {
            throw new ObjectContentManagerException("Cannot get the collection field : "
                    + collectionDescriptor.getFieldName()
                    + "for class " + collectionDescriptor.getClassDescriptor().getClassName(), e);
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.impl.AbstractCollectionConverterImpl#doIsNull(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, Class)
     */
    protected boolean doIsNull(Session session, Node parentNode, CollectionDescriptor collectionDescriptor, Class collectionFieldClass) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);
        return !parentNode.hasProperty(jcrName);
    }

    private void addUuidProperties(Session session, Node parentNode,
                                   CollectionDescriptor collectionDescriptor,
                                   ManageableMap manageableMap)
            throws UnsupportedRepositoryOperationException, RepositoryException, VersionException, LockException, ConstraintViolationException {
        try {
            if (manageableMap == null) {
                return;
            }

            String jcrName = getCollectionJcrName(collectionDescriptor);
            Value[] values = new Value[manageableMap.getSize()];
            ValueFactory valueFactory = session.getValueFactory();
            Map map = (Map) manageableMap.getObjects(); 
            Iterator keyIterator = map.keySet().iterator();
            
            for (int i = 0; i < manageableMap.getSize(); i++) {
                String key = (String) keyIterator.next();
                Object object = map.get(key);
                if (object != null) {
                    ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(object.getClass());

                    FieldDescriptor fieldDescriptor = classDescriptor.getUuidFieldDescriptor();
                    if (fieldDescriptor == null) {
                        throw new JcrMappingException("The bean doesn't have an uuid - classdescriptor : "
                                + classDescriptor.getClassName());
                    }

                    String uuid = (String) ReflectionUtils.getNestedProperty(object, fieldDescriptor.getFieldName());
                    values[i] = valueFactory.createValue(MapReferenceValueEncoder.encodeKeyAndReference(key, uuid), PropertyType.STRING);
                }
            }

            parentNode.setProperty(jcrName, values);

        }
        catch (Exception e) {
            throw new ObjectContentManagerException("Cannot insert collection field : "
                    + collectionDescriptor.getFieldName()
                    + " of class "
                    + collectionDescriptor.getClassDescriptor().getClassName(), e);
        }
    }

}