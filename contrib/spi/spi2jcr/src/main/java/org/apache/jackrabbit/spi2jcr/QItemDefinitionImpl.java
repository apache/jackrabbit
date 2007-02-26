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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ItemDefinition;

/**
 * This abstract class implements the <code>QItemDefinition</code>
 * interface and additionally provides setter methods for the
 * various item definition attributes.
 * TODO: mostly copied from spi2dav, move common parts to spi-commons.
 */
abstract class QItemDefinitionImpl implements QItemDefinition {

    /**
     * The special wildcard name used as the name of residual item definitions.
     */
    public static final QName ANY_NAME = new QName("", "*");

    /**
     * The name of the child item.
     */
    private final QName name;

    /**
     * The name of the declaring node type.
     */
    private final QName declaringNodeType;

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
    protected int hashCode = 0;

    /**
     *
     * @param itemDef
     * @param nsResolver
     * @throws RepositoryException
     */
    QItemDefinitionImpl(ItemDefinition itemDef,
                        NamespaceResolver nsResolver)
        throws RepositoryException {
        try {
            this.autoCreated = itemDef.isAutoCreated();
            this.declaringNodeType = NameFormat.parse(itemDef.getDeclaringNodeType().getName(), nsResolver);
            this.mandatory = itemDef.isMandatory();
            this.name = itemDef.getName().equals(ANY_NAME.getLocalName()) ? ANY_NAME : NameFormat.parse(itemDef.getName(), nsResolver);
            this.onParentVersion = itemDef.getOnParentVersion();
            this.writeProtected = itemDef.isProtected();
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    //--------------------------------------------------------------< QItemDefinition >
    /**
     * {@inheritDoc}
     */
    public QName getDeclaringNodeType() {
        return declaringNodeType;
    }

    /**
     * {@inheritDoc}
     */
    public QName getQName() {
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

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two item definitions for equality. Returns <code>true</code>
     * if the given object is an item defintion and has the same attributes
     * as this item definition.
     *
     * @param obj the object to compare this item definition with
     * @return <code>true</code> if the object is equal to this item definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QItemDefinition) {
            QItemDefinition other = (QItemDefinition) obj;
            return (declaringNodeType == null
                    ? other.getDeclaringNodeType() == null
                    : declaringNodeType.equals(other.getDeclaringNodeType()))
                    && (name == null ? other.getQName() == null : name.equals(other.getQName()))
                    && autoCreated == other.isAutoCreated()
                    && onParentVersion == other.getOnParentVersion()
                    && writeProtected == other.isProtected()
                    && mandatory == other.isMandatory();
        }
        return false;
    }

    /**
     * See {@link QNodeDefinition#hashCode()} and {@link QPropertyDefinition#hashCode()}.
     *
     * @return
     */
    public abstract int hashCode();
}
