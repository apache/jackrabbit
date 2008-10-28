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
package org.apache.jackrabbit.ocm.nodemanagement.impl.jackrabbit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ChildNodeDefDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.PropertyDefDescriptor;
import org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager;
import org.apache.jackrabbit.ocm.nodemanagement.exception.NamespaceCreationException;
import org.apache.jackrabbit.ocm.nodemanagement.exception.NodeTypeCreationException;
import org.apache.jackrabbit.ocm.nodemanagement.exception.NodeTypeRemovalException;
import org.apache.jackrabbit.ocm.nodemanagement.exception.OperationNotSupportedException;
import org.apache.jackrabbit.spi.Name;

/** This is the NodeTypeManager implementation for Apache Jackrabbit.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class NodeTypeManagerImpl implements NodeTypeManager
{
    /**
     * Logging.
     */
    private static Log log = LogFactory.getLog(NodeTypeManagerImpl.class);

    /** Namespace helper class for Jackrabbit.
     */
    private NamespaceHelper namespaceHelper = new NamespaceHelper();

    /** Creates a new instance of NodeTypeManagerImpl. */
    public NodeTypeManagerImpl()
    {
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createNamespace
     */
    public void createNamespace(Session session, String namespace, String namespaceUri)
    throws NamespaceCreationException
    {
        if (session != null)
        {
            try
            {
                session.getWorkspace().getNamespaceRegistry().registerNamespace(namespace, namespaceUri);
                log.info("Namespace created: " +
                        "{" + namespaceUri + "}" + namespace);
            }
            catch (Exception e)
            {
                throw new NamespaceCreationException(e);
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createNodeTypes
     */
    public void createNodeTypes(Session session, MappingDescriptor mappingDescriptor)
    throws NodeTypeCreationException
    {
    	if (mappingDescriptor != null && mappingDescriptor.getClassDescriptorsByClassName().size() > 0)
        {

        }
        else
        {
            throw new NodeTypeCreationException("The MappingDescriptor can't be null or empty.");
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createNodeTypes
     */
    public void createNodeTypes(Session session, ClassDescriptor[] classDescriptors)
    throws NodeTypeCreationException
    {
        if (classDescriptors != null && classDescriptors.length > 0)
        {
            log.info("Trying to create " + classDescriptors.length +
                    " JCR node types.");
            for (int i = 0; i < classDescriptors.length; i++)
            {
                createSingleNodeType(session, classDescriptors[i]);
            }
        }
        else
        {
            throw new NodeTypeCreationException("The ClassDescriptor can't be null or empty.");
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createNodeTypesFromMappingFiles
     */
    public void createNodeTypesFromMappingFiles(Session session,
            InputStream[] mappingXmlFiles)
            throws NodeTypeCreationException
    {
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createSingleNodeType
     */
    public void createSingleNodeType(Session session, ClassDescriptor classDescriptor)
    throws NodeTypeCreationException
    {
        try
        {
            getNamespaceHelper().setRegistry(session.getWorkspace().getNamespaceRegistry());
            ArrayList list = new ArrayList();

            if (classDescriptor.getJcrType() != null &&
                    (classDescriptor.getJcrType().startsWith("nt:")
                    || classDescriptor.getJcrType().startsWith("mix:")))
            {
                throw new NodeTypeCreationException("Namespace nt and mix are reserved namespaces. Please specify your own.");
            }

            if (checkSuperTypes(session.getWorkspace().getNodeTypeManager(),
                    classDescriptor.getJcrSuperTypes()))
            {
                NodeTypeDef nodeTypeDef = getNodeTypeDef(classDescriptor.getJcrType(),
                        classDescriptor.getJcrSuperTypes(),
                        classDescriptor.getClassName());

                List propDefs = new ArrayList();
                List nodeDefs = new ArrayList();
                if (classDescriptor.getFieldDescriptors() != null)
                {
                    Iterator fieldIterator = classDescriptor.getFieldDescriptors().iterator();
                    while (fieldIterator.hasNext())
                    {
                        FieldDescriptor field = (FieldDescriptor) fieldIterator.next();
                        if (!field.isPath()) {
                            propDefs.add(getPropertyDefinition(field.getFieldName(), field, nodeTypeDef.getName()));
                        }
                    }
                }

                if (classDescriptor.getBeanDescriptors() != null) {
                    Iterator beanIterator = classDescriptor.getBeanDescriptors().iterator();
                    while (beanIterator.hasNext()) {
                        BeanDescriptor field = (BeanDescriptor) beanIterator.next();
                        if (this.isPropertyType(field.getJcrType())) {
                            propDefs.add(getPropertyDefinition(field.getFieldName(), field, nodeTypeDef.getName()));
                        } else {
                            nodeDefs.add(getNodeDefinition(field.getFieldName(), field, nodeTypeDef.getName()));
                        }
                    }
                }

                if (classDescriptor.getCollectionDescriptors() != null) {
                    Iterator collectionIterator = classDescriptor.getCollectionDescriptors().iterator();
                    while (collectionIterator.hasNext()) {
                        CollectionDescriptor field = (CollectionDescriptor) collectionIterator.next();
                        if (this.isPropertyType(field.getJcrType())) {
                            propDefs.add(getPropertyDefinition(field.getFieldName(), field, nodeTypeDef.getName()));
                        } else {
                            nodeDefs.add(getNodeDefinition(field.getFieldName(), field, nodeTypeDef.getName()));
                        }
                    }
                }

                nodeTypeDef.setPropertyDefs((PropDef[]) propDefs.toArray(new PropDef[propDefs.size()]));
                nodeTypeDef.setChildNodeDefs((NodeDef[]) nodeDefs.toArray(new NodeDef[nodeDefs.size()]));

                list.add(nodeTypeDef);
                createNodeTypesFromList(session, list);
                log.info("Registered JCR node type '" + nodeTypeDef.getName() +
                        "' for class '" + classDescriptor.getClassName() + "'");
            }
            else
            {
                throw new NodeTypeCreationException("JCR supertypes could not be resolved.");
            }
        }
        catch (Exception e)
        {
            log.error("Could not create node types from class descriptor.", e);
            throw new NodeTypeCreationException(e);
        }
    }

    /** Checks if all JCR super types for a given node type exist.
     *
     * @param ntMgr NodeTypeManager
     * @param superTypes Comma separated String with JCR node types
     * @return true/false
     */
    private boolean checkSuperTypes(javax.jcr.nodetype.NodeTypeManager ntMgr,
            String superTypes)
    {
        boolean exists = true;

        if (superTypes != null && superTypes.length() > 0)
        {
            String[] superTypesArray = superTypes.split(",");
            log.debug("JCR super types found: " + superTypesArray.length);
            for (int i = 0; i < superTypesArray.length; i++)
            {
                try
                {
                    ntMgr.getNodeType(superTypesArray[i]);
                }
                catch (Exception e)
                {
                    log.error("JCR super type '" + superTypesArray[i] + "' does not exist!");
                    exists = false;
                    break;
                }
            }
        }

        return exists;
    }

    /** Creates a NodeTypeDef object.
     *
     * @param jcrNodeType Name of JCR node type
     * @param jcrSuperTypes JCR node super types
     * @return type
     */
    public NodeTypeDef getNodeTypeDef(String jcrNodeType, String jcrSuperTypes,
            String className)
    {
        NodeTypeDef type = new NodeTypeDef();
        type.setMixin(false);

        if (jcrNodeType != null && (! jcrNodeType.equals("")))
        {
            type.setName(getNamespaceHelper().getName(jcrNodeType));
        }
        else
        {
            type.setName(getNamespaceHelper().getName(className));
        }

        type.setSupertypes(getJcrSuperTypes(jcrSuperTypes));
        type.setPrimaryItemName(getNamespaceHelper().getName(jcrNodeType));
        return type;
    }

    /** Creates a PropDefImpl object.
     *
     * @param fieldName The name of the field
     * @param field property definition descriptor
     * @param declaringNodeType Node Type QName where the property belongs to
     * @return property
     */
    public PropDefImpl getPropertyDefinition(String fieldName,
            PropertyDefDescriptor field, Name declaringNodeType)
    {
        PropDefImpl property = new PropDefImpl();

        if (field.getJcrName() != null)
        {
            property.setName(getNamespaceHelper().getName(field.getJcrName()));
        	
        }
        else
        {
            property.setName(getNamespaceHelper().getName(fieldName));
        }

        if (field.getJcrType() != null)
        {
            property.setRequiredType(PropertyType.valueFromName(field.getJcrType()));
        }
        else
        {
            log.info("No property type set for " + property.getName() +
                    ". Setting 'String' type.");
            property.setRequiredType(PropertyType.valueFromName("String"));
        }

        property.setDeclaringNodeType(declaringNodeType);
        property.setAutoCreated(field.isJcrAutoCreated());
        property.setMandatory(field.isJcrMandatory());
        property.setMultiple(field.isJcrMultiple());

        if (field.getJcrOnParentVersion() != null &&
                field.getJcrOnParentVersion().length() > 0)
        {
            property.setOnParentVersion(OnParentVersionAction.valueFromName(field.getJcrOnParentVersion()));
        }

        property.setProtected(field.isJcrProtected());
        return property;
    }

    /** Creates a NodeDefImpl object.
     *
     * @param fieldName Name of the field
     * @param field child node definition descriptor
     * @param declaringNodeType Node Type QName where the chid node belongs to
     * @return child node definition
     */
    private NodeDefImpl getNodeDefinition(String fieldName,
        ChildNodeDefDescriptor field, Name declaringNodeType) {

        NodeDefImpl node = new NodeDefImpl();

        if (field.getJcrName() != null) {
            node.setName(getNamespaceHelper().getName(field.getJcrName()));
        } else {
            node.setName(getNamespaceHelper().getName("*"));
        }

        if (field.getJcrType() != null) {
            node.setRequiredPrimaryTypes(getJcrSuperTypes(field.getJcrType()));
        }

        node.setDeclaringNodeType(declaringNodeType);
        node.setAutoCreated(field.isJcrAutoCreated());
        node.setMandatory(field.isJcrMandatory());
        node.setAllowsSameNameSiblings(field.isJcrSameNameSiblings());
        node.setDefaultPrimaryType( getNamespaceHelper().getName( field.getDefaultPrimaryType() ) );

        if (field.getJcrOnParentVersion() != null
            && field.getJcrOnParentVersion().length() > 0) {
            node.setOnParentVersion(OnParentVersionAction.valueFromName(field.getJcrOnParentVersion()));
        }

        node.setProtected(field.isJcrProtected());
        return node;
    }

    /**
     *
     * @param propDef
     * @return
     */
    protected String showPropertyDefinition(PropertyDefinition propDef)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("----");
        sb.append("\nName: " + propDef.getName());
        sb.append("\nAutocreated: " + propDef.isAutoCreated());
        sb.append("\nMandatory: " + propDef.isMandatory());
        sb.append("\n----");
        return sb.toString();
    }

    /** Creates a QName array from a comma separated list of JCR super types in
     * a given String.
     *
     * @param superTypes JCR super types
     * @return qNameSuperTypes
     */
    public Name[] getJcrSuperTypes(String superTypes)
    {
    	Name[] nameSuperTypes = null;
        if (superTypes != null && superTypes.length() > 0)
        {
            String[] superTypesArray = superTypes.split(",");
            log.debug("JCR super types found: " + superTypesArray.length);
            nameSuperTypes = new Name[superTypesArray.length];
            for (int i = 0; i < superTypesArray.length; i++)
            {
                String superTypeName = superTypesArray[i].trim();
                nameSuperTypes[i] = getNamespaceHelper().getName(superTypeName);
                log.debug("Setting JCR super type: " + superTypeName);
            }
        }

        return nameSuperTypes;
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createSingleNodeTypeFromMappingFile
     */
    public void createSingleNodeTypeFromMappingFile(Session session,
            InputStream mappingXmlFile, String jcrNodeType)
            throws NodeTypeCreationException
    {
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createNodeTypeFromClass
     */
    public void createNodeTypeFromClass(Session session, Class clazz,
            String jcrNodeType, boolean reflectSuperClasses)
            throws NodeTypeCreationException
    {
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createNodeTypesFromConfiguration
     */
    public void createNodeTypesFromConfiguration(Session session,
            InputStream jcrRepositoryConfigurationFile)
            throws OperationNotSupportedException, NodeTypeCreationException
    {
        try
        {
            NodeTypeDef[] types = NodeTypeReader.read(jcrRepositoryConfigurationFile);

            ArrayList list = new ArrayList();
            for (int i = 0; i < types.length; i++)
            {
                list.add(types[i]);
            }

            createNodeTypesFromList(session, list);
            log.info("Registered " + list.size() + " nodetypes from xml configuration file.");
        }
        catch (Exception e)
        {
            log.error("Could not create node types from configuration file.", e);
            throw new NodeTypeCreationException(e);
        }
    }

    /**
     *
     * @param session
     * @param nodeTypes
     * @throws org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException
     * @throws javax.jcr.RepositoryException
     */
    private void createNodeTypesFromList(Session session, List nodeTypes)
    throws InvalidNodeTypeDefException, RepositoryException
    {
        getNodeTypeRegistry(session).registerNodeTypes(nodeTypes);
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#removeNodeTypes
     */
    public void removeNodeTypesFromConfiguration(Session session, InputStream jcrRepositoryConfigurationFile)
    throws NodeTypeRemovalException
    {
    	try
        {
            NodeTypeDef[] types = NodeTypeReader.read(jcrRepositoryConfigurationFile);

            ArrayList list = new ArrayList();
            for (int i = 0; i < types.length; i++)
            {
                list.add(types[i]);
            }

            removeNodeTypesFromList(session, list);
            log.info("Registered " + list.size() + " nodetypes from xml configuration file.");
        }
        catch (Exception e)
        {
            log.error("Could not create node types from configuration file.", e);
            throw new NodeTypeRemovalException(e);
        }
    }

    private void removeNodeTypesFromList(Session session, List nodeTypes)
    throws NodeTypeRemovalException
    {
        for (Iterator nodeTypeIterator = nodeTypes.iterator(); nodeTypeIterator.hasNext();)
        {
			NodeTypeDef nodeTypeDef = (NodeTypeDef) nodeTypeIterator.next();
			this.removeSingleNodeType(session, nodeTypeDef.getName());
			
		}
    	
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#createSingleNodeTypeFromMappingFile
     */
    public void removeNodeTypesFromMappingFile(Session session, InputStream[] mappingXmlFile)
            throws NodeTypeRemovalException
    {
    }

    public void removeSingleNodeType(Session session, Name name)
    throws NodeTypeRemovalException
    {
        try
        {
            getNodeTypeRegistry(session).unregisterNodeType(name);
        }
        catch (Exception e)
        {
            throw new NodeTypeRemovalException(e);
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#removeSingleNodeType
     */
    public void removeSingleNodeType(Session session, String jcrNodeType)
    throws NodeTypeRemovalException
    {
        try
        {
            getNodeTypeRegistry(session).unregisterNodeType(getNamespaceHelper().getName(jcrNodeType));
        }
        catch (Exception e)
        {
            throw new NodeTypeRemovalException(e);
        }
    }

    /** Returns the jackrabbit NodeTypeRegistry from an open session.
     *
     * @param session Repository session
     * @return nodeTypeRegistry
     */
    private NodeTypeRegistry getNodeTypeRegistry(Session session)
    throws RepositoryException
    {
        Workspace wsp = session.getWorkspace();
        javax.jcr.nodetype.NodeTypeManager ntMgr = wsp.getNodeTypeManager();
        NodeTypeRegistry ntReg =
                ((org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl) ntMgr).getNodeTypeRegistry();
        return ntReg;
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#getPrimaryNodeTypeNames
     */
    public List getPrimaryNodeTypeNames(Session session, String namespace)
    {
        return null;
    }

    /**
     * @see org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager#getAllPrimaryNodeTypeNames
     */
    public List getAllPrimaryNodeTypeNames(Session session)
    {
        return null;
    }

    /** Getter for property namespaceHelper.
     *
     * @return namespaceHelper
     */
    public NamespaceHelper getNamespaceHelper()
    {
        return namespaceHelper;
    }

    /** Setter for property namespaceHelper.
     *
     * @param object namespaceHelper
     */
    public void setNamespaceHelper(NamespaceHelper object)
    {
        this.namespaceHelper = object;
    }


    private boolean isPropertyType(String type)
    {
    	return (type.equals(PropertyType.TYPENAME_BINARY) ||
    	        type.equals(PropertyType.TYPENAME_BOOLEAN) ||
    	        type.equals(PropertyType.TYPENAME_DATE) ||
    	        type.equals(PropertyType.TYPENAME_DOUBLE) ||
    	        type.equals(PropertyType.TYPENAME_LONG) ||
    	        type.equals(PropertyType.TYPENAME_NAME) ||
    	        type.equals(PropertyType.TYPENAME_PATH) ||
    	        type.equals(PropertyType.TYPENAME_REFERENCE) ||
    	        type.equals(PropertyType.TYPENAME_STRING));    	
    }
}
