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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.QName;

import javax.jcr.version.OnParentVersionAction;

/**
 * An <code>ItemDef</code> ...
 */
public abstract class ItemDef implements Cloneable {

    // '*' denoting residual child item definition
    public static final QName ANY_NAME =
            new QName(Constants.NS_DEFAULT_URI, "*");

    protected QName declaringNodeType = null;
    private QName name = ANY_NAME;
    private boolean autoCreated = false;
    private int onParentVersion = OnParentVersionAction.COPY;
    private boolean writeProtected = false;
    private boolean mandatory = false;

    protected Object clone() throws CloneNotSupportedException {
        // delegate to superclass which does a shallow copy;
        // but since all fields are either primitives or immutables
        // this is sufficient
        return super.clone();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ItemDef) {
            ItemDef other = (ItemDef) obj;
            return (declaringNodeType == null ? other.declaringNodeType == null : declaringNodeType.equals(other.declaringNodeType))
                    && (name == null ? other.name == null : name.equals(other.name))
                    && autoCreated == other.autoCreated
                    && onParentVersion == other.onParentVersion
                    && writeProtected == other.writeProtected
                    && mandatory == other.mandatory;
        }
        return false;
    }

    public void setDeclaringNodeType(QName declaringNodeType) {
        if (declaringNodeType == null) {
            throw new IllegalArgumentException("declaringNodeType can not be null");
        }
        this.declaringNodeType = declaringNodeType;
    }

    public void setName(QName name) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }

    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    public void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    public void setProtected(boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public QName getDeclaringNodeType() {
        return declaringNodeType;
    }

    public QName getName() {
        return name;
    }

    public boolean isAutoCreated() {
        return autoCreated;
    }

    public int getOnParentVersion() {
        return onParentVersion;
    }

    public boolean isProtected() {
        return writeProtected;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public boolean definesResidual() {
        return name.equals(ANY_NAME);
    }

    public abstract boolean definesNode();
}
