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

// @formatter:off
package org.apache.jackrabbit.server.remoting.davex;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.commons.json.JsonParser;
import org.apache.jackrabbit.commons.webdav.JcrValueType;
import org.apache.jackrabbit.server.util.RequestData;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/** <code>JsonDiffHandler</code>... */
class JsonDiffHandler implements DiffHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonDiffHandler.class);

    private static final String ORDER_POSITION_AFTER = "#after";
    private static final String ORDER_POSITION_BEFORE = "#before";
    private static final String ORDER_POSITION_FIRST = "#first";
    private static final String ORDER_POSITION_LAST = "#last";

    private final Session session;
    private final ValueFactory vf;
    private final String requestItemPath;
    private final RequestData data;

    JsonDiffHandler(Session session, String requestItemPath, RequestData data) throws RepositoryException {
        this.session = session;
        this.requestItemPath = requestItemPath;
        this.data = data;
        vf = session.getValueFactory();
    }

    //--------------------------------------------------------< DiffHandler >---
    /**
     * @see DiffHandler#addNode(String, String)
     */
    public void addNode(String targetPath, String diffValue) throws DiffException {
        if (diffValue == null || !(diffValue.startsWith("{") && diffValue.endsWith("}"))) {
            throw new DiffException("Invalid 'addNode' value '" + diffValue + "'");
        }

        try {
            String itemPath = getItemPath(targetPath);
            String parentPath = Text.getRelativeParent(itemPath, 1);
            String nodeName = Text.getName(itemPath);

            addNode(parentPath, nodeName, diffValue);

        } catch (RepositoryException e) {
            throw new DiffException(e.getMessage(), e);
        }
    }

    /**
     * @see DiffHandler#setProperty(String, String) 
     */
    public void setProperty(String targetPath, String diffValue) throws DiffException {
        try {
            String itemPath = getItemPath(targetPath);
            Item item = session.getItem(Text.getRelativeParent(itemPath, 1));
            if (!item.isNode()) {
                throw new DiffException("No such node " + itemPath, new ItemNotFoundException(itemPath));
            }

            Node parent = (Node) item;
            String propName = Text.getName(itemPath);

            if (JcrConstants.JCR_MIXINTYPES.equals(propName)) {
                setMixins(parent, extractValuesFromRequest(targetPath));
            } else if (JcrConstants.JCR_PRIMARYTYPE.equals(propName)) {
                setPrimaryType(parent, extractValuesFromRequest(targetPath));
            } else {
                if (diffValue == null || diffValue.length() == 0) {
                    // single valued property with value present in multipart.
                    Value[] vs = extractValuesFromRequest(targetPath);
                    if (vs.length == 0) {
                        if (parent.hasProperty(propName)) {
                            // avoid problems with single vs. multi valued props.
                            parent.getProperty(propName).remove();
                        } else {
                            // property does not exist -> stick to rule that missing
                            // [] indicates single valued.
                            parent.setProperty(propName, (Value) null);
                        }
                    } else if (vs.length == 1) {
                        parent.setProperty(propName, vs[0]);
                    } else {
                        throw new DiffException("Unexpected number of values in multipart. Was " + vs.length + " but expected 1.");
                    }
                } else if (diffValue.startsWith("[") && diffValue.endsWith("]")) {
                    // multivalued property
                    if (diffValue.length() == 2) {
                        // empty array OR values in multipart
                        Value[] vs = extractValuesFromRequest(targetPath);
                        parent.setProperty(propName, vs);
                    } else {
                        // json array
                        Value[] vs = extractValues(diffValue);
                        parent.setProperty(propName, vs);
                    }
                } else {
                    // single prop value included in the diff
                    Value v = extractValue(diffValue);
                    parent.setProperty(propName, v);
                }
            }
        } catch (RepositoryException e) {
            throw new DiffException(e.getMessage(), e);
        } catch (IOException e) {
            if (e instanceof DiffException) {
                throw (DiffException) e;
            } else {
                throw new DiffException(e.getMessage(), e);
            }
        }
    }

    /**
     * @see DiffHandler#remove(String, String) 
     */
    public void remove(String targetPath, String diffValue) throws DiffException {
        if (!(diffValue == null || diffValue.trim().length() == 0)) {
            throw new DiffException("'remove' may not have a diffValue.");
        }
        try {
            String itemPath = getItemPath(targetPath);
            session.getItem(itemPath).remove();
        } catch (RepositoryException e) {
            throw new DiffException(e.getMessage(), e);
        }
    }

    /**
     * @see DiffHandler#move(String, String) 
     */
    public void move(String targetPath, String diffValue) throws DiffException {
        if (diffValue == null || diffValue.length() == 0) {
            throw new DiffException("Invalid 'move' value '" + diffValue + "'");
        }
        try {
            String srcPath = getItemPath(targetPath);
            String orderPosition = getOrderPosition(diffValue);
            if (orderPosition == null) {
                // simple move
                String destPath = getItemPath(diffValue);
                session.move(srcPath, destPath);
            } else {
                String srcName = Text.getName(srcPath);
                int pos = diffValue.lastIndexOf('#');
                String destName = (pos == 0) ? null : Text.getName(diffValue.substring(0, pos));

                Item item = session.getItem(Text.getRelativeParent(srcPath, 1));
                if (!item.isNode()) {
                    throw new ItemNotFoundException(srcPath);
                }
                Node parent = (Node) item;

                if (ORDER_POSITION_FIRST.equals(orderPosition)) {
                    if (destName != null) {
                        throw new DiffException(ORDER_POSITION_FIRST + " may not have a leading destination.");
                    }
                    destName = Text.getName(parent.getNodes().nextNode().getPath());
                    parent.orderBefore(srcName, destName);
                } else if (ORDER_POSITION_LAST.equals(orderPosition)) {
                    if (destName != null) {
                        throw new DiffException(ORDER_POSITION_LAST + " may not have a leading destination.");
                    }
                    parent.orderBefore(srcName, null);
                } else if (ORDER_POSITION_AFTER.equals(orderPosition)) {
                    if (destName == null) {
                        throw new DiffException(ORDER_POSITION_AFTER + " must have a leading destination.");
                    }
                    for (NodeIterator it = parent.getNodes(); it.hasNext();) {
                        Node child = it.nextNode();
                        if (destName.equals(child.getName())) {
                            if (it.hasNext()) {
                                destName = Text.getName(it.nextNode().getName());
                            } else {
                                destName = null;
                            }
                            break;
                        }
                    }
                    // reorder... if no child node matches the original destName
                    // the reorder will fail. no extra check.
                    parent.orderBefore(srcName, destName);
                } else {
                    // standard jcr reorder (before)
                    parent.orderBefore(srcName, destName);
                }
            }

        } catch (RepositoryException e) {
            throw new DiffException(e.getMessage(), e);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * 
     * @param diffPath
     * @return
     * @throws RepositoryException
     */
    String getItemPath(String diffPath) throws RepositoryException {
        StringBuffer itemPath;
        if (!diffPath.startsWith("/")) {
            // diff path is relative to the item path retrieved from the
            // request URI -> calculate item path.
            itemPath = new StringBuffer(requestItemPath);
            if (!requestItemPath.endsWith("/")) {
                itemPath.append('/');
            }
            itemPath.append(diffPath);
        } else {
            itemPath = new StringBuffer(diffPath);
        }
        return normalize(itemPath.toString());
    }

    private void addNode(String parentPath, String nodeName, String diffValue)
            throws DiffException, RepositoryException {
        Item item = session.getItem(parentPath);
        if (!item.isNode()) {
            throw new ItemNotFoundException(parentPath);
        }

        Node parent = (Node) item;
        try {
            NodeHandler hndlr = new NodeHandler(parent, nodeName);            
            new JsonParser(hndlr).parse(diffValue);
        } catch (IOException e) {
            if (e instanceof DiffException) {
                throw (DiffException) e;
            } else {
                throw new DiffException(e.getMessage(), e);
            }
        }
    }

    private static String normalize(String path) {
        if (path.indexOf('.') == -1) {
            return path;
        }
        String[]  elems = Text.explode(path, '/', false);
        LinkedList<String> queue = new LinkedList<String>();
        String last = "..";
        for (String segm : elems) {
            if ("..".equals(segm) && !"..".equals(last)) {
                queue.removeLast();
                if (queue.isEmpty()) {
                    last = "..";
                } else {
                    last = queue.getLast();
                }
            } else if (!".".equals(segm)) {
                last = segm;
                queue.add(last);
            }
        }
        return "/" + Text.implode(queue.toArray(new String[queue.size()]), "/");
    }
    
    private static Node importNode(Node parent, String nodeName, String ntName,
                                   String uuid) throws RepositoryException {

        String uri = "http://www.jcp.org/jcr/sv/1.0";
        String prefix = "sv:";

        ContentHandler ch = parent.getSession().getImportContentHandler(parent.getPath(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        try {
            ch.startDocument();

            String nN = "node";
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute(uri, "name", prefix + "name", "CDATA", nodeName);
            ch.startElement(uri, nN, prefix + nN, attrs);

            // primary node type
            String pN = "property";
            attrs = new AttributesImpl();
            attrs.addAttribute(uri, "name", prefix + "name", "CDATA", JcrConstants.JCR_PRIMARYTYPE);
            attrs.addAttribute(uri, "type", prefix + "type", "CDATA", PropertyType.nameFromValue(PropertyType.NAME));
            ch.startElement(uri, pN, prefix + pN, attrs);
            ch.startElement(uri, "value", prefix + "value", new AttributesImpl());
            char[] val = ntName.toCharArray();
            ch.characters(val, 0, val.length);
            ch.endElement(uri, "value", prefix + "value");
            ch.endElement(uri, pN, prefix + pN);

            // uuid
            attrs = new AttributesImpl();
            attrs.addAttribute(uri, "name", prefix + "name", "CDATA", JcrConstants.JCR_UUID);
            attrs.addAttribute(uri, "type", prefix + "type", "CDATA", PropertyType.nameFromValue(PropertyType.STRING));
            ch.startElement(uri, pN, prefix + pN, attrs);
            ch.startElement(uri, "value", prefix + "value", new AttributesImpl());
            val = uuid.toCharArray();
            ch.characters(val, 0, val.length);
            ch.endElement(uri, "value", prefix + "value");
            ch.endElement(uri, pN, prefix + pN);

            ch.endElement(uri, nN, prefix + nN);
            ch.endDocument();

        } catch (SAXException e) {
            throw new RepositoryException(e);
        }

        Node n = null;
        NodeIterator it = parent.getNodes(nodeName);
        while (it.hasNext()) {
            n = it.nextNode();
        }
        if (n == null) {
            throw new RepositoryException("Internal error: No child node added.");
        }
        return n;
    }

    private static void setPrimaryType(Node n, Value[] values) throws RepositoryException, DiffException {
        if (values.length == 1) {
            String ntName = values[0].getString();
            if (!ntName.equals(n.getPrimaryNodeType().getName())) {
                n.setPrimaryType(ntName);
            } // else: same primaryType as before -> nothing to do.
        } else {
            throw new DiffException("Invalid diff: jcr:primarytype cannot have multiple values, nor can it's value be removed.");
        }
    }

    private static void setMixins(Node n, Value[] values) throws RepositoryException {
        if (values.length == 0) {
            // remove all mixins
            NodeType[] mixins = n.getMixinNodeTypes();
            for (NodeType mixin : mixins) {
                String mixinName = mixin.getName();
                n.removeMixin(mixinName);
            }
        } else {
            List<String> newMixins = new ArrayList<String>(values.length);
            for (Value value : values) {
                newMixins.add(value.getString());
            }
            NodeType[] mixins = n.getMixinNodeTypes();
            for (NodeType mixin : mixins) {
                String mixinName = mixin.getName();
                if (!newMixins.remove(mixinName)) {
                    n.removeMixin(mixinName);
                }
            }
            for (String newMixinName : newMixins) {
                n.addMixin(newMixinName);
            }
        }
    }

    private static String getOrderPosition(String diffValue) {
        String position = null;
        if (diffValue.indexOf('#') > -1) {
            if (diffValue.endsWith(ORDER_POSITION_FIRST) ||
                    diffValue.endsWith(ORDER_POSITION_LAST) ||
                    diffValue.endsWith(ORDER_POSITION_BEFORE) ||
                    diffValue.endsWith(ORDER_POSITION_AFTER)) {
                position = diffValue.substring(diffValue.lastIndexOf('#'));
            } // else: apparently # is part of the move path.
        }
        return position;
    }

    private Value[] extractValuesFromRequest(String paramName) throws RepositoryException, IOException {
        ValueFactory vf = session.getValueFactory();
        Value[] vs;
        InputStream[] ins = data.getFileParameters(paramName);
        if (ins != null) {
            vs = new Value[ins.length];
            for (int i = 0; i < ins.length; i++) {
                vs[i] = vf.createValue(ins[i]);
            }
        } else {
            String[] strs = data.getParameterValues(paramName);
            if (strs == null) {
                vs = new Value[0];
            } else {
                List<Value> valList = new ArrayList<Value>(strs.length);
                for (int i = 0; i < strs.length; i++) {
                    if (strs[i] != null) {
                        String[] types = data.getParameterTypes(paramName);
                        int type = (types == null || types.length <= i) ? PropertyType.UNDEFINED : JcrValueType.typeFromContentType(types[i]);
                        if (type == PropertyType.UNDEFINED) {
                            valList.add(vf.createValue(strs[i]));
                        } else {
                            valList.add(vf.createValue(strs[i], type));
                        }
                    }
                }
                vs = valList.toArray(new Value[valList.size()]);
            }
        }
        return vs;
    }

    private Value extractValue(String diffValue) throws RepositoryException, DiffException, IOException {
        ValueHandler hndlr = new ValueHandler();
        // surround diff value { key : } to make it parsable
        new JsonParser(hndlr).parse("{\"a\":"+diffValue+"}");

        return hndlr.getValue();
    }

    private Value[] extractValues(String diffValue) throws RepositoryException, DiffException, IOException {
        ValuesHandler hndlr = new ValuesHandler();
        // surround diff value { key : } to make it parsable
        new JsonParser(hndlr).parse("{\"a\":"+diffValue+"}");
        
        return hndlr.getValues();
    }

    //--------------------------------------------------------------------------
    /**
     * Inner class used to parse a single value
     */
    private final class ValueHandler implements JsonHandler {
        private Value v;

        public void object() throws IOException {
            // ignore
        }
        public void endObject() throws IOException {
            // ignore
        }
        public void array() throws IOException {
            // ignore
        }
        public void endArray() throws IOException {
            // ignore
        }
        public void key(String key) throws IOException {
            // ignore
        }

        public void value(String value) throws IOException {
            v = (value == null) ? null : vf.createValue(value);
        }
        public void value(boolean value) throws IOException {
            v = vf.createValue(value);
        }
        public void value(long value) throws IOException {
            v = vf.createValue(value);
        }
        public void value(double value) throws IOException {
            v = vf.createValue(value);
        }

        private Value getValue() {
            return v;
        }
    }

    /**
     * Inner class used to parse the values from a simple json array
     */
    private final class ValuesHandler implements JsonHandler {
        private List<Value> values = new ArrayList<Value>();

        public void object() throws IOException {
            // ignore
        }
        public void endObject() throws IOException {
            // ignore
        }
        public void array() throws IOException {
            // ignore
        }
        public void endArray() throws IOException {
            // ignore
        }
        public void key(String key) throws IOException {
            // ignore
        }

        public void value(String value) throws IOException {
            if (value != null) {
                values.add(vf.createValue(value));
            } else {
                log.warn("Null element for a multivalued property -> Ignore.");
            }
        }
        public void value(boolean value) throws IOException {
            values.add(vf.createValue(value));
        }
        public void value(long value) throws IOException {
            values.add(vf.createValue(value));
        }
        public void value(double value) throws IOException {
            values.add(vf.createValue(value));
        }

        private Value[] getValues() {
            return values.toArray(new Value[values.size()]);
        }
    }
    
 // @formatter:on
    /**
     * Inner class for parsing a simple json object defining a node and its
     * child nodes and/or child properties
     */
	private final class NodeHandler implements JsonHandler {

		private Node parent;
		private String key;
		ContentHandler contentHandler;
		
		// use for parsing
		private static final int OBJECT = 0;
		private static final int ARRAY = 1;

		List<String> currentPropValues;

		// Stack of JSON events.
		private Stack<Integer> parse = new Stack<Integer>();
		
		// Stack of ImportState items.
		private Stack<ImportState> states = new Stack<ImportState>();

		private NodeHandler(Node parentNode, String nodeName)
				throws IOException {
			key = nodeName;
			this.parent = parentNode;
		}

		private void startImportNode(ImportState state)
				throws RepositoryException {

			String uri = "http://www.jcp.org/jcr/sv/1.0";
			String prefix = "sv:";

			final String TYPE_CDATA = "CDATA";
			char[] val = null;

			try {

				AttributesImpl attrSet = new AttributesImpl();
				String localName = "name";
				String qName = prefix + localName;
				attrSet.addAttribute(uri, localName, qName, TYPE_CDATA,
						state.getName());

				localName = "node";
				qName = prefix + localName;
				contentHandler.startElement(uri, localName, qName, attrSet);

				// node's primary type.
				attrSet = new AttributesImpl();
				localName = "name";
				qName = prefix + localName;
				attrSet.addAttribute(uri, localName, qName, TYPE_CDATA,
						JcrConstants.JCR_PRIMARYTYPE);

				localName = "type";
				qName = prefix + localName;
				attrSet.addAttribute(uri, localName, qName, TYPE_CDATA,
						PropertyType.nameFromValue(PropertyType.NAME));

				localName = "property";
				qName = prefix + localName;
				contentHandler.startElement(uri, localName, qName, attrSet);

				localName = "value";
				qName = prefix + localName;
				contentHandler.startElement(uri, localName, qName,
						new AttributesImpl());
				val = state.getNodeTypeName().toCharArray();
				contentHandler.characters(val, 0, val.length);

				contentHandler.endElement(uri, localName, qName);

				localName = "property";
				qName = prefix + localName;
				contentHandler.endElement(uri, localName, qName);

				// node's uuid
				if (state.getUUID() != null) {
					attrSet = new AttributesImpl();
					localName = "name";
					qName = prefix + localName;
					attrSet.addAttribute(uri, localName, qName, TYPE_CDATA,
							state.getUUID());

					localName = "type";
					qName = prefix + localName;
					attrSet.addAttribute(uri, localName, qName, TYPE_CDATA,
							PropertyType.nameFromValue(PropertyType.STRING));

					localName = "property";
					qName = prefix + localName;
					contentHandler.startElement(uri, localName, qName, attrSet);

					localName = "value";
					qName = prefix + localName;
					contentHandler.startElement(uri, localName, qName,
							new AttributesImpl());
					val = state.getUUID().toCharArray();
					contentHandler.characters(val, 0, val.length);
					contentHandler.endElement(uri, localName, qName);

					localName = "property";
					qName = prefix + localName;
					contentHandler.endElement(uri, localName, qName);

				}
				if (state.getMixinNames().size() > 0) {
					String[] mixins = state.getMixinNames().toArray(
							new String[state.getMixinNames().size()]);
					// sv:name
					attrSet = new AttributesImpl();
					localName = "name";
					qName = prefix + localName;
					attrSet.addAttribute(uri, localName, qName, TYPE_CDATA,
							JcrConstants.JCR_MIXINTYPES);

					// sv:type
					localName = "type";
					qName = prefix + localName;
					attrSet.addAttribute(uri, localName, qName, TYPE_CDATA,
							PropertyType.nameFromValue(PropertyType.NAME));

					// sv:property
					localName = "property";
					qName = prefix + localName;
					contentHandler.startElement(uri, localName, qName, attrSet);

					// sv:value
					localName = "value";
					qName = prefix + localName;
					for (int i = 0; i < mixins.length; i++) {
						contentHandler.startElement(uri, localName, qName,
								new AttributesImpl());
						val = mixins[i].toCharArray();
						contentHandler.characters(val, 0, val.length);
						contentHandler.endElement(uri, localName, qName);
					}

					// </sv:property>
					localName = "property";
					qName = prefix + localName;
					contentHandler.endElement(uri, localName, qName);
				}

				// process all properties of the current ImportState
				for (PropInfo prop : state.getPropInfos()) {
					importProperty(uri, prefix, TYPE_CDATA, prop);
				}

			} catch (SAXException e) {
				throw new RepositoryException(e);
			}

		}

		private void importProperty(String uri, String prefix, String type,
				PropInfo prop) throws SAXException {
			AttributesImpl attrs = new AttributesImpl();

			String localName = "name";
			String qName = prefix + localName;
			attrs.addAttribute(uri, localName, qName, type, prop.getName());

			localName = "type";
			qName = prefix + localName;
			attrs.addAttribute(uri, localName, qName, type,
					PropertyType.nameFromValue(prop.getType()));

			localName = "property";
			qName = prefix + localName;
			contentHandler.startElement(uri, localName, qName, attrs);

			localName = "value";
			qName = prefix + localName;
			char val[] = null;
			for (int i = 0; i < prop.values.length; i++) {
				contentHandler.startElement(uri, localName, qName,
						new AttributesImpl());
				val = prop.values[i].toCharArray();
				contentHandler.characters(val, 0, val.length);
				contentHandler.endElement(uri, localName, qName);
			}

			localName = "property";
			qName = prefix + localName;
			contentHandler.endElement(uri, localName, qName);
		}

		public void object() throws IOException {

			if (!parse.isEmpty()) {
				// st.peek();
				if (parse.peek() == OBJECT) {
					if (!states.empty()) {
						// process current node first.
						ImportState current = states.peek();
						try {
							if (contentHandler == null) {
								if (current.getUUID() != null) {
									contentHandler = getContentHandler(
											parent,
											ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
								} else {
									contentHandler = getContentHandler(
											parent,
											ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
								}
								startDocument();
							}
							startImportNode(current);
							current.processed = true;
						} catch (RepositoryException e) {
							throw new DiffException(e.getMessage(), e);
						}
					}
				} else {
					throw new DiffException(
							"Invalid DIFF format: A JSONObject may only be contained in JSonObjects.");
				}
			}

			ImportState is = new ImportState(key);
			parse.push(OBJECT);
			states.push(is);
		}

		public void endObject() throws IOException {

			if (parse.peek() != OBJECT) {
				throw new DiffException("Invalid DIFF format.");
			}
			try {
				if (!states.empty()) {
					ImportState state = states.pop();
					if (!state.processed) {
						startImportNode(state);
						state.processed = true;
					}
					endImportNode();
				}
				if (states.empty()) { // everything has been imported.
					endDocument();
				}
			} catch (RepositoryException e) {
				throw new DiffException("Repository exception", e); // TODO: exception message?
			}
		}

		public void array() throws IOException {
			
			if (!parse.isEmpty()) {
				if (parse.peek() == ARRAY) {
					throw new DiffException(
							"Invalid DIFF format: The JSONArray may only contain simple values.");
				}
				parse.push(ARRAY);
			}
		}

		public void endArray() throws IOException {
			ImportState state = states.peek();
			if (parse.pop() != ARRAY) {
				throw new DiffException(
						"Invalid DIFF format: The JSONArray may only contain simple values.");
			}

			if (key.equals(JcrConstants.JCR_MIXINTYPES)) {
				for (String v : currentPropValues) {
					addMixin(state, v);
				}
			} else { // multi-valued non-jcr:mixin property e.g jcr:privileges
				PropInfo prop = createPropInfo(key, PropertyType.STRING); // TODO: property type? 
				state.addPropInfo(prop);
			}
			currentPropValues.clear();
		}

		public void key(String key) throws IOException {
			this.key = key;
		}

		public void value(String value) throws IOException {
			Value v = (value == null) ? null : vf.createValue(value);
			value(v);
		}

		public void value(boolean value) throws IOException {
			value(vf.createValue(value));
		}

		public void value(long value) throws IOException {
			Value v = vf.createValue(value);
			value(v);
		}

		public void value(double value) throws IOException {
			value(vf.createValue(value));
		}

		private void addMixin(ImportState state, String mixin) {
			state.addMixin(mixin);
		}

		// --------------- private methods ---------------

		private ContentHandler getContentHandler(Node parent, int ib)
				throws RepositoryException {
			return parent.getSession().getImportContentHandler(
					parent.getPath(), ib);
		}

		private void startDocument() throws IOException {
			try {
				contentHandler.startDocument();
			} catch (SAXException e) {
				throw new DiffException("Fail to start import!", e);
			}
		}

		private void endImportNode() throws IOException {
			String uri = "http://www.jcp.org/jcr/sv/1.0";
			String prefix = "sv:";

			String localName = "node";
			String qName = prefix + localName;
			try {
				contentHandler.endElement(uri, localName, qName);
			} catch (SAXException e) {
				throw new DiffException("Fail to end import", e);
			}
		}

		private void endDocument() throws IOException {
			try {
				contentHandler.endDocument();
			} catch (SAXException e) {
				throw new DiffException("Fail to end import", e);
			}
			
			
		}

		private void addValue(Value v) throws IOException {
			if (currentPropValues == null) {
				currentPropValues = new ArrayList<String>();
			}
			try {
				currentPropValues.add(v.getString());
			} catch (ValueFormatException vFCause) {
				throw new DiffException("illegal property value " + v, vFCause);
			} catch (IllegalStateException iSCause) {
				throw new DiffException("illegal property value " + v, iSCause);
			} catch (RepositoryException rCause) {
				throw new DiffException("illegal property value " + v, rCause);
			}
		}

		private void value(Value v) throws IOException {

			ImportState state = states.peek();
			addValue(v);
			if (parse.peek() != ARRAY) { // single-valued property
				// jcr:primaryType
				if (key.equals(JcrConstants.JCR_PRIMARYTYPE)) {
					state.setNodeType(currentPropValues.get(0));

				} else if (key.equals(JcrConstants.JCR_UUID)) { // jcr:uuid
					state.setUUID(currentPropValues.get(0));
				} else {
					// a single value property which is not a system property.
					// NOTE: the jcr:mixinTypes property will be collected and
					// set for the node when this handler
					// receives an endArray event. This is because
					// jcr.mixinTypes is a multi-valued property and thus we
					// wait until we
					// collect all its values.
					PropInfo prop = createPropInfo(key, PropertyType.STRING);
					state.addPropInfo(prop);
				}
				currentPropValues.clear();
			}
		}

		private PropInfo createPropInfo(String propName, int propType)
				throws IOException {
			return new PropInfo(propName, propType,
					currentPropValues.toArray(new String[currentPropValues
							.size()]));
		}
	}

	final class PropInfo {
		String name;
		final int type;
		String values[];

		public PropInfo(String propName, int propType, String vls[]) {
			name = propName;
			type = propType;
			values = vls;
		}

		public int getType() {
			return type;
		}

		public String getName() {
			return name;
		}
	}

	final class ImportState {
		private String nodeName;
		private String ntName;
		private String uuid;
		private List<String> mixinNames = new ArrayList<String>();;
		private List<PropInfo> props = new ArrayList<PropInfo>();

		boolean processed = false;

		ImportState(String key) {
			this.nodeName = key;
		}

		public void setNodeType(String ntName) {
			this.ntName = ntName;
		}

		public void setUUID(String uuid) {
			this.uuid = uuid;
		}

		public String getName() {
			return nodeName;
		}

		public String getNodeTypeName() {
			return ntName;
		}

		public String getUUID() {
			return uuid;
		}

		public List<String> getMixinNames() {
			return mixinNames;
		}

		public void addMixin(String mixinName) {
			mixinNames.add(mixinName);
		}

		public void addPropInfo(PropInfo prop) {
			props.add(prop);
		}

		public List<PropInfo> getPropInfos() {
			return props;
		}
	}

 }
