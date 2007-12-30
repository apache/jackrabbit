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
package org.apache.jackrabbit.ocm.manager.beanconverter;


import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.exception.RepositoryException;
import org.apache.jackrabbit.ocm.manager.beanconverter.impl.ParentBeanConverterImpl;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;

/**
 * Interface describing a custom bean converter. 
 *
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public interface BeanConverter {
	
	
    /**
     * Insert the object.
     *
     * @param session the JCR session
     * @param parentNode The node which will contain the converter bean
     * @param beanDescriptor The bean descriptor
     * @param beanClassDescriptor the Class Descriptor associated to the bean to insert
     * @param bean the bean to convert( insert into the JCR structure)
     * @param parentClassDescriptor The Class Descriptor associated to the parent object 
     * @param parent the object which will contain the bean to convert 
     * 
     * @throws ObjectContentManagerException thrown in case the insert fails; marks a failure due to logic of
     *  the insert (parent node cannot be accessed, the insert fails, etc.)
     * @throws RepositoryException thrown in case the underlying repository has thrown a
     *  <code>javax.jcr.RepositoryException</code> that is not possible to be handled or
     *  wrapped in ObjectContentManagerException; marks a repository failure
     * @throws JcrMappingException throws in case the mapping of the bean is not correct
     */
    void insert(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object bean, ClassDescriptor parentClassDescriptor, Object parent)
    throws ObjectContentManagerException, RepositoryException, JcrMappingException;

    /**
     * Update repository from bean values.
     *
     * @param session the JCR session
     * @param parentNode The node which will contain the converter bean
     * @param beanDescriptor The bean descriptor
     * @param beanClassDescriptor the Class Descriptor associated to the bean to update
     * @param bean the bean to convert( insert into the JCR structure)
     * @param parentClassDescriptor The Class Descriptor associated to the parent object
     * @param parent the object which will contain the bean to convert 
     * 
     * @throws ObjectContentManagerException thrown in case the update fails; marks a failure due to logic
     *  of update (parent node cannot be accessed, the update fails, etc.)
     * @throws RepositoryException thrown in case the underlying repository has thrown a
     *  <code>javax.jcr.RepositoryException</code> that is not possible to be handled or
     *  wrapped in ObjectContentManagerException; marks a repository failure
     * @throws JcrMappingException throws in case the mapping of the bean is not correct
     */
    void update(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object bean, ClassDescriptor parentClassDescriptor, Object parent)
    throws ObjectContentManagerException, RepositoryException, JcrMappingException;
    
    /**
     * Retrieve a bean from the repository.
     * 
     * @param session the JCR session
     * @param parentNode The parent node
     * @param beanDescriptor The bean descriptor
     * @param beanClassDescriptor the Class Descriptor associated to the bean to insert
     * @param beanClass The bean Class
     * @param parent The parent which contain the bean to retrieve
     * 
     * @throws ObjectContentManagerException thrown in case the bean cannot be retrieved or initialized; 
     *  marks a failure due to logic of retrieval
     * @throws RepositoryException thrown in case the underlying repository has thrown a
     *  <code>javax.jcr.RepositoryException</code> that is not possible to be handled or
     *  wrapped in ObjectContentManagerException; marks a repository failure
     * @throws JcrMappingException throws in case the mapping of the bean is not correct
     */
    Object getObject(Session session, Node parentNode, BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Class beanClass, Object parent) 
    throws ObjectContentManagerException, RepositoryException, JcrMappingException;


    /**
     * Remove the bean from the repository.
     * 
     * @param session the JCR session
     * @param parentNode The node which will contain the converter bean
     * @param beanDescriptor The bean descriptor    
     * @param beanClassDescriptor the Class Descriptor associated to the bean to update
     * @param bean the bean to convert( insert into the JCR structure)
     * @param parentClassDescriptor The Class Descriptor associated to the parent object 
     * @param parent the object which contains the bean to convert 
     * 
     * @throws ObjectContentManagerException thrown in case the bean cannot be removed; 
     *  marks a failure due to logic of removal
     * @throws RepositoryException thrown in case the underlying repository has thrown a
     *  <code>javax.jcr.RepositoryException</code> that is not possible to be handled or
     *  wrapped in ObjectContentManagerException; marks a repository failure
     * @throws JcrMappingException throws in case the mapping of the bean is not correct
     */
    void remove(Session session, Node parentNode,  BeanDescriptor beanDescriptor, ClassDescriptor beanClassDescriptor, Object bean, ClassDescriptor parentClassDescriptor, Object parent)
    throws ObjectContentManagerException, RepositoryException, JcrMappingException;
    
    /**
     * Get the bean path. 
     * 
     * When the bean is mapped to a subnode, the bean path is the parent node path + the jcrname of the current bean.
     * Sometime a BeanConverter can be used to access to a bean which is not mapped to a subnode. In this case, 
     * another implementation can be provided in this method getPath. {@link  ParentBeanConverterImpl} is a good example.
     * 
     * @param session the JCR session 
     * @param beanDescriptor The descriptor of the bean to convert
     * @param parentNode the node which contain this bean (its corresponfing subnode)
     * @return the bean path
     * 
     * @throws RepositoryException thrown in case the underlying repository has thrown a
     *  <code>javax.jcr.RepositoryException</code> that is not possible to be handled or
     *  wrapped in ObjectContentManagerException; marks a repository failure
     * 
     */
    String getPath(Session session, BeanDescriptor beanDescriptor, Node parentNode)
    throws ObjectContentManagerException;
    

}
