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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.conversion.NameResolver;
import org.apache.jackrabbit.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.name.NameFactoryImpl;
import org.apache.jackrabbit.name.NameConstants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.IOException;
import java.io.Writer;

/**
 * A <code>SysViewSAXEventGenerator</code> instance can be used to generate SAX events
 * representing the serialized form of an item in System View XML.
 */
public class SysViewSAXEventGenerator extends AbstractSAXEventGenerator {

    public static final String CDATA_TYPE = "CDATA";
    public static final String ENUMERATION_TYPE = "ENUMERATION";

    private static final String NS_XMLSCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String NS_XMLSCHEMA_INSTANCE_PREFIX = "xsi";
    private static final String NS_XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
    private static final String NS_XMLSCHEMA_PREFIX = "xs";

    private static final Attributes ATTRS_EMPTY = new AttributesImpl();
    private static final Attributes ATTRS_BINARY_ENCODED_VALUE;
    static {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(Name.NS_XMLNS_URI, NS_XMLSCHEMA_INSTANCE_PREFIX, "xmlns:" + NS_XMLSCHEMA_INSTANCE_PREFIX, CDATA_TYPE, NS_XMLSCHEMA_INSTANCE_URI);
        attrs.addAttribute(Name.NS_XMLNS_URI, NS_XMLSCHEMA_PREFIX, "xmlns:" + NS_XMLSCHEMA_PREFIX, CDATA_TYPE, NS_XMLSCHEMA_URI);
        attrs.addAttribute(NS_XMLSCHEMA_INSTANCE_URI, "type", NS_XMLSCHEMA_INSTANCE_PREFIX + ":type", "CDATA", NS_XMLSCHEMA_PREFIX + ":base64Binary");
        ATTRS_BINARY_ENCODED_VALUE = attrs;
    }

    /**
     * Name resolver for producing qualified XML names.
     */
    private final NameResolver resolver;

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties will
     *                       be serialized; otherwise the entire hierarchy starting with
     *                       <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be serialized.
     * @param contentHandler the content handler to feed the SAX events to
     * @throws RepositoryException if an error occurs
     */
    public SysViewSAXEventGenerator(Node node, boolean noRecurse,
                                    boolean skipBinary,
                                    ContentHandler contentHandler)
            throws RepositoryException {
        super(node, noRecurse, skipBinary, contentHandler);
        resolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Node node, int level)
            throws RepositoryException, SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addNamespacePrefixes(level, attrs);
        // name attribute
        String nodeName;
        if (node.getDepth() == 0) {
            // root node needs a name
            nodeName = jcrRoot;
        } else {
            // encode node name to make sure it's a valid xml name
            nodeName = node.getName();
        }

        addAttribute(attrs, NameConstants.SV_NAME, CDATA_TYPE, nodeName);
        // start node element
        startElement(NameConstants.SV_NODE, attrs);
    }

    /**
     * {@inheritDoc}
     */
    protected void enteringProperties(Node node, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    protected void leavingProperties(Node node, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Node node, int level)
            throws RepositoryException, SAXException {
        // end node element
        endElement(NameConstants.SV_NODE);
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Property prop, int level)
            throws RepositoryException, SAXException {
        AttributesImpl attrs = new AttributesImpl();
        // name attribute
        addAttribute(attrs, NameConstants.SV_NAME, CDATA_TYPE, prop.getName());
        // type attribute
        try {
            String typeName = PropertyType.nameFromValue(prop.getType());
            addAttribute(attrs, NameConstants.SV_TYPE, ENUMERATION_TYPE, typeName);
        } catch (IllegalArgumentException e) {
            // should never be getting here
            throw new RepositoryException(
                    "unexpected property-type ordinal: " + prop.getType(), e);
        }

        // start property element
        startElement(NameConstants.SV_PROPERTY, attrs);

        // values
        if (prop.getType() == PropertyType.BINARY && skipBinary) {
            // empty value element
            startElement(NameConstants.SV_VALUE, new AttributesImpl());
            endElement(NameConstants.SV_VALUE);
        } else {
            boolean multiValued = prop.getDefinition().isMultiple();
            Value[] vals;
            if (multiValued) {
                vals = prop.getValues();
            } else {
                vals = new Value[]{prop.getValue()};
            }
            for (int i = 0; i < vals.length; i++) {
                Value val = vals[i];

                Attributes attributes = ATTRS_EMPTY;
                boolean mustSendBinary = false;

                if (val.getType() != PropertyType.BINARY) {
                    String ser = val.getString();
                    for (int ci = 0; ci < ser.length() && mustSendBinary == false; ci++) {
                        char c = ser.charAt(ci);
                        if (c >= 0 && c < 32 && c != '\r' && c != '\n' && c != '\t') {
                            mustSendBinary = true;
                        }
                    }

                    if (mustSendBinary) {
                        contentHandler.startPrefixMapping(NS_XMLSCHEMA_INSTANCE_PREFIX, NS_XMLSCHEMA_INSTANCE_URI);
                        contentHandler.startPrefixMapping(NS_XMLSCHEMA_PREFIX, NS_XMLSCHEMA_URI);
                        attributes = ATTRS_BINARY_ENCODED_VALUE;
                    }
                }

                // start value element
                startElement(NameConstants.SV_VALUE, attributes);

                // characters
                Writer writer = new Writer() {
                    public void close() /*throws IOException*/ {
                    }

                    public void flush() /*throws IOException*/ {
                    }

                    public void write(char[] cbuf, int off, int len) throws IOException {
                        try {
                            contentHandler.characters(cbuf, off, len);
                        } catch (SAXException se) {
                            throw new IOException(se.toString());
                        }
                    }
                };
                try {
                    ValueHelper.serialize(val, false, mustSendBinary, writer);
                    // no need to close our Writer implementation
                    //writer.close();
                } catch (IOException ioe) {
                    // check if the exception wraps a SAXException
                    // (see Writer.write(char[], int, int) above)
                    Throwable t = ioe.getCause();
                    if (t != null && t instanceof SAXException) {
                        throw (SAXException) t;
                    } else {
                        throw new SAXException(ioe);
                    }
                }

                // end value element
                endElement(NameConstants.SV_VALUE);

                if (mustSendBinary) {
                    contentHandler.endPrefixMapping(NS_XMLSCHEMA_INSTANCE_PREFIX);
                    contentHandler.endPrefixMapping(NS_XMLSCHEMA_PREFIX);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Property prop, int level)
            throws RepositoryException, SAXException {
        endElement(NameConstants.SV_PROPERTY);
    }

    //-------------------------------------------------------------< private >

    /**
     * Adds an attribute to the given XML attribute set. The local part of
     * the given {@link Name} is assumed to be a valid XML NCName, i.e. it
     * won't be encoded.
     *
     * @param attributes the XML attribute set
     * @param name name of the attribute
     * @param type XML type of the attribute
     * @param value value of the attribute
     * @throws NamespaceException if the namespace of the attribute is not found
     */
    private void addAttribute(
            AttributesImpl attributes, Name name, String type, String value)
            throws NamespaceException {
        attributes.addAttribute(
                name.getNamespaceURI(), name.getLocalName(),
                resolver.getJCRName(name), type, value);
    }

    /**
     * Starts an XML element. The local part of the given {@link Name} is
     * assumed to be a valid XML NCName, i.e. it won't be encoded.
     *
     * @param name name of the element
     * @param attributes XML attributes
     * @throws NamespaceException if the namespace of the element is not found
     */
    private void startElement(Name name, Attributes attributes)
            throws NamespaceException, SAXException {
        contentHandler.startElement(
                name.getNamespaceURI(), name.getLocalName(),
                resolver.getJCRName(name), attributes);
    }

    /**
     * Ends an XML element. The local part of the given {@link Name} is
     * assumed to be a valid XML NCName, i.e. it won't be encoded.
     *
     * @param name name of the element
     * @throws NamespaceException if the namespace of the element is not found
     */
    private void endElement(Name name)
            throws NamespaceException, SAXException {
        contentHandler.endElement(
                name.getNamespaceURI(), name.getLocalName(),
                resolver.getJCRName(name));
    }

}
