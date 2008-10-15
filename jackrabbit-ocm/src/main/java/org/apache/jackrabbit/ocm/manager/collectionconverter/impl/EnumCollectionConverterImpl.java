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


import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.UndefinedTypeConverterImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjectsUtil;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjects;
import org.apache.jackrabbit.ocm.manager.enumconverter.EnumTypeConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Collection Mapping/convertion implementation used for enum collections
 *
 * This collection mapping strategy maps a collection into a JCR multi value property
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 * @author <a href='mailto:boni.g@bioimagene.com'>Boni Gopalan</a>
 */
public class EnumCollectionConverterImpl extends AbstractCollectionConverterImpl {

	private EnumTypeConverter enumConverter = new EnumTypeConverter();
    /**
     * Constructor
     *
     * @param atomicTypeConverters
     * @param objectConverter
     * @param mapper
     */
    public EnumCollectionConverterImpl(		Map atomicTypeConverters,
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
                                      ManageableObjects objects) throws RepositoryException {
        try {
            if (objects == null) {
                return;
            }

            String jcrName = getCollectionJcrName(collectionDescriptor);
            Value[] values = new Value[objects.getSize()];
            ValueFactory valueFactory = session.getValueFactory();
            Iterator collectionIterator = objects.getIterator();
            for (int i = 0; i < objects.getSize(); i++) {
                Object fieldValue = collectionIterator.next();
                values[i] = enumConverter.getValue(valueFactory, fieldValue);
            }

            parentNode.setProperty(jcrName, values);
        }
        catch(ValueFormatException vfe) {
            throw new ObjectContentManagerException("Cannot insert collection field : "
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
                                 ManageableObjects objects) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);

        // Delete existing values
        if (parentNode.hasProperty(jcrName)) {
            parentNode.setProperty(jcrName, (Value[]) null);
        }

        if (objects == null) {
            return;
        }


        // Add all collection element into an Value array
        Value[] values = new Value[objects.getSize()];
        ValueFactory valueFactory = session.getValueFactory();
        int i = 0;
        for (Iterator collectionIterator = objects.getIterator(); collectionIterator.hasNext(); i++) {
            Object fieldValue = collectionIterator.next();
            values[i] = enumConverter.getValue(valueFactory, fieldValue);
        }

        parentNode.setProperty(jcrName, values);
    }

    /**
     * @see AbstractCollectionConverterImpl#doGetCollection(Session, Node, CollectionDescriptor, Class)
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
            if (values == null || values.length <= 0){
            	return null;
            }
            
            ManageableObjects objects = ManageableObjectsUtil.getManageableObjects(collectionFieldClass);
            for (int i = 0; i < values.length; i++) {
                ((ManageableCollection) objects).addObject(enumConverter.getObject(values[i]));
            }

            return objects;
        }
        catch(ValueFormatException vfe) {
          throw new ObjectContentManagerException("Cannot get the collection field : "
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