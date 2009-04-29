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
package org.apache.jackrabbit.api.jsr283;

import javax.jcr.Value;

/**
 * This interface holds extensions made in JCR 2.0 while work
 * is in progress implementing JCR 2.0.
 *
 * @since JCR 2.0
 */
public interface Repository extends javax.jcr.Repository {
    
    /**
     * Returns <code>true</code> if <code>key</code> is a standard descriptor
     * defined by the string constants in this interface and <code>false</code>
     * if it is either a valid implementation-specific key or not a valid key.
     *
     * @param key a descriptor key.
     *
     * @return whether <code>key</code> is a standard descriptor.
     *
     * @since JCR 2.0
     */
    public boolean isStandardDescriptor(String key);

    /**
     * Returns <code>true</code> if <code>key</code> is a valid single-value
     * descriptor; otherwise returns <code>false</code>.
     *
     * @param key a descriptor key.
     *
     * @return whether the specified desdfriptor is multi-valued.
     *
     * @since JCR 2.0
     */
    public boolean isSingleValueDescriptor(String key);

    /**
     * The value of a single-value descriptor is found by passing the key for
     * that descriptor to this method. If <code>key</code> is the key of a
     * multi-value descriptor or not a valid key this method returns
     * <code>null</code>.
     *
     * @param key a descriptor key.
     *
     * @return The value of the indicated descriptor
     *
     * @since JCR 2.0
     */
    public Value getDescriptorValue(String key);

    /**
     * The value array of a multi-value descriptor is found by passing the key
     * for that descriptor to this method. If <code>key</code> is the key of a
     * single-value descriptor then this method returns that value as an array
     * of size one. If <code>key</code> is not a valid key this method returns
     * <code>null</code>.
     *
     * @param key a descriptor key.
     *
     * @return the value array for the indicated descriptor
     *
     * @since JCR 2.0
     */
    public Value[] getDescriptorValues(String key);

}
