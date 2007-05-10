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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollectionUtil;
import org.apache.jackrabbit.ocm.persistence.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Collection Mapping/convertion based on node type.
 *
 * This collection mapping strategy maps a collection into several nodes based on specific node type.
 *
 *
 * If the collection element class contains an id (see the FieldDescriptor definition), this id value is used to build the collection element node.
 * Otherwise, the element node name is a simple constant (collection-element)
 *
 * Example - without an id attribute:
 *   /test (Main object containing the collection field )
 *          /collection-element (node used to store the first collection element)
 *                /item-prop
 *                ....
 *          /collection-element (node used to store the second collection element)
 *          ...
 *
 * Example - with an id attribute:
 *   /test (Main object containing the collection field )
 *          /aValue (id value assigned to the first element)
 *                /item-prop
 *                ....
 *          /anotherValue (id value assigned to the first element)
 *          ...
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class NTCollectionConverterImpl extends AbstractCollectionConverterImpl {

    private final static Log log = LogFactory.getLog(NTCollectionConverterImpl.class);

    private static final String COLLECTION_ELEMENT_NAME = "collection-element";

    /**
     * Constructor
     *
     * @param atomicTypeConverters
     * @param objectConverter
     * @param mapper
     */
    public NTCollectionConverterImpl(Map atomicTypeConverters,
                                     ObjectConverter objectConverter,
                                     Mapper mapper) {
        super(atomicTypeConverters, objectConverter, mapper);
    }

    /**
     * @see AbstractCollectionConverterImpl#doInsertCollection(Session, Node, CollectionDescriptor, ManageableCollection)
     */
    protected void doInsertCollection(Session session,
                                      Node parentNode,
                                      CollectionDescriptor collectionDescriptor,
                                      ManageableCollection collection) {
        if (collection == null) {
            return;
        }
        
        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass( ReflectionUtils.forName(collectionDescriptor.getElementClassName()));

        Iterator collectionIterator = collection.getIterator();        
        while (collectionIterator.hasNext()) {
            Object item = collectionIterator.next();
            String elementJcrName = null;

            // If the element object has a unique id => the element jcr node name = the id value
            if (elementClassDescriptor.hasIdField()) {
                String idFieldName = elementClassDescriptor.getIdFieldDescriptor().getFieldName();
                elementJcrName = ReflectionUtils.getNestedProperty(item, idFieldName).toString();
            }
            else {                
                elementJcrName = COLLECTION_ELEMENT_NAME;
            }

            objectConverter.insert(session, parentNode, elementJcrName, item);
        }
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.persistence.collectionconverter.CollectionConverter#updateCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollection)
     */
    protected void doUpdateCollection(Session session,
                                      Node parentNode,
                                      CollectionDescriptor collectionDescriptor,
                                      ManageableCollection collection) throws RepositoryException {
        Mapper mapper = collectionDescriptor.getClassDescriptor().getMappingDescriptor().getMapper();
        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(
                ReflectionUtils.forName(collectionDescriptor.getElementClassName()));

        if (collection == null || !elementClassDescriptor.hasIdField()) {
            this.deleteCollectionItems(session,
                                       parentNode,
                                       elementClassDescriptor.getJcrNodeType());
        }

        if (collection == null) {
            return;
        }

        Iterator collectionIterator = collection.getIterator();
        Map updatedItems = new HashMap();
        while (collectionIterator.hasNext()) {
            Object item = collectionIterator.next();
        
            String elementJcrName = null;

            if (elementClassDescriptor.hasIdField()) {
                String idFieldName = elementClassDescriptor.getIdFieldDescriptor().getFieldName();
                elementJcrName = ReflectionUtils.getNestedProperty(item, idFieldName).toString();

                // Update existing JCR Nodes
                if (parentNode.hasNode(elementJcrName)) {
                    objectConverter.update(session, parentNode, elementJcrName, item);
                }
                else {
                    // Add new collection elements
                    objectConverter.insert(session, parentNode, elementJcrName, item);
                }

                updatedItems.put(elementJcrName, item);
            }
            else {
                elementJcrName = COLLECTION_ELEMENT_NAME;
                objectConverter.insert(session, parentNode, elementJcrName, item);
            }
        }

        // Delete JCR nodes that are not present in the collection
        if (elementClassDescriptor.hasIdField()) {
            Iterator nodeIterator = this.getCollectionNodes(session, parentNode,
                    elementClassDescriptor.getJcrNodeType()).iterator();

            while (nodeIterator.hasNext()) {
                Node child = (Node) nodeIterator.next();
                
                if (!updatedItems.containsKey(child.getName())) {
                    child.remove();
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.persistence.collectionconverter.CollectionConverter#getCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, java.lang.Class)
     */
    protected ManageableCollection doGetCollection(Session session,
                                                   Node parentNode,
                                                   CollectionDescriptor collectionDescriptor,
                                                   Class collectionFieldClass) throws RepositoryException {
	    ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass( ReflectionUtils.forName(collectionDescriptor.getElementClassName())); 
        ManageableCollection collection = ManageableCollectionUtil.getManageableCollection(collectionFieldClass);
        Class elementClass = ReflectionUtils.forName(collectionDescriptor.getElementClassName());
        Iterator children = this.getCollectionNodes(session, parentNode,
                elementClassDescriptor.getJcrNodeType()).iterator();

        while (children.hasNext()) {
            Node itemNode = (Node) children.next();
            log.debug("Collection node found : " + itemNode.getPath());
            Object item = objectConverter.getObject(session,  itemNode.getPath());
            collection.addObject(item);
        }

        return collection;
    }
    
    /**
     * @see AbstractCollectionConverterImpl#doIsNull(Session, Node, CollectionDescriptor, Class)
     */
    protected boolean doIsNull(Session session,
                                              Node parentNode,
                                              CollectionDescriptor collectionDescriptor,
                                              Class collectionFieldClass) throws RepositoryException {

    	    // This collection converter returns at least a empty collection (see in doGetCollection) 
        return false;
    }         

    private Collection getCollectionNodes(Session session, Node parentNode, String itemNodeType)
    throws PathNotFoundException, ValueFormatException, RepositoryException {

        List collectionNodes = new ArrayList();

        // TODO : review this workaround used to support version nodes
        // Searching on the version storage has some bugs => loop on all child noded and check the property jcr:frozenPrimaryType
        // I have to investigate in more detail what's happen exactly
        if (!parentNode.getPath().startsWith("/jcr:system/jcr:versionStorage")) {
            NodeIterator nodeIterator = parentNode.getNodes();
            while (nodeIterator.hasNext()) {
                Node child = nodeIterator.nextNode();

                if (child.isNodeType(itemNodeType)) {
                    collectionNodes.add(child);
                }
            }
        }
        else {
            NodeIterator nodeIterator = parentNode.getNodes();
            while (nodeIterator.hasNext()) {
                Node child = nodeIterator.nextNode();

                if (child.getProperty("jcr:frozenPrimaryType").getString().equals(itemNodeType)) {
                    collectionNodes.add(child);
                }
            }

        }

        return collectionNodes;
    }

    private void deleteCollectionItems(Session session, Node parentNode, String itemNodeType) 
    throws VersionException, 
           LockException, 
           ConstraintViolationException, 
           PathNotFoundException, 
           ValueFormatException, 
           RepositoryException
    {
        Iterator nodeIterator = this.getCollectionNodes(session, parentNode, itemNodeType).iterator();
        while (nodeIterator.hasNext()) {
            Node node = (Node) nodeIterator.next();
            node.remove();
        }
    }
}
