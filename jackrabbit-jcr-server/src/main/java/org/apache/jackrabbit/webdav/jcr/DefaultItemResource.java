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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.commons.xml.SerializingContentHandler;
import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.jcr.property.LengthsProperty;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * <code>DefaultItemResource</code> represents JCR property item.
 *
 * @see Property
 */
public class DefaultItemResource extends AbstractItemResource {

    private static Logger log = LoggerFactory.getLogger(DefaultItemResource.class);

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
     * Always returns 'now'
     *
     * @return
     * @see DavResource#getModificationTime()
     */
    public long getModificationTime() {
        return new Date().getTime();
    }

    /**
     * In case an underlying repository {@link Property property} exists the following
     * logic is applyed to spool the property content:
     * <ul>
     * <li>Property is not multi valued: Return the {@link javax.jcr.Value#getStream()
     * stream representation} of the property value.</li>
     * <li>Property is multivalue: Return the xml representation of the values.</li>
     * </ul>
     *
     * @param outputContext
     * @see DavResource#spool(OutputContext)
     */
    @Override
    public void spool(OutputContext outputContext) throws IOException {
        // write properties
        super.spool(outputContext);
        // spool content
        OutputStream out = outputContext.getOutputStream();
        if (out != null && exists()) {
            if (isMultiple()) {
                spoolMultiValued(out);
            } else {
                spoolSingleValued(out);
            }
        }
    }

    private void spoolMultiValued(OutputStream out) {
        try {
            Document doc = DomUtil.createDocument();
            doc.appendChild(getProperty(JCR_VALUES).toXml(doc));

            ContentHandler handler =
                SerializingContentHandler.getSerializer(out);

            Transformer transformer =
                TransformerFactory.newInstance().newTransformer();
            transformer.transform(
                    new DOMSource(doc), new SAXResult(handler));
        } catch (SAXException e) {
            log.error("Failed to set up XML serializer for " + item, e);
        } catch (TransformerConfigurationException e) {
            log.error("Failed to set up XML transformer for " + item, e);
        } catch (ParserConfigurationException e) {
            log.error("Failed to set up XML document for " + item, e);
        } catch (TransformerException e) {
            log.error("Failed to serialize the values of " + item, e);
        }
    }

    private void spoolSingleValued(OutputStream out) throws IOException {
        try {
            Binary binary = ((Property) item).getBinary();
            try {
                InputStream in = binary.getStream();
                try {
                    IOUtil.spool(in, out);
                } finally {
                    in.close();
                }
            } finally {
                binary.dispose();
            }
        } catch (RepositoryException e) {
            log.error("Cannot obtain stream from " + item, e);
        }
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
    @Override
    public void setProperty(DavProperty<?> property) throws DavException {
        internalSetProperty(property);
        complete();
    }

    /**
     * Internal method that performs the setting or adding of properties
     *
     * @param property
     * @throws DavException
     * @see #setProperty(DavProperty)
     * @see #alterProperties(List)
     */
    private void internalSetProperty(DavProperty<?> property) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        try {
            Property prop = (Property) item;
            int defaultType = prop.getType();
            ValueFactory vfact = getRepositorySession().getValueFactory();
            ValuesProperty vp = new ValuesProperty(property, defaultType, vfact);
            if (property.getName().equals(JCR_VALUE)) {
                prop.setValue(vp.getJcrValue(vp.getValueType(), vfact));
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
    @Override
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Loops over the given <code>List</code> and alters the properties accordingly.
     * Changes are persisted at the end only according to the rules defined with
     * the {@link #complete()} method.<p>
     * Please note: since there is only a single property than can be set
     * from a client (i.e. jcr:value OR jcr:values) this method either succeeds
     * or throws an exception, even if this violates RFC 2518.
     *
     * @param changeList
     * @throws DavException
     * @see DavResource#alterProperties(List)
     */
    @Override
    public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
        for (PropEntry propEntry : changeList) {
            if (propEntry instanceof DavPropertyName) {
                // altering any properties fails if an attempt is made to remove
                // a property
                throw new DavException(DavServletResponse.SC_FORBIDDEN);
            } else if (propEntry instanceof DavProperty<?>) {
                DavProperty<?> prop = (DavProperty<?>) propEntry;
                internalSetProperty(prop);
            } else {
                throw new IllegalArgumentException("unknown object in change list: " + propEntry.getClass().getName());
            }
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
        List<DavResource> drl = Collections.emptyList();
        return new DavResourceIteratorImpl(drl);
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
    @Override
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
    @Override
    protected void initProperties() {
        super.initProperties();
        if (exists()) {
            try {
                Property prop = (Property)item;
                int type = prop.getType();

                // set the content type
                String contentType;
                if (isMultiple()) {
                    contentType = IOUtil.buildContentType("text/xml","utf-8");
                } else {
                    contentType = IOUtil.buildContentType(JcrValueType.contentTypeFromType(type), "utf-8");

                }
                properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTTYPE, contentType));

                // add jcr-specific resource properties
                properties.add(new DefaultDavProperty<String>(JCR_TYPE, PropertyType.nameFromValue(type)));
                if (isMultiple()) {
                    properties.add(new ValuesProperty(prop.getValues()));
                    properties.add(new LengthsProperty(prop.getLengths()));
                } else {
                    properties.add(new ValuesProperty(prop.getValue()));
                    long length = prop.getLength();
                    properties.add(new DefaultDavProperty<String>(JCR_LENGTH, String.valueOf(length), true));
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
            if (exists() && ((Property)item).isMultiple()) {
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Error while retrieving property definition: " + e.getMessage());
        }
        return false;
    }
}
