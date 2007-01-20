/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.lite;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.jcr.Repository;

import org.apache.jackrabbit.base.BaseRepository;

/**
 * TODO
 */
public class LiteRepository extends BaseRepository implements Repository {

    private final Properties descriptors = new Properties();

    public LiteRepository() {
        setDescriptor(
                Repository.SPEC_NAME_DESC,
                "Content Repository API for Java(TM) Technology Specification");
        setDescriptor(Repository.SPEC_VERSION_DESC, "1.0");
    }

    /**
     * Sets the value of the repository descriptor with the given key.
     *
     * @param key descriptor key
     * @param value descriptor value
     */
    protected void setDescriptor(String key, String value) {
        descriptors.setProperty(key, value);
    }
    
    /**
     * Returns the value of the repository descriptor with the given key.
     *
     * @param descriptor key
     * @return descriptor value, or <code>null</code> if not found
     * @see Repository#getDescriptor(String)
     */
    public String getDescriptor(String key) {
        return descriptors.getProperty(key);
    }

    /**
     * Returns the available repository descriptor keys.
     *
     * @return descriptor keys
     * @see Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        List keys = Collections.list(descriptors.propertyNames());
        return (String[]) keys.toArray(new String[keys.size()]);
    }

}
