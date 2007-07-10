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
package org.apache.jackrabbit.ocm.annotation.mapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.annotation.Collection;
import org.apache.jackrabbit.ocm.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager;
import org.apache.jackrabbit.ocm.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the OCM mapper that builds class descriptors based on
 * the use of annotations on the underlying classes
 * 
 * @author Philip Dodds
 * 
 */
public class AnnotatedObjectMapper implements Mapper {

	/** namespace prefix constant */
	public static final String OCM_NAMESPACE_PREFIX = "ocm";

	/** namespace constant */
	public static final String OCM_NAMESPACE = "http://jackrabbit.apache.org/ocm";
	
	private static final String OCM_NAMESPACE_XML = "/org/apache/jackrabbit/ocm/annotation/ocm_nodetypes.xml";

	private static Logger log = LoggerFactory
			.getLogger(AnnotatedObjectMapper.class);

	private List<String> annotatedClassNames = new ArrayList<String>();

	private Map<Class, ClassDescriptor> descriptorMap = new HashMap<Class, ClassDescriptor>();

	private Map<String, ClassDescriptor> nodeTypeMap = new HashMap<String, ClassDescriptor>();

	public AnnotatedObjectMapper(Session session,
			List<String> annotatedClassNames) throws ClassNotFoundException {
		this.annotatedClassNames = annotatedClassNames;
		try {
			List<ClassDescriptor> classDescriptorsRequiringRegistration = buildClassDescriptors(session);
			registerNamespaces(session);
			registerNodeTypes(session);

			NodeTypeManager nodeTypeManager = new NodeTypeManagerImpl();

			if (!classDescriptorsRequiringRegistration.isEmpty()) {
				log.info("Registering "
						+ classDescriptorsRequiringRegistration.size()
						+ " missing node types");
				nodeTypeManager
						.createNodeTypes(
								session,
								(ClassDescriptor[]) classDescriptorsRequiringRegistration
										.toArray(new ClassDescriptor[0]));
			}
			else
			{
				log.warn("No class descriptor to register in the JCR repository");
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to register node types", e);
		}
	}

	private void registerNamespaces(Session session) throws RepositoryException {

		String[] jcrNamespaces = session.getWorkspace().getNamespaceRegistry()
				.getPrefixes();
		boolean createNamespace = true;
		for (int i = 0; i < jcrNamespaces.length; i++) {
			if (jcrNamespaces[i].equals(OCM_NAMESPACE_PREFIX)) {
				createNamespace = false;
				log.debug("Jackrabbit OCM namespace exists.");
			}
		}

		if (createNamespace) {
			session.getWorkspace().getNamespaceRegistry().registerNamespace(
					OCM_NAMESPACE_PREFIX, OCM_NAMESPACE);
			log.info("Successfully created Jackrabbit OCM namespace.");
		}

		for (ClassDescriptor descriptor : descriptorMap.values()) {
			Class<?> clazz;
			try {
				clazz = Class.forName(descriptor.getClassName());
				log.info("Registering Namespace "
						+ AnnotationHelper.getJcrNode(clazz).namespace()
						+ " with prefix "
						+ AnnotationHelper.getJcrNode(clazz).prefix());
				session.getWorkspace().getNamespaceRegistry()
						.registerNamespace(
								AnnotationHelper.getJcrNode(clazz).prefix(),
								AnnotationHelper.getJcrNode(clazz).namespace());
			} catch (NamespaceException e) {
				log.info("Unable to register namespace, " + e.getMessage());
			} catch (Exception e2) {
				throw new RuntimeException(e2);
			}
		}

	}

	private void registerNodeTypes(Session session)
	    throws InvalidNodeTypeDefException, javax.jcr.RepositoryException,
	    IOException {
	    	InputStream xml = getClass().getResourceAsStream(OCM_NAMESPACE_XML);
	    	
	    	if (xml == null) {
	    	    throw new FileNotFoundException(OCM_NAMESPACE_XML + " was not found in the classpath"); 
	    	}
        
        	// HINT: throws InvalidNodeTypeDefException, IOException
        	NodeTypeDef[] types = NodeTypeReader.read(xml);
        
        	Workspace workspace = session.getWorkspace();
        	javax.jcr.nodetype.NodeTypeManager ntMgr = workspace.getNodeTypeManager();
        	NodeTypeRegistry ntReg = ((org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl) ntMgr).getNodeTypeRegistry();
        
        	for (int j = 0; j < types.length; j++) {
        	    NodeTypeDef def = types[j];
        
        	    try {
        		ntReg.getNodeTypeDef(def.getName());
        	    } catch (NoSuchNodeTypeException nsne) {
        		// HINT: if not already registered than register custom node
        		// type
        		ntReg.registerNodeType(def);
        	    }
        
        	}
        }

	private List<ClassDescriptor> buildClassDescriptors(Session session)
			throws ClassNotFoundException {
		List<ClassDescriptor> classDescriptorsToRegister = new ArrayList<ClassDescriptor>();
		for (String className : getAnnotatedClassNames()) {
			Class clazz = Class.forName(className);
			ClassDescriptor classDescriptor = buildClassDescriptor(clazz);
			try {
				session.getWorkspace().getNodeTypeManager().getNodeType(
						classDescriptor.getJcrNodeType());
				log.info("Class " + className + " already registered");
			} catch (NoSuchNodeTypeException e) {
				log.info("Class " + className + " will be registered");
				classDescriptorsToRegister.add(classDescriptor);
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}

			descriptorMap.put(clazz, classDescriptor);
			nodeTypeMap.put(classDescriptor.getJcrNodeType(), classDescriptor);
		}

		return classDescriptorsToRegister;
	}

	private ClassDescriptor buildClassDescriptor(Class clazz)
			throws ClassNotFoundException {
		ClassDescriptor descriptor = new ClassDescriptor();
		descriptor.setClassName(clazz.getName());
		descriptor.setJcrNodeType(AnnotationHelper.getJcrNodeType(clazz));
		descriptor.setJcrSuperTypes("nt:base");
		descriptor.setInterface(clazz.isInterface());

		// TODO add mixin support
		// descriptor.setMixin(AnnotationHelper.getJcrNode(clazz).mixin());

		addFieldDescriptors(descriptor, clazz);
		addCollectionDescriptors(descriptor, clazz);
		return descriptor;
	}

	private void addCollectionDescriptors(ClassDescriptor descriptor,
			Class clazz) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		PropertyDescriptor[] propertyDescriptors = beanInfo
				.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Collection jcrChildNode = propertyDescriptor.getReadMethod()
					.getAnnotation(Collection.class);
			if (jcrChildNode != null) {
				Class targetClass = jcrChildNode.type();
				CollectionDescriptor collectionDescriptor = new CollectionDescriptor();
				ClassDescriptor classDescriptor = descriptorMap
						.get(targetClass);

				if (classDescriptor == null)
					throw new RuntimeException(
							"Unable to reference class "
									+ targetClass.getName()
									+ " as a child node since it has not been registered, ordering perhaps?");

				collectionDescriptor.setJcrName(AnnotationHelper
						.getJcrNodeType(targetClass));
				collectionDescriptor.setDefaultPrimaryType(AnnotationHelper
						.getJcrNodeType(targetClass));
				collectionDescriptor.setJcrSameNameSiblings(jcrChildNode
						.sameNameSiblings());
				collectionDescriptor.setJcrAutoCreated(jcrChildNode
						.autoCreate());
				collectionDescriptor.setJcrProtected(jcrChildNode.protect());
				collectionDescriptor.setJcrOnParentVersion(jcrChildNode
						.onParentVersion());
				collectionDescriptor.setJcrMandatory(jcrChildNode.mandatory());
				collectionDescriptor.setAutoInsert(jcrChildNode.autoInsert());
				collectionDescriptor.setAutoRetrieve(jcrChildNode
						.autoRetrieve());
				collectionDescriptor.setAutoUpdate(jcrChildNode.autoUpdate());
				collectionDescriptor.setCollectionClassName(propertyDescriptor
						.getReadMethod().getReturnType().getName());
				collectionDescriptor.setElementClassName(targetClass.getName());
				collectionDescriptor.setCollectionConverter(jcrChildNode.converter().getName());
				collectionDescriptor.setFieldName(propertyDescriptor.getName());

				collectionDescriptor.setJcrNodeType(AnnotationHelper
						.getJcrNodeType(targetClass));
				collectionDescriptor.setJcrSameNameSiblings(jcrChildNode
						.sameNameSiblings());
				collectionDescriptor.setProxy(jcrChildNode.proxy());

				descriptor.addCollectionDescriptor(collectionDescriptor);
			}
		}

	}

	private void addFieldDescriptors(ClassDescriptor descriptor, Class clazz) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Field jcrProperty = propertyDescriptor.getReadMethod().getAnnotation(Field.class);
			if (jcrProperty != null) {
				FieldDescriptor fieldDescriptor = new FieldDescriptor();
				PropertyUtils.getPropertyDescriptors(clazz);
				fieldDescriptor.setFieldName(propertyDescriptor.getName());
				fieldDescriptor.setPath(jcrProperty.path());
				fieldDescriptor.setId(jcrProperty.id());
				fieldDescriptor.setFieldType(propertyDescriptor.getReadMethod().getReturnType().getName());
				if ((jcrProperty.jcrName() != null) && (!jcrProperty.jcrName().equals("")))
				{
					fieldDescriptor.setJcrName(jcrProperty.jcrName());	
				}
				else
				{
					fieldDescriptor.setJcrName(propertyDescriptor.getName());	
				}
				
				fieldDescriptor.setJcrMandatory(jcrProperty.mandatory());
				fieldDescriptor.setJcrAutoCreated(jcrProperty.autoCreated());
				fieldDescriptor.setJcrMultiple(jcrProperty.mutiple());
				//fieldDescriptor.setJcrType(value)
				descriptor.addFieldDescriptor(fieldDescriptor);
			}
		}

	}

	public ClassDescriptor getClassDescriptorByClass(Class arg0) {
		return descriptorMap.get(arg0);
	}

	public ClassDescriptor getClassDescriptorByNodeType(String arg0) {
		return nodeTypeMap.get(arg0);
	}

	public List<String> getAnnotatedClassNames() {
		return annotatedClassNames;
	}

	public void setAnnotatedClassNames(List<String> annotatedClassNames) {
		this.annotatedClassNames = annotatedClassNames;
	}

}
