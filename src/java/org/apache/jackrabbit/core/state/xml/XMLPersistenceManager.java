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
package org.apache.jackrabbit.core.state.xml;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.fs.*;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.*;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ContentFilter;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;

import javax.jcr.PropertyType;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * <code>XMLPersistenceManager</code> is a <code>FileSystem</code>-based
 * <code>PersistenceManager</code> that persists <code>ItemState</code>
 * and <code>NodeReferences</code> objects in XML format.
 */
public class XMLPersistenceManager extends AbstractPersistenceManager {

    private static Logger log = Logger.getLogger(XMLPersistenceManager.class);

    /**
     * hexdigits for toString
     */
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    /**
     * The default encoding used in serialization
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * The XML elements and attributes used in serialization
     */
    private static final String NODE_ELEMENT = "node";
    private static final String UUID_ATTRIBUTE = "uuid";
    private static final String NODETYPE_ATTRIBUTE = "nodeType";
    private static final String PARENTUUID_ATTRIBUTE = "parentUUID";
    private static final String DEFINITIONID_ATTRIBUTE = "definitionId";

    private static final String PARENTS_ELEMENT = "parents";
    private static final String PARENT_ELEMENT = "parent";

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
    private FileSystem itemStateStore;
    // file system where BLOB data is stored
    private FileSystem blobStore;

    /**
     * Creates a new <code>XMLPersistenceManager</code> instance.
     */
    public XMLPersistenceManager() {
        initialized = false;
    }

    private String buildNodeFolderPath(String uuid) {
        StringBuffer sb = new StringBuffer();
        char[] chars = uuid.toCharArray();
        int cnt = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '-') {
                continue;
            }
            //if (cnt > 0 && cnt % 4 == 0) {
            if (cnt == 4 || cnt == 8) {
                sb.append('/');
            }
            sb.append(chars[i]);
            cnt++;
        }
        return sb.toString();
    }

    private String buildPropFilePath(String parentUUID, QName propName) {
        String fileName;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(propName.getNamespaceURI().getBytes());
            md5.update(propName.getLocalName().getBytes());
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
            log.fatal(msg, nsae);
            throw new InternalError(msg + nsae);
        }
        return buildNodeFolderPath(parentUUID) + "/" + fileName;
    }

    private String buildBlobFilePath(String parentUUID, QName propName, int i) {
        return buildNodeFolderPath(parentUUID) + "/"
                + FileSystemPathUtil.escapeName(propName.toString()) + "." + i + ".bin";
    }

    private String buildNodeFilePath(String uuid) {
        return buildNodeFolderPath(uuid) + "/" + NODEFILENAME;
    }

    private String buildNodeReferencesFilePath(String uuid) {
        return buildNodeFolderPath(uuid) + "/" + NODEREFSFILENAME;
    }

    private void readState(Element nodeElement, NodeState state)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!nodeElement.getName().equals(NODE_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: " + nodeElement.getName() + ")";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check uuid
        if (!state.getUUID().equals(nodeElement.getAttributeValue(UUID_ATTRIBUTE))) {
            String msg = "invalid serialized state: uuid mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check nodetype
        String ntName = nodeElement.getAttributeValue(NODETYPE_ATTRIBUTE);
        if (!QName.valueOf(ntName).equals(state.getNodeTypeName())) {
            String msg = "invalid serialized state: nodetype mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }

        // now we're ready to read state

        // primary parent
        String parentUUID = nodeElement.getAttributeValue(PARENTUUID_ATTRIBUTE);
        if (parentUUID.length() > 0) {
            state.setParentUUID(parentUUID);
        }

        // definition id
        String definitionId = nodeElement.getAttributeValue(DEFINITIONID_ATTRIBUTE);
        state.setDefinitionId(NodeDefId.valueOf(definitionId));

        // parent uuid's
        Iterator iter = nodeElement.getChild(PARENTS_ELEMENT).getChildren(PARENT_ELEMENT).iterator();
        ArrayList parentUUIDs = new ArrayList();
        while (iter.hasNext()) {
            Element parentElement = (Element) iter.next();
            parentUUIDs.add(parentElement.getAttributeValue(UUID_ATTRIBUTE));
        }
        if (parentUUIDs.size() > 0) {
            state.setParentUUIDs(parentUUIDs);
        }

        // mixin types
        Element mixinsElement = nodeElement.getChild(MIXINTYPES_ELEMENT);
        if (mixinsElement != null) {
            iter = mixinsElement.getChildren(MIXINTYPE_ELEMENT).iterator();
            Set mixins = new HashSet();
            while (iter.hasNext()) {
                Element mixinElement = (Element) iter.next();
                mixins.add(QName.valueOf(mixinElement.getAttributeValue(NAME_ATTRIBUTE)));
            }
            if (mixins.size() > 0) {
                state.setMixinTypeNames(mixins);
            }
        }

        // property entries
        iter = nodeElement.getChild(PROPERTIES_ELEMENT).getChildren(PROPERTY_ELEMENT).iterator();
        while (iter.hasNext()) {
            Element propElement = (Element) iter.next();
            String propName = propElement.getAttributeValue(NAME_ATTRIBUTE);
            // @todo deserialize type and values
            state.addPropertyEntry(QName.valueOf(propName));
        }

        // child node entries
        iter = nodeElement.getChild(NODES_ELEMENT).getChildren(NODE_ELEMENT).iterator();
        while (iter.hasNext()) {
            Element childNodeElement = (Element) iter.next();
            String childName = childNodeElement.getAttributeValue(NAME_ATTRIBUTE);
            String childUUID = childNodeElement.getAttributeValue(UUID_ATTRIBUTE);
            state.addChildNodeEntry(QName.valueOf(childName), childUUID);
        }
    }

    private void readState(Element propElement, PropertyState state)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!propElement.getName().equals(PROPERTY_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: " + propElement.getName() + ")";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check name
        if (!state.getName().equals(QName.valueOf(propElement.getAttributeValue(NAME_ATTRIBUTE)))) {
            String msg = "invalid serialized state: name mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check parentUUID
        String parentUUID = propElement.getAttributeValue(PARENTUUID_ATTRIBUTE);
        if (!parentUUID.equals(state.getParentUUID())) {
            String msg = "invalid serialized state: parentUUID mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }

        // now we're ready to read state

        // type
        String typeName = propElement.getAttributeValue(TYPE_ATTRIBUTE);
        int type;
        try {
            type = PropertyType.valueFromName(typeName);
        } catch (IllegalArgumentException iae) {
            // should never be getting here
            throw new ItemStateException("unexpected property-type: " + typeName, iae);
        }
        state.setType(type);

        // multiValued
        String multiValued = propElement.getAttributeValue(MULTIVALUED_ATTRIBUTE);
        state.setMultiValued(Boolean.getBoolean(multiValued));

        // definition id
        String definitionId = propElement.getAttributeValue(DEFINITIONID_ATTRIBUTE);
        state.setDefinitionId(PropDefId.valueOf(definitionId));

        // values
        Iterator iter = propElement.getChild(VALUES_ELEMENT).getChildren(VALUE_ELEMENT).iterator();
        ArrayList values = new ArrayList();
        while (iter.hasNext()) {
            Element valueElement = (Element) iter.next();
            Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
            List content = valueElement.getContent(filter);

            InternalValue val;
            if (!content.isEmpty()) {
                // read serialized value
                String text = valueElement.getTextTrim();
                if (type == PropertyType.BINARY) {
                    // special handling required for binary value:
                    // the value stores the path to the actual binary file in the blob store
                    try {
                        val = InternalValue.create(new FileSystemResource(blobStore, text));
                    } catch (IOException ioe) {
                        String msg = "error while reading serialized binary valuey";
                        log.debug(msg);
                        throw new ItemStateException(msg, ioe);
                    }
                } else {
                    val = InternalValue.valueOf(text, type);
                }
                values.add(val);
            } else {
                continue;
            }
        }
        state.setValues((InternalValue[]) values.toArray(new InternalValue[values.size()]));
    }

    private void readState(Element refsElement, NodeReferences refs)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!refsElement.getName().equals(NODEREFERENCES_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: " + refsElement.getName() + ")";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        // check targetId
        if (!refs.getTargetId().equals(NodeReferencesId.valueOf(refsElement.getAttributeValue(TARGETID_ATTRIBUTE)))) {
            String msg = "invalid serialized state: targetId  mismatch";
            log.debug(msg);
            throw new ItemStateException(msg);
        }

        // now we're ready to read the references data

        // property id's
        refs.clearAllReferences();
        Iterator iter = refsElement.getChildren(NODEREFERENCE_ELEMENT).iterator();
        while (iter.hasNext()) {
            Element elem = (Element) iter.next();
            refs.addReference(PropertyId.valueOf(elem.getAttributeValue(PROPERTYID_ATTRIBUTE)));
        }
    }

    //---------------------------------------------------< PersistenceManager >
    /**
     * @see PersistenceManager#init
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        itemStateStore = new BasedFileSystem(context.getFileSystem(), "/data");

        //blobStore = new BasedFileSystem(wspFS, "/blobs");
        /**
         * store blob's in local file system in a sub directory
         * of the workspace home directory
         * todo make blob store configurable
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
        blobFS.init();
        blobStore = blobFS;

        initialized = true;
    }

    /**
     * @see PersistenceManager#close
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // close blob store
            blobStore.close();
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
     * @see PersistenceManager#load
     */
    public synchronized NodeState load(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String nodeFilePath = buildNodeFilePath(id.getUUID());

        try {
            if (!itemStateStore.isFile(nodeFilePath)) {
                throw new NoSuchItemStateException(id.toString());
            }
            InputStream in = itemStateStore.getInputStream(nodeFilePath);

            try {
                SAXBuilder builder = new SAXBuilder();
                Element rootElement = builder.build(in).getRootElement();
                String ntName = rootElement.getAttributeValue(NODETYPE_ATTRIBUTE);

                NodeState state = createNew(id);
                state.setNodeTypeName(QName.valueOf(ntName));
                readState(rootElement, state);
                return state;
            } finally {
                in.close();
            }
        } catch (JDOMException jde) {
            e = jde;
            // fall through
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
     * @see PersistenceManager#load
     */
    public synchronized PropertyState load(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String propFilePath = buildPropFilePath(id.getParentUUID(), id.getName());

        try {
            if (!itemStateStore.isFile(propFilePath)) {
                throw new NoSuchItemStateException(id.toString());
            }
            InputStream in = itemStateStore.getInputStream(propFilePath);
            try {
                SAXBuilder builder = new SAXBuilder();
                Element rootElement = builder.build(in).getRootElement();

                PropertyState state = createNew(id);
                readState(rootElement, state);
                return state;
            } finally {
                in.close();
            }
        } catch (JDOMException jde) {
            e = jde;
            // fall through
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
     * @see AbstractPersistenceManager#store
     */
    protected void store(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = state.getUUID();
        String nodeFilePath = buildNodeFilePath(uuid);
        FileSystemResource nodeFile = new FileSystemResource(itemStateStore, nodeFilePath);
        try {
            nodeFile.makeParentDirs();
            OutputStream os = nodeFile.getOutputStream();
            Writer writer = null;
            try {
                String encoding = DEFAULT_ENCODING;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(os, encoding));
                } catch (UnsupportedEncodingException e) {
                    // should never get here!
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    encoding = osw.getEncoding();
                    writer = new BufferedWriter(osw);
                }

                writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
                writer.write("<" + NODE_ELEMENT + " "
                        + UUID_ATTRIBUTE + "=\"" + state.getUUID() + "\" "
                        + PARENTUUID_ATTRIBUTE + "=\"" + (state.getParentUUID() == null ? "" : state.getParentUUID()) + "\" "
                        + DEFINITIONID_ATTRIBUTE + "=\"" + state.getDefinitionId().toString() + "\" "
                        + NODETYPE_ATTRIBUTE + "=\"" + state.getNodeTypeName() + "\">\n");
                // parents
                writer.write("\t<" + PARENTS_ELEMENT + ">\n");
                Iterator iter = state.getParentUUIDs().iterator();
                while (iter.hasNext()) {
                    writer.write("\t\t<" + PARENT_ELEMENT + " "
                            + UUID_ATTRIBUTE + "=\"" + iter.next() + "\"/>\n");
                }
                writer.write("\t</" + PARENTS_ELEMENT + ">\n");

                // mixin types
                writer.write("\t<" + MIXINTYPES_ELEMENT + ">\n");
                iter = state.getMixinTypeNames().iterator();
                while (iter.hasNext()) {
                    writer.write("\t\t<" + MIXINTYPE_ELEMENT + " "
                            + NAME_ATTRIBUTE + "=\"" + iter.next() + "\"/>\n");
                }
                writer.write("\t</" + MIXINTYPES_ELEMENT + ">\n");

                // properties
                writer.write("\t<" + PROPERTIES_ELEMENT + ">\n");
                iter = state.getPropertyEntries().iterator();
                while (iter.hasNext()) {
                    NodeState.PropertyEntry entry = (NodeState.PropertyEntry) iter.next();
                    writer.write("\t\t<" + PROPERTY_ELEMENT + " "
                            + NAME_ATTRIBUTE + "=\"" + entry.getName() + "\">\n");
                    // @todo serialize type, definition id and values
                    writer.write("\t\t</" + PROPERTY_ELEMENT + ">\n");
                }
                writer.write("\t</" + PROPERTIES_ELEMENT + ">\n");

                // child nodes
                writer.write("\t<" + NODES_ELEMENT + ">\n");
                iter = state.getChildNodeEntries().iterator();
                while (iter.hasNext()) {
                    NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
                    writer.write("\t\t<" + NODE_ELEMENT + " "
                            + NAME_ATTRIBUTE + "=\"" + entry.getName() + "\" "
                            + UUID_ATTRIBUTE + "=\"" + entry.getUUID() + "\">\n");
                    writer.write("\t\t</" + NODE_ELEMENT + ">\n");
                }
                writer.write("\t</" + NODES_ELEMENT + ">\n");

                writer.write("</" + NODE_ELEMENT + ">\n");
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            String msg = "failed to write node state: " + uuid;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see AbstractPersistenceManager#store
     */
    protected void store(PropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String propFilePath = buildPropFilePath(state.getParentUUID(), state.getName());
        FileSystemResource propFile = new FileSystemResource(itemStateStore, propFilePath);
        try {
            propFile.makeParentDirs();
            OutputStream os = propFile.getOutputStream();
            // write property state to xml file
            Writer writer = null;
            try {
                String encoding = DEFAULT_ENCODING;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(os, encoding));
                } catch (UnsupportedEncodingException e) {
                    // should never get here!
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    encoding = osw.getEncoding();
                    writer = new BufferedWriter(osw);
                }

                String typeName;
                int type = state.getType();
                try {
                    typeName = PropertyType.nameFromValue(type);
                } catch (IllegalArgumentException iae) {
                    // should never be getting here
                    throw new ItemStateException("unexpected property-type ordinal: " + type, iae);
                }

                writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
                writer.write("<" + PROPERTY_ELEMENT + " " +
                        NAME_ATTRIBUTE + "=\"" + state.getName() + "\" " +
                        PARENTUUID_ATTRIBUTE + "=\"" + state.getParentUUID() + "\" " +
                        MULTIVALUED_ATTRIBUTE + "=\"" + Boolean.toString(state.isMultiValued()) + "\" " +
                        DEFINITIONID_ATTRIBUTE + "=\"" + state.getDefinitionId().toString() + "\" " +
                        TYPE_ATTRIBUTE + "=\"" + typeName + "\">\n");
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
                                // spool binary value to file in blob store
                                BLOBFileValue blobVal = (BLOBFileValue) val.internalValue();
                                String binPath = buildBlobFilePath(state.getParentUUID(), state.getName(), i);
                                OutputStream binOut = null;
                                FileSystemResource internalBlobFile = new FileSystemResource(blobStore, binPath);
                                internalBlobFile.makeParentDirs();
                                try {
                                    binOut = internalBlobFile.getOutputStream();
                                    blobVal.spool(binOut);
                                } finally {
                                    try {
                                        if (binOut != null) {
                                            binOut.close();
                                        }
                                    } catch (IOException ioe) {
                                    }
                                }
                                // store path to binary file as property value
                                writer.write(binPath);
                                // FIXME: hack!
                                // replace value instance with value
                                // backed by internal file and delete temp file
                                values[i] = InternalValue.create(internalBlobFile);
                                if (blobVal.isTempFile()) {
                                    blobVal.delete();
                                    blobVal = null;
                                }
                            } else {
                                // escape '<' and '&'
                                char chars[] = val.toString().toCharArray();
                                int j = 0, last = 0;
                                while (j < chars.length) {
                                    char c = chars[j];
                                    if (c == '<') {
                                        writer.write(chars, last, j - last);
                                        writer.write("&lt;");
                                        last = j + 1;
                                    } else if (c == '&') {
                                        writer.write(chars, last, j - last);
                                        writer.write("&amp;");
                                        last = j + 1;
                                    }
                                    j++;
                                }
                                writer.write(chars, last, j - last);
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
            String msg = "failed to store property state: " + state.getParentUUID() + "/" + state.getName();
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see AbstractPersistenceManager#destroy
     */
    protected void destroy(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = state.getUUID();
        String nodeFilePath = buildNodeFilePath(uuid);
        FileSystemResource nodeFile = new FileSystemResource(itemStateStore, nodeFilePath);
        try {
            if (nodeFile.exists()) {
                // delete resource and prune empty parent folders
                nodeFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete node state: " + uuid;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see AbstractPersistenceManager#destroy
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
                    if (val.getType() == PropertyType.BINARY) {
                        BLOBFileValue blobVal = (BLOBFileValue) val.internalValue();
                        // delete blob file and prune empty parent folders
                        blobVal.delete(true);
                    }
                }
            }
        }
        // delete property file
        String propFilePath = buildPropFilePath(state.getParentUUID(), state.getName());
        FileSystemResource propFile = new FileSystemResource(itemStateStore, propFilePath);
        try {
            if (propFile.exists()) {
                // delete resource and prune empty parent folders
                propFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete property state: " + state.getParentUUID() + "/" + state.getName();
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#load
     */
    public synchronized NodeReferences load(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String uuid = id.getUUID();

        String refsFilePath = buildNodeReferencesFilePath(uuid);
        try {
            if (!itemStateStore.isFile(refsFilePath)) {
                throw new NoSuchItemStateException(uuid);
            }

            InputStream in = itemStateStore.getInputStream(refsFilePath);

            try {
                SAXBuilder builder = new SAXBuilder();
                Element rootElement = builder.build(in).getRootElement();

                NodeReferences refs = new NodeReferences(id);
                readState(rootElement, refs);
                return refs;
            } finally {
                in.close();
            }
        } catch (JDOMException jde) {
            e = jde;
            // fall through
        } catch (IOException ioe) {
            e = ioe;
            // fall through
        } catch (FileSystemException fse) {
            e = fse;
            // fall through
        }
        String msg = "failed to load references: " + uuid;
        log.debug(msg);
        throw new ItemStateException(msg, e);
    }

    /**
     * @see AbstractPersistenceManager#store
     */
    protected void store(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = refs.getUUID();
        String refsFilePath = buildNodeReferencesFilePath(uuid);
        FileSystemResource refsFile = new FileSystemResource(itemStateStore, refsFilePath);
        try {
            refsFile.makeParentDirs();
            OutputStream os = refsFile.getOutputStream();
            BufferedWriter writer = null;
            try {
                String encoding = DEFAULT_ENCODING;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(os, encoding));
                } catch (UnsupportedEncodingException e) {
                    // should never get here!
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    encoding = osw.getEncoding();
                    writer = new BufferedWriter(osw);
                }
                writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
                writer.write("<" + NODEREFERENCES_ELEMENT + " "
                        + TARGETID_ATTRIBUTE + "=\"" + refs.getTargetId() + "\">\n");
                // write references (i.e. the id's of the REFERENCE properties)
                Iterator iter = refs.getReferences().iterator();
                while (iter.hasNext()) {
                    PropertyId propId = (PropertyId) iter.next();
                    writer.write("\t<" + NODEREFERENCE_ELEMENT + " "
                            + PROPERTYID_ATTRIBUTE + "=\"" + propId + "\"/>\n");
                }
                writer.write("</" + NODEREFERENCES_ELEMENT + ">\n");
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            String msg = "failed to store references: " + uuid;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see AbstractPersistenceManager#destroy
     */
    protected void destroy(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = refs.getUUID();
        String refsFilePath = buildNodeReferencesFilePath(uuid);
        FileSystemResource refsFile = new FileSystemResource(itemStateStore, refsFilePath);
        try {
            if (refsFile.exists()) {
                // delete resource and prune empty parent folders
                refsFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete references: " + uuid;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#exists(NodeId)
     */
    public synchronized boolean exists(NodeId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            NodeId nodeId = (NodeId) id;
            String nodeFilePath = buildNodeFilePath(nodeId.getUUID());
            FileSystemResource nodeFile = new FileSystemResource(itemStateStore, nodeFilePath);
            return nodeFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of item state: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#exists(PropertyId)
     */
    public synchronized boolean exists(PropertyId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String propFilePath = buildPropFilePath(id.getParentUUID(), id.getName());
            FileSystemResource propFile = new FileSystemResource(itemStateStore, propFilePath);
            return propFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of item state: " + id;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#exists(NodeReferencesId id)
     */
    public synchronized boolean exists(NodeReferencesId id)
            throws ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String uuid = id.getUUID();
            String refsFilePath = buildNodeReferencesFilePath(uuid);
            FileSystemResource refsFile = new FileSystemResource(itemStateStore, refsFilePath);
            return refsFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of references: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }
}
