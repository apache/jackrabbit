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
package org.apache.jackrabbit.spi.commons.nodetype;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * A builder for {@link QItemDefinition}.
 */
public abstract class QItemDefinitionBuilder {

    private Name name = NameConstants.ANY_NAME;
    private Name declaringType = null;
    private boolean isAutocreated = false;
    private int onParentVersion = OnParentVersionAction.COPY;
    private boolean isProtected = false;
    private boolean isMandatory = false;

    /**
     * @param name  the name of the child item definition being build
     * @see ItemDefinition#getName()
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * @return the name of the child item definition being build.
     * @see ItemDefinition#getName()
     */
    public Name getName() {
        return name;
    }

    /**
     * @param type  the name of the declaring node type.
     * @see ItemDefinition#getDeclaringNodeType()
     */
    public void setDeclaringNodeType(Name type) {
        declaringType = type;
    }

    /**
     * @return the name of the declaring node type.
     * @see ItemDefinition#getDeclaringNodeType()
     */
    public Name getDeclaringNodeType() {
        return declaringType;
    }

    /**
     * @param autocreate <code>true</code> if building a 'autocreate' child item
     * definition, false otherwise.
     * @see ItemDefinition#isAutoCreated()
     */
    public void setAutoCreated(boolean autocreate) {
        isAutocreated = autocreate;
    }

    /**
     * @return <code>true</code> if building a 'autocreate' child item
     * definition, false otherwise.
     * @see ItemDefinition#isAutoCreated()
     */
    public boolean getAutoCreated() {
        return isAutocreated;
    }

    /**
     * @param onParent the 'onParentVersion' attribute of the child item definition being built
     * @see ItemDefinition#getOnParentVersion()
     */
    public void setOnParentVersion(int onParent) {
        onParentVersion = onParent;
    }

    /**
     * @return the 'onParentVersion' attribute of the child item definition being built
     * @see ItemDefinition#getOnParentVersion()
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * @param isProtected <code>true</code> if building a 'protected' child
     * item definition, false otherwise.
     * @see ItemDefinition#isProtected()
     */
    public void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    /**
     * @return <code>true</code> if building a 'protected' child item
     * definition, false otherwise.
     * @see ItemDefinition#isProtected()
     */
    public boolean getProtected() {
        return isProtected;
    }

    /**
     * @param isMandatory <code>true</code> if building a 'mandatory' child
     * item definition, false otherwise.
     * @see ItemDefinition#isMandatory()
     */
    public void setMandatory(boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    /**
     * @return <code>true</code> if building a 'mandatory' child item
     * definition, false otherwise.
     * @see ItemDefinition#isMandatory()
     */
    public boolean getMandatory() {
        return isMandatory;
    }

}
