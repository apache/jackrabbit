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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.version.OnParentVersionAction;

/**
 * <code>AbstractItemDefinitionTemplate</code> serves as base class for
 * <code>NodeDefinitionTemplateImpl</code> and
 * <code>PropertyDefinitionTemplateImpl</code>.
 */
abstract class AbstractItemDefinitionTemplate implements ItemDefinition {

    private static final Logger log = LoggerFactory.getLogger(AbstractItemDefinitionTemplate.class);

    private Name name;
    private boolean autoCreated;
    private boolean mandatory;
    private int opv = OnParentVersionAction.COPY;
    private boolean protectedStatus;

    protected final NamePathResolver resolver;

    /**
     * Package private constructor
     *
     * @param resolver
     */
    AbstractItemDefinitionTemplate(NamePathResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Package private constructor
     *
     * @param def
     * @param resolver
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    AbstractItemDefinitionTemplate(ItemDefinition def, NamePathResolver resolver) throws ConstraintViolationException {
        this.resolver = resolver;

        if (def instanceof ItemDefinitionImpl) {
            name = ((ItemDefinitionImpl) def).itemDef.getName();
        } else {
            setName(def.getName());
        }
        autoCreated = def.isAutoCreated();
        mandatory = def.isMandatory();
        opv = def.getOnParentVersion();
        protectedStatus = def.isProtected();
    }

    //-----------------------------------------------< ItemDefinition setters >
    /**
     * Sets the name of the child item.
     *
     * @param name a <code>String</code>.
     * @throws ConstraintViolationException
     */
    public void setName(String name) throws ConstraintViolationException {
        if (ItemDefinitionImpl.ANY_NAME.equals(name)) {
            // handle the * special case that isn't a valid JCR name but a valid
            // name for a ItemDefinition (residual).
            this.name = NameConstants.ANY_NAME;
        } else {
            try {
                this.name = resolver.getQName(name);
            } catch (RepositoryException e) {
                throw new ConstraintViolationException(e);
            }
        }
    }

    /**
     * Sets the auto-create status of the child item.
     *
     * @param autoCreated a <code>boolean</code>.
     */
    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    /**
     * Sets the mandatory status of the child item.
     *
     * @param mandatory a <code>boolean</code>.
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Sets the on-parent-version status of the child item.
     *
     * @param opv an <code>int</code> constant member of <code>OnParentVersionAction</code>.
     * @throws IllegalArgumentException If the given <code>opv</code> flag isn't valid.
     */
    public void setOnParentVersion(int opv) {
        // validate the given opv-action
        OnParentVersionAction.nameFromValue(opv);
        this.opv = opv;
    }

    /**
     * Sets the protected status of the child item.
     *
     * @param protectedStatus a <code>boolean</code>.
     */
    public void setProtected(boolean protectedStatus) {
        this.protectedStatus = protectedStatus;
    }

    //-------------------------------------------------------< ItemDefinition >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        if (name == null) {
            return null;
        } else {
            try {
                return resolver.getJCRName(name);
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in item definition name", e);
                return name.toString();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getDeclaringNodeType() {
        return null;
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
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return opv;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProtected() {
        return protectedStatus;
    }
}
