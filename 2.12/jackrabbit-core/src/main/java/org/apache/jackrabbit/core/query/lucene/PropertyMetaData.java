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
package org.apache.jackrabbit.core.query.lucene;

/**
 * <code>PropertyMetaData</code> encapsulates the payload byte array and
 * provides methods to access the property meta data.
 */
public final class PropertyMetaData {

    /**
     * The property type.
     */
    private final int propertyType;

    /**
     * Creates a new PropertyMetaData with the given <code>propertyType</code>.
     *
     * @param propertyType the property type.
     */
    public PropertyMetaData(int propertyType) {
        this.propertyType = propertyType;
    }

    /**
     * @return the property type.
     * @see javax.jcr.PropertyType
     */
    public int getPropertyType() {
        return propertyType;
    }

    /**
     * Creates a <code>PropertyMetaData</code> from a byte array.
     *
     * @param data the payload data array.
     * @return a <code>PropertyMetaData</code> from a byte array.
     */
    public static PropertyMetaData fromByteArray(byte[] data) {
        return new PropertyMetaData(data[0]);
    }

    /**
     * @return returns a byte array representation of this PropertyMetaData for
     *         use as a lucene token payload.
     */
    public byte[] toByteArray() {
        return new byte[]{(byte) propertyType};
    }
}
