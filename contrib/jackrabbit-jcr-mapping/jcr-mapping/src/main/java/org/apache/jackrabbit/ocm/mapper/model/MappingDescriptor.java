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
package org.apache.jackrabbit.ocm.mapper.model;


import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.DigesterMapperImpl;

/**
 * This class match to the complete xml mapping files.
 * it contains mainly a collection of {@link ClassDescriptor}
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @version $Id: Exp $
 */
public class MappingDescriptor {
	
	private static final Log log = LogFactory.getLog(MappingDescriptor.class);
    private HashMap classDescriptorsByClassName = new HashMap();
    private HashMap classDescriptorsByNodeType = new HashMap();

    private Mapper mapper;

    private String packageName;
    
    public void setPackage(String pckgName) {
        this.packageName = pckgName;
    }
    
    /**
     * Add a new ClassDescriptor
     *
     * @param classDescriptor The class descriptor to add
     */
    public void addClassDescriptor(ClassDescriptor classDescriptor) {
    	
        log.debug("Adding the class descriptor for : " + classDescriptor.getClassName());	
        if (null != this.packageName && !"".equals(this.packageName)) {
            classDescriptor.setClassName(this.packageName + "." + classDescriptor.getClassName());

            if (null != classDescriptor.getExtend() && !"".equals(classDescriptor.getExtend())) {
                classDescriptor.setExtend(this.packageName + "." + classDescriptor.getExtend());
            }
        }

        classDescriptorsByClassName.put(classDescriptor.getClassName(), classDescriptor);
        
        if (null != classDescriptor.getJcrType() && !  "".equals(classDescriptor.getJcrType()) && 
        		 ! ManagerConstant.NT_UNSTRUCTURED.equals(classDescriptor.getJcrType()))
        	 {
             classDescriptorsByNodeType.put(classDescriptor.getJcrType(), classDescriptor);	
        	 }
        classDescriptor.setMappingDescriptor(this);
    }

    /**
     * Get the classdescriptor to used for the class
     * @param className the class name
     * @return the class descriptor found or null
     */
    public ClassDescriptor getClassDescriptorByName(String className) {
        return (ClassDescriptor) classDescriptorsByClassName.get(className);
    }
    
    public ClassDescriptor getClassDescriptorByNodeType(String nodeType)
    {
        return (ClassDescriptor) classDescriptorsByNodeType.get(nodeType);	
    }

    /**
     * Get all class descriptors by class name
     * @return all class descriptors found
     */
    public Map getClassDescriptorsByClassName() {
        return classDescriptorsByClassName;
    }
    /**
     * Get all class descriptors by class name
     * @return all class descriptors found
     */
    public Map getClassDescriptorsByNodeType() {
        return classDescriptorsByNodeType;
    }
    public Mapper getMapper() {
        return this.mapper;
    }

    public void setMapper(Mapper parentMapper) {
        this.mapper = parentMapper;
    }
}
