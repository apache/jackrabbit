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
package org.apache.jackrabbit.spi2dav;

import org.w3c.dom.Element;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * This class implements the <code>QNodeDefinition</code> interface and additionally
 * provides setter methods for the various node definition attributes.
 */
public class QNodeDefinitionImpl extends QItemDefinitionImpl implements QNodeDefinition {

    /**
     * The name of the default primary type.
     */
    private final Name defaultPrimaryType;

    /**
     * The names of the required primary types.
     */
    private final Name[] requiredPrimaryTypes;

    /**
     * The 'allowsSameNameSiblings' flag.
     */
    private final boolean allowsSameNameSiblings;

    /**
     * Create a new <code>QNodeDefinitionImpl</code>
     *
     * @param declaringNodeType
     * @param ndefElement
     * @param resolver
     * @throws RepositoryException
     */
    QNodeDefinitionImpl(Name declaringNodeType, Element ndefElement, NamePathResolver resolver)
        throws RepositoryException  {
        super(declaringNodeType, ndefElement, resolver);
        // TODO: webdav server sends jcr names -> nsResolver required. improve this.
        // NOTE: the server should send the namespace-mappings as addition ns-defininitions
        try {

            if (ndefElement.hasAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE)) {
                defaultPrimaryType = resolver.getQName(ndefElement.getAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE));
            } else {
                defaultPrimaryType = null;
            }

            Element reqPrimaryTypes = DomUtil.getChildElement(ndefElement, REQUIREDPRIMARYTYPES_ELEMENT, null);
            if (reqPrimaryTypes != null) {
                List qNames = new ArrayList();
                ElementIterator it = DomUtil.getChildren(reqPrimaryTypes, REQUIREDPRIMARYTYPE_ELEMENT, null);
                while (it.hasNext()) {
                    qNames.add(resolver.getQName(DomUtil.getTextTrim(it.nextElement())));
                }
                requiredPrimaryTypes = (Name[]) qNames.toArray(new Name[qNames.size()]);
            } else {
                requiredPrimaryTypes = new Name[] { NameConstants.NT_BASE };
            }

            if (ndefElement.hasAttribute(SAMENAMESIBLINGS_ATTRIBUTE)) {
                allowsSameNameSiblings = Boolean.valueOf(ndefElement.getAttribute(SAMENAMESIBLINGS_ATTRIBUTE)).booleanValue();
            } else {
                allowsSameNameSiblings = false;
            }
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    //--------------------------------------------------------------< QNodeDefinition >
    /**
     * {@inheritDoc}
     */
    public Name getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>true</code>
     */
    public boolean definesNode() {
        return true;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two node definitions for equality. Returns <code>true</code>
     * if the given object is a node defintion and has the same attributes
     * as this node definition.
     *
     * @param obj the object to compare this node definition with
     * @return <code>true</code> if the object is equal to this node definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QNodeDefinition) {
            QNodeDefinition other = (QNodeDefinition) obj;
            return super.equals(obj)
                    && Arrays.equals(requiredPrimaryTypes, other.getRequiredPrimaryTypes())
                    && (defaultPrimaryType == null
                            ? other.getDefaultPrimaryType() == null
                            : defaultPrimaryType.equals(other.getDefaultPrimaryType()))
                    && allowsSameNameSiblings == other.allowsSameNameSiblings();
        }
        return false;
    }

    /**
     * Overwrites {@link QItemDefinitionImpl#hashCode()}.
     * 
     * @return
     */
    public int hashCode() {
        if (hashCode == 0) {
            // build hashCode (format: <declaringNodeType>/<name>/<requiredPrimaryTypes>)
            StringBuffer sb = new StringBuffer();

            if (getDeclaringNodeType() != null) {
                sb.append(getDeclaringNodeType().toString());
                sb.append('/');
            }
            if (definesResidual()) {
                sb.append('*');
            } else {
                sb.append(getName().toString());
            }
            sb.append('/');
            // set of required node type names, sorted in ascending order
            TreeSet set = new TreeSet();
            Name[] names = getRequiredPrimaryTypes();
            for (int i = 0; i < names.length; i++) {
                set.add(names[i]);
            }
            sb.append(set.toString());

            hashCode = sb.toString().hashCode();
        }
        return hashCode;
    }
}
