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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.Name;

import java.io.Serializable;

/**
 * This abstract class implements the <code>QItemDefinition</code>
 * interface and additionally provides setter methods for the
 * various item definition attributes.
 */
public abstract class QItemDefinitionImpl implements QItemDefinition, Serializable {

    /**
     * The name of the child item.
     */
    private final Name name;

    /**
     * The name of the declaring node type.
     */
    private final Name declaringNodeType;

    /**
     * The 'autoCreated' flag.
     */
    private final boolean autoCreated;

    /**
     * The 'onParentVersion' attribute.
     */
    private final int onParentVersion;

    /**
     * The 'protected' flag.
     */
    private final boolean writeProtected;

    /**
     * The 'mandatory' flag.
     */
    private final boolean mandatory;

    /**
     * HashCode of this object
     */
    protected transient int hashCode = 0;

    /**
     * Creates a new <code>QItemDefinitionImpl</code>.
     *
     * @param name              the name of the child item.
     * @param declaringNodeType the declaring node type
     * @param isAutoCreated     if this item is auto created.
     * @param isMandatory       if this is a mandatory item.
     * @param onParentVersion   the on parent version behaviour.
     * @param isProtected       if this item is protected.
     */
    QItemDefinitionImpl(Name name, Name declaringNodeType,
                        boolean isAutoCreated, boolean isMandatory,
                        int onParentVersion, boolean isProtected) {
        this.name = name;
        this.declaringNodeType = declaringNodeType;
        this.autoCreated = isAutoCreated;
        this.mandatory = isMandatory;
        this.onParentVersion = onParentVersion;
        this.writeProtected = isProtected;
    }

    //--------------------------------------------------------------< QItemDefinition >
    /**
     * {@inheritDoc}
     */
    public Name getDeclaringNodeType() {
        return declaringNodeType;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
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
        return Name.NS_DEFAULT_URI.equals(name.getNamespaceURI()) && "*".equals(name.getLocalName());
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two item definitions for equality. Returns <code>true</code>
     * if the given object is an item definition and has the same attributes
     * as this item definition.
     *
     * @param obj the object to compare this item definition with
     * @return <code>true</code> if the object is equal to this item definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QItemDefinition) {
            QItemDefinition other = (QItemDefinition) obj;
            return (declaringNodeType == null
                    ? other.getDeclaringNodeType() == null
                    : declaringNodeType.equals(other.getDeclaringNodeType()))
                    && (name == null ? other.getName() == null : name.equals(other.getName()))
                    && autoCreated == other.isAutoCreated()
                    && onParentVersion == other.getOnParentVersion()
                    && writeProtected == other.isProtected()
                    && mandatory == other.isMandatory();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 37 * h + getDeclaringNodeType().hashCode();
        h = 37 * h + getName().hashCode();
        h = 37 * h + getOnParentVersion();
        h = 37 * h + (isProtected() ? 11 : 43);
        h = 37 * h + (isMandatory() ? 11 : 43);
        h = 37 * h + (isAutoCreated() ? 11 : 43);
        return h;
    }
}
