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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.NullTypeConverterImpl;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Helper class used to map simple fields.
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * 
 */
public class SimpleFieldsHelper
{

	private final static Log log = LogFactory.getLog(SimpleFieldsHelper.class);

	private static final AtomicTypeConverter NULL_CONVERTER = new NullTypeConverterImpl();

	private AtomicTypeConverterProvider atomicTypeConverterProvider;

	/**
	 * Constructor
	 * 
	 * @param converterProvider The atomic type converter provider
	 * 
	 */
	public SimpleFieldsHelper(AtomicTypeConverterProvider converterProvider) 
	{
		this.atomicTypeConverterProvider = converterProvider;
	}

	
	/**
	 * Retrieve simple fields (atomic fields)
	 * 
	 * @throws JcrMappingException
	 * @throws org.apache.jackrabbit.ocm.exception.RepositoryException
	 */
	public Object retrieveSimpleFields(Session session, ClassDescriptor classDescriptor, Node node, Object object) 
	{
		Object initializedBean = object;
		try {
			Iterator fieldDescriptorIterator = classDescriptor.getFieldDescriptors().iterator();

			if (classDescriptor.usesNodeTypePerHierarchyStrategy() && classDescriptor.hasDiscriminator()) 
			{
				if (!node.hasProperty(ManagerConstant.DISCRIMINATOR_PROPERTY_NAME)) 
				{
					throw new ObjectContentManagerException("Class '"
							+ classDescriptor.getClassName()
							+ "' has not a discriminator property.");
				}
			}			
			while (fieldDescriptorIterator.hasNext()) {
				FieldDescriptor fieldDescriptor = (FieldDescriptor) fieldDescriptorIterator.next();

				String fieldName = fieldDescriptor.getFieldName();
				String propertyName = fieldDescriptor.getJcrName();

				if (fieldDescriptor.isPath()) {
					// HINT: lazy initialize target bean - The bean can be null
					// when it is inline
					if (null == initializedBean) {
						initializedBean = ReflectionUtils.newInstance(classDescriptor.getClassName());
					}

					ReflectionUtils.setNestedProperty(initializedBean, fieldName, node.getPath());

				} else {
					if (fieldDescriptor.isUuid()) {
						if (null == initializedBean) {
							initializedBean = ReflectionUtils.newInstance(classDescriptor.getClassName());
						}

						ReflectionUtils.setNestedProperty(initializedBean, fieldName, node.getUUID());

					} else {
						initializedBean = retrieveSimpleField(classDescriptor, node, initializedBean, fieldDescriptor, fieldName, propertyName);
					}
				}

			}
		} catch (ValueFormatException vfe) {
			throw new ObjectContentManagerException(
					"Cannot retrieve properties of object " + object + " from node " + node, vfe);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException( "Cannot retrieve properties of object " + object
							+ " from node " + node, re);
		}

		return initializedBean;
	}


	private Object retrieveSimpleField(ClassDescriptor classDescriptor, Node node, Object initializedBean, FieldDescriptor fieldDescriptor, String fieldName, String propertyName) throws RepositoryException, ValueFormatException, PathNotFoundException {

	    Value propValue;
		if (node.hasProperty(propertyName)) 
		{
			propValue = node.getProperty(propertyName).getValue();
			
		} 
		else if (fieldDescriptor.getJcrDefaultValue() != null)
		{
		    ValueFactory vf = node.getSession().getValueFactory();
		    propValue = vf.createValue(fieldDescriptor.getJcrDefaultValue());
		}
		else
		{
		    PropertyDefinition propDef = getPropertyDefinition(node, propertyName);
		    
		    if (propDef != null && propDef.getDefaultValues() != null && propDef.getDefaultValues().length == 1)
		    {
                log.debug("retrieveSimpleField: Use default value from property definition for missing mapped property " + propertyName + " of class '" + classDescriptor.getClassName() + "'");
		        propValue = propDef.getDefaultValues()[0];
		    } else
		    {
                log.debug("retrieveSimpleField: No default value available for missing mapped property " + propertyName + " of class '" + classDescriptor.getClassName() + "'");
		        propValue = null;
		    }
		}
		
        // HINT: lazy initialize target bean - The bean can be null when it is inline
		if (initializedBean == null)
		{
		    
		    // if we do not have a value, we do nothing at all and just return null
		    if (propValue == null)
		    {
		        return null;
		    }
		    
		    // otherwise create the bean to set the value
		    initializedBean = ReflectionUtils.newInstance(classDescriptor.getClassName());
		}

        AtomicTypeConverter converter = getAtomicTypeConverter(fieldDescriptor, initializedBean, fieldName);
        Object fieldValue = (propValue != null) ? converter.getObject(propValue) : null;
        ReflectionUtils.setNestedProperty(initializedBean, fieldName, fieldValue);

        return initializedBean;
	}
	
	public void storeSimpleFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
		try {
			ValueFactory valueFactory = session.getValueFactory();

			Iterator fieldDescriptorIterator = classDescriptor.getFieldDescriptors().iterator();
			while (fieldDescriptorIterator.hasNext()) {
				FieldDescriptor fieldDescriptor = (FieldDescriptor) fieldDescriptorIterator.next();

				String fieldName = fieldDescriptor.getFieldName();
				String jcrName = fieldDescriptor.getJcrName();

				// Of course, Path && UUID fields are not stored as property
				if (fieldDescriptor.isPath() || fieldDescriptor.isUuid()) {
					continue;
				}

				storeSimpleField(object, objectNode, valueFactory, fieldDescriptor, fieldName, jcrName);
			}
		} catch (ValueFormatException vfe) {
			throw new ObjectContentManagerException("Cannot persist properties of object " + object + ". Value format exception.", vfe);
		} catch (VersionException ve) {
			throw new ObjectContentManagerException("Cannot persist properties of object " + object + ". Versioning exception.", ve);
		} catch (LockException le) {
			throw new ObjectContentManagerException("Cannot persist properties of object " + object + " on locked node.", le);
		} catch (ConstraintViolationException cve) {
			throw new ObjectContentManagerException("Cannot persist properties of object " + object + ". Constraint violation.", cve);
		} catch (RepositoryException re) {
			throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Cannot persist properties of object "
					+ object, re);
		}
	}


	private void storeSimpleField(Object object, Node objectNode, ValueFactory valueFactory, FieldDescriptor fieldDescriptor, String fieldName, String jcrName) throws RepositoryException, PathNotFoundException, ValueFormatException, VersionException, LockException, ConstraintViolationException {
		
		boolean protectedProperty = isProtectedProperty(objectNode, fieldDescriptor, jcrName);

		if (!protectedProperty) 
		{ // DO NOT TRY TO WRITE PROTECTED  PROPERTIES
			
			Object fieldValue = ReflectionUtils.getNestedProperty(object, fieldName);
			// if the value and if there is a default value for this field => set this default value
			String defaultValue = fieldDescriptor.getJcrDefaultValue();
			if ((fieldValue == null) && (defaultValue != null))
			{
				//Not sure that we have the attribute with the default value in all use cases				
				ReflectionUtils.setNestedProperty(object, fieldName, defaultValue);
				fieldValue = ReflectionUtils.getNestedProperty(object, fieldName);
				
			}
			AtomicTypeConverter converter = getAtomicTypeConverter(fieldDescriptor, object, fieldName);
			Value value = converter.getValue(valueFactory, fieldValue);
	
			checkProperty(objectNode, fieldDescriptor, value);
			objectNode.setProperty(jcrName, value);
		}			

	}


	private boolean isProtectedProperty(Node objectNode, FieldDescriptor fieldDescriptor, String jcrName) throws RepositoryException, PathNotFoundException 
	{
		// Return true if the property is defined as protected in the mapping file
		if (fieldDescriptor.isJcrProtected())
		{
			return true; 
		}

		// Check if the property is defined as protected in the JCR repo
		
		// 1. Check if the property already exists and may be queried
		if (objectNode.hasProperty(jcrName))
		{
		    return objectNode.getProperty(jcrName).getDefinition().isProtected();
		}
		    
		// 2. Find a definition for the property and checks its protected status
		PropertyDefinition definition = getPropertyDefinition(objectNode, jcrName);
		if (definition != null)
		{
		    return definition.isProtected();
		}
		
        // This property is not defined in one of the node types
		return false;		
	}
	
	private void checkProperty(Node objectNode, FieldDescriptor fieldDescriptor, Value value) throws RepositoryException {
	    if (value == null)
	    {
	        PropertyDefinition definition = getPropertyDefinition(objectNode, fieldDescriptor.getJcrName());
	        if (definition != null)
	        {
	            if (definition.isMandatory() && !definition.isAutoCreated())
	            {
	                throw new ObjectContentManagerException("Class of type:" + fieldDescriptor.getClassDescriptor().getClassName()
                        + " has property: " + fieldDescriptor.getFieldName() + " declared as JCR property: "
                        + fieldDescriptor.getJcrName() + " This property is mandatory but property in bean has value null");
	            }
	        }
	    }
	}	
	
	private AtomicTypeConverter getAtomicTypeConverter(FieldDescriptor fd, Object object, String fieldName) {
		Class fieldTypeClass = null;
		// Check if an atomic converter is assigned to the field converter
		String atomicTypeConverterClass = fd.getConverter();
		if (null != atomicTypeConverterClass)
		{
			return (AtomicTypeConverter) ReflectionUtils.newInstance(atomicTypeConverterClass);
		}
		else
		{
			// Get the default atomic converter in function of the classname
			 if (null != object) {
				fieldTypeClass = ReflectionUtils.getPropertyType(object, fieldName);
			}

			if (null != fieldTypeClass) {
				return this.atomicTypeConverterProvider.getAtomicTypeConverter(fieldTypeClass);
			} else {
				return NULL_CONVERTER;
			}
			
		}
	}
	
	/**
	 * Returns the <code>PropertyDefinition</code> for the name property in
	 * the given node type. If the node type has no matching node type
	 * definition <code>null</code> is returned.
	 * <p>
	 * This method scans as follows: If a PropertyDefinition with the exact
	 * name is found which is single-valued, this property definition is
	 * returned. Otherwise the first residual property definition which is
	 * single-valued is returned. Otherwise <code>null</code> is returned.
	 * 
	 * @param nodeType The <code>NodeType</code> to search for matching node
	 *         type definitions for the given property.
	 * @param propertyName The name of the property for which the
	 *         <code>PropertyDefinition</code> is requested.
	 *         
	 * @return The <code>PropertyDefinition</code> for the given property or
	 *         <code>null</code> if none can be found in the type.
	 */
	private PropertyDefinition getPropertyDefinition(NodeType nodeType, String propertyName) {
        PropertyDefinition[] pd = nodeType.getPropertyDefinitions();
        PropertyDefinition candidate = null;
        for (int i=0; i < pd.length; i++)
        {
            // ignore multi-value properties
            if (pd[i].isMultiple())
            {
                continue;
            }
            
            // if we have an exact match, use this and return
            if (propertyName.equals(pd[i].getName()))
            {
                return pd[i];
            }
            
            // if we have a residual property definition consider as candidate
            if (pd[i].getName() == null && candidate == null)
            {
                candidate = pd[i];
            }
        }
        
        // return the potential residal candidate definition
        return candidate;
	}

	
	/**
	 * Returns a <code>PropertyDefinition</code> for the given property name.
	 * This method first looks for a matching property definition in the
	 * primary node type and then in the list of mixin node types of the node.
	 * Only single-valued property definitions are considered by this method.
	 * If a definition whose name is the same as the <code>propertyName</code>
	 * is found, this definition is returned. Otherwise a residual property
	 * definition may be returned.
	 * 
	 * @param node The <code>Node</code> whose primary and mixin node types are
	 *         to be scanned for a single-valued property definition.
	 * @param propertyName The name of the property for which the property
	 *         definition is to be returned.
	 *         
	 * @return The <code>PropertyDefinition</code> for the named property or
	 *         <code>null</code> if no single-valued exact or residual property
	 *         definintion may be found in the node's primary or mixin node
	 *         types.
	 *         
	 * @throws RepositoryException If an error occurrs accessing the primary or
	 *         mixin node types of the node.
	 */
	private PropertyDefinition getPropertyDefinition(Node node, String propertyName) throws RepositoryException {
	    
	    // try to find the definition in the primary node type
        NodeType nt = node.getPrimaryNodeType();
        PropertyDefinition propDef = getPropertyDefinition(nt, propertyName);
        
        // return the definition if it is not residual
        if (propDef != null && propDef.getName() != null)
        {
            return propDef;
        }
        
        // otherwise look it up in any of the mixin node types
        NodeType[] mixins = node.getMixinNodeTypes();
        for (int i = 0; mixins != null && i < mixins.length; i++)
        {
            PropertyDefinition candidate = getPropertyDefinition(mixins[i], propertyName);
            
            // use this property definition if not residual
            if (candidate != null && candidate.getName() != null)
            {
                return propDef;
            }
            
            // otherwise use this if we do not have a candidate yet
            if (propDef == null)
            {
                propDef = candidate;
            }
        }

        // nothing found
        return propDef;
	}
}
