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
package org.apache.jackrabbit.state.nodetype;

import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.name.QName;

/**
 * Item definition state. This base class contains the common
 * state properties used by both property and node definitions.
 */
public class ItemDefinitionState implements Comparable {

    /** The qualified name of the defined item. */
    private QName name = null;

    /** The AutoCreated item definition property. */
    private boolean autoCreated = false;

    /** The Mandatory item definition property. */
    private boolean mandatory = false;

    /** The OnParentVersion item definition property. */
    private int onParentVersion = OnParentVersionAction.COPY;

    /** The Protected item definition property. */
    private boolean isProtected = false; // avoid the reserved word "protected"

    /**
     * Returns the qualified name of the defined item.
     *
     * @return qualified name
     */
    public QName getName() {
        return name;
    }

    /**
     * Sets the qualified name of the defined item.
     *
     * @param name new qualified name
     */
    public void setName(QName name) {
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

    public int compareTo(Object object) {
        ItemDefinitionState that = (ItemDefinitionState) object;
        if ((this.name == null) != (that.name == null)) {
            return (name != null) ? -1 : 1;
        } else if (this.name != null && this.name.compareTo(that.name) != 0) {
            return this.name.compareTo(that.name);
        } else if (this.autoCreated != that.autoCreated) {
            return autoCreated ? -1 : 1;
        } else if (this.mandatory != that.mandatory) {
            return mandatory ? -1 : 1;
        } else if (this.isProtected != that.isProtected) {
            return isProtected ? -1 : 1;
        } else {
            return this.onParentVersion - that.onParentVersion;
        }
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof ItemDefinitionState) {
            ItemDefinitionState that = (ItemDefinitionState) object;
            return ((name != null) ? name.equals(that.name) : (that.name == null))
                && this.autoCreated == that.autoCreated
                && this.isProtected == that.isProtected
                && this.mandatory == that.mandatory
                && this.onParentVersion == that.onParentVersion;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int code = 37;
        code = code * 17 + ((name != null) ? name.hashCode() : 0);
        code = code * 17 + (autoCreated ? 1 : 0);
        code = code * 17 + (isProtected ? 1 : 0);
        code = code * 17 + (mandatory ? 1 : 0);
        code = code * 17 + onParentVersion;
        return code;
    }
    
}
