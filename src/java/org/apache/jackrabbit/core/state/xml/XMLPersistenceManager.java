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
package org.apache.jackrabbit.core.state.xml;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
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
 * <code>XMLPersistenceManager</code> ...
 */
public class XMLPersistenceManager implements PersistenceManager {

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
    private static final String COUNT_ATTRIBUTE = "count";
    private static final String VALUES_ELEMENT = "values";
    private static final String VALUE_ELEMENT = "value";

    private static final String NODES_ELEMENT = "nodes";

    private static final String NODEFILENAME = ".node.xml";

    private static final String NODEREFSFILENAME = ".references";

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
            //fileName = new String(chars) + ".xml";
            fileName = new String(chars);
        } catch (NoSuchAlgorithmException nsae) {
            // should never get here as MD5 should always be available in the JRE
            String msg = "MD5 not available: ";
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

    private void readState(Element nodeElement, PersistentNodeState state)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!nodeElement.getName().equals(NODE_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: " + nodeElement.getName() + ")";
            log.error(msg);
            throw new ItemStateException(msg);
        }
        // check uuid
        if (!state.getUUID().equals(nodeElement.getAttributeValue(UUID_ATTRIBUTE))) {
            String msg = "invalid serialized state: uuid mismatch";
            log.error(msg);
            throw new ItemStateException(msg);
        }
        // check nodetype
        String ntName = nodeElement.getAttributeValue(NODETYPE_ATTRIBUTE);
        if (!QName.valueOf(ntName).equals(state.getNodeTypeName())) {
            String msg = "invalid serialized state: nodetype mismatch";
            log.error(msg);
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

    private void readState(Element propElement, PersistentPropertyState state)
            throws ItemStateException {
        // first do some paranoid sanity checks
        if (!propElement.getName().equals(PROPERTY_ELEMENT)) {
            String msg = "invalid serialization format (unexpected element: " + propElement.getName() + ")";
            log.error(msg);
            throw new ItemStateException(msg);
        }
        // check name
        if (!state.getName().equals(QName.valueOf(propElement.getAttributeValue(NAME_ATTRIBUTE)))) {
            String msg = "invalid serialized state: name mismatch";
            log.error(msg);
            throw new ItemStateException(msg);
        }
        // check parentUUID
        String parentUUID = propElement.getAttributeValue(PARENTUUID_ATTRIBUTE);
        if (!parentUUID.equals(state.getParentUUID())) {
            String msg = "invalid serialized state: parentUUID mismatch";
            log.error(msg);
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
                        log.error(msg, ioe);
                        throw new ItemStateException(msg, ioe);
                    }
                } else {
                    val = InternalValue.valueOf(text, type);
                }
            } else {
                // null value
                val = null;
            }
            values.add(val);
        }
        state.setValues((InternalValue[]) values.toArray(new InternalValue[values.size()]));
    }

    private void readState(Properties props, PersistentPropertyState state)
            throws ItemStateException {
        // type
        String typeName = props.getProperty(TYPE_ATTRIBUTE);
        int type;
        try {
            type = PropertyType.valueFromName(typeName);
        } catch (IllegalArgumentException iae) {
            // should never be getting here
            throw new ItemStateException("unexpected property-type: " + typeName, iae);
        }
        state.setType(type);

        // definition id
        if (props.containsKey(DEFINITIONID_ATTRIBUTE)) {
            state.setDefinitionId(PropDefId.valueOf(props.getProperty(DEFINITIONID_ATTRIBUTE)));
        }

        // # of values
        int cnt = Integer.parseInt(props.getProperty(COUNT_ATTRIBUTE));

        // values
        InternalValue[] values = new InternalValue[cnt];
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (TYPE_ATTRIBUTE.equals(name) || COUNT_ATTRIBUTE.equals(name)
                    || DEFINITIONID_ATTRIBUTE.equals(name)) {
                continue;
            }
            int i = Integer.parseInt(name);
            InternalValue val;
            String text = props.getProperty(name);
            if (text != null) {
                if (type == PropertyType.BINARY) {
                    // special handling required for binary value:
                    // the value stores the path to the actual binary file in the blob store
                    try {
                        val = InternalValue.create(new FileSystemResource(blobStore, text));
                    } catch (IOException ioe) {
                        String msg = "error while reading serialized binary valuey";
                        log.error(msg, ioe);
                        throw new ItemStateException(msg, ioe);
                    }
                } else {
                    val = InternalValue.valueOf(text, type);
                }
            } else {
                // null value
                val = null;
            }
            values[i] = val;
        }
        state.setValues(values);
    }

    //---------------------------------------------------< PersistenceManager >
    /**
     * @see PersistenceManager#init
     */
    public void init(WorkspaceConfig wspConfig) throws Exception {
        FileSystem wspFS = wspConfig.getFileSystem();
        itemStateStore = new BasedFileSystem(wspFS, "/data");

        //blobStore = new BasedFileSystem(wspFS, "/blobs");
        /**
         * store blob's in local file system in a sub directory
         * of the workspace home directory
         * todo make blob store configurable
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setPath(wspConfig.getHomeDir() + "/blobs");
        blobFS.init();
        blobStore = blobFS;

        initialized = true;
    }

    /**
     * @see PersistenceManager#loadNodeState
     */
    public synchronized PersistentNodeState loadNodeState(String uuid)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String nodeFilePath = buildNodeFilePath(uuid);
        try {
            if (!itemStateStore.isFile(nodeFilePath)) {
                throw new NoSuchItemStateException(uuid);
            }
            InputStream in = itemStateStore.getInputStream(nodeFilePath);

            try {
                SAXBuilder builder = new SAXBuilder();
                Element rootElement = builder.build(in).getRootElement();
                String ntName = rootElement.getAttributeValue(NODETYPE_ATTRIBUTE);

                PersistentNodeState state = createNodeStateInstance(uuid, QName.valueOf(ntName));
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
        String msg = "failed to read node state: " + uuid;
        log.error(msg, e);
        throw new ItemStateException(msg, e);
    }

    /**
     * @see PersistenceManager#reload
     */
    public synchronized void reload(PersistentNodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String uuid = state.getUUID();
        String nodeFilePath = buildNodeFilePath(uuid);
        try {
            InputStream in = itemStateStore.getInputStream(nodeFilePath);
            try {
                SAXBuilder builder = new SAXBuilder();
                Element rootElement = builder.build(in).getRootElement();

                // reset state
                state.removeAllParentUUIDs();
                state.removeAllPropertyEntries();
                state.removeAllChildNodeEntries();

                readState(rootElement, state);
                return;
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
        String msg = "failed to read node state: " + uuid;
        log.error(msg, e);
        throw new ItemStateException(msg, e);
    }

    /**
     * @see PersistenceManager#loadPropertyState
     */
    public synchronized PersistentPropertyState loadPropertyState(String parentUUID, QName propName)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

/*
	Exception e = null;
	String propFilePath = buildPropFilePath(parentUUID, propName);
	try {
	    if (!fs.isFile(propFilePath)) {
		throw new NoSuchItemStateException(parentUUID + "/" + propName);
	    }
	    InputStream in = fs.getInputStream(propFilePath);
	    try {
		SAXBuilder builder = new SAXBuilder();
		Element rootElement = builder.build(in).getRootElement();

		PersistentPropertyState state = createPropertyStateInstance(propName, parentUUID);
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
	String msg = "failed to read property state: " + parentUUID + "/" + propName;
	log.error(msg, e);
	throw new ItemStateException(msg, e);
*/
        Exception e = null;
        String propFilePath = buildPropFilePath(parentUUID, propName);
        try {
            if (!itemStateStore.isFile(propFilePath)) {
                throw new NoSuchItemStateException(parentUUID + "/" + propName);
            }
            InputStream in = itemStateStore.getInputStream(propFilePath);
            try {
                Properties props = new Properties();
                props.load(in);
                PersistentPropertyState state = createPropertyStateInstance(propName, parentUUID);
                readState(props, state);

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
        String msg = "failed to read property state: " + parentUUID + "/" + propName;
        log.error(msg, e);
        throw new ItemStateException(msg, e);
    }

    /**
     * @see PersistenceManager#reload
     */
    public synchronized void reload(PersistentPropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String parentUUID = state.getParentUUID();
        QName propName = state.getName();
        String propFilePath = buildPropFilePath(parentUUID, propName);
        try {
            InputStream in = itemStateStore.getInputStream(propFilePath);
            try {
                SAXBuilder builder = new SAXBuilder();
                Element rootElement = builder.build(in).getRootElement();
                readState(rootElement, state);
                return;
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
        String msg = "failed to read property state: " + parentUUID + "/" + propName;
        log.error(msg, e);
        throw new ItemStateException(msg, e);
    }

    /**
     * @see PersistenceManager#store
     */
    public synchronized void store(PersistentNodeState state) throws ItemStateException {
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
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#store
     */
    public synchronized void store(PersistentPropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String propFilePath = buildPropFilePath(state.getParentUUID(), state.getName());
        FileSystemResource propFile = new FileSystemResource(itemStateStore, propFilePath);
        try {
            propFile.makeParentDirs();
            OutputStream os = propFile.getOutputStream();
/*
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
*/
            Properties props = new Properties();

            // type
            String typeName;
            int type = state.getType();
            try {
                typeName = PropertyType.nameFromValue(type);
            } catch (IllegalArgumentException iae) {
                // should never be getting here
                throw new ItemStateException("unexpected property-type ordinal: " + type, iae);
            }
            props.setProperty(TYPE_ATTRIBUTE, typeName);

            // definition id
            props.setProperty(DEFINITIONID_ATTRIBUTE, state.getDefinitionId().toString());

            InternalValue[] values = state.getValues();

            // # of values
            props.setProperty(COUNT_ATTRIBUTE, Integer.toString(values == null ? 0 : values.length));

            // values
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
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
                            props.setProperty(Integer.toString(i), binPath);
                            // FIXME: hack!
                            // replace value instance with value
                            // backed by internal file and delete temp file
                            values[i] = InternalValue.create(internalBlobFile);
                            if (blobVal.isTempFile()) {
                                blobVal.delete();
                                blobVal = null;
                            }
                        } else {
                            props.setProperty(Integer.toString(i), val.toString());
                        }
                    } else {
                        // null value
                        props.setProperty(Integer.toString(i), null);
                    }
                }
            }

            try {
                props.store(os, null);
            } finally {
                // make sure stream is closed
                os.close();
            }
        } catch (Exception e) {
            String msg = "failed to store property state: " + state.getParentUUID() + "/" + state.getName();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#destroy
     */
    public synchronized void destroy(PersistentNodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = state.getUUID();
        String nodeFilePath = buildNodeFilePath(uuid);
        FileSystemResource nodeFile = new FileSystemResource(itemStateStore, nodeFilePath);
        try {
            nodeFile.delete();
            // prune empty folders
            String parentDir = FileSystemPathUtil.getParentDir(nodeFilePath);
            while (!parentDir.equals(FileSystem.SEPARATOR)
                    && !itemStateStore.hasChildren(parentDir)) {
                itemStateStore.deleteFolder(parentDir);
                parentDir = FileSystemPathUtil.getParentDir(parentDir);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete node state: " + uuid;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#destroy
     */
    public synchronized void destroy(PersistentPropertyState state) throws ItemStateException {
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
                        blobVal.delete();
                    }
                }
            }
        }
        // delete property xml file
        String propFilePath = buildPropFilePath(state.getParentUUID(), state.getName());
        FileSystemResource propFile = new FileSystemResource(itemStateStore, propFilePath);
        try {
            propFile.delete();
            // prune empty folders
            String parentDir = FileSystemPathUtil.getParentDir(propFilePath);
            while (!parentDir.equals(FileSystem.SEPARATOR)
                    && !itemStateStore.hasChildren(parentDir)) {
                itemStateStore.deleteFolder(parentDir);
                parentDir = FileSystemPathUtil.getParentDir(parentDir);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete property state: " + state.getParentUUID() + "/" + state.getName();
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#createNodeStateInstance
     */
    public PersistentNodeState createNodeStateInstance(String uuid, QName nodeTypeName) {
        return new XMLNodeState(uuid, nodeTypeName, null, this);
    }

    /**
     * @see PersistenceManager#createPropertyStateInstance
     */
    public PersistentPropertyState createPropertyStateInstance(QName name, String parentUUID) {
        return new XMLPropertyState(name, parentUUID, this);
    }

    /**
     * @see PersistenceManager#createNodeReferencesInstance(String)
     */
    public NodeReferences createNodeReferencesInstance(String uuid) {
        return new NodeReferences(new NodeId(uuid));
    }

    /**
     * @see PersistenceManager#loadNodeReferences(String uuid)
     */
    public NodeReferences loadNodeReferences(String uuid) throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String refsFilePath = buildNodeReferencesFilePath(uuid);
        try {
            if (!itemStateStore.isFile(refsFilePath)) {
                throw new NoSuchItemStateException(uuid);
            }
            NodeReferences refs = createNodeReferencesInstance(uuid);

            InputStream in = itemStateStore.getInputStream(refsFilePath);
            BufferedReader reader = null;
            try {
                String encoding = DEFAULT_ENCODING;
                try {
                    reader = new BufferedReader(new InputStreamReader(in, encoding));
                } catch (UnsupportedEncodingException uee) {
                    // should never get here!
                    InputStreamReader isw = new InputStreamReader(in);
                    encoding = isw.getEncoding();
                    reader = new BufferedReader(isw);
                }
                // read references (i.e. the id's of the REFERENCE properties)
                String s;
                while ((s = reader.readLine()) != null) {
                    if (s.length() > 0) {
                        PropertyId propId = PropertyId.valueOf(s);
                        refs.addReference(propId);
                    }
                }
            } finally {
                reader.close();
            }

            return refs;
        } catch (IOException ioe) {
            e = ioe;
            // fall through
        } catch (FileSystemException fse) {
            e = fse;
            // fall through
        }
        String msg = "failed to load references: " + uuid;
        log.error(msg, e);
        throw new ItemStateException(msg, e);
    }

    /**
     * @see PersistenceManager#reload(NodeReferences)
     */
    public void reload(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Exception e = null;
        String uuid = refs.getTargetId().getUUID();
        String refsFilePath = buildNodeReferencesFilePath(uuid);
        try {
            if (!itemStateStore.isFile(refsFilePath)) {
                throw new NoSuchItemStateException(uuid);
            }

            refs.clearAllReferences();

            InputStream in = itemStateStore.getInputStream(refsFilePath);
            BufferedReader reader = null;
            try {
                String encoding = DEFAULT_ENCODING;
                try {
                    reader = new BufferedReader(new InputStreamReader(in, encoding));
                } catch (UnsupportedEncodingException uee) {
                    // should never get here!
                    InputStreamReader isw = new InputStreamReader(in);
                    encoding = isw.getEncoding();
                    reader = new BufferedReader(isw);
                }
                // read references (i.e. the id's of the REFERENCE properties)
                String s;
                while ((s = reader.readLine()) != null) {
                    if (s.length() > 0) {
                        PropertyId propId = PropertyId.valueOf(s);
                        refs.addReference(propId);
                    }
                }
            } finally {
                reader.close();
            }

            return;
        } catch (IOException ioe) {
            e = ioe;
            // fall through
        } catch (FileSystemException fse) {
            e = fse;
            // fall through
        }
        String msg = "failed to load references: " + uuid;
        log.error(msg, e);
        throw new ItemStateException(msg, e);
    }

    /**
     * @see PersistenceManager#store(NodeReferences)
     */
    public void store(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = refs.getTargetId().getUUID();
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
                // write references (i.e. the id's of the REFERENCE properties)
                Iterator iter = refs.getReferences().iterator();
                while (iter.hasNext()) {
                    PropertyId propId = (PropertyId) iter.next();
                    writer.write(propId.toString());
                    writer.newLine();
                }
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            String msg = "failed to store references: " + uuid;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#destroy(NodeReferences)
     */
    public void destroy(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = refs.getTargetId().getUUID();
        String refsFilePath = buildNodeReferencesFilePath(uuid);
        FileSystemResource refsFile = new FileSystemResource(itemStateStore, refsFilePath);
        try {
            refsFile.delete();
            // prune empty folders
            String parentDir = FileSystemPathUtil.getParentDir(refsFilePath);
            while (!parentDir.equals(FileSystem.SEPARATOR)
                    && !itemStateStore.hasChildren(parentDir)) {
                itemStateStore.deleteFolder(parentDir);
                parentDir = FileSystemPathUtil.getParentDir(parentDir);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete references: " + uuid;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }
}
