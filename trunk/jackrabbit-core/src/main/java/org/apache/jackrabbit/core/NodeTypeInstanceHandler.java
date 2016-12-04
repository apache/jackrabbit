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
package org.apache.jackrabbit.core;

import java.util.Calendar;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * The <code>NodeTypeInstanceHandler</code> is used to provide or initialize
 * system protected properties (or child nodes).
 *
 */
public class NodeTypeInstanceHandler {

    /**
     * Default user id in the case where the creating user cannot be determined.
     */
    public static final String DEFAULT_USERID = "system";

    /**
     * userid to use for the "*By" autocreated properties
     */
    private final String userId;

    /**
     * Creates a new node type instance handler.
     * @param userId the user id. if <code>null</code>, {@value #DEFAULT_USERID} is used.
     */
    public NodeTypeInstanceHandler(String userId) {
        this.userId = userId == null
                ? DEFAULT_USERID
                : userId;
    }

    /**
     * Sets the system-generated or node type -specified default values
     * of the given property. If such values are not specified, then the
     * property is not modified.
     *
     * @param property property state
     * @param parent parent node state
     * @param def property definition
     * @throws RepositoryException if the default values could not be created
     */
    public void setDefaultValues(
            PropertyState property, NodeState parent, QPropertyDefinition def)
            throws RepositoryException {
        InternalValue[] values =
            computeSystemGeneratedPropertyValues(parent, def);
        if (values == null && def.getDefaultValues() != null) {
            values = InternalValue.create(def.getDefaultValues());
        }
        if (values != null) {
            property.setValues(values);
        }
    }

    /**
     * Computes the values of well-known system (i.e. protected) properties.
     *
     * @param parent the parent node state
     * @param def the definition of the property to compute
     * @return the computed values
     */
    public InternalValue[] computeSystemGeneratedPropertyValues(NodeState parent, 
                                                                QPropertyDefinition def) {

        InternalValue[] genValues = null;

        Name name = def.getName();
        Name declaringNT = def.getDeclaringNodeType();

        if (NameConstants.JCR_UUID.equals(name)) {
            // jcr:uuid property of the mix:referenceable node type
            if (NameConstants.MIX_REFERENCEABLE.equals(declaringNT)) {
                genValues = new InternalValue[]{InternalValue.create(parent.getNodeId().toString())};
            }
        } else if (NameConstants.JCR_PRIMARYTYPE.equals(name)) {
            // jcr:primaryType property (of any node type)
            genValues = new InternalValue[]{InternalValue.create(parent.getNodeTypeName())};
        } else if (NameConstants.JCR_MIXINTYPES.equals(name)) {
            // jcr:mixinTypes property (of any node type)
            Set<Name> mixins = parent.getMixinTypeNames();
            genValues = new InternalValue[mixins.size()];
            int i = 0;
            for (Name n : mixins) {
                genValues[i++] = InternalValue.create(n);
            }
        } else if (NameConstants.JCR_CREATED.equals(name)) {
            // jcr:created property of a version or a mix:created
            if (NameConstants.MIX_CREATED.equals(declaringNT)
                    || NameConstants.NT_VERSION.equals(declaringNT)) {
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (NameConstants.JCR_CREATEDBY.equals(name)) {
            // jcr:createdBy property of a mix:created
            if (NameConstants.MIX_CREATED.equals(declaringNT)) {
                genValues = new InternalValue[]{InternalValue.create(userId)};
            }
        } else if (NameConstants.JCR_LASTMODIFIED.equals(name)) {
            // jcr:lastModified property of a mix:lastModified
            if (NameConstants.MIX_LASTMODIFIED.equals(declaringNT)) {
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (NameConstants.JCR_LASTMODIFIEDBY.equals(name)) {
            // jcr:lastModifiedBy property of a mix:lastModified
            if (NameConstants.MIX_LASTMODIFIED.equals(declaringNT)) {
                genValues = new InternalValue[]{InternalValue.create(userId)};
            }
        } else if (NameConstants.JCR_ETAG.equals(name)) {
            // jcr:etag property of a mix:etag
            if (NameConstants.MIX_ETAG.equals(declaringNT)) {
                // TODO: provide real implementation
                genValues = new InternalValue[]{InternalValue.create("")};
            }
        }
        return genValues;
    }

}