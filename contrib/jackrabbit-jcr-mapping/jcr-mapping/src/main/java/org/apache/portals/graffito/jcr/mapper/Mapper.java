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
package org.apache.portals.graffito.jcr.mapper;

import org.apache.portals.graffito.jcr.mapper.model.ClassDescriptor;

/**
 * This component read the mapping file and gives an access to the ClassDescriptors (the mapping object model)
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * 
 */
public interface Mapper
{
    /**
     * Get the mapping defition to be used for a specific java bean
     * @param clazz The java bean class
     * @return The mapping class found for the desired java bean class 
     */
    public abstract ClassDescriptor getClassDescriptorByClass(Class clazz);
    
    /**
     * Get the mapping defition to be used for a specific JCR node type
     * @param jcrNodeType the jcr node type
     * @return The mapping class found for the desired java bean class 
     */
    public abstract ClassDescriptor getClassDescriptorByNodeType(String jcrNodeType);    
}
