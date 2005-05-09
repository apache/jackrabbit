/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.state.nodetype;

import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.name.Name;

/**
 * Item definition state. This base class contains the common
 * state properties used by both property and node definitions.
 */
public class ItemDefinitionState {

    /** The qualified name of the defined item. */
    private Name name;

    /** The AutoCreated item definition property. */
    private boolean autoCreated;

    /** The Mandatory item definition property. */
    private boolean mandatory;

    /** The OnParentVersion item definition property. */
    private int onParentVersion;

    /** The Protected item definition property. */
    private boolean isProtected; // avoid the reserved word "protected"

    /**
     * Creates an empty item definition state instance. This constructor
     * is protected because this class must only be used through subclasses.
     */
    protected ItemDefinitionState() {
        name = null;
        autoCreated = false;
        mandatory = false;
        onParentVersion = OnParentVersionAction.IGNORE;
        isProtected = false;
    }

    /**
     * Returns the qualified name of the defined item.
     *
     * @return qualified name
     */
    public Name getName() {
        return name;
    }

    /**
     * Sets the qualified name of the defined item.
     *
     * @param name new qualified name
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * Returns the value of the AutoCreated item definition property.
     *
     * @return AutoCreated property value
     */
    public boolean isAutoCreated() {
        return autoCreated;
    }

    /**
     * Sets the value of the AutoCreated item definition property.
     *
     * @param autoCreated new AutoCreated property value
     */
    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    /**
     * Returns the value of the Mandatory item definition property.
     *
     * @return Mandatory property value
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Sets the value of the Mandatory item definition property.
     *
     * @param mandatory new Mandatory property value
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Returns the value of the OnParentVerson item definition property.
     *
     * @return OnParentVersion property value
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * Sets the value of the OnParentVersion item definition property.
     *
     * @param onParentVersion new OnParentVersion property value
     */
    public void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    /**
     * Returns the value of the Protected item definition property.
     *
     * @return Protected property value
     */
    public boolean isProtected() {
        return isProtected;
    }

    /**
     * Sets the value of the Protected item definition property.
     *
     * @param isProtected new Protected property value
     */
    public void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

}
