/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.Base64;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

/**
 * A <code>SysViewSAXEventGenerator</code> instance can be used to generate SAX events
 * representing the serialized form of an item in System View XML.
 */
public class SysViewSAXEventGenerator extends AbstractSAXEventGenerator {

    private static Logger log = Logger.getLogger(SysViewSAXEventGenerator.class);

    protected final HierarchyManager hierMgr;

    /**
     * The XML elements and attributes used in serialization
     */
    public static final String NODE_ELEMENT = "node";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String VALUE_ELEMENT = "value";

    public static final String NAME_ATTRIBUTE = "name";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String CDATA_TYPE = "CDATA";
    public static final String ENUMERATION_TYPE = "ENUMERATION";

    public static final String NS_SV_PREFIX = NamespaceRegistryImpl.NS_SV_PREFIX;
    public static final String NS_SV_URI = NamespaceRegistryImpl.NS_SV_URI;

    /**
     * Constructor
     *
     * @param nodeState      the node state which should be serialized
     * @param nodeName       name of the node to be serialized
     * @param noRecurse      if true, only <code>nodeState</code> and its properties will
     *                       be serialized; otherwise the entire hierarchy starting with
     *                       <code>nodeState</code> will be serialized.
     * @param binaryAsLink   specifies if binary properties are turned into links
     * @param stateProvider  item state provider for retrieving child item state
     * @param nsReg          the namespace registry to be used for namespace declarations
     * @param accessMgr      the access manager
     * @param hierMgr        the hierarchy manager used to resolve paths
     * @param contentHandler the content handler to feed the SAX events to
     */
    public SysViewSAXEventGenerator(NodeState nodeState, QName nodeName,
                                    boolean noRecurse, boolean binaryAsLink,
                                    ItemStateManager stateProvider,
                                    NamespaceRegistryImpl nsReg,
                                    AccessManagerImpl accessMgr,
                                    HierarchyManager hierMgr,
                                    ContentHandler contentHandler) {
        super(nodeState, nodeName, noRecurse, binaryAsLink,
                stateProvider, nsReg, accessMgr, contentHandler);
        this.hierMgr = hierMgr;
    }

    /**
     * @see AbstractSAXEventGenerator#entering(NodeState, QName, int)
     */
    protected void entering(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        AttributesImpl attrs = new AttributesImpl();
        // name attribute
        String nodeName;
        try {
            if (state.getParentUUIDs().size() == 0) {
                // use dummy name for root node
                nodeName = NODENAME_ROOT.toJCRName(nsReg);
            } else {
                nodeName = name.toJCRName(nsReg);
            }
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace";
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
        attrs.addAttribute(NS_SV_URI, NAME_ATTRIBUTE, NS_SV_PREFIX + ":" + NAME_ATTRIBUTE, CDATA_TYPE, nodeName);
        // start node element
        contentHandler.startElement(NS_SV_URI, NODE_ELEMENT, NS_SV_PREFIX + ":" + NODE_ELEMENT, attrs);
    }

    /**
     * @see AbstractSAXEventGenerator#enteringProperties(NodeState, QName, int)
     */
    protected void enteringProperties(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * @see AbstractSAXEventGenerator#leavingProperties(NodeState, QName, int)
     */
    protected void leavingProperties(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * @see AbstractSAXEventGenerator#leaving(NodeState, QName, int)
     */
    protected void leaving(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        // end node element
        contentHandler.endElement(NS_SV_URI, NODE_ELEMENT, NS_SV_PREFIX + ":" + NODE_ELEMENT);
    }

    /**
     * @see AbstractSAXEventGenerator#entering(PropertyState, int)
     */
    protected void entering(PropertyState state, int level)
            throws RepositoryException, SAXException {
        String name;
        try {
            name = state.getName().toJCRName(nsReg);
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace";
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
        AttributesImpl attrs = new AttributesImpl();
        // name attribute
        attrs.addAttribute(NS_SV_URI, NAME_ATTRIBUTE, NS_SV_PREFIX + ":" + NAME_ATTRIBUTE, CDATA_TYPE, name);
        // type attribute
        int type = state.getType();
        String typeName;
        try {
            typeName = PropertyType.nameFromValue(type);
        } catch (IllegalArgumentException iae) {
            // should never be getting here
            throw new RepositoryException("unexpected property-type ordinal: " + type, iae);
        }
        if (type == PropertyType.BINARY && binaryAsLink) {
            typeName = PropertyType.TYPENAME_PATH;
        }
        attrs.addAttribute(NS_SV_URI, TYPE_ATTRIBUTE, NS_SV_PREFIX + ":" + TYPE_ATTRIBUTE, ENUMERATION_TYPE, typeName);

        // start property element
        contentHandler.startElement(NS_SV_URI, PROPERTY_ELEMENT, NS_SV_PREFIX + ":" + PROPERTY_ELEMENT, attrs);

        // values
        InternalValue[] values = state.getValues();
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    // start value element
                    contentHandler.startElement(NS_SV_URI, VALUE_ELEMENT, NS_SV_PREFIX + ":" + VALUE_ELEMENT, new AttributesImpl());

                    // characters
                    if (type == PropertyType.BINARY) {
                        if (binaryAsLink) {
                            String path;
                            try {
                                path = hierMgr.getPath(new PropertyId(state.getParentUUID(), state.getName())).toJCRPath(nsReg);
                            } catch (NoPrefixDeclaredException npde) {
                                // should never get here...
                                String msg = "internal error: encountered unregistered namespace";
                                log.debug(msg);
                                throw new RepositoryException(msg, npde);
                            }
                            char[] chars = path.toCharArray();
                            contentHandler.characters(chars, 0, chars.length);
                        } else {
                            // binary data, base64 encoding required
                            BLOBFileValue blob = (BLOBFileValue) values[i].internalValue();
                            InputStream in = blob.getStream();
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
                                Base64.encode(in, writer);
                                in.close();
                                writer.close();
                            } catch (IOException ioe) {
                                // check if the exception wraps a SAXException
                                Throwable t = ioe.getCause();
                                if (t != null && t instanceof SAXException) {
                                    throw (SAXException) t;
                                } else {
                                    throw new SAXException(ioe);
                                }
                            }
                        }
                    } else {
                        char[] chars = values[i].toJCRValue(nsReg).getString().toCharArray();
                        contentHandler.characters(chars, 0, chars.length);
                    }

                    // end value element
                    contentHandler.endElement(NS_SV_URI, VALUE_ELEMENT, NS_SV_PREFIX + ":" + VALUE_ELEMENT);
                }
            }
        }
    }

    /**
     * @see AbstractSAXEventGenerator#leaving(PropertyState, int)
     */
    protected void leaving(PropertyState state, int level)
            throws RepositoryException, SAXException {
        contentHandler.endElement(NS_SV_URI, PROPERTY_ELEMENT, NS_SV_PREFIX + ":" + PROPERTY_ELEMENT);
    }
}
