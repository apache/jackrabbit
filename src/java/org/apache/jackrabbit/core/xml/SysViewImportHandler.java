/*
 * Copyright 2004 The Apache Software Foundation.
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
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.util.Base64;
import org.apache.jackrabbit.core.util.ValueHelper;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.jcr.*;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.nodetype.ConstraintViolationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * <code>SysViewImportHandler</code>  ...
 */
class SysViewImportHandler extends DefaultHandler {
    private static Logger log = Logger.getLogger(SysViewImportHandler.class);

    private SessionImpl session;

    /**
     * stack of ImportState instances; an instance is pushed onto the stack
     * in the startElement method every time a sv:node element is encountered;
     * the same instance is popped from the stack in the endElement method
     * when the corresponding sv:node element is encountered.
     */
    private Stack stateStack = new Stack();

    /** list of all reference properties that need to be adjusted */
    private LinkedList references = new LinkedList();

    /** list of all mix:referenceable */
    private HashMap referees = new HashMap();

    /**
     * fields used temporarily while processing sv:property and sv:value elements
     */
    private QName currentPropName;
    private int currentPropType = PropertyType.UNDEFINED;
    private ArrayList currentPropValues;
    private StringBuffer currentPropValue;

    SysViewImportHandler(NodeImpl importTargetNode, SessionImpl session) {
	ImportState state = new ImportState();
	state.node = importTargetNode;
	this.session = session;
	stateStack.push(state);
    }

    //-------------------------------------------------------< ContentHandler >
    /**
     * @see ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
	try {
	    String elemName;
	    String nsURI;
	    if (namespaceURI != null && !"".equals(namespaceURI)) {
		nsURI = namespaceURI;
		elemName = localName;
	    } else {
		try {
		    nsURI = QName.fromJCRName(qName, session.getNamespaceResolver()).getNamespaceURI();
		    elemName = QName.fromJCRName(qName, session.getNamespaceResolver()).getLocalName();
		} catch (BaseException e) {
		    // should never happen...
		    String msg = "internal error: failed to parse/resolve element name " + qName;
		    log.error(msg, e);
		    throw new SAXException(msg, e);
		}
	    }
	    // check namespace
	    if (!NamespaceRegistryImpl.NS_SV_URI.equals(nsURI)) {
		throw new SAXException(new InvalidSerializedDataException("invalid namespace for element in system view xml document: " + nsURI));
	    }
	    // check element name
	    if (SysViewSAXEventGenerator.NODE_ELEMENT.equals(elemName)) {
		// sv:node element

		// node name (value of sv:name attribute)
		String name = atts.getValue(SysViewSAXEventGenerator.NS_SV_URI, SysViewSAXEventGenerator.NAME_ATTRIBUTE);
		if (name == null) {
		    // try qualified name
		    name = atts.getValue(SysViewSAXEventGenerator.NS_SV_PREFIX + ":" + SysViewSAXEventGenerator.NAME_ATTRIBUTE);
		}
		if (name == null) {
		    throw new SAXException(new InvalidSerializedDataException("missing mandatory sv:name attributeof element sv:node"));
		}

		ImportState current = (ImportState) stateStack.peek();
		if (current.node == null) {
		    // need to create current node first
		    createNode(current);
		}

		// push new ImportState instance onto the stack
		ImportState state = new ImportState();
		state.parent = current.node;
		try {
		    state.nodeName = QName.fromJCRName(name, session.getNamespaceResolver());
		} catch (IllegalNameException ine) {
		    throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, ine));
		} catch (UnknownPrefixException upe) {
		    throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, upe));
		}
		stateStack.push(state);
	    } else if (SysViewSAXEventGenerator.PROPERTY_ELEMENT.equals(elemName)) {
		// sv:property element

		// reset temp fields
		currentPropValues = new ArrayList();

		// property name (value of sv:name attribute)
		String name = atts.getValue(SysViewSAXEventGenerator.NS_SV_URI, SysViewSAXEventGenerator.NAME_ATTRIBUTE);
		if (name == null) {
		    // try qualified name
		    name = atts.getValue(SysViewSAXEventGenerator.NS_SV_PREFIX + ":" + SysViewSAXEventGenerator.NAME_ATTRIBUTE);
		}
		if (name == null) {
		    throw new SAXException(new InvalidSerializedDataException("missing mandatory sv:name attributeof element sv:property"));
		}
		try {
		    currentPropName = QName.fromJCRName(name, session.getNamespaceResolver());
		} catch (IllegalNameException ine) {
		    throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, ine));
		} catch (UnknownPrefixException upe) {
		    throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, upe));
		}
		// property type (sv:type attribute)
		String type = atts.getValue(SysViewSAXEventGenerator.NS_SV_URI, SysViewSAXEventGenerator.TYPE_ATTRIBUTE);
		if (type == null) {
		    // try qualified name
		    type = atts.getValue(SysViewSAXEventGenerator.NS_SV_PREFIX + ":" + SysViewSAXEventGenerator.TYPE_ATTRIBUTE);
		}
		if (type == null) {
		    throw new SAXException(new InvalidSerializedDataException("missing mandatory sv:type attributeof element sv:property"));
		}
		currentPropType = PropertyType.valueFromName(type);
	    } else if (SysViewSAXEventGenerator.VALUE_ELEMENT.equals(elemName)) {
		// sv:value element

		// reset temp fields
		currentPropValue = new StringBuffer();
	    } else {
		throw new SAXException(new InvalidSerializedDataException("unexpected element found in system view xml document: " + elemName));
	    }
	} catch (RepositoryException re) {
	    throw new SAXException(re);
	}
    }

    /**
     * @see ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
	if (currentPropValue != null) {
	    // property value (character data of sv:value element)
	    currentPropValue.append(ch, start, length);
	}
    }

    /**
     * @see ContentHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
	try {
	    String elemName;
	    if (localName != null && !"".equals(localName)) {
		elemName = localName;
	    } else {
		try {
		    elemName = QName.fromJCRName(qName, session.getNamespaceResolver()).getLocalName();
		} catch (BaseException e) {
		    // should never happen...
		    String msg = "internal error: failed to parse/resolve element name " + qName;
		    log.error(msg, e);
		    throw new SAXException(msg, e);
		}
	    }
	    // check element name
	    ImportState current = (ImportState) stateStack.peek();
	    if (SysViewSAXEventGenerator.NODE_ELEMENT.equals(elemName)) {
		// sv:node element
		if (current.node == null) {
		    // need to create current node first
		    createNode(current);
		}
		// pop current state from stack
		stateStack.pop();
	    } else if (SysViewSAXEventGenerator.PROPERTY_ELEMENT.equals(elemName)) {
		// sv:property element

		// check if all system properties (jcr:primaryType, jcr:uuid etc.)
		// have been collected and create node as necessary
		if (currentPropName.equals(ItemImpl.PROPNAME_PRIMARYTYPE)) {
		    try {
			current.primaryType = QName.fromJCRName((String) currentPropValues.get(0), session.getNamespaceResolver());
		    } catch (IllegalNameException ine) {
			throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + currentPropValues.get(0), ine));
		    } catch (UnknownPrefixException upe) {
			throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + currentPropValues.get(0), upe));
		    }
		} else if (currentPropName.equals(ItemImpl.PROPNAME_MIXINTYPES)) {
		    if (current.mixinTypes == null) {
			current.mixinTypes = new ArrayList(currentPropValues.size());
		    }
		    for (int i = 0; i < currentPropValues.size(); i++) {
			try {
			    QName mixin = QName.fromJCRName((String) currentPropValues.get(i), session.getNamespaceResolver());
			    current.mixinTypes.add(mixin);
			} catch (IllegalNameException ine) {
			    throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + currentPropValues.get(i), ine));
			} catch (UnknownPrefixException upe) {
			    throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + currentPropValues.get(i), upe));
			}
		    }
		} else if (currentPropName.equals(ItemImpl.PROPNAME_UUID)) {
		    current.uuid = (String) currentPropValues.get(0);
		    // jcr:uuid is the last system property; we can assume that all
		    // required system properties have been collected by now
		    if (current.node == null) {
			// now that we're collected all required system properties
			// we're ready to create the node
			createNode(current);
		    }
		} else if (currentPropName.equals(VersionManager.PROPNAME_BASE_VERSION)) {
                    // ignore so far
		} else if (currentPropName.equals(VersionManager.PROPNAME_VERSION_HISTORY)) {
                    // ignore so far
		} else if (currentPropName.equals(VersionManager.PROPNAME_PREDECESSORS)) {
                    // ignore so far
		} else if (currentPropName.equals(VersionManager.PROPNAME_IS_CHECKED_OUT)) {
                    // ignore so far
		} else {
		    // non-system property encountered; we can assume that all
		    // required system properties have been collected by now
		    if (current.node == null) {
			// now that we're collected all required system properties
			// we're ready to create the node
			createNode(current);
		    }

		    // convert values to native type and set property
		    Value[] vals = new Value[currentPropValues.size()];
		    for (int i = 0; i < currentPropValues.size(); i++) {
			String value = (String) currentPropValues.get(i);
			if (currentPropType == PropertyType.BINARY) {
			    // base64 encoded binary value
			    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    try {
				Base64.decode(value, baos);
				baos.close();
			    } catch (IOException ioe) {
				throw new SAXException("failed to decode binary value", ioe);
			    }
			    vals[i] = new BinaryValue(baos.toByteArray());
			} else {
			    vals[i] = ValueHelper.convert(value, currentPropType);
			}
		    }
		    if (current.node.hasProperty(currentPropName)) {
			PropertyDef def = current.node.getProperty(currentPropName).getDefinition();
			if (def.isProtected()) {
			    // ignore protected property
			    // reset temp fields and get outta here
			    currentPropValues = null;
			    return;
			}
		    }
		    // multi- or single-valued property?
		    if (vals.length == 1) {
			// could be single- or multi-valued (n == 1)
			try {
			    // try setting single-value
			    current.node.setProperty(currentPropName, vals[0]);
			} catch (ValueFormatException vfe) {
                            // try setting value array
                            current.node.setProperty(currentPropName, vals);
			} catch (ConstraintViolationException vfe) {
			    // try setting value array
			    current.node.setProperty(currentPropName, vals);
			}
		    } else {
			// can only be multi-valued (n == 0 || n > 1)
			current.node.setProperty(currentPropName, vals);
		    }
                    // check if reference for later resolution
                    if (currentPropType==PropertyType.REFERENCE) {
                        references.add(current.node.getProperty(currentPropName));
                    }
		}

		// reset temp fields
		currentPropValues = null;
	    } else if (SysViewSAXEventGenerator.VALUE_ELEMENT.equals(elemName)) {
		// sv:value element
		currentPropValues.add(currentPropValue.toString());
		// reset temp fields
		currentPropValue = null;
	    } else {
		throw new SAXException(new InvalidSerializedDataException("invalid element in system view xml document: " + elemName));
	    }
	} catch (RepositoryException re) {
	    throw new SAXException(re);
	}
    }

    private void createNode(ImportState state) throws RepositoryException {
	if (state.primaryType == null) {
	    throw new InvalidSerializedDataException("missing mandatory jcr:primaryType property");
	}
	if (state.uuid != null) {
	    // @todo what are the semantics of the uuid with respect to import?
	    // move existing node with given uuid to current position?
	    // create with new uuid? what about reference values refering to given uuid?
	}
	if (state.parent.hasNode(state.nodeName)) {
            state.node = state.parent.getNode(state.nodeName);
	    NodeDef def = state.node.getDefinition();
	    if (def.isProtected()) {
                // @todo how to handle protected/auto-created child node?

            } else if (def.isAutoCreate()) {
		// @todo how to handle protected/auto-created child node?

	    }
	} else {
            state.node = (NodeImpl) state.parent.addNode(state.nodeName, state.primaryType);
            if (state.mixinTypes != null) {
                for (int i = 0; i < state.mixinTypes.size(); i++) {
                    NodeTypeImpl mixin = session.getNodeTypeManager().getNodeType((QName) state.mixinTypes.get(i));
                    state.node.addMixin(mixin.getName());
                }
            }
        }

        // check for mix:referenceable
        if (state.node.isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            log.info("adding refereee: ori=" + state.uuid + " new=" + state.node.getUUID());
            referees.put(state.uuid, state.node.getUUID());
        }
    }

    public void endDocument() throws SAXException {
        try {
            // adjust all reference properties
            Iterator iter = references.iterator();
            while (iter.hasNext()) {
                Property prop = (Property) iter.next();
                if (prop.getDefinition().isMultiple()) {
                    Value[] values = prop.getValues();
                    Value[] newVals = new Value[values.length];
                    for (int i=0; i<values.length; i++) {
                        Value val = values[i];
                        if (val.getType()==PropertyType.REFERENCE) {
                            String original = val.getString();
                            String adjusted = (String) referees.get(original);
                            if (adjusted==null) {
                                log.error("Reference " + original + " of property can not be adjusted! " + prop.getPath());
                                newVals[i] = val;
                            } else {
                                newVals[i] = new ReferenceValue(session.getNodeByUUID(adjusted));
                            }
                        } else {
                            newVals[i] = val;
                        }
                    }
                    prop.setValue(newVals);
                } else {
                    Value val = prop.getValue();
                    if (val.getType()==PropertyType.REFERENCE) {
                        String original = val.getString();
                        String adjusted = (String) referees.get(original);
                        if (adjusted==null) {
                            log.error("Reference " + original + " of property can not be adjusted! " + prop.getPath());
                        } else {
                            prop.setValue(session.getNodeByUUID(adjusted));
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while adjusting reference proerties: " + e.toString());
        }
    }


    //--------------------------------------------------------< inner classes >
    class ImportState {
	QName primaryType = null;
	ArrayList mixinTypes = null;
	String uuid = null;

	QName nodeName = null;
	NodeImpl parent = null;
	NodeImpl node = null;
    }
}
