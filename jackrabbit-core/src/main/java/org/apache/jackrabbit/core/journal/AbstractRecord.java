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
package org.apache.jackrabbit.core.journal;

import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefWriter;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.uuid.Constants;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import javax.jcr.NamespaceException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * Base implementation for a record.
 */
public abstract class AbstractRecord implements Record {

    /**
     * Indicator for a literal UUID.
     */
    private static final byte UUID_LITERAL = 'L';

    /**
     * Indicator for a UUID index.
     */
    private static final byte UUID_INDEX = 'I';

    /**
     * Maps NodeId to Integer index.
     */
    private final BidiMap nodeIdIndex = new DualHashBidiMap();

    /**
     * Namespace resolver.
     */
    protected final NamespaceResolver nsResolver;

    /**
     * Name and Path resolver.
     */
    protected final NamePathResolver resolver;

    /**
     * Create a new instance of this class.
     */
    public AbstractRecord(NamespaceResolver nsResolver, NamePathResolver resolver) {
        this.nsResolver = nsResolver;
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     */
    public void writeQName(Name name) throws JournalException {
        try {
            writeString(resolver.getJCRName(name));
        } catch (NamespaceException e) {
            String msg = "Undeclared prefix error while writing name.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writePathElement(Path.Element element) throws JournalException {
        writeQName(element.getName());
        writeInt(element.getIndex());
    }

    /**
     * {@inheritDoc}
     */
    public void writePath(Path path) throws JournalException {
        try {
            writeString(resolver.getJCRPath(path));
        } catch (NamespaceException e) {
            String msg = "Undeclared prefix error while writing path.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeNodeId(NodeId nodeId) throws JournalException {
        if (nodeId == null) {
            writeByte(UUID_INDEX);
            writeInt(-1);
        } else {
            int index = getOrCreateIndex(nodeId);
            if (index != -1) {
                writeByte(UUID_INDEX);
                writeInt(index);
            } else {
                writeByte(UUID_LITERAL);
                write(nodeId.getUUID().getRawBytes());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writePropertyId(PropertyId propertyId) throws JournalException {
        writeNodeId(propertyId.getParentId());
        writeQName(propertyId.getName());
    }

    /**
     * {@inheritDoc}
     */
    public void writeNodeTypeDef(NodeTypeDef ntd) throws JournalException {
        try {
            StringWriter sw = new StringWriter();
            CompactNodeTypeDefWriter writer = new CompactNodeTypeDefWriter(sw, nsResolver, resolver, true);
            writer.write(ntd);
            writer.close();

            writeString(sw.toString());
        } catch (IOException e) {
            String msg = "I/O error while writing node type definition.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name readQName() throws JournalException {
        try {
            return resolver.getQName(readString());
        } catch (NameException e) {
            String msg = "Unknown prefix error while reading name.";
            throw new JournalException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Illegal name error while reading name.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Path.Element readPathElement() throws JournalException {
        try {
            Name name = resolver.getQName(readString());
            int index = readInt();
            if (index != 0) {
                return PathFactoryImpl.getInstance().createElement(name, index);
            } else {
                return PathFactoryImpl.getInstance().createElement(name);
            }
        } catch (NameException e) {
            String msg = "Unknown prefix error while reading path element.";
            throw new JournalException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Illegal name error while reading path element.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Path readPath() throws JournalException {
        try {
            return resolver.getQPath(readString());
        } catch (MalformedPathException e) {
            String msg = "Malformed path error while reading path.";
            throw new JournalException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Malformed path error while reading path.";
            throw new JournalException(msg, e);
        } catch (NameException e) {
            String msg = "Malformed path error while reading path.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeId readNodeId() throws JournalException {
        byte uuidType = readByte();
        if (uuidType == UUID_INDEX) {
            int index = readInt();
            if (index == -1) {
                return null;
            } else {
                return (NodeId) nodeIdIndex.getKey(new Integer(index));
            }
        } else if (uuidType == UUID_LITERAL) {
            byte[] b = new byte[Constants.UUID_BYTE_LENGTH];
            readFully(b);
            NodeId nodeId = new NodeId(new UUID(b));
            nodeIdIndex.put(nodeId, new Integer(nodeIdIndex.size()));
            return nodeId;
        } else {
            String msg = "Unknown UUID type found: " + uuidType;
            throw new JournalException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PropertyId readPropertyId() throws JournalException {
        return new PropertyId(readNodeId(), readQName());
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeDef readNodeTypeDef() throws JournalException {
        try {
            StringReader sr = new StringReader(readString());

            CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(
                    sr, "(internal)", new NamespaceMapping(nsResolver));
            List ntds = reader.getNodeTypeDefs();
            if (ntds.size() != 1) {
                throw new JournalException("Expected one node type definition: got " + ntds.size());
            }
            return (NodeTypeDef) ntds.get(0);
        } catch (ParseException e) {
            String msg = "Parse error while reading node type definition.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * Get a <code>NodeId</code>'s existing cache index, creating a new entry if necesary.
     *
     * @param nodeId nodeId to lookup
     * @return cache index of existing entry or <code>-1</code> to indicate the entry was added
     */
    private int getOrCreateIndex(NodeId nodeId) {
        Integer index = (Integer) nodeIdIndex.get(nodeId);
        if (index == null) {
            nodeIdIndex.put(nodeId, new Integer(nodeIdIndex.size()));
            return -1;
        } else {
            return index.intValue();
        }
    }
}