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
package org.apache.jackrabbit.spi;

/**
 * The <code>PropertyInfo</code> provides the basic information to build a
 * <code>Property</code>. The definition must be calculated from the parent
 * node type or retrieved from the RepositoryService.
 */
public interface PropertyInfo extends ItemInfo {

    /**
     * @return identifier for the item that is based on this info object. the id
     * can either be an absolute path or a uniqueID (+ relative path).
     * @see RepositoryService#getNodeInfo(SessionInfo, NodeId)
     */
    public PropertyId getId();

    /**
     * @return The {@link javax.jcr.PropertyType type} of the <code>Property</code>
     * base on this <code>PropertyInfo</code>. Note, that
     * {@link javax.jcr.PropertyType#UNDEFINED} will never be returned as the
     * value of a <code>Property</code> always has a defined type.
     * @see javax.jcr.PropertyType
     */
    public int getType();

    /**
     * @return true if the <code>Property</code> based on this info object is
     * multivalue.
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     */
    public boolean isMultiValued();

    /**
     * @return The values present on this <code>PropertyInfo</code>.
     */
    public QValue[] getValues();
}
