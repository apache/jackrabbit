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
package org.apache.jackrabbit.webdav.spi;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.core.util.ValueHelper;
import org.jdom.Element;

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
    public DefaultItemResource(DavResourceLocator locator, DavSession session, DavResourceFactory factory) {
        super(locator, session, factory);
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
     * @todo undo incomplete modifications...
     */
    public void setProperty(DavProperty property) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        try {
            Property prop = (Property) item;
            int type = prop.getType();
            if (property.getName().equals(JCR_VALUE)) {
                Value val = ValueHelper.convert(String.valueOf(property.getValue()), type);
                prop.setValue(val);
            } else if (property.getName().equals(JCR_VALUES)) {
                prop.setValue(new ValuesProperty(property).getValues());
            } else {
                throw new DavException(DavServletResponse.SC_CONFLICT);
            }
            complete();

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
                    properties.add(new DefaultDavProperty(JCR_VALUE, prop.getString()));
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

    //------------------------------------------------------< inner classes >---
    /**
     * <code>ValuesProperty</code> extends {@link DavProperty} providing
     * utilities to handle the multiple values of the property item represented
     * by this resource.
     */
    private class ValuesProperty extends AbstractDavProperty {

        private final Element[] value;

        /**
         * Wrap the specified <code>DavProperty</code> in a new <code>ValuesProperty</code>.
         *
         * @param property
         */
        private ValuesProperty(DavProperty property) {
            super(JCR_VALUES, false);

            if (!JCR_VALUES.equals(property.getName())) {
               throw new IllegalArgumentException("ValuesProperty may only be created with a property that has name="+JCR_VALUES.getName());
            }

            Element[] elems = new Element[0];
            if (property.getValue() instanceof List) {
                Iterator elemIt = ((List)property.getValue()).iterator();
                ArrayList valueElements = new ArrayList();
                while (elemIt.hasNext()) {
                    Object el = elemIt.next();
                    /* make sure, only Elements with name 'value' are used for
                     * the 'value' field. any other content (other elements, text,
                     * comment etc.) is ignored. NO bad-request/conflict error is
                     * thrown.
                     */
                    if (el instanceof Element && "value".equals(((Element)el).getName())) {
                        valueElements.add(el);
                    }
                }
                /* fill the 'value' with the valid 'value' elements found before */
                elems = (Element[])valueElements.toArray(new Element[valueElements.size()]);
            } else {
                new IllegalArgumentException("ValuesProperty may only be created with a property that has a list of 'value' elements as content.");
            }
            // finally set the value to the DavProperty
            value = elems;
        }

        /**
         * Create a new <code>ValuesProperty</code> from the given {@link Value Value
         * array}.
         *
         * @param values Array of Value objects as obtained from the JCR property.
         */
        private ValuesProperty(Value[] values) throws ValueFormatException, RepositoryException {
            super(JCR_VALUES, false);

            Element[] propValue = new Element[values.length];
            for (int i = 0; i < values.length; i++) {
                propValue[i] = new Element(XML_VALUE, ItemResourceConstants.NAMESPACE);
                propValue[i].addContent(values[i].getString());
            }
            // finally set the value to the DavProperty
            value = propValue;
        }

        /**
         * Converts the value of this property to a {@link javax.jcr.Value value array}.
         * Please note, that the convertion is done by using the {@link ValueHelper}
         * class that is not part of the JSR170 API.
         *
         * @return Array of Value objects
         * @throws RepositoryException
         */
        private Value[] getValues() throws ValueFormatException, RepositoryException {
            Element[] propValue = (Element[])getValue();
            Value[] values = new Value[propValue.length];
            for (int i = 0; i < propValue.length; i++) {
                values[i] = ValueHelper.convert(propValue[i].getText(), ((Property)item).getType());
            }
            return values;
        }

        /**
         * Returns an array of {@link Element}s representing the value of this
         * property.
         *
         * @return an array of {@link Element}s
         */
        public Object getValue() {
            return value;
        }
    }

    /**
     * <code>LengthsProperty</code> extends {@link DavProperty} providing
     * utilities to handle the multiple lengths of the property item represented
     * by this resource.
     */
    private class LengthsProperty extends AbstractDavProperty {

        private final Element[] value;

        /**
         * Create a new <code>LengthsProperty</code> from the given long array.
         *
         * @param lengths as retrieved from the JCR property
         */
        private LengthsProperty(long[] lengths) {
            super(JCR_LENGTHS, false);

            Element[] elems = new Element[lengths.length];
            for (int i = 0; i < lengths.length; i++) {
                elems[i] = new Element(XML_LENGTH, ItemResourceConstants.NAMESPACE);
                elems[i].addContent(String.valueOf(lengths[i]));
            }
            this.value = elems;
        }

        /**
         * Returns an array of {@link Element}s representing the value of this
         * property.
         *
         * @return an array of {@link Element}s
         */
        public Object getValue() {
            return value;
        }
    }
}