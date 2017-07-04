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
package org.apache.jackrabbit.server.remoting.davex;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.webdav.JcrValueType;
import org.apache.jackrabbit.server.util.RequestData;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.commons.json.JsonParser;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.LinkedList;

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
    private final ProtectedRemoveManager protectedRemoveManager;

    private NodeTypeManager ntManager;

    JsonDiffHandler(Session session, String requestItemPath, RequestData data) throws RepositoryException {
        this(session, requestItemPath, data, null);
    }

    JsonDiffHandler(Session session, String requestItemPath, RequestData data, ProtectedRemoveManager protectedRemoveManager) throws RepositoryException {
        this.session = session;
        this.requestItemPath = requestItemPath;
        this.data = data;
        vf = session.getValueFactory();
        this.protectedRemoveManager = protectedRemoveManager;
    }

    //--------------------------------------------------------< DiffHandler >---
    /**
     * @see DiffHandler#addNode(String, String)
     */
    @Override
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
    @Override
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
    @Override
    public void remove(String targetPath, String diffValue) throws DiffException {
        if (!(diffValue == null || diffValue.trim().length() == 0)) {
            throw new DiffException("'remove' may not have a diffValue.");
        }
        try {
            String itemPath = getItemPath(targetPath);
            Item item = session.getItem(itemPath);

            ItemDefinition def = (item.isNode()) ? ((Node) item).getDefinition() : ((Property) item).getDefinition();
            if (def.isProtected()) {
                // delegate to the manager.
                if (protectedRemoveManager == null || !protectedRemoveManager.remove(session, itemPath)) {
                   throw new ConstraintViolationException("Cannot remove protected node: no suitable handler configured.");
                }
            } else {
                item.remove();
            }
        } catch (RepositoryException e) {
            throw new DiffException(e.getMessage(), e);
        }
    }

    /**
     * @see DiffHandler#move(String, String) 
     */
    @Override
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

    private NodeTypeManager getNodeTypeManager() throws RepositoryException {
        if (ntManager == null) {
            ntManager = session.getWorkspace().getNodeTypeManager();
        }
        return ntManager;
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

    private static ContentHandler createContentHandler(Node parent) throws RepositoryException {
        return parent.getSession().getImportContentHandler(parent.getPath(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
    }

    private static Node importNode(Node parent, String nodeName, String ntName,
                                   String uuid) throws RepositoryException {

        String uri = "http://www.jcp.org/jcr/sv/1.0";
        String prefix = "sv:";

        ContentHandler ch = createContentHandler(parent);
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
        new JsonParser(hndlr).parse("{\"a\":" + diffValue + "}");

        return hndlr.getValues();
    }

    //--------------------------------------------------------------------------
    /**
     * Inner class used to parse a single value
     */
    private final class ValueHandler implements JsonHandler {
        private Value v;

        @Override
        public void object() throws IOException {
            // ignore
        }
        @Override
        public void endObject() throws IOException {
            // ignore
        }
        @Override
        public void array() throws IOException {
            // ignore
        }
        @Override
        public void endArray() throws IOException {
            // ignore
        }
        @Override
        public void key(String key) throws IOException {
            // ignore
        }

        @Override
        public void value(String value) throws IOException {
            v = (value == null) ? null : vf.createValue(value);
        }
        @Override
        public void value(boolean value) throws IOException {
            v = vf.createValue(value);
        }
        @Override
        public void value(long value) throws IOException {
            v = vf.createValue(value);
        }
        @Override
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

        @Override
        public void object() throws IOException {
            // ignore
        }
        @Override
        public void endObject() throws IOException {
            // ignore
        }
        @Override
        public void array() throws IOException {
            // ignore
        }
        @Override
        public void endArray() throws IOException {
            // ignore
        }
        @Override
        public void key(String key) throws IOException {
            // ignore
        }

        @Override
        public void value(String value) throws IOException {
            if (value != null) {
                values.add(vf.createValue(value));
            } else {
                log.warn("Null element for a multivalued property -> Ignore.");
            }
        }
        @Override
        public void value(boolean value) throws IOException {
            values.add(vf.createValue(value));
        }
        @Override
        public void value(long value) throws IOException {
            values.add(vf.createValue(value));
        }
        @Override
        public void value(double value) throws IOException {
            values.add(vf.createValue(value));
        }

        private Value[] getValues() {
            return values.toArray(new Value[values.size()]);
        }
    }

    /**
     * Inner class for parsing a simple json object defining a node and its
     * child nodes and/or child properties
     */
    private final class NodeHandler implements JsonHandler {
        private Node parent;
        private String key;

        private Stack<ImportItem> st = new Stack<ImportItem>();

        private NodeHandler(Node parent, String nodeName) throws IOException {
            this.parent = parent;
            key = nodeName;
        }

        @Override
        public void object() throws IOException {
            ImportNode n;
            if (st.isEmpty()) {
                try {
                    n = new ImportNode(parent.getPath(), key);
                } catch (RepositoryException e) {
                    throw new DiffException(e.getMessage(), e);
                }

            } else {
                ImportItem obj = st.peek();
                n = new ImportNode(obj.getPath(), key);
                if (obj instanceof ImportNode) {
                    ((ImportNode) obj).addNode(n);
                } else {
                    throw new DiffException("Invalid DIFF format: The JSONArray may only contain simple values.");
                }
            }
            st.push(n);
        }

        @Override
        public void endObject() throws IOException {
            // element on stack must be ImportMvProp since array may only
            // contain simple values, no arrays/objects are allowed.
            ImportItem obj = st.pop();
            if (!((obj instanceof ImportNode))) {
                throw new DiffException("Invalid DIFF format.");
            }
            if (st.isEmpty()) {
                // everything parsed -> start adding all nodes and properties
                try {
                    if (obj.mandatesImport(parent)) {
                        obj.importItem(createContentHandler(parent));
                    } else {
                        obj.createItem(parent);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    throw new DiffException(e.getMessage(), e);
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                    throw new DiffException(e.getMessage(), e);
                }
            }
        }

        @Override
        public void array() throws IOException {            
            ImportItem obj = st.peek();
            ImportMvProp prop = new ImportMvProp(obj.getPath(), key);
            if (obj instanceof ImportNode) {
                ((ImportNode)obj).addProp(prop);
            } else {
                throw new DiffException("Invalid DIFF format: The JSONArray may only contain simple values.");
            }
            st.push(prop);
        }

        @Override
        public void endArray() throws IOException {
            // element on stack must be ImportMvProp since array may only
            // contain simple values, no arrays/objects are allowed.
            ImportItem obj = st.pop();
            if (!((obj instanceof ImportMvProp))) {
                throw new DiffException("Invalid DIFF format: The JSONArray may only contain simple values.");
            }
        }

        @Override
        public void key(String key) throws IOException {
            this.key = key;
        }

        @Override
        public void value(String value) throws IOException {
            Value v = (value == null) ? null : vf.createValue(value);
            value(v);
        }

        @Override
        public void value(boolean value) throws IOException {
            value(vf.createValue(value));
        }

        @Override
        public void value(long value) throws IOException {
            Value v = vf.createValue(value);
            value(v);
        }

        @Override
        public void value(double value) throws IOException {
            value(vf.createValue(value));
        }

        private void value(Value v) throws IOException {
            ImportItem obj = st.peek();
            if (obj instanceof ImportMvProp) {
                ((ImportMvProp) obj).values.add(v);
            } else {
                ((ImportNode) obj).addProp(new ImportProp(obj.getPath(), key, v));
            }
        }
    }

    private abstract class ImportItem {

        static final String TYPE_CDATA = "CDATA";

        final String parentPath;
        final String name;
        final String path;

        private ImportItem(String parentPath, String name) throws IOException {
            if (name == null) {
                throw new DiffException("Invalid DIFF format: NULL key.");
            }
            this.name = name;
            this.parentPath = parentPath;
            this.path = parentPath+"/"+name;
        }

        void setNameAttribute(AttributesImpl attr) {
            attr.addAttribute(Name.NS_SV_URI, "name", Name.NS_SV_PREFIX +":name", TYPE_CDATA, name);
        }

        String getPath() {
            return path;
        }

        abstract boolean mandatesImport(Node parent);

        abstract void createItem(Node parent) throws RepositoryException, IOException;

        abstract void importItem(ContentHandler contentHandler) throws IOException;
    }
    
    private final class ImportNode extends ImportItem {

        private static final String LOCAL_NAME = "node";

        private ImportProp ntName;
        private ImportProp uuid;

        private List<ImportNode> childN = new ArrayList<ImportNode>();
        private List<ImportProperty> childP = new ArrayList<ImportProperty>();

        private ImportNode(String parentPath, String name) throws IOException {
            super(parentPath, name);
        }

        private String getUUID() {
            if (uuid != null && uuid.value != null) {
                try {
                    return uuid.value.getString();
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
            return null;
        }

        private String getPrimaryType() {
            if (ntName != null && ntName.value != null) {
                try {
                    return ntName.value.getString();
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
            return null;
        }

        @Override
        boolean mandatesImport(Node parent) {
            String primaryType = getPrimaryType();
            // Very simplistic and simplified test for protection that doesn't
            // take mixin types into account and ignores all JCR primary types
            if (!primaryType.startsWith(Name.NS_NT_PREFIX)) {
                try {
                    NodeType nt = getNodeTypeManager().getNodeType(primaryType);
                    for (NodeDefinition nd : nt.getChildNodeDefinitions()) {
                        if (nd.isProtected()) {
                            return true;
                        }
                    }
                    for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                        if (!pd.getName().startsWith(Name.NS_JCR_PREFIX) && pd.isProtected()) {
                            return true;
                        }
                    }
                } catch (RepositoryException e) {
                    log.warn(e.getMessage(), e);
                }
            }
            return false;
        }

        void addProp(ImportProp prop) {
            if (prop.name.equals(JcrConstants.JCR_PRIMARYTYPE)) {
                ntName = prop;
            } else if (prop.name.equals(JcrConstants.JCR_UUID)) {
                uuid = prop;
            } else {
                // regular property
                childP.add(prop);
            }
        }

        void addProp(ImportMvProp prop) {
            childP.add(prop);
        }

        void addNode(ImportNode node) {
            childN.add(node);
        }

        @Override
        void importItem(ContentHandler contentHandler) throws IOException {
            try {
                AttributesImpl attr = new AttributesImpl();
                setNameAttribute(attr);
                contentHandler.startElement(Name.NS_SV_URI, LOCAL_NAME, Name.NS_SV_PREFIX+":"+LOCAL_NAME, attr);

                if (ntName != null && ntName.value != null) {
                    ntName.importItem(contentHandler);
                }
                if (uuid != null && uuid.value != null) {
                    uuid.importItem(contentHandler);
                }

                for(ImportProperty prop : childP) {
                    prop.importItem(contentHandler);
                }

                for(ImportNode node : childN) {
                    node.importItem(contentHandler);
                }
                contentHandler.endElement(Name.NS_SV_URI, LOCAL_NAME, Name.NS_SV_PREFIX+":"+LOCAL_NAME);
            } catch(SAXException e) {
                throw new DiffException(e.getMessage(), e);
            }
        }

        @Override
        void createItem(Node parent) throws RepositoryException, IOException {
            if (mandatesImport(parent)) {
                ContentHandler ch = createContentHandler(parent);
                try {
                    ch.startDocument();
                    importItem(ch);
                    ch.endDocument();
                } catch (SAXException e) {
                    throw new DiffException(e.getMessage(), e);
                }
            } else {
                Node n;
                String uuidValue = getUUID();
                String primaryType = getPrimaryType();
                if (uuidValue == null) {
                    n = (primaryType == null) ? parent.addNode(name) : parent.addNode(name,  primaryType);
                } else {
                    n = importNode(parent, name, primaryType, uuidValue);
                }
                // create all properties
                for (ImportItem obj : childP) {
                    obj.createItem(n);
                }
                // recursively create all child nodes
                for (ImportItem obj : childN) {
                    obj.createItem(n);
                }
            }
        }
    }

    private abstract class ImportProperty extends ImportItem {

        static final String VALUE = "value";
        static final String TYPE = "type";
        static final String LOCAL_NAME = "property";

        private ImportProperty(String parentPath, String name) throws IOException {
            super(parentPath, name);
        }

        @Override
        boolean mandatesImport(Node parent) {
            // TODO: verify again if a protected property (except for jcr:primaryType and jcr:mixinTypes) will ever change outside the scope of importing the whole tree.
            return false;
        }

        @Override
        void importItem(ContentHandler contentHandler) throws IOException {
            try {
                AttributesImpl propAtts = new AttributesImpl();
                setNameAttribute(propAtts);
                setTypeAttribute(propAtts);
                contentHandler.startElement(Name.NS_SV_URI, LOCAL_NAME, Name.NS_SV_PREFIX+":"+LOCAL_NAME, propAtts);
                startValueElement(contentHandler);
                contentHandler.endElement(Name.NS_SV_URI, LOCAL_NAME, Name.NS_SV_PREFIX+":"+LOCAL_NAME);
            } catch(SAXException e) {
                throw new DiffException(e.getMessage(), e);
            }
        }

        void setTypeAttribute(AttributesImpl attr) {
            String type = null;
            if (name.equals(JcrConstants.JCR_PRIMARYTYPE)) {
                type = PropertyType.nameFromValue(PropertyType.NAME);
            } else if (name.equals(JcrConstants.JCR_MIXINTYPES)) {
                type = PropertyType.nameFromValue(PropertyType.NAME);
            } else if (name.equals(JcrConstants.JCR_UUID)) {
                type = PropertyType.nameFromValue(PropertyType.STRING);
            } else {
                type = PropertyType.nameFromValue(PropertyType.UNDEFINED);
            }
            attr.addAttribute(Name.NS_SV_URI, TYPE, Name.NS_SV_PREFIX+":"+TYPE, TYPE_CDATA, type);
        }

        abstract void startValueElement(ContentHandler contentHandler) throws IOException;
    }

    private final class ImportProp extends ImportProperty {

        private final Value value;

        private ImportProp(String parentPath, String name, Value value) throws IOException {
            super(parentPath, name);
            try {
                if (value == null) {
                    this.value = extractValuesFromRequest(getPath())[0];
                } else {
                    this.value = value;
                }
            } catch (RepositoryException e) {
                throw new DiffException(e.getMessage(), e);
            }
        }

        @Override
        void createItem(Node parent) throws RepositoryException {
            parent.setProperty(name, value);
        }

        @Override
        void startValueElement(ContentHandler contentHandler) throws IOException {
            try {
                String str = value.getString();
                contentHandler.startElement(Name.NS_SV_URI, VALUE, Name.NS_SV_PREFIX + ":" + VALUE, new AttributesImpl());
                contentHandler.characters(str.toCharArray(), 0, str.length());
                contentHandler.endElement(Name.NS_SV_URI, VALUE, Name.NS_SV_PREFIX + ":" + VALUE);
            } catch (SAXException e) {
                throw new DiffException(e.getMessage());
            } catch (ValueFormatException e) {
                throw new DiffException(e.getMessage());
            } catch (RepositoryException e) {
                throw new DiffException(e.getMessage());
            }
        }
    }

    private final class ImportMvProp extends ImportProperty  {

        private List<Value> values = new ArrayList<Value>();

        private ImportMvProp(String parentPath, String name) throws IOException {
            super(parentPath, name);
        }

        @Override
        void createItem(Node parent) throws RepositoryException {
            Value[] vls = values.toArray(new Value[values.size()]);
            if (JcrConstants.JCR_MIXINTYPES.equals(name)) {
                setMixins(parent, vls);
            } else {
                parent.setProperty(name, vls);
            }
        }

        @Override
        void startValueElement(ContentHandler contentHandler) throws IOException {
            try {
                // Multi-valued property with values present in the request
                // multi-part
                if (values.size() == 0) {
                    values = Arrays.asList(extractValuesFromRequest(getPath()));
                }

                for (Value v : values) {
                    String str = v.getString();
                    contentHandler.startElement(Name.NS_SV_URI, VALUE, Name.NS_SV_PREFIX + ":" + VALUE, new AttributesImpl());
                    contentHandler.characters(str.toCharArray(), 0, str.length());
                    contentHandler.endElement(Name.NS_SV_URI, VALUE, Name.NS_SV_PREFIX + ":" + VALUE);
                }
            } catch (SAXException e) {
                throw new DiffException(e.getMessage());
            } catch (ValueFormatException e) {
                throw new DiffException(e.getMessage());
            } catch (RepositoryException e) {
                throw new DiffException(e.getMessage());
            }
        }
    }
}
