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
package org.apache.jackrabbit.ocm.nodemanagement.impl.jeceira;

import java.io.InputStream;
import java.util.List;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;
import org.apache.jackrabbit.ocm.nodemanagement.NodeTypeManager;
import org.apache.jackrabbit.ocm.nodemanagement.exception.NamespaceCreationException;
import org.apache.jackrabbit.ocm.nodemanagement.exception.NodeTypeCreationException;
import org.apache.jackrabbit.ocm.nodemanagement.exception.NodeTypeRemovalException;
import org.apache.jackrabbit.ocm.nodemanagement.exception.OperationNotSupportedException;

/** This is the NodeTypeManager implementation for Jeceira.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class NodeTypeManagerImpl implements NodeTypeManager {

    /**
     * Logging.
     */
    private static Log log = LogFactory.getLog(NodeTypeManagerImpl.class);

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
    }

    public void createNodeTypes(Session session, MappingDescriptor mappingDescriptor)
    throws NodeTypeCreationException
    {
    }

    public void createNodeTypes(Session session, ClassDescriptor[] classDescriptors)
    throws NodeTypeCreationException
    {
    }

    public void createNodeTypesFromMappingFiles(Session session,
            InputStream[] mappingXmlFiles)
            throws NodeTypeCreationException
    {
    }

    public void createSingleNodeType(Session session, ClassDescriptor classDescriptor)
    throws NodeTypeCreationException
    {
    }

    public void createSingleNodeTypeFromMappingFile(Session session,
            InputStream mappingXmlFile, String jcrNodeType)
            throws NodeTypeCreationException
    {
    }

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
    }

    public void removeNodeTypes(Session session, InputStream[] mappingXmlFiles)
    throws NodeTypeRemovalException
    {
    }

    public void removeSingleNodeType(Session session, String jcrNodeType)
    throws NodeTypeRemovalException
    {
    }

    public List getPrimaryNodeTypeNames(Session session, String namespace)
    {
        return null;
    }

    public List getAllPrimaryNodeTypeNames(Session session)
    {
        return null;
    }

	public void removeNodeTypesFromConfiguration(Session session, InputStream jcrRepositoryConfigurationFile) throws NodeTypeRemovalException {
	}

	public void removeNodeTypesFromMappingFile(Session session, InputStream[] mappingXmlFiles) throws NodeTypeRemovalException {
	}

}
