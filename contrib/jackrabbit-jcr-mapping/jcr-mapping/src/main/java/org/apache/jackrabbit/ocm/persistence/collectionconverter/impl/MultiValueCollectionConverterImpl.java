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

package org.apache.jackrabbit.ocm.persistence.collectionconverter.impl;


import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.ocm.exception.PersistenceException;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.persistence.atomictypeconverter.AtomicTypeConverter;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollectionUtil;
import org.apache.jackrabbit.ocm.persistence.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Collection Mapping/convertion implementation used for multi values properties
 *
 * This collection mapping strategy maps a collection into a JCR multi value property
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class MultiValueCollectionConverterImpl extends AbstractCollectionConverterImpl {

    /**
     * Constructor
     *
     * @param atomicTypeConverters
     * @param objectConverter
     * @param mapper
     */
    public MultiValueCollectionConverterImpl(Map atomicTypeConverters,
                                             ObjectConverter objectConverter,
                                             Mapper mapper) {
        super(atomicTypeConverters, objectConverter, mapper);
    }

    /**
     *
     * @see AbstractCollectionConverterImpl#doInsertCollection(Session, Node, CollectionDescriptor, ManageableCollection)
     */
    protected void doInsertCollection(Session session,
                                      Node parentNode,
                                      CollectionDescriptor collectionDescriptor,
                                      ManageableCollection collection) throws RepositoryException {
        try {
            if (collection == null) {
                return;
            }

            String jcrName = getCollectionJcrName(collectionDescriptor);
            Value[] values = new Value[collection.getSize()];
            ValueFactory valueFactory = session.getValueFactory();
            Iterator collectionIterator = collection.getIterator();
            for (int i = 0; i < collection.getSize(); i++) {
                Object fieldValue = collectionIterator.next();
                AtomicTypeConverter atomicTypeConverter = (AtomicTypeConverter) atomicTypeConverters
                    .get(fieldValue.getClass());
                values[i] = atomicTypeConverter.getValue(valueFactory, fieldValue);
            }

            parentNode.setProperty(jcrName, values);
        }
        catch(ValueFormatException vfe) {
            throw new PersistenceException("Cannot insert collection field : " 
                    + collectionDescriptor.getFieldName()
                    + " of class "
                    + collectionDescriptor.getClassDescriptor().getClassName(), vfe);
        }
    }

    /**
     *
     * @see AbstractCollectionConverterImpl#doUpdateCollection(Session, Node, CollectionDescriptor, ManageableCollection)
     */
    protected void doUpdateCollection(Session session,
                                 Node parentNode,
                                 CollectionDescriptor collectionDescriptor,
                                 ManageableCollection collection) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);

        // Delete existing values
        if (parentNode.hasProperty(jcrName)) {
            parentNode.setProperty(jcrName, (Value[]) null);
        }

        if (collection == null) {
            return;
        }


        // Add all collection element into an Value array
        Value[] values = new Value[collection.getSize()];
        ValueFactory valueFactory = session.getValueFactory();
        int i = 0; 
        for (Iterator collectionIterator = collection.getIterator(); collectionIterator.hasNext(); i++) {
            Object fieldValue = collectionIterator.next();
            AtomicTypeConverter atomicTypeConverter = (AtomicTypeConverter) atomicTypeConverters
                .get(fieldValue.getClass());
            values[i] = atomicTypeConverter.getValue(valueFactory, fieldValue);
        }

        parentNode.setProperty(jcrName, values);
    }

    /**
     * @see AbstractCollectionConverterImpl#doGetCollection(Session, Node, CollectionDescriptor, Class)
     */
    protected ManageableCollection doGetCollection(Session session,
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

            ManageableCollection collection = ManageableCollectionUtil.getManageableCollection(collectionFieldClass);
            String elementClassName = collectionDescriptor.getElementClassName();
            Class elementClass = ReflectionUtils.forName(elementClassName);
            for (int i = 0; i < values.length; i++) {
                AtomicTypeConverter atomicTypeConverter = (AtomicTypeConverter) atomicTypeConverters
                    .get(elementClass);
                collection.addObject(atomicTypeConverter.getObject(values[i]));
            }

            return collection;
        }
        catch(ValueFormatException vfe) {
          throw new PersistenceException("Cannot get the collection field : "
                  + collectionDescriptor.getFieldName()
                  + "for class " + collectionDescriptor.getClassDescriptor().getClassName(),
                  vfe);
        }
    }
    
    /**
     * @see AbstractCollectionConverterImpl#doIsNull(Session, Node, CollectionDescriptor, Class)
     */
    protected boolean doIsNull(Session session,
                                              Node parentNode,
                                              CollectionDescriptor collectionDescriptor,
                                              Class collectionFieldClass) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);

         if (!parentNode.hasProperty(jcrName)) {
            return true;
        }
        return false;
    }     
}