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
import org.apache.xerces.util.XMLChar; 
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>DocViewSAXEventGenerator</code> instance can be used to generate
 * SAX events representing the serialized form of an item in Document View XML.
 */
public class DocViewSAXEventGenerator extends AbstractSAXEventGenerator {

    private static Logger log = Logger.getLogger(DocViewSAXEventGenerator.class);

    protected final HierarchyManager hierMgr;

    public static final String CDATA_TYPE = "CDATA";

    // jcr:xmltext
    public static final QName NODENAME_XMLTEXT =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "xmltext");
    // jcr:xmlcharacters
    public static final QName PROPNAME_XMLCHARACTERS =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "xmlcharacters");

    // used to temporarily store properties of a node
    private final List props;

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
    public DocViewSAXEventGenerator(NodeState nodeState, QName nodeName,
                                    boolean noRecurse, boolean binaryAsLink,
                                    ItemStateManager stateProvider,
                                    NamespaceRegistryImpl nsReg,
                                    AccessManagerImpl accessMgr,
                                    HierarchyManager hierMgr,
                                    ContentHandler contentHandler) {
        super(nodeState, nodeName, noRecurse, binaryAsLink,
                stateProvider, nsReg, accessMgr, contentHandler);
        this.hierMgr = hierMgr;

        props = new ArrayList();
    }

    /**
     * @see AbstractSAXEventGenerator#entering(NodeState, QName, int)
     */
    protected void entering(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * @see AbstractSAXEventGenerator#enteringProperties(NodeState, QName, int)
     */
    protected void enteringProperties(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        // reset list of properties
        props.clear();
    }

    /**
     * @see AbstractSAXEventGenerator#leavingProperties(NodeState, QName, int)
     */
    protected void leavingProperties(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        if (name.equals(NODENAME_XMLTEXT)) {
            // the node represents xml character data
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                PropertyState propState = (PropertyState) iter.next();
                QName propName = propState.getName();
                if (propName.equals(PROPNAME_XMLCHARACTERS)) {
                    InternalValue val = propState.getValues()[0];
                    char[] chars = val.toJCRValue(nsReg).getString().toCharArray();
                    contentHandler.characters(chars, 0, chars.length);
                }
            }
        } else {
            // regular node

            // element name
            String elemName;
            try {
                if (state.getParentUUIDs().size() == 0) {
                    // root node needs a name
                    elemName = NODENAME_ROOT.toJCRName(nsReg);
                } else {
                    elemName = name.toJCRName(nsReg);
                }
            } catch (NoPrefixDeclaredException npde) {
                // should never get here...
                String msg = "internal error: encountered unregistered namespace";
                log.debug(msg);
                throw new RepositoryException(msg, npde);
            }
            if (!XMLChar.isValidName(elemName)) {
                throw new InvalidSerializedDataException("the node name is not a valid xml element name: " + elemName);
            }

            // attributes (properties)
            AttributesImpl attrs = new AttributesImpl();
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                PropertyState propState = (PropertyState) iter.next();
                QName propName = propState.getName();
                // attribute name
                String attrName;
                try {
                    attrName = propName.toJCRName(nsReg);
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    String msg = "internal error: encountered unregistered namespace";
                    log.debug(msg);
                    throw new RepositoryException(msg, npde);
                }
                if (!XMLChar.isValidName(attrName)) {
                    throw new InvalidSerializedDataException("the property name is not a valid xml attribute name: " + attrName);
                }
                // attribute value
                String attrValue;
                int type = propState.getType();
                // process property value (guaranteed to be single-valued and non-null)
                InternalValue val = propState.getValues()[0];
                if (type == PropertyType.BINARY) {
                    if (binaryAsLink) {
                        try {
                            attrValue = hierMgr.getPath(new PropertyId(propState.getParentUUID(), propState.getName())).toJCRPath(nsReg);
                        } catch (NoPrefixDeclaredException npde) {
                            // should never get here...
                            String msg = "internal error: encountered unregistered namespace";
                            log.debug(msg);
                            throw new RepositoryException(msg, npde);
                        }
                    } else {
                        // binary data, base64 encoding required
                        BLOBFileValue blob = (BLOBFileValue) val.internalValue();
                        InputStream in = blob.getStream();
                        StringWriter writer = new StringWriter();
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
                        attrValue = writer.toString();
                    }
                } else {
                    attrValue = val.toJCRValue(nsReg).getString();
                }
                attrs.addAttribute(propName.getNamespaceURI(), propName.getLocalName(), attrName, CDATA_TYPE, attrValue);
            }
            // start element (node)
            contentHandler.startElement(name.getNamespaceURI(), name.getLocalName(), elemName, attrs);
        }
    }

    /**
     * @see AbstractSAXEventGenerator#leaving(NodeState, QName, int)
     */
    protected void leaving(NodeState state, QName name, int level)
            throws RepositoryException, SAXException {
        if (name.equals(NODENAME_XMLTEXT)) {
            // the node represents xml character data
            // (already processed in leavingProperties(NodeState, QName, int)
            return;
        }
        // element name
        String elemName;
        try {
            if (state.getParentUUIDs().size() == 0) {
                // root node needs a name
                elemName = NODENAME_ROOT.toJCRName(nsReg);
            } else {
                elemName = name.toJCRName(nsReg);
            }
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace";
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }

        // end element (node)
        contentHandler.endElement(name.getNamespaceURI(), name.getLocalName(), elemName);
    }

    /**
     * @see AbstractSAXEventGenerator#entering(PropertyState, int)
     */
    protected void entering(PropertyState state, int level)
            throws RepositoryException, SAXException {
        // @todo should properties declared in the 'jcr:' namespace be skipped in the document view export?
/*
	// skip 'system' properties (e.g. jcr:primaryType, jcr:mixinTypes, jcr:uuid)
	if (NamespaceRegistryImpl.NS_JCR_URI.equals(state.getName().getNamespaceURI())) {
	    return;
	}
*/
        // collect property state in temporary list (will be processed in leavingProperties(NodeState, QName, int)
        InternalValue[] values = state.getValues();
        if (values != null && values.length > 0) {
            if (values.length != 1) {
                throw new InvalidSerializedDataException("unable to serialize multi-valued property in document view: " + state.getName().getLocalName());
            }
            if (values[0] != null) {
                props.add(state);
            }
        }
    }

    /**
     * @see AbstractSAXEventGenerator#leaving(PropertyState, int)
     */
    protected void leaving(PropertyState state, int level)
            throws RepositoryException, SAXException {
        // nop
    }
}
