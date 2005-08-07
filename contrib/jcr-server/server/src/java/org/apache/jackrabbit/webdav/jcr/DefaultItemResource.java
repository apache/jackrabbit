/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.jcr.property.LengthsProperty;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.value.ValueHelper;

import javax.jcr.*;
import java.io.*;
import java.util.*;

/**
 * <code>DefaultItemResource</code> represents JCR property item.
 *
 * @see Property
 */
public class DefaultItemResource extends AbstractItemResource {

    private static Logger log = Logger.getLogger(DefaultItemResource.class);

    /**
     * Create a new <code>DefaultItemResource</code>.
     *
     * @param locator
     * @param session
     */
    public DefaultItemResource(DavResourceLocator locator, DavSession session,
                               DavResourceFactory factory, Item item) {
        super(locator, session, factory, item);
    }

    //----------------------------------------------< DavResource interface >---
    /**
     * Returns false.
     *
     * @return false
     * @see DavResource#isCollection()
     */
    public boolean isCollection() {
        return false;
    }

    /**
     * In case an underlaying repository {@link Property property} exists the following
     * logic is applyed to obtain the stream:<ul>
     * <li>Property is not multivalue: Return the {@link javax.jcr.Value#getStream()
     * stream representation} of the property value.</li>
     * <li>Property is multivalue: Return stream that provides the system view of
     * that item.</li>
     * </ul>
     *
     * @return
     * @see DavResource#getStream()
     */
    public InputStream getStream() {
        InputStream in = null;
        if (exists()) {
            try {
                // NOTE: stream cannot be obtained for multivalue properties
                if (!isMultiple()) {
                    in = ((Property)item).getStream();
                }
            } catch (ValueFormatException e) {
                // should not occur
                log.error("Cannot obtain stream from resource: " + e.getMessage());
            } catch (RepositoryException e) {
                log.error("Cannot obtain stream from resource: " + e.getMessage());
            }
        }
        return in;
    }

    /**
     * Sets the given property. Note, that {@link #JCR_VALUE} and {@link #JCR_VALUES}
     * are the only resource properties that are allowed to be modified. Any other
     * property is read-only and will throw an exception ('Conflict').
     *
     * @param property
     * @throws DavException
     * @see DavResource#setProperty(org.apache.jackrabbit.webdav.property.DavProperty)
     *
     * todo: undo incomplete modifications...
     */
    public void setProperty(DavProperty property) throws DavException {
        internalSetProperty(property);
        complete();
    }

    /**
     * Internal method that performs the setting or adding of properties
     *
     * @param property
     * @throws DavException
     * @see #setProperty(DavProperty)
     * @see #alterProperties(DavPropertySet, DavPropertyNameSet)
     */
    private void internalSetProperty(DavProperty property) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        try {
            Property prop = (Property) item;
            int type = prop.getType();
            if (property.getName().equals(JCR_VALUE)) {
                String strVal = (property.getValue() != null) ? String.valueOf(property.getValue()) : "";
                Value val = ValueHelper.deserialize(strVal, type, false);
                prop.setValue(val);
            } else if (property.getName().equals(JCR_VALUES)) {
                prop.setValue(new ValuesProperty(property).getValues(prop.getType()));
            } else {
                throw new DavException(DavServletResponse.SC_CONFLICT);
            }
        } catch (IllegalArgumentException e) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Removing properties is not allowed, for a single-value JCR-property without
     * a value does not exist. For multivalue properties an empty {@link Value values array}
     * may be specified with by setting the {@link #JCR_VALUES 'values' webdav property}.
     *
     * @param propertyName
     * @throws DavException
     * @see org.apache.jackrabbit.webdav.DavResource#removeProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Loops over the given <code>Set</code>s and alters the properties accordingly.
     * Changes are persisted at the end only according to the rules defined with
     * the {@link #complete()} method.
     *
     * @param setProperties
     * @param removePropertyNames
     * @throws DavException
     * @see DavResource#alterProperties(DavPropertySet, DavPropertyNameSet)
     */
    public void alterProperties(DavPropertySet setProperties,
                                DavPropertyNameSet removePropertyNames)
        throws DavException {

        // altering any properties fails if an attempt is made to remove a property
        if (removePropertyNames != null && !removePropertyNames.isEmpty()) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }

        // only set/add >> existance of resource is checked inside internal method
        DavPropertyIterator setIter = setProperties.iterator();
        while (setIter.hasNext()) {
            DavProperty prop = setIter.nextProperty();
            internalSetProperty(prop);
        }
        complete();
    }

    /**
     * Method is not allowed.
     *
     * @see org.apache.jackrabbit.webdav.DavResource#addMember(org.apache.jackrabbit.webdav.DavResource, InputStream)
     */
    public void addMember(DavResource resource, InputStream in) throws DavException {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED, "Cannot add members to a non-collection resource");
    }

    /**
     * Method is not allowed.
     *
     * @see DavResource#addMember(DavResource)
     */
    public void addMember(DavResource resource) throws DavException {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED, "Cannot add members to a non-collection resource");
    }

    /**
     * Always returns an empty iterator for a non-collection resource might
     * not have internal members.
     *
     * @return an empty iterator
     * @see DavResource#getMembers()
     */
    public DavResourceIterator getMembers() {
        log.warn("A non-collection resource never has internal members.");
        return new DavResourceIteratorImpl(new ArrayList(0));
    }

    /**
     * Method is not allowed.
     *
     * @see DavResource#removeMember(DavResource)
     */
    public void removeMember(DavResource member) throws DavException {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED, "Cannot remove members from a non-collection resource");
    }

    /**
     * {@link javax.jcr.Property JCR properties} are locked if their
     * parent node is locked; thus this method will always return the
     * {@link ActiveLock lock} object from the collection this resource is
     * internal member of.
     *
     * @param type
     * @param scope
     * @return lock present on this resource or <code>null</code> if this resource
     * has no lock.
     * @see DavResource#getLock(Type, Scope)
     */
    public ActiveLock getLock(Type type, Scope scope) {
        if (Type.WRITE.equals(type)) {
            return getCollection().getLock(type, scope);
        } else {
            return super.getLock(type, scope);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Add resource specific properties.
     */
    protected void initProperties() {
        super.initProperties();
        if (exists()) {
            try {
                Property prop = (Property)item;
                int type = prop.getType();

                // set the content type
                String contentType;
                if (!isMultiple()) {
                    contentType = (type == PropertyType.BINARY) ? "application/octet-stream" : "text/plain";
                    properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, contentType));
                } // else: no contenttype for multivalue properties

                // add jcr-specific resource properties
                properties.add(new DefaultDavProperty(JCR_TYPE, PropertyType.nameFromValue(type)));
                if (isMultiple()) {
                    properties.add(new ValuesProperty(prop.getValues()));
                    properties.add(new LengthsProperty(prop.getLengths()));
                } else {
                    properties.add(new DefaultDavProperty(JCR_VALUE, ValueHelper.serialize(prop.getValue(), false)));
                    properties.add(new DefaultDavProperty(JCR_LENGTH, String.valueOf(prop.getLength())));
                }
            } catch (RepositoryException e) {
                log.error("Failed to retrieve resource properties: "+e.getMessage());
            }
        }
    }

    /**
     * Returns true if the JCR Property represented by this resource is a multi
     * value property. Note: if this resource does not exist or if the definition
     * could not be retrieved false is returned.
     *
     * @return true if the underlaying resource is a multi value property.
     */
    private boolean isMultiple() {
        try {
            if (exists() && ((Property)item).getDefinition().isMultiple()) {
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Error while retrieving property definition: " + e.getMessage());
        }
        return false;
    }
}