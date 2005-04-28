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

import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.util.ISO9075;
import org.apache.jackrabbit.core.value.ValueHelper;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>DocViewSAXEventGenerator</code> instance can be used to generate
 * SAX events representing the serialized form of an item in Document View XML.
 */
public class DocViewSAXEventGenerator extends AbstractSAXEventGenerator {

    private static Logger log = Logger.getLogger(DocViewSAXEventGenerator.class);

    public static final String CDATA_TYPE = "CDATA";

    // used to temporarily store properties of a node
    private final List props;

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties
     *                       will be serialized; otherwise the entire hierarchy
     *                       starting with <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be
     *                       serialized.
     * @param session        the session to be used for resolving namespace
     *                       mappings
     * @param contentHandler the content handler to feed the SAX events to
     */
    public DocViewSAXEventGenerator(NodeImpl node, boolean noRecurse,
                                    boolean skipBinary,
                                    SessionImpl session,
                                    ContentHandler contentHandler) {
        super(node, noRecurse, skipBinary, session, contentHandler);

        props = new ArrayList();
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(NodeImpl node, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    protected void enteringProperties(NodeImpl node, int level)
            throws RepositoryException, SAXException {
        // reset list of properties
        props.clear();
    }

    /**
     * {@inheritDoc}
     */
    protected void leavingProperties(NodeImpl node, int level)
            throws RepositoryException, SAXException {
        QName name = node.getQName();
        if (name.equals(JCR_XMLTEXT)) {
            // the node represents xml character data
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                PropertyImpl prop = (PropertyImpl) iter.next();
                QName propName = prop.getQName();
                if (propName.equals(JCR_XMLCHARACTERS)) {
                    // assume jcr:xmlcharacters is single-valued
                    char[] chars = prop.getValue().getString().toCharArray();
                    contentHandler.characters(chars, 0, chars.length);
                }
            }
        } else {
            // regular node

            // encode node name to make sure it's a valid xml name
            name = ISO9075.encode(name);
            // element name
            String elemName;
            try {
                if (node.getDepth() == 0) {
                    // root node needs a name
                    elemName = JCR_ROOT.toJCRName(session.getNamespaceResolver());
                } else {
                    elemName = name.toJCRName(session.getNamespaceResolver());
                }
            } catch (NoPrefixDeclaredException npde) {
                // should never get here...
                String msg = "internal error: encountered unregistered namespace";
                log.debug(msg);
                throw new RepositoryException(msg, npde);
            }

            // attributes (properties)
            AttributesImpl attrs = new AttributesImpl();
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                PropertyImpl prop = (PropertyImpl) iter.next();
                QName propName = prop.getQName();
                // encode property name to make sure it's a valid xml name
                propName = ISO9075.encode(propName);
                // attribute name
                String attrName;
                try {
                    attrName = propName.toJCRName(session.getNamespaceResolver());
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    String msg =
                            "internal error: encountered unregistered namespace";
                    log.debug(msg);
                    throw new RepositoryException(msg, npde);
                }
                // attribute value
                if (prop.getType() == PropertyType.BINARY && skipBinary) {
                    // add empty attribute
                    attrs.addAttribute(propName.getNamespaceURI(),
                            propName.getLocalName(), attrName, CDATA_TYPE, "");
                } else {
                    StringBuffer attrValue = new StringBuffer();
                    // process property value(s)
                    boolean multiValued = prop.getDefinition().isMultiple();
                    Value[] vals;
                    if (multiValued) {
                        vals = prop.getValues();
                    } else {
                        vals = new Value[]{prop.getValue()};
                    }
                    for (int i = 0; i < vals.length; i++) {
                        if (i > 0) {
                            // use space as delimiter for multi-valued properties
                            attrValue.append(" ");
                        }
                        attrValue.append(ValueHelper.serialize(vals[i], true));
                    }
                    attrs.addAttribute(propName.getNamespaceURI(),
                            propName.getLocalName(), attrName, CDATA_TYPE,
                            attrValue.toString());
                }
            }
            // start element (node)
            contentHandler.startElement(name.getNamespaceURI(),
                    name.getLocalName(), elemName, attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(NodeImpl node, int level)
            throws RepositoryException, SAXException {
        QName name = node.getQName();
        if (name.equals(JCR_XMLTEXT)) {
            // the node represents xml character data
            // (already processed in leavingProperties(NodeImpl, int)
            return;
        }
        // element name
        String elemName;
        try {
            if (node.getDepth() == 0) {
                // root node needs a name
                elemName = JCR_ROOT.toJCRName(session.getNamespaceResolver());
            } else {
                elemName = name.toJCRName(session.getNamespaceResolver());
            }
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace";
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }

        // end element (node)
        contentHandler.endElement(name.getNamespaceURI(), name.getLocalName(),
                elemName);
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(PropertyImpl prop, int level)
            throws RepositoryException, SAXException {
        props.add(prop);
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(PropertyImpl prop, int level)
            throws RepositoryException, SAXException {
        // nop
    }
}
