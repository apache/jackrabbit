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
package org.apache.portals.graffito.jcr.spring;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.exception.JcrMappingException;
import org.apache.portals.graffito.jcr.mapper.impl.DigesterDescriptorReader;
import org.apache.portals.graffito.jcr.mapper.model.ClassDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.MappingDescriptor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * Factory bean for loading mapping files. This factory beans can load several file descriptors
 * and assembles them into an overall class descriptor. 
 * 
 * @author Costin Leau
 *
 */
public class MappingDescriptorFactoryBean implements FactoryBean, InitializingBean {

    private static final Log log = LogFactory.getLog(MappingDescriptorFactoryBean.class);

    private MappingDescriptor mappingDescriptor;

    private Resource[] mappings;

    /**
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject() throws Exception {
        return mappingDescriptor;
    }

    /**
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class getObjectType() {
        return (this.mappingDescriptor != null) ? this.mappingDescriptor.getClass() : ClassDescriptor.class;
    }

    /**
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (mappings == null || mappings.length == 0)
            throw new IllegalArgumentException("at least one mapping file is needed");

        createMappingDescriptor();
    }

    /**
     * Subclasses can extend this method to provide custom behavior when creating 
     * the mapping descriptor
     */
    protected void createMappingDescriptor() throws IOException, JcrMappingException {
        // load the descriptors step by step and concatenate everything in an over-all
        // descriptor
    	   DigesterDescriptorReader reader = new DigesterDescriptorReader();
        mappingDescriptor = reader.loadClassDescriptors(mappings[0].getInputStream());
        boolean debug = log.isDebugEnabled();

        for (int i = 1; i < mappings.length; i++) {
            if (mappings[i] != null) {
                MappingDescriptor descriptor = reader.loadClassDescriptors(mappings[i].getInputStream());
                for (Iterator iter = descriptor.getClassDescriptorsByClassName().values().iterator(); iter.hasNext();) {
                    mappingDescriptor.addClassDescriptor((ClassDescriptor) iter.next());
                }
            }
        }
    }

    /**
     * @return Returns the descriptors.
     */
    public Resource[] getMappings() {
        return mappings;
    }

    /**
     * @param descriptors The descriptors to set.
     */
    public void setMappings(Resource[] descriptors) {
        this.mappings = descriptors;
    }

}
