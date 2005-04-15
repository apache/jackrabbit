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

import org.apache.jackrabbit.core.QName;

import javax.jcr.version.OnParentVersionAction;

/**
 * This class is an abstract implementation for the <code>ItemDef</code>
 * interface and holds the generic item definition attributes.
 */
public abstract class ItemDefImpl implements ItemDef {

    /**
     * The name of this item definition.
     */
    private QName name = ANY_NAME;

    /**
     * The name of the declaring node type.
     */
    protected QName declaringNodeType = null;

    /**
     * The 'autoCreated' flag.
     */
    private boolean autoCreated = false;

    /**
     * The 'onParentVersion' attribute.
     */
    private int onParentVersion = OnParentVersionAction.COPY;

    /**
     * The 'writeProtected' flag.
     */
    private boolean writeProtected = false;

    /**
     * The 'mandatory' flag.
     */
    private boolean mandatory = false;

    /**
     * Sets the declaring node type.
     *
     * @param declaringNodeType
     */
    public void setDeclaringNodeType(QName declaringNodeType) {
        if (declaringNodeType == null) {
            throw new IllegalArgumentException("declaringNodeType can not be null");
        }
        this.declaringNodeType = declaringNodeType;
    }

    /**
     * Sets the name.
     *
     * @param name
     */
    public void setName(QName name) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }

    /**
     * Sets the auto create flag.
     *
     * @param autoCreated
     */
    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    /**
     * Sets the on parent version.
     *
     * @param onParentVersion
     */
    public void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    /**
     * Sets the protected flag.
     *
     * @param writeProtected
     */
    public void setProtected(boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    /**
     * Sets the mandatory flag.
     *
     * @param mandatory
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * {@inheritDoc}
     */
    public QName getDeclaringNodeType() {
        return declaringNodeType;
    }

    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoCreated() {
        return autoCreated;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProtected() {
        return writeProtected;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * {@inheritDoc}
     */
    public boolean definesResidual() {
        return name.equals(ANY_NAME);
    }

    /**
     * Checks if this item definition is equal to the given one. Two item
     * definitions are equal if they are the same object or if all their
     * attributes are equal.
     *
     * @param obj the object to compare to
     * @return <code>true</code> if this item definition is equals to obj;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ItemDefImpl) {
            ItemDefImpl other = (ItemDefImpl) obj;
            return (declaringNodeType == null
                        ? other.declaringNodeType == null
                        : declaringNodeType.equals(other.declaringNodeType))
                    && (name == null ? other.name == null : name.equals(other.name))
                    && autoCreated == other.autoCreated
                    && onParentVersion == other.onParentVersion
                    && writeProtected == other.writeProtected
                    && mandatory == other.mandatory;
        }
        return false;
    }
}
