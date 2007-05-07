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
package org.apache.portals.graffito.jcr.mapper.model;

/**
 * ChildNodeDefDescriptor is used by the node type management tools based on
 * class descriptors to manage child node definitions
 *
 * @author <a href="mailto:fmeschbe[at]apache[dot]com">Felix Meschberger</a>
 */
public interface ChildNodeDefDescriptor {

    /**
     * @return Returns the name of the property.
     */
    String getJcrName();

    /**
     * @return Returns the child node type name.
     */
    String getJcrNodeType();

    /**
     * @return Whether the child node is auto created.
     */
    boolean isJcrAutoCreated();

    /**
     * @return Whether the child node is mandatory.
     */
    boolean isJcrMandatory();

    /**
     * @return What to do on parent version creation.
     */
    String getJcrOnParentVersion();

    /**
     * @return Whether the child node is protected.
     */
    boolean isJcrProtected();

    /**
     * @return Whether the child node definition allows for same name sibblings.
     */
    boolean isJcrSameNameSiblings();
}
