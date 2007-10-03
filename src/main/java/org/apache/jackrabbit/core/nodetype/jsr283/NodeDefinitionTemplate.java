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
package org.apache.jackrabbit.core.nodetype.jsr283;

import javax.jcr.nodetype.NodeDefinition;

/**
 * The <code>NodeDefinitionTemplate</code> interface extends
 * <code>NodeDefinition</code> with the addition of write methods, enabling the
 * characteristics of a child node definition to be set, after which the
 * <code>NodeDefinitionTemplate</code> is added to a <code>NodeTypeTemplate</code>.
 * <p/>
 * See the corresponding <code>get<code/> methods for each attribute in
 * <code>NodeDefinition</code> for the default values assumed when a new empty
 * <code>NodeDefinitionTemplate</code> is created (as opposed to one extracted
 * from an existing <code>NodeType</code>).
 *
 * @since JCR 2.0
 */
public interface NodeDefinitionTemplate extends NodeDefinition {

    /**
     * Sets the name of the node.
     *
     * @param name a <code>String</code>.
     */
    public void setName(String name);

    /**
     * Sets the auto-create status of the node.
     *
     * @param autoCreated a <code>boolean</code>.
     */
    public void setAutoCreated(boolean autoCreated);

    /**
     * Sets the mandatory status of the node.
     *
     * @param mandatory a <code>boolean</code>.
     */
    public void setMandatory(boolean mandatory);

    /**
     * Sets the on-parent-version status of the node.
     *
     * @param opv an <code>int</code> constant member of <code>OnParentVersionAction</code>.
     */
    public void setOnParentVersion(int opv);

    /**
     * Sets the protected status of the node.
     *
     * @param protectedStatus a <code>boolean</code>.
     */
    public void setProtected(boolean protectedStatus);

    /**
     * Sets the required primary types of this node.
     *
     * @param requiredPrimaryTypes a <code>String</code> array.
     */
    public void setRequiredPrimaryTypes(String[] requiredPrimaryTypes);

    /**
     * Sets the default primary type of this node.
     *
     * @param defaultPrimaryType a <code>String</code>.
     */
    public void setDefaultPrimaryType(String defaultPrimaryType);


    /**
     * Sets the same-name sibling status of this node.
     *
     * @param allowSameNameSiblings a <code>boolean</code>.
     */
    public void setSameNameSiblings(boolean allowSameNameSiblings);
}
