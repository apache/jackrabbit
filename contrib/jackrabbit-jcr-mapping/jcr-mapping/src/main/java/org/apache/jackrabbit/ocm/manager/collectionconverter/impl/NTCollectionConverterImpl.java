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
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollectionUtil;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Collection Mapping/convertion based on node type.
 *
 * This collection mapping strategy maps the collection elements into subnodes based on the same node types.
 * 
 * There are 2 constraints in this collection converter : 
 * 1/ this is not possible to have 2 different collections in the main object which are used the same jcr node type for their elements. 
 * 2/ this is not possible to make a distinction between an empty collection and an null collection. 
 *
 *
 * If the collection element class contains an id (see the FieldDescriptor definition), this id value is used to build the collection element node name.
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
 *          Each "collection-element" nodes have the same jcr node type
 *
 * Example - with an id attribute:
 *   /test (Main object containing the collection field )
 *          /aValue (id value assigned to the first element)
 *                /item-prop
 *                ....
 *          /anotherValue (id value assigned to the first element)
 *          ...
 *          
 *          Each collection element nodes have the same jcr node type
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
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter#updateCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection)
     */
    protected void doUpdateCollection(Session session,
                                      Node parentNode,
                                      CollectionDescriptor collectionDescriptor,
                                      ManageableCollection collection) throws RepositoryException {
        
        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(
                ReflectionUtils.forName(collectionDescriptor.getElementClassName()));

        if (collection == null || !elementClassDescriptor.hasIdField()) {
            this.deleteCollectionItems(session,
                                       parentNode,
                                       elementClassDescriptor.getJcrType());
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
         NodeIterator nodes = this.getCollectionNodes(session, parentNode, 
        		                                              elementClassDescriptor.getJcrType());
         if (nodes != null && elementClassDescriptor.hasIdField()) {
            

            while (nodes.hasNext()) {
                Node child = (Node) nodes.next();
                
                if (!updatedItems.containsKey(child.getName())) {
                    child.remove();
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter#getCollection(javax.jcr.Session, javax.jcr.Node, org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor, java.lang.Class)
     */
    protected ManageableCollection doGetCollection(Session session,
                                                   Node parentNode,
                                                   CollectionDescriptor collectionDescriptor,
                                                   Class collectionFieldClass) throws RepositoryException {
	    ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass( ReflectionUtils.forName(collectionDescriptor.getElementClassName())); 
        ManageableCollection collection = ManageableCollectionUtil.getManageableCollection(collectionFieldClass);

        NodeIterator nodes = this.getCollectionNodes(session, parentNode, elementClassDescriptor.getJcrType());
        
        if (nodes == null || nodes.getSize() == 0)
        {
        	return null;
        }
                
        while (nodes.hasNext()) {
            Node itemNode = (Node) nodes.next();
            log.debug("Collection node found : " + itemNode.getPath());
            Object item = objectConverter.getObject(session,  itemNode.getPath());
            collection.addObject(item);
        }

        return collection;
    }
    
    /**
     * @see AbstractCollectionConverterImpl#doIsNull(Session, Node, CollectionDescriptor, Class)
     * 
     * return true If the parent node doesn't contains node based on the node type associated to the collection elements
     *  
     */
    protected boolean doIsNull(Session session,
                                              Node parentNode,
                                              CollectionDescriptor collectionDescriptor,
                                              Class collectionFieldClass) throws RepositoryException {

        String elementClassName = collectionDescriptor.getElementClassName();
        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(elementClassName));
		QueryResult queryResult = getQuery(session, parentNode, elementClassDescriptor.getJcrType());    	
    	return queryResult.getNodes().getSize() == 0;
    }
        
    private NodeIterator getCollectionNodes(Session session, Node parentNode, String itemNodeType)
    throws PathNotFoundException, ValueFormatException, RepositoryException {

        List collectionNodes = null;
        
        QueryResult queryResult = getQuery(session, parentNode, itemNodeType);
        return  queryResult.getNodes();
        
    }

    private void deleteCollectionItems(Session session, Node parentNode, String itemNodeType) 
    throws VersionException, 
           LockException, 
           ConstraintViolationException, 
           PathNotFoundException, 
           ValueFormatException, 
           RepositoryException
    {
        NodeIterator nodes = this.getCollectionNodes(session, parentNode, itemNodeType);
        if (nodes == null || nodes.getSize()==0) return;
        
        while (nodes.hasNext()) {
            Node node = (Node) nodes.next();
            node.remove();
        }
    }
    
   
	
	private QueryResult getQuery(Session session, Node parentNode, String jcrNodeType) throws RepositoryException, InvalidQueryException {
    	String jcrExpression= "";    	
    	if (!parentNode.getPath().startsWith("/jcr:system/jcr:versionStorage")) 
    	{
            jcrExpression = "SELECT * FROM " + jcrNodeType + " WHERE jcr:path LIKE '" + parentNode.getPath() 
                                       + "/%' AND NOT jcr:path LIKE '" + parentNode.getPath() + "/%/%'";
    	}
    	else
    	{
    	
    		jcrExpression = "SELECT * FROM nt:frozenNode" + " WHERE jcr:path LIKE '" + parentNode.getPath() + "/%'" 
    		                 + " AND NOT jcr:path LIKE '" + parentNode.getPath() + "/%/%'"
    		                 + " AND jcr:frozenPrimaryType = '" + jcrNodeType + "'";

    		                
    	}
        Query jcrQuery = session.getWorkspace().getQueryManager().createQuery(jcrExpression, javax.jcr.query.Query.SQL);
        QueryResult queryResult = jcrQuery.execute();
		return queryResult;
	}
}
