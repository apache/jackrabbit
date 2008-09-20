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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjectsUtil;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableMap;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjects;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Default Collection Mapping/convertion implementation.
 *
 * This collection mapping strategy maps a collection under an extra JCR node (specify by the jcrName in the CollectionDescriptor).
 * It is usefull when the node type "nt:unstructured" is applied to the collection elements. By this way, it is possible
 * to distinguish the collection elements from the other main object fields.
 *
 * If the collection element class contains an id (see the ID FieldDescriptor definition), this id value is used to build the collection element node.
 * Otherwise, the element node name is a simple constant.
 *
 * Example - without an id attribute:
 *   /test (Main object containing the collection field )
 *     /mycollection (extra node used to store the entire collection)
 *          /collection-element (node used to store the first collection element)
 *                /item-prop
 *                ....
 *          /collection-element (node used to store the second collection element)
 *          ...
 *
 * Example - with an id attribute:
 *   /test (Main object containing the collection field )
 *     /mycollection (extra node used to store the entire collection)
 *          /aValue (id value assigned to the first element)
 *                /item-prop
 *                ....
 *          /anotherValue (id value assigned to the first element)
 *          ...

 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class DefaultCollectionConverterImpl extends AbstractCollectionConverterImpl {

    protected static final String COLLECTION_ELEMENT_NAME = "collection-element";

    /**
     * Constructor
     * @param atomicTypeConverters
     * @param objectConverter
     * @param mapper
     */
    public DefaultCollectionConverterImpl(Map atomicTypeConverters,
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
                                      ManageableObjects objects) throws RepositoryException {
        if (objects == null) {
            return;
        }

        String jcrName = collectionDescriptor.getJcrName();

        if (jcrName == null) {
            throw new JcrMappingException(
                    "The JcrName attribute is not defined for the CollectionDescriptor : "
                    + collectionDescriptor.getFieldName() + " for the classdescriptor : " + collectionDescriptor.getClassDescriptor().getClassName());
        }

        Node collectionNode = parentNode.addNode(jcrName);

        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass( ReflectionUtils.forName(collectionDescriptor.getElementClassName()));

        if (objects instanceof ManageableCollection)
           insertManageableCollection(session, objects, collectionNode, elementClassDescriptor, collectionDescriptor);
        else 
           insertManageableMap(session, objects, collectionNode);
        	
    }

	private void insertManageableCollection(Session session,
			ManageableObjects objects, Node collectionNode,
			ClassDescriptor elementClassDescriptor,
            CollectionDescriptor collectionDescriptor) {
		Iterator collectionIterator = objects.getIterator();
        while (collectionIterator.hasNext()) {
            Object item = collectionIterator.next();
            String elementJcrName = null;

            // If the element object has a unique id => the element jcr node name = the id value
            if (elementClassDescriptor.hasIdField()) {
                String idFieldName = elementClassDescriptor.getIdFieldDescriptor()
                                                           .getFieldName();
                elementJcrName = ReflectionUtils.getNestedProperty(item, idFieldName).toString();
            }
            else {
                elementJcrName = collectionDescriptor.getJcrElementName();
                if (elementJcrName == null) { // use PathFormat.checkFormat() here?
                    elementJcrName = COLLECTION_ELEMENT_NAME;
                }
            }

            objectConverter.insert(session, collectionNode, elementJcrName, item);
        }
	}
	
	private void insertManageableMap(Session session, ManageableObjects objects, Node collectionNode) {

		
		Map map = (Map) objects.getObjects(); 
		for (Object key : map.keySet())
		{
			Object item = map.get(key);
			objectConverter.insert(session, collectionNode, key.toString(), item);
			
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
    	boolean hasNode = parentNode.hasNode(jcrName);
        // If the new value for the collection is null, drop the node matching to the collection
    	if (objects == null)
        {
            if (hasNode)
            {
                parentNode.getNode(jcrName).remove();
            }
            return;
        }

    	// If there is not yet a node matching to the collection, insert the collection
    	if (! hasNode)
    	{
    		this.doInsertCollection(session, parentNode, collectionDescriptor, objects);
    		return;
    	}

    	// update process
    	if (objects instanceof ManageableCollection)
           updateManagableCollection(session, parentNode, collectionDescriptor, objects, jcrName);
    	else
    	   updateManagableMap(session, parentNode, collectionDescriptor, objects, jcrName);
    		
    }

	private void updateManagableCollection(Session session, Node parentNode,
			CollectionDescriptor collectionDescriptor,
			ManageableObjects objects, String jcrName)
			throws PathNotFoundException, RepositoryException,
			VersionException, LockException, ConstraintViolationException,
			ItemExistsException {
		ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass( ReflectionUtils.forName(collectionDescriptor.getElementClassName()));
        Node collectionNode = parentNode.getNode(jcrName);
        //  If the collection elements have not an id, it is not possible to find the matching JCR nodes 
        //  => delete the complete collection
        if (!elementClassDescriptor.hasIdField()) {
            collectionNode.remove();
            collectionNode = parentNode.addNode(jcrName);
        }

        Iterator collectionIterator = objects.getIterator();

        Map updatedItems = new HashMap();
        while (collectionIterator.hasNext()) {
            Object item = collectionIterator.next();

            String elementJcrName = null;

            if (elementClassDescriptor.hasIdField()) {

                String idFieldName = elementClassDescriptor.getIdFieldDescriptor().getFieldName();
                elementJcrName = ReflectionUtils.getNestedProperty(item, idFieldName).toString();

                // Update existing JCR Nodes
                if (collectionNode.hasNode(elementJcrName)) {
                    objectConverter.update(session, collectionNode, elementJcrName, item);
                }
                else {
                    // Add new collection elements
                    objectConverter.insert(session, collectionNode, elementJcrName, item);
                }

                updatedItems.put(elementJcrName, item);
            }
            else {
                elementJcrName = collectionDescriptor.getJcrElementName();
                if (elementJcrName == null) { // use PathFormat.checkFormat() here?
                    elementJcrName = COLLECTION_ELEMENT_NAME;
                }
                objectConverter.insert(session, collectionNode, elementJcrName, item);
            }
        }

        // Delete JCR nodes that are not present in the collection
        if (elementClassDescriptor.hasIdField()) {
            NodeIterator nodeIterator = collectionNode.getNodes();
            List removeNodes = new ArrayList();
            while (nodeIterator.hasNext()) {
                Node child = nodeIterator.nextNode();
                if (!updatedItems.containsKey(child.getName())) {
                    removeNodes.add(child);
                }
            }
            for(int i = 0; i < removeNodes.size(); i++) {
                ((Node) removeNodes.get(i)).remove();
            }
        }
	}
	
	
	private void updateManagableMap(Session session, Node parentNode,
									CollectionDescriptor collectionDescriptor,
									ManageableObjects objects, String jcrName)
									throws PathNotFoundException, RepositoryException,
									VersionException, LockException, ConstraintViolationException,
									ItemExistsException {
		
		
		ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass( ReflectionUtils.forName(collectionDescriptor.getElementClassName()));
        Node collectionNode = parentNode.getNode(jcrName);

        Map map = (Map) objects.getObjects(); 
        Map updatedItems = new HashMap();
		for (Object key : map.keySet())
		{
			Object item = map.get(key);
			// Update existing JCR Nodes
            if (collectionNode.hasNode(key.toString())) {
                objectConverter.update(session, collectionNode, key.toString(), item);
            }
            else {
                // Add new collection elements
                objectConverter.insert(session, collectionNode, key.toString(), item);
            } 
            updatedItems.put(key.toString(), item);
		}

		// Delete the nodes that are not present in the Map 
        NodeIterator nodeIterator = collectionNode.getNodes();
        List removeNodes = new ArrayList();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            if (!updatedItems.containsKey(child.getName())) {
                    removeNodes.add(child);
            }
        }
        for(int i = 0; i < removeNodes.size(); i++) {
            ((Node) removeNodes.get(i)).remove();
        }

	}

    /**
     * @see AbstractCollectionConverterImpl#doGetCollection(Session, Node, CollectionDescriptor, Class)
     */
    protected ManageableObjects doGetCollection(Session session,
                                              Node parentNode,
                                              CollectionDescriptor collectionDescriptor,
                                              Class collectionFieldClass) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);

        if (parentNode == null || !parentNode.hasNode(jcrName)) {
            return null;
        }

        ManageableObjects objects = ManageableObjectsUtil.getManageableObjects(collectionFieldClass);
        Node collectionNode = parentNode.getNode(jcrName);
        NodeIterator children = collectionNode.getNodes();
        Class elementClass = ReflectionUtils.forName(collectionDescriptor.getElementClassName());

        while (children.hasNext()) {
            Node itemNode = children.nextNode();
            Object item = objectConverter.getObject(session, elementClass, itemNode.getPath());
            if ( objects instanceof ManageableCollection)
            	((ManageableCollection)objects).addObject(item);
            else 
            	((ManageableMap) objects).addObject(itemNode.getName(), item);

        }

        return objects;
    }

    /**
     * @see AbstractCollectionConverterImpl#doIsNull(Session, Node, CollectionDescriptor, Class)
     */
    protected boolean doIsNull(Session session,
                                              Node parentNode,
                                              CollectionDescriptor collectionDescriptor,
                                              Class collectionFieldClass) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);

        if (parentNode == null || !parentNode.hasNode(jcrName)) {
            return true;
        }
        return false;
    }
}