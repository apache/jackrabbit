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
package org.apache.jackrabbit.core.persistence.xml;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.persistence.AbstractPersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.persistence.util.ResourceBasedBLOBStore;
import org.apache.jackrabbit.core.util.DOMWalker;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>XMLPersistenceManager</code> is a <code>FileSystem</code>-based
 * <code>PersistenceManager</code> that persists <code>ItemState</code>
 * and <code>NodeReferences</code> objects in XML format.
 *
 * @deprecated Please migrate to a bundle persistence manager
 *   (<a href="https://issues.apache.org/jira/browse/JCR-2802">JCR-2802</a>)
 */
@Deprecated
public class XMLPersistenceManager extends AbstractPersistenceManager {

    private static Logger log = LoggerFactory.getLogger(XMLPersistenceManager.class);

    /**
     * hexdigits for toString
     */
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    /**
     * The default encoding used in serialization
     */
    public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

    /**
     * The XML elements and attributes used in serialization
     */
    private static final String NODE_ELEMENT = "node";
    private static final String UUID_ATTRIBUTE = "uuid";
    private static final String NODETYPE_ATTRIBUTE = "nodeType";
    private static final String PARENTUUID_ATTRIBUTE = "parentUUID";
    private static final String MODCOUNT_ATTRIBUTE = "modCount";

    private static final String MIXINTYPES_ELEMENT = "mixinTypes";
    private static final String MIXINTYPE_ELEMENT = "mixinType";

    private static final String PROPERTIES_ELEMENT = "properties";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String MULTIVALUED_ATTRIBUTE = "multiValued";

    private static final String VALUES_ELEMENT = "values";
    private static final String VALUE_ELEMENT = "value";

    private static final String NODES_ELEMENT = "nodes";

    private static final String NODEREFERENCES_ELEMENT = "references";
    private static final String TARGETID_ATTRIBUTE = "targetId";
    private static final String NODEREFERENCE_ELEMENT = "reference";
    private static final String PROPERTYID_ATTRIBUTE = "propertyId";

    private static final String NODEFILENAME = ".node.xml";

    private static final String NODEREFSFILENAME = ".references.xml";

    private boolean initialized;

    // file system where the item state is stored
    private FileSystem itemStateFS;
    // file system where BLOB data is stored
    private FileSystem blobFS;
    // BLOBStore that manages BLOB data in the file system
    private BLOBStore blobStore;

    /**
     * Template for the subdirectory path for the files associated with
     * a single node. The template is processed by replacing each
     * "<code>x</code>" with the next hex digit in the UUID string.
     * All other characters in the template are used as-is.
     */
    private String nodePathTemplate = "xxxx/xxxx/xxxxxxxxxxxxxxxxxxxxxxxx";

    private final NameFactory factory;

    /**
     * Creates a new <code>XMLPersistenceManager</code> instance.
     */
    public XMLPersistenceManager() {
        initialized = false;
        factory = NameFactoryImpl.getInstance();
    }

    /**
     * Returns the node path template.
     *
     * @return node path template
     */
    public String getNodePathTemplate() {
        return nodePathTemplate;
    }

    /**
     * Sets the node path template.
     *
     * @param template node path template
     */
    public void setNodePathTemplate(String template) {
        nodePathTemplate = template;
    }

    /**
     * Builds the path of the node folder for the given node identifier
     * based on the configured node path template.
     *
     * @param id node identifier
     * @return node folder path
     */
    private String buildNodeFolderPath(NodeId id) {
        StringBuilder sb = new StringBuilder();
        char[] chars = id.toString().toCharArray();
        int cnt = 0;
        for (int i = 0; i < nodePathTemplate.length(); i++) {
            char ch = nodePathTemplate.charAt(i);
            if (ch == 'x' && cnt < chars.length) {
                ch = chars[cnt++];
                if (ch == '-') {
                    ch = chars[cnt++];
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private String buildPropFilePath(PropertyId id) {
        String fileName;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(id.getName().getNamespaceURI().getBytes());
            md5.update(id.getName().getLocalName().getBytes());
            byte[] bytes = md5.digest();
            char[] chars = new char[32];
            for (int i = 0, j = 0; i < 16; i++) {
                chars[j++] = HEXDIGITS[(bytes[i] >> 4) & 0x0f];
                chars[j++] = HEXDIGITS[bytes[i] & 0x0f];
            }
            fileName = new String(chars) + ".xml";
        } catch (NoSuchAlgorithmException nsae) {
            // should never get here as MD5 should always be available in the JRE
            String msg = "MD5 not available";
            log.error(msg, nsae);
            throw new InternalError(msg + nsae);
        }
        return buildNodeFolderPath(id.getParentId()) + "/" + fileName;
    }

    private String buildNodeFilePath(NodeId id) {
        return buildNodeFolderPath(id) + "/" + NODEFILENAME;
    }

    private String buildNodeReferencesFilePath(NodeId id) {
        return buildNodeFolderPath(id) + "/" + NODEREFSFILENAME;
    }

    private void readState(DOMWalker walker, NodeState state)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!walker.getName().equals(NODE_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: "
                    + walker.getName() + ")";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check uuid
        if (!state.getNodeId().toString().equals(walker.getAttribute(UUID_ATTRIBUTE))) {
            String msg = "invalid serialized state: uuid mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check nodetype
        String ntName = walker.getAttribute(NODETYPE_ATTRIBUTE);
        if (!factory.create(ntName).equals(state.getNodeTypeName())) {
            String msg = "invalid serialized state: nodetype mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }

        // now we're ready to read state

        // primary parent
        String parentUUID = walker.getAttribute(PARENTUUID_ATTRIBUTE);
        if (parentUUID.length() > 0) {
            state.setParentId(NodeId.valueOf(parentUUID));
        }

        // modification count
        String modCount = walker.getAttribute(MODCOUNT_ATTRIBUTE);
        state.setModCount(Short.parseShort(modCount));

        // mixin types
        if (walker.enterElement(MIXINTYPES_ELEMENT)) {
            Set<Name> mixins = new HashSet<Name>();
            while (walker.iterateElements(MIXINTYPE_ELEMENT)) {
                mixins.add(factory.create(walker.getAttribute(NAME_ATTRIBUTE)));
            }
            if (mixins.size() > 0) {
                state.setMixinTypeNames(mixins);
            }
            walker.leaveElement();
        }

        // property entries
        if (walker.enterElement(PROPERTIES_ELEMENT)) {
            while (walker.iterateElements(PROPERTY_ELEMENT)) {
                String propName = walker.getAttribute(NAME_ATTRIBUTE);
                // @todo deserialize type and values
                state.addPropertyName(factory.create(propName));
            }
            walker.leaveElement();
        }

        // child node entries
        if (walker.enterElement(NODES_ELEMENT)) {
            while (walker.iterateElements(NODE_ELEMENT)) {
                String childName = walker.getAttribute(NAME_ATTRIBUTE);
                String childUUID = walker.getAttribute(UUID_ATTRIBUTE);
                state.addChildNodeEntry(factory.create(childName), NodeId.valueOf(childUUID));
            }
            walker.leaveElement();
        }
    }

    private void readState(DOMWalker walker, PropertyState state)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!walker.getName().equals(PROPERTY_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: "
                    + walker.getName() + ")";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check name
        if (!state.getName().equals(factory.create(walker.getAttribute(NAME_ATTRIBUTE)))) {
            String msg = "invalid serialized state: name mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check parentUUID
        NodeId parentId = NodeId.valueOf(walker.getAttribute(PARENTUUID_ATTRIBUTE));
        if (!parentId.equals(state.getParentId())) {
            String msg = "invalid serialized state: parentUUID mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }

        // now we're ready to read state

        // type
        String typeName = walker.getAttribute(TYPE_ATTRIBUTE);
        int type;
        try {
            type = PropertyType.valueFromName(typeName);
        } catch (IllegalArgumentException iae) {
            // should never be getting here
            throw new ItemStateException("unexpected property-type: " + typeName, iae);
        }
        state.setType(type);

        // multiValued
        String multiValued = walker.getAttribute(MULTIVALUED_ATTRIBUTE);
        state.setMultiValued(Boolean.parseBoolean(multiValued));

        // modification count
        String modCount = walker.getAttribute(MODCOUNT_ATTRIBUTE);
        state.setModCount(Short.parseShort(modCount));

        // values
        ArrayList<InternalValue> values = new ArrayList<InternalValue>();
        if (walker.enterElement(VALUES_ELEMENT)) {
            while (walker.iterateElements(VALUE_ELEMENT)) {
                // read serialized value
                String content = walker.getContent();
                if (PropertyType.STRING == type) {
                    // STRING value can be empty; ignore length
                    values.add(InternalValue.valueOf(content, type));
                } else if (content.length() > 0) {
                    // non-empty non-STRING value
                    if (type == PropertyType.BINARY) {
                        try {
                            // special handling required for binary value:
                            // the value stores the id of the BLOB data
                            // in the BLOB store
                            if (blobStore instanceof ResourceBasedBLOBStore) {
                                // optimization: if the BLOB store is resource-based
                                // retrieve the resource directly rather than having
                                // to read the BLOB from an input stream
                                FileSystemResource fsRes =
                                        ((ResourceBasedBLOBStore) blobStore).getResource(content);
                                values.add(InternalValue.create(fsRes));
                            } else {
                                InputStream in = blobStore.get(content);
                                try {
                                    values.add(InternalValue.create(in));
                                } finally {
                                    IOUtils.closeQuietly(in);
                                }
                            }
                        } catch (Exception e) {
                            String msg = "error while reading serialized binary value";
                            log.debug(msg);
                            throw new ItemStateException(msg, e);
                        }
                    } else {
                        // non-empty non-STRING non-BINARY value
                        values.add(InternalValue.valueOf(content, type));
                    }
                } else {
                    // empty non-STRING value
                    log.warn(state.getPropertyId() + ": ignoring empty value of type "
                            + PropertyType.nameFromValue(type));
                }
            }
            walker.leaveElement();
        }
        state.setValues((InternalValue[])
                values.toArray(new InternalValue[values.size()]));
    }

    private void readState(DOMWalker walker, NodeReferences refs)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!walker.getName().equals(NODEREFERENCES_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: " + walker.getName() + ")";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check targetId
        if (!refs.getTargetId().equals(NodeId.valueOf(walker.getAttribute(TARGETID_ATTRIBUTE)))) {
            String msg = "invalid serialized state: targetId  mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }

        // now we're ready to read the references data

        // property id's
        refs.clearAllReferences();
        while (walker.iterateElements(NODEREFERENCE_ELEMENT)) {
            refs.addReference(PropertyId.valueOf(walker.getAttribute(PROPERTYID_ATTRIBUTE)));
        }
    }

    //---------------------------------------------------< PersistenceManager >
    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        itemStateFS = new BasedFileSystem(context.getFileSystem(), "/data");

        /**
         * store BLOB data in local file system in a sub directory
         * of the workspace home directory
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
        blobFS.init();
        this.blobFS = blobFS;
        blobStore = new FileSystemBLOBStore(blobFS);

        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // close BLOB file system
            blobFS.close();
            blobFS = null;
            blobStore = null;
            /**
             * there's no need close the item state store because it
             * is based in the workspace's file system which is
             * closed by the repository
             */
        } finally {
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeState load(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String nodeFilePath = buildNodeFilePath(id);

        try {
            if (!itemStateFS.isFile(nodeFilePath)) {
                throw new NoSuchItemStateException(id.toString());
            }
            InputStream in = itemStateFS.getInputStream(nodeFilePath);

            try {
                DOMWalker walker = new DOMWalker(in);
                String ntName = walker.getAttribute(NODETYPE_ATTRIBUTE);

                NodeState state = createNew(id);
                state.setNodeTypeName(factory.create(ntName));
                readState(walker, state);
                return state;
            } finally {
                in.close();
            }
        } catch (IOException ioe) {
            e = ioe;
            // fall through
        } catch (FileSystemException fse) {
            e = fse;
            // fall through
        }
        String msg = "failed to read node state: " + id;
        log.debug(msg);
        throw new ItemStateException(msg, e);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized PropertyState load(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String propFilePath = buildPropFilePath(id);

        try {
            if (!itemStateFS.isFile(propFilePath)) {
                throw new NoSuchItemStateException(id.toString());
            }
            InputStream in = itemStateFS.getInputStream(propFilePath);
            try {
                DOMWalker walker = new DOMWalker(in);
                PropertyState state = createNew(id);
                readState(walker, state);
                return state;
            } finally {
                in.close();
            }
        } catch (IOException ioe) {
            e = ioe;
            // fall through
        } catch (FileSystemException fse) {
            e = fse;
            // fall through
        }
        String msg = "failed to read property state: " + id.toString();
        log.debug(msg);
        throw new ItemStateException(msg, e);
    }

    /**
     * {@inheritDoc}
     */
    protected void store(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        NodeId id = state.getNodeId();
        String nodeFilePath = buildNodeFilePath(id);
        FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
        try {
            nodeFile.makeParentDirs();
            OutputStream os = nodeFile.getOutputStream();
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(os, DEFAULT_ENCODING));

                String parentId = (state.getParentId() == null) ? "" : state.getParentId().toString();
                String encodedNodeType = Text.encodeIllegalXMLCharacters(state.getNodeTypeName().toString());
                writer.write("<?xml version=\"1.0\" encoding=\"" + DEFAULT_ENCODING.name() + "\"?>\n");
                writer.write("<" + NODE_ELEMENT + " "
                        + UUID_ATTRIBUTE + "=\"" + id + "\" "
                        + PARENTUUID_ATTRIBUTE + "=\"" + parentId + "\" "
                        + MODCOUNT_ATTRIBUTE + "=\"" + state.getModCount() + "\" "
                        + NODETYPE_ATTRIBUTE + "=\"" + encodedNodeType + "\">\n");

                // mixin types
                writer.write("\t<" + MIXINTYPES_ELEMENT + ">\n");
                for (Name mixin : state.getMixinTypeNames()) {
                    writer.write("\t\t<" + MIXINTYPE_ELEMENT + " "
                            + NAME_ATTRIBUTE + "=\"" + Text.encodeIllegalXMLCharacters(mixin.toString()) + "\"/>\n");
                }
                writer.write("\t</" + MIXINTYPES_ELEMENT + ">\n");

                // properties
                writer.write("\t<" + PROPERTIES_ELEMENT + ">\n");
                for (Name propName : state.getPropertyNames()) {
                    writer.write("\t\t<" + PROPERTY_ELEMENT + " "
                            + NAME_ATTRIBUTE + "=\"" + Text.encodeIllegalXMLCharacters(propName.toString()) + "\">\n");
                    // @todo serialize type, definition id and values
                    writer.write("\t\t</" + PROPERTY_ELEMENT + ">\n");
                }
                writer.write("\t</" + PROPERTIES_ELEMENT + ">\n");

                // child nodes
                writer.write("\t<" + NODES_ELEMENT + ">\n");
                for (ChildNodeEntry entry : state.getChildNodeEntries()) {
                    writer.write("\t\t<" + NODE_ELEMENT + " "
                            + NAME_ATTRIBUTE + "=\"" + Text.encodeIllegalXMLCharacters(entry.getName().toString()) + "\" "
                            + UUID_ATTRIBUTE + "=\"" + entry.getId() + "\">\n");
                    writer.write("\t\t</" + NODE_ELEMENT + ">\n");
                }
                writer.write("\t</" + NODES_ELEMENT + ">\n");

                writer.write("</" + NODE_ELEMENT + ">\n");
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            String msg = "failed to write node state: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void store(PropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String propFilePath = buildPropFilePath(state.getPropertyId());
        FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
        try {
            propFile.makeParentDirs();
            OutputStream os = propFile.getOutputStream();
            // write property state to xml file
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(os, DEFAULT_ENCODING));

                String typeName;
                int type = state.getType();
                try {
                    typeName = PropertyType.nameFromValue(type);
                } catch (IllegalArgumentException iae) {
                    // should never be getting here
                    throw new ItemStateException("unexpected property-type ordinal: " + type, iae);
                }

                writer.write("<?xml version=\"1.0\" encoding=\"" + DEFAULT_ENCODING.name() + "\"?>\n");
                writer.write("<" + PROPERTY_ELEMENT + " "
                        + NAME_ATTRIBUTE + "=\"" + Text.encodeIllegalXMLCharacters(state.getName().toString()) + "\" "
                        + PARENTUUID_ATTRIBUTE + "=\"" + state.getParentId() + "\" "
                        + MULTIVALUED_ATTRIBUTE + "=\"" + Boolean.toString(state.isMultiValued()) + "\" "
                        + MODCOUNT_ATTRIBUTE + "=\"" + state.getModCount() + "\" "
                        + TYPE_ATTRIBUTE + "=\"" + typeName + "\">\n");
                // values
                writer.write("\t<" + VALUES_ELEMENT + ">\n");
                InternalValue[] values = state.getValues();
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        writer.write("\t\t<" + VALUE_ELEMENT + ">");
                        InternalValue val = values[i];
                        if (val != null) {
                            if (type == PropertyType.BINARY) {
                                // special handling required for binary value:
                                // put binary value in BLOB store
                                InputStream in = val.getStream();
                                String blobId = blobStore.createId(state.getPropertyId(), i);
                                try {
                                    blobStore.put(blobId, in, val.getLength());
                                } finally {
                                    IOUtils.closeQuietly(in);
                                }
                                // store id of BLOB as property value
                                writer.write(blobId);
                                // replace value instance with value backed by resource
                                // in BLOB store and discard old value instance (e.g. temp file)
                                if (blobStore instanceof ResourceBasedBLOBStore) {
                                    // optimization: if the BLOB store is resource-based
                                    // retrieve the resource directly rather than having
                                    // to read the BLOB from an input stream
                                    FileSystemResource fsRes =
                                            ((ResourceBasedBLOBStore) blobStore).getResource(blobId);
                                    values[i] = InternalValue.create(fsRes);
                                } else {
                                    in = blobStore.get(blobId);
                                    try {
                                        values[i] = InternalValue.create(in);
                                    } finally {
                                        try {
                                            in.close();
                                        } catch (IOException e) {
                                            // ignore
                                        }
                                    }
                                }
                                val.discard();
                            } else {
                                writer.write(Text.encodeIllegalXMLCharacters(val.toString()));
                            }
                        }
                        writer.write("</" + VALUE_ELEMENT + ">\n");
                    }
                }
                writer.write("\t</" + VALUES_ELEMENT + ">\n");
                writer.write("</" + PROPERTY_ELEMENT + ">\n");
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            String msg = "failed to store property state: " + state.getParentId() + "/" + state.getName();
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void destroy(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        NodeId id = state.getNodeId();
        String nodeFilePath = buildNodeFilePath(id);
        FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
        try {
            if (nodeFile.exists()) {
                // delete resource and prune empty parent folders
                nodeFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete node state: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void destroy(PropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // delete binary values (stored as files)
        InternalValue[] values = state.getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                if (val != null) {
                    val.deleteBinaryResource();
                }
            }
        }
        // delete property file
        String propFilePath = buildPropFilePath(state.getPropertyId());
        FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
        try {
            if (propFile.exists()) {
                // delete resource and prune empty parent folders
                propFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete property state: " + state.getParentId() + "/" + state.getName();
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeReferences loadReferencesTo(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String refsFilePath = buildNodeReferencesFilePath(id);
        try {
            if (!itemStateFS.isFile(refsFilePath)) {
                throw new NoSuchItemStateException(id.toString());
            }

            InputStream in = itemStateFS.getInputStream(refsFilePath);

            try {
                DOMWalker walker = new DOMWalker(in);
                NodeReferences refs = new NodeReferences(id);
                readState(walker, refs);
                return refs;
            } finally {
                in.close();
            }
        } catch (IOException ioe) {
            e = ioe;
            // fall through
        } catch (FileSystemException fse) {
            e = fse;
            // fall through
        }
        String msg = "failed to load references: " + id;
        log.debug(msg);
        throw new ItemStateException(msg, e);
    }

    /**
     * {@inheritDoc}
     */
    protected void store(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String refsFilePath = buildNodeReferencesFilePath(refs.getTargetId());
        FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
        try {
            refsFile.makeParentDirs();
            OutputStream os = refsFile.getOutputStream();
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(os, DEFAULT_ENCODING));

                writer.write("<?xml version=\"1.0\" encoding=\"" + DEFAULT_ENCODING.name() + "\"?>\n");
                writer.write("<" + NODEREFERENCES_ELEMENT + " "
                        + TARGETID_ATTRIBUTE + "=\"" + refs.getTargetId() + "\">\n");
                // write references (i.e. the id's of the REFERENCE properties)
                for (PropertyId propId : refs.getReferences()) {
                    writer.write("\t<" + NODEREFERENCE_ELEMENT + " "
                            + PROPERTYID_ATTRIBUTE + "=\"" + propId + "\"/>\n");
                }
                writer.write("</" + NODEREFERENCES_ELEMENT + ">\n");
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            String msg = "failed to store " + refs;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void destroy(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String refsFilePath = buildNodeReferencesFilePath(refs.getTargetId());
        FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
        try {
            if (refsFile.exists()) {
                // delete resource and prune empty parent folders
                refsFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete " + refs;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean exists(NodeId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String nodeFilePath = buildNodeFilePath(id);
            FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
            return nodeFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of item state: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean exists(PropertyId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String propFilePath = buildPropFilePath(id);
            FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
            return propFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of item state: " + id;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean existsReferencesTo(NodeId id)
            throws ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String refsFilePath = buildNodeReferencesFilePath(id);
            FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
            return refsFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of references: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }
}
