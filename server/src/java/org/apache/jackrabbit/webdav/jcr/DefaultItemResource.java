/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.jcr.property.LengthsProperty;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.log4j.Logger;

import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

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
    public DefaultItemResource(DavResourceLocator locator, JcrDavSession session,
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
     * In case an underlying repository {@link Property property} exists the following
     * logic is applyed to obtain the stream:<ul>
     * <li>Property is not multivalue: Return the {@link javax.jcr.Value#getStream()
     * stream representation} of the property value.</li>
     * <li>Property is multivalue: Return stream that provides the system view of
     * that item.</li>
     * </ul>
     *
     * @return
     */
    InputStream getStream() {
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
            int defaultType = prop.getType();
            ValuesProperty vp = new ValuesProperty(property, defaultType);
            if (property.getName().equals(JCR_VALUE)) {
                prop.setValue(vp.getJcrValue(vp.getValueType()));
            } else if (property.getName().equals(JCR_VALUES)) {
                prop.setValue(vp.getJcrValues());
            } else {
                throw new DavException(DavServletResponse.SC_CONFLICT);
            }
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
     * the {@link #complete()} method.<p>
     * Please note: since there is only a single property than can be set
     * from a client (i.e. jcr:value OR jcr:values) this method either succeeds
     * or throws an exception, even if this violates RFC 2518.
     *
     * @param setProperties
     * @param removePropertyNames
     * @throws DavException
     * @see DavResource#alterProperties(DavPropertySet, DavPropertyNameSet)
     */
    public MultiStatusResponse alterProperties(DavPropertySet setProperties,
                                DavPropertyNameSet removePropertyNames)
        throws DavException {

        // altering any properties fails if an attempt is made to remove a property
        if (removePropertyNames != null && !removePropertyNames.isEmpty()) {
            Iterator it = removePropertyNames.iterator();
            while (it.hasNext()) {
                throw new DavException(DavServletResponse.SC_FORBIDDEN);
            }
        }

        // only set/add >> existance of resource is checked inside internal method
        DavPropertyIterator setIter = setProperties.iterator();
        while (setIter.hasNext()) {
            DavProperty prop = setIter.nextProperty();
            internalSetProperty(prop);
        }
        complete();
        return new MultiStatusResponse(getHref(), DavServletResponse.SC_OK);
    }

    /**
     * Method is not allowed.
     *
     * @see org.apache.jackrabbit.webdav.DavResource#addMember(org.apache.jackrabbit.webdav.DavResource, InputContext)
     */
    public void addMember(DavResource resource, InputContext inputContext) throws DavException {
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
                    properties.add(new ValuesProperty(prop.getValue()));
                    properties.add(new DefaultDavProperty(JCR_LENGTH, String.valueOf(prop.getLength()), true));
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
     * @return true if the underlying resource is a multi value property.
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