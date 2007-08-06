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
package org.apache.jackrabbit.ocm.manager.objectconverter.impl;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.beanconverter.BeanConverter;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.cache.impl.RequestObjectCacheImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollectionUtil;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.DefaultCollectionConverterImpl;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerUtil;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.jackrabbit.ocm.repository.RepositoryUtil;

/**
 * Default implementation for {@link ObjectConverterImpl}
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart  Christophe </a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class ObjectConverterImpl implements ObjectConverter {

	private static final String DEFAULT_BEAN_CONVERTER = "org.apache.jackrabbit.ocm.manager.beanconverter.impl.DefaultBeanConverterImpl";

	private final static Log log = LogFactory.getLog(ObjectConverterImpl.class);

	private Mapper mapper;

	private AtomicTypeConverterProvider atomicTypeConverterProvider;

	private ProxyManager proxyManager;
	
	private SimpleFieldsHelper simpleFieldsHelp;
	
	private ObjectCache requestObjectCache; 

	/**
	 * No-arg constructor.
	 */
	public ObjectConverterImpl() {
	}

	/**
	 * Constructor
	 * 
	 * @param mapper
	 *            The mapper to used
	 * @param converterProvider
	 *            The atomic type converter provider
	 * 
	 */
	public ObjectConverterImpl(Mapper mapper, AtomicTypeConverterProvider converterProvider) {
		this.mapper = mapper;
		this.atomicTypeConverterProvider = converterProvider;
		this.proxyManager = new ProxyManagerImpl();
		this.simpleFieldsHelp = new SimpleFieldsHelper(atomicTypeConverterProvider);
		this.requestObjectCache = new RequestObjectCacheImpl();
	}

	/**
	 * Constructor
	 * 
	 * @param mapper
	 *            The mapper to used
	 * @param converterProvider
	 *            The atomic type converter provider
	 * 
	 */
	public ObjectConverterImpl(Mapper mapper, AtomicTypeConverterProvider converterProvider, ProxyManager proxyManager, ObjectCache requestObjectCache) {
		this.mapper = mapper;
		this.atomicTypeConverterProvider = converterProvider;
		this.proxyManager = proxyManager;
		this.simpleFieldsHelp = new SimpleFieldsHelper(atomicTypeConverterProvider);
		this.requestObjectCache = requestObjectCache; 
	}	
	/**
	 * Set the <code>Mapper</code> used to solve mappings.
	 * 
	 * @param mapper
	 *            a <code>Mapper</code>
	 */
	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Sets the converter provider.
	 * 
	 * @param converterProvider
	 *            an <code>AtomicTypeConverterProvider</code>
	 */
	public void setAtomicTypeConverterProvider(AtomicTypeConverterProvider converterProvider) {
		this.atomicTypeConverterProvider = converterProvider;
	}

	/**
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#insert(javax.jcr.Session,
	 *      java.lang.Object)
	 */
	public void insert(Session session, Object object) {
		String path = this.getPath(session, object);
		try {
			String parentPath = RepositoryUtil.getParentPath(path);
			String nodeName = RepositoryUtil.getNodeName(path);
			Node parentNode = (Node) session.getItem(parentPath);
			this.insert(session, parentNode, nodeName, object);

		} catch (PathNotFoundException pnfe) {
			throw new ObjectContentManagerException("Impossible to insert the object at '" + path + "'", pnfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to insert the object at '" + path
					+ "'", re);
		}
	}

	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#insert(javax.jcr.Session,
	 *      javax.jcr.Node, java.lang.String, java.lang.Object)
	 */
	public void insert(Session session, Node parentNode, String nodeName, Object object) {
		ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(object.getClass());

		String jcrNodeType = classDescriptor.getJcrNodeType();
		if ((jcrNodeType == null) || jcrNodeType.equals("")) {
			jcrNodeType = ManagerConstant.NT_UNSTRUCTURED;
		}

		Node objectNode = null;
		try {
			objectNode = parentNode.addNode(nodeName, jcrNodeType);

		} catch (NoSuchNodeTypeException nsnte) {
			throw new JcrMappingException("Unknown node type " + jcrNodeType + " for mapped class " + object.getClass(), nsnte);
		} catch (RepositoryException re) {
			throw new ObjectContentManagerException("Cannot create new node of type " + jcrNodeType + " from mapped class "
					+ object.getClass(), re);
		}

		String[] mixinTypes = classDescriptor.getJcrMixinTypes();
		String mixinTypeName = null;
		try {

			// Add mixin types
			if (null != classDescriptor.getJcrMixinTypes()) {
				for (int i = 0; i < mixinTypes.length; i++) {
					mixinTypeName = mixinTypes[i].trim();
					objectNode.addMixin(mixinTypeName);
				}
			}

			// Add mixin types defined in the associated interfaces 
			if (!classDescriptor.hasDiscriminator() && classDescriptor.hasInterfaces()) {
				Iterator interfacesIterator = classDescriptor.getImplements().iterator();
				while (interfacesIterator.hasNext()) {
					String interfaceName = (String) interfacesIterator.next();
					ClassDescriptor interfaceDescriptor = mapper
							.getClassDescriptorByClass(ReflectionUtils.forName(interfaceName));
					objectNode.addMixin(interfaceDescriptor.getJcrNodeType().trim());
				}
			}

			// If required, add the discriminator node type 
			if (classDescriptor.hasDiscriminator()) {
				mixinTypeName = ManagerConstant.DISCRIMINATOR_NODE_TYPE;
				objectNode.addMixin(mixinTypeName);
				objectNode.setProperty(ManagerConstant.DISCRIMINATOR_PROPERTY_NAME, ReflectionUtils.getBeanClass(object)
						.getName());
			}
					
			
		} catch (NoSuchNodeTypeException nsnte) {
			throw new JcrMappingException("Unknown mixin type " + mixinTypeName + " for mapped class " + object.getClass(), nsnte);
		} catch (RepositoryException re) {
			throw new ObjectContentManagerException("Cannot create new node of type " + jcrNodeType + " from mapped class "
					+ object.getClass(), re);
		}

		simpleFieldsHelp.storeSimpleFields(session, object, classDescriptor, objectNode);
		insertBeanFields(session, object, classDescriptor, objectNode);
		insertCollectionFields(session, object, classDescriptor, objectNode);
	}

	/**
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#update(javax.jcr.Session,
	 *      java.lang.Object)
	 */
	public void update(Session session, Object object) {
		String path = this.getPath(session, object);
		try {
			String parentPath = RepositoryUtil.getParentPath(path);
			String nodeName = RepositoryUtil.getNodeName(path);
			Node parentNode = (Node) session.getItem(parentPath);
			this.update(session, parentNode, nodeName, object);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectContentManagerException("Impossible to update the object at '" + path + "'", pnfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to update the object at '" + path
					+ "'", re);
		}
	}

	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#update(javax.jcr.Session,
	 *      javax.jcr.Node, java.lang.String, java.lang.Object)
	 */
	public void update(Session session, Node parentNode, String nodeName, Object object) {
		try {
			ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.getBeanClass(object));
			Node objectNode = parentNode.getNode(nodeName);

			checkNodeType(session, classDescriptor);

			checkCompatiblePrimaryNodeTypes(session, objectNode, classDescriptor, false);

			simpleFieldsHelp.storeSimpleFields(session, object, classDescriptor, objectNode);
			updateBeanFields(session, object, classDescriptor, objectNode);
			updateCollectionFields(session, object, classDescriptor, objectNode);
		} catch (PathNotFoundException pnfe) {
			throw new ObjectContentManagerException("Impossible to update the object: " + nodeName + " at node : " + parentNode, pnfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to update the object: "
					+ nodeName + " at node : " + parentNode, re);
		}
	}

	/**
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#getObject(javax.jcr.Session,
	 *      java.lang.Class, java.lang.String)
	 */
	public Object getObject(Session session, String path) {
		try {
			if (!session.itemExists(path)) {
				return null;
			}

			if (requestObjectCache.isCached(path))
		    {
		        return requestObjectCache.getObject(path);	  
		    }			
			
			ClassDescriptor classDescriptor = null;
			Node node = (Node) session.getItem(path);
			if (node.hasProperty(ManagerConstant.DISCRIMINATOR_PROPERTY_NAME)) {
				String className = node.getProperty(ManagerConstant.DISCRIMINATOR_PROPERTY_NAME).getValue().getString();
				classDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(className));
			} else {
				String nodeType = node.getPrimaryNodeType().getName();
				if (nodeType.equals(ManagerConstant.FROZEN_NODE_TYPE)) {
					nodeType = node.getProperty(ManagerConstant.FROZEN_PRIMARY_TYPE_PROPERTY).getString();
				}
				classDescriptor = mapper.getClassDescriptorByNodeType(nodeType);
			}

			if (null == classDescriptor) {
				throw new JcrMappingException("Impossible to find the classdescriptor for " + path
						+ ". There is no discriminator and associated  JCR node type");
			}

			Object object = ReflectionUtils.newInstance(classDescriptor.getClassName());
			
            if (! requestObjectCache.isCached(path))
            {
			  requestObjectCache.cache(path, object);
            }

			simpleFieldsHelp.retrieveSimpleFields(session, classDescriptor, node, object);				
			retrieveBeanFields(session, classDescriptor, node, path, object, false);
			retrieveCollectionFields(session, classDescriptor, node, object, false);
			
			return object;
		
		} catch (PathNotFoundException pnfe) {
			// HINT should never get here
			throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
		}
	}



	/**
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#getObject(javax.jcr.Session,
	 *      java.lang.Class, java.lang.String)
	 */
	public Object getObject(Session session, Class clazz, String path) 
	{
		try {
			if (!session.itemExists(path)) {
				return null;
			}
			
			if (requestObjectCache.isCached(path))
		    {
		        return requestObjectCache.getObject(path);	  
		    }			

			ClassDescriptor classDescriptor = getClassDescriptor(clazz);

			checkNodeType(session, classDescriptor);

			Node node = (Node) session.getItem(path);
			if (!classDescriptor.isInterface()) {
				checkCompatiblePrimaryNodeTypes(session, node, classDescriptor, true);
			}

			Object object = null;
			if (classDescriptor.usesNodeTypePerHierarchyStrategy()) {
				if (!node.hasProperty(ManagerConstant.DISCRIMINATOR_PROPERTY_NAME)) {
					throw new ObjectContentManagerException("Cannot fetch object of type '" + clazz.getName()
							+ "' using NODETYPE_PER_HIERARCHY. Discriminator property is not present.");
				}

				String className = node.getProperty(ManagerConstant.DISCRIMINATOR_PROPERTY_NAME).getValue().getString();
				classDescriptor = getClassDescriptor(ReflectionUtils.forName(className));
				object = ReflectionUtils.newInstance(className);
			} else {
				if (classDescriptor.usesNodeTypePerConcreteClassStrategy()) {
					String nodeType = node.getPrimaryNodeType().getName();
					if (!nodeType.equals(classDescriptor.getJcrNodeType())) {
						classDescriptor = classDescriptor.getDescendantClassDescriptor(nodeType);
					}
				}
				object = ReflectionUtils.newInstance(classDescriptor.getClassName());

			}
			
            if (! requestObjectCache.isCached(path))
            {
			  requestObjectCache.cache(path, object);
            }
			
            simpleFieldsHelp.retrieveSimpleFields(session, classDescriptor, node, object);			
			retrieveBeanFields(session, classDescriptor, node, path, object, false);
			retrieveCollectionFields(session, classDescriptor, node, object, false);
			
			return object;
		} catch (PathNotFoundException pnfe) {
			// HINT should never get here
			throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
		}
	}

	public void retrieveAllMappedAttributes(Session session, Object object) {
		String path = null;
		try {
			ClassDescriptor classDescriptor = getClassDescriptor(object.getClass());
			String pathFieldName = classDescriptor.getPathFieldDescriptor().getFieldName();
			path = (String) ReflectionUtils.getNestedProperty(object, pathFieldName);
			Node node = (Node) session.getItem(path);
			retrieveBeanFields(session, classDescriptor, node, path, object, true);
			retrieveCollectionFields(session, classDescriptor, node, object, true);
			
		} catch (PathNotFoundException pnfe) {

			throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
		}
	}

	public void retrieveMappedAttribute(Session session, Object object, String attributeName) {
		String path = null;
		ClassDescriptor classDescriptor = null;
		try {
			classDescriptor = getClassDescriptor(object.getClass());
			String pathFieldName = classDescriptor.getPathFieldDescriptor().getFieldName();
			path = (String) ReflectionUtils.getNestedProperty(object, pathFieldName);
			Node node = (Node) session.getItem(path);
			BeanDescriptor beanDescriptor = classDescriptor.getBeanDescriptor(attributeName);
			if (beanDescriptor != null)
			{
				this.retrieveBeanField(session, beanDescriptor, node, path, object, true);				
			}
			// Check if the attribute is a collection
			else
			{
				CollectionDescriptor collectionDescriptor = classDescriptor.getCollectionDescriptor(attributeName);
				if (collectionDescriptor != null)
				{
					this.retrieveCollectionField(session, collectionDescriptor, node, object, true);
				}
				else
				{
					throw new ObjectContentManagerException("Impossible to retrieve the mapped attribute. The attribute '" + 
							                                                         attributeName + "'  is not a bean or a collection for the class : " + classDescriptor.getClassName());
				}
			}
			
		} catch (PathNotFoundException pnfe) {

			throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
		}
	}

	/**
	 * Validates the node type used by the class descriptor.
	 * 
	 * @param session
	 *            the current session
	 * @param classDescriptor
	 *            descriptor
	 * @throws JcrMappingException
	 *             thrown if the node type is unknown
	 * @throws org.apache.jackrabbit.ocm.exception.RepositoryException
	 *             thrown if an error occured in the underlying repository
	 */
	private void checkNodeType(Session session, ClassDescriptor classDescriptor) {
		String jcrTypeName = null;
		try {

			//Don't check the primary node type for interfaces. They are only associated to mixin node type
			if (classDescriptor.isInterface()) {
				String[] mixinTypes = classDescriptor.getJcrMixinTypes();
				for (int i = 0; i < mixinTypes.length; i++) {
					jcrTypeName = mixinTypes[i];
					session.getWorkspace().getNodeTypeManager().getNodeType(jcrTypeName);
				}
			} else {
				jcrTypeName = classDescriptor.getJcrNodeType();
				if (jcrTypeName != null && !jcrTypeName.equals("")) {
					session.getWorkspace().getNodeTypeManager().getNodeType(jcrTypeName);
				}
			}
		} catch (NoSuchNodeTypeException nsnte) {
			throw new JcrMappingException("Mapping for class '" + classDescriptor.getClassName()
					+ "' use unknown primary or mixin node type '" + jcrTypeName + "'");
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException(re);
		}
	}

	/**
	 * Checks if the node type in the class descriptor is compatible with the
	 * specified node node type.
	 * 
	 * @param session
	 *            the current session
	 * @param node
	 *            node against whose node type the compatibility is checked
	 * @param classDescriptor
	 *            class descriptor
	 * @param checkVersionNode
	 *            <tt>true</tt> if the check should continue in case the
	 *            <tt>node</tt> is a version node, <tt>false</tt> if no
	 *            check against version node should be performed
	 * 
	 * @throws ObjectContentManagerException
	 *             thrown if node types are incompatible
	 * @throws org.apache.jackrabbit.ocm.exception.RepositoryException
	 *             thrown if an error occured in the underlying repository
	 */
	private void checkCompatiblePrimaryNodeTypes(Session session, Node node, ClassDescriptor classDescriptor,
			boolean checkVersionNode) {
		try {
			NodeType nodeType = node.getPrimaryNodeType();

			boolean compatible = checkCompatibleNodeTypes(nodeType, classDescriptor);

			if (!compatible && checkVersionNode && ManagerConstant.FROZEN_NODE_TYPE.equals(nodeType.getName())) {
				NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
				nodeType = ntMgr.getNodeType(node.getProperty(ManagerConstant.FROZEN_PRIMARY_TYPE_PROPERTY).getString());

				compatible = checkCompatibleNodeTypes(nodeType, classDescriptor);
			}

			if (!compatible) {
				throw new ObjectContentManagerException("Cannot map object of type '" + classDescriptor.getClassName() + "'. Node type '"
						+ node.getPrimaryNodeType().getName() + "' does not match descriptor node type '"
						+ classDescriptor.getJcrNodeType() + "'");
			}
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException(re);
		}
	}

	/**
	 * Node types compatibility check.
	 * 
	 * @param nodeType
	 *            target node type
	 * @param descriptor
	 *            descriptor containing source node type
	 * @return <tt>true</tt> if nodes are considered compatible,
	 *         <tt>false</tt> otherwise
	 */
	private boolean checkCompatibleNodeTypes(NodeType nodeType, ClassDescriptor descriptor) {

		//return true if node type is not used
		if (descriptor.getJcrNodeType() == null || descriptor.getJcrNodeType().equals("")) {
			return true;
		}

		if (nodeType.getName().equals(descriptor.getJcrNodeType())) {
			return true;
		}

		NodeType[] superTypes = nodeType.getSupertypes();
		for (int i = 0; i < superTypes.length; i++) {
			if (superTypes[i].getName().equals(descriptor.getJcrNodeType())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#getPath(javax.jcr.Session,
	 *      java.lang.Object)
	 * @throws JcrMappingException
	 */
	public String getPath(Session session, Object object) {
		ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(object.getClass());

		final FieldDescriptor pathFieldDescriptor = classDescriptor.getPathFieldDescriptor();
		if (pathFieldDescriptor == null) {
			throw new JcrMappingException(
					"Class of type: "
							+ object.getClass().getName()
							+ " has no path mapping. Maybe attribute path=\"true\" for a field element of this class in jcrmapping.xml is missing?");
		}
		String pathField = pathFieldDescriptor.getFieldName();

		return (String) ReflectionUtils.getNestedProperty(object, pathField);
	}

	/**
	 * Retrieve bean fields
	 */
	private void retrieveBeanFields(Session session, ClassDescriptor classDescriptor, Node node, String path, Object object,
			boolean forceToRetrieve) {
		Iterator beanDescriptorIterator = classDescriptor.getBeanDescriptors().iterator();
		while (beanDescriptorIterator.hasNext()) {
			BeanDescriptor beanDescriptor = (BeanDescriptor) beanDescriptorIterator.next();
			this.retrieveBeanField(session, beanDescriptor, node, path, object, forceToRetrieve);
		}
	}

	
	private void retrieveBeanField(Session session,BeanDescriptor beanDescriptor, Node node, String path, Object object, boolean forceToRetrieve )
	{
		if (!beanDescriptor.isAutoRetrieve() && !forceToRetrieve) 
		{
			return;
		}
		

		String beanName = beanDescriptor.getFieldName();
		String beanPath = ObjectContentManagerUtil.getPath(session, beanDescriptor, node);
	    
		Object bean = null;
		if (requestObjectCache.isCached(beanPath))
	    {
	        bean = requestObjectCache.getObject(beanPath);	
	    	ReflectionUtils.setNestedProperty(object, beanName, bean);  	
	    }
		else 
		{
			Class beanClass = ReflectionUtils.getPropertyType(object, beanName);
			
			
			String converterClassName = null;		
			if (null == beanDescriptor.getConverter() || "".equals(beanDescriptor.getConverter())) 
			{
				converterClassName = DEFAULT_BEAN_CONVERTER;
			}
			else
			{
				converterClassName = beanDescriptor.getConverter();
			}
						
			Object[] param = {this.mapper, this, this.atomicTypeConverterProvider};			
			BeanConverter beanConverter = (BeanConverter) ReflectionUtils.invokeConstructor(converterClassName, param);
			if (beanDescriptor.isProxy()) 
			{
				bean = proxyManager.createBeanProxy(session, this, beanClass, beanConverter.getPath(session, beanDescriptor, node));
			} 
			else
			{
				bean = beanConverter.getObject(session, node, beanDescriptor,  mapper.getClassDescriptorByClass(beanClass), beanClass, bean);
			}			
			requestObjectCache.cache(beanPath, bean);			
			ReflectionUtils.setNestedProperty(object, beanName, bean);
		}
	}
	
	
	
	/**
	 * Retrieve Collection fields
	 */
	private void retrieveCollectionFields(Session session, ClassDescriptor classDescriptor, Node parentNode, Object object,
			boolean forceToRetrieve) {
		Iterator collectionDescriptorIterator = classDescriptor.getCollectionDescriptors().iterator();
		while (collectionDescriptorIterator.hasNext()) {
			CollectionDescriptor collectionDescriptor = (CollectionDescriptor) collectionDescriptorIterator.next();
			this.retrieveCollectionField(session, collectionDescriptor, parentNode, object, forceToRetrieve);			
		}
	}

	private void retrieveCollectionField(Session session, CollectionDescriptor collectionDescriptor, Node parentNode, Object object, boolean forceToRetrieve)
	{
		if (!collectionDescriptor.isAutoRetrieve() && !forceToRetrieve) {
			return;
		}

		CollectionConverter collectionConverter = this.getCollectionConverter(session, collectionDescriptor);
		Class collectionFieldClass = ReflectionUtils.getPropertyType(object, collectionDescriptor.getFieldName());
		ManageableCollection collection = null;
		if (collectionDescriptor.isProxy()) {
			collection = (ManageableCollection) proxyManager.createCollectionProxy(session, collectionConverter, parentNode,
					collectionDescriptor, collectionFieldClass);

		} else {
			collection = collectionConverter.getCollection(session, parentNode, collectionDescriptor, collectionFieldClass);
		}

		ReflectionUtils.setNestedProperty(object, collectionDescriptor.getFieldName(), collection);		
	}
	
	/**
	 * Insert Bean fields
	 */
	private void insertBeanFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
		Iterator beanDescriptorIterator = classDescriptor.getBeanDescriptors().iterator();
		while (beanDescriptorIterator.hasNext()) {
			BeanDescriptor beanDescriptor = (BeanDescriptor) beanDescriptorIterator.next();

			if (!beanDescriptor.isAutoInsert()) {
				continue;
			}

			String jcrName = beanDescriptor.getJcrName();
			Object bean = ReflectionUtils.getNestedProperty(object, beanDescriptor.getFieldName());
			if (bean != null) 
			{
				String converterClassName = null;
				
				if (null == beanDescriptor.getConverter() || "".equals(beanDescriptor.getConverter())) 
				{
					converterClassName = DEFAULT_BEAN_CONVERTER;
				}
				else
				{
					converterClassName = beanDescriptor.getConverter();
				}
                
				Object[] param = {this.mapper, this, this.atomicTypeConverterProvider};
				BeanConverter beanConverter = (BeanConverter) ReflectionUtils.invokeConstructor(converterClassName, param);
				beanConverter.insert(session, objectNode, beanDescriptor, mapper.getClassDescriptorByClass(bean.getClass()), bean, classDescriptor, object);
			}
		}
	}

	/**
	 * Update Bean fields
	 */
	private void updateBeanFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
		String jcrName = null;
		Iterator beanDescriptorIterator = classDescriptor.getBeanDescriptors().iterator();
		while (beanDescriptorIterator.hasNext()) 
		{
			BeanDescriptor beanDescriptor = (BeanDescriptor) beanDescriptorIterator.next();
			if (!beanDescriptor.isAutoUpdate()) {
				continue;
			}

			jcrName = beanDescriptor.getJcrName();
			Object bean = ReflectionUtils.getNestedProperty(object, beanDescriptor.getFieldName());
			
			String converterClassName = null;
			if (null == beanDescriptor.getConverter() || "".equals(beanDescriptor.getConverter())) 
			{
				converterClassName = DEFAULT_BEAN_CONVERTER;
			} 
			else 
			{
				converterClassName = beanDescriptor.getConverter();
			}
			
			Object[] param = {this.mapper, this, this.atomicTypeConverterProvider };
			BeanConverter beanConverter = (BeanConverter) ReflectionUtils.invokeConstructor(converterClassName, param);
			Class beanClass = ReflectionUtils.getPropertyType(object, beanDescriptor.getFieldName());
			// if the bean is null, remove existing node
			if ((bean == null)) 
			{
				
				beanConverter.remove(session, objectNode, beanDescriptor, mapper.getClassDescriptorByClass(beanClass), bean, classDescriptor, object);

			} else 
			{
				beanConverter.update(session, objectNode, beanDescriptor, mapper.getClassDescriptorByClass(beanClass), bean, classDescriptor, object);
			}

		}
	}

	private void insertCollectionFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
		Iterator collectionDescriptorIterator = classDescriptor.getCollectionDescriptors().iterator();

		while (collectionDescriptorIterator.hasNext()) {
			CollectionDescriptor collectionDescriptor = (CollectionDescriptor) collectionDescriptorIterator.next();

			if (!collectionDescriptor.isAutoInsert()) {
				continue;
			}

			CollectionConverter collectionConverter = this.getCollectionConverter(session, collectionDescriptor);
			Object collection = ReflectionUtils.getNestedProperty(object, collectionDescriptor.getFieldName());
			ManageableCollection manageableCollection = ManageableCollectionUtil.getManageableCollection(collection);

			collectionConverter.insertCollection(session, objectNode, collectionDescriptor, manageableCollection);
		}
	}

	private void updateCollectionFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
		Iterator collectionDescriptorIterator = classDescriptor.getCollectionDescriptors().iterator();

		while (collectionDescriptorIterator.hasNext()) {
			CollectionDescriptor collectionDescriptor = (CollectionDescriptor) collectionDescriptorIterator.next();
			if (!collectionDescriptor.isAutoUpdate()) {
				continue;
			}

			CollectionConverter collectionConverter = this.getCollectionConverter(session, collectionDescriptor);
			Object collection = ReflectionUtils.getNestedProperty(object, collectionDescriptor.getFieldName());
			ManageableCollection manageableCollection = ManageableCollectionUtil.getManageableCollection(collection);

			collectionConverter.updateCollection(session, objectNode, collectionDescriptor, manageableCollection);
		}
	}


	private CollectionConverter getCollectionConverter(Session session, CollectionDescriptor collectionDescriptor) {
		String className = collectionDescriptor.getCollectionConverter();
		Map atomicTypeConverters = this.atomicTypeConverterProvider.getAtomicTypeConverters();
		if (className == null) {
			return new DefaultCollectionConverterImpl(atomicTypeConverters, this, this.mapper);
		} else {
			return (CollectionConverter) ReflectionUtils.invokeConstructor(className, new Object[]{atomicTypeConverters, this,
					this.mapper});
		}

	}

	private ClassDescriptor getClassDescriptor(Class beanClass) {
		ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(beanClass);
		if (null == classDescriptor) {
			throw new JcrMappingException("Class of type: " + beanClass.getName()
					+ " is not JCR persistable. Maybe element 'class-descriptor' for this type in mapping file is missing");
		}

		return classDescriptor;
	}

}
