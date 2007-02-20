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

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefWriter;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.uuid.Constants;
import org.apache.jackrabbit.uuid.UUID;

import java.util.ArrayList;
import java.util.List;
import java.io.StringWriter;
import java.io.IOException;
import java.io.StringReader;

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
     * UUID index.
     */
    private final ArrayList uuidIndex = new ArrayList();

    /**
     * Namespace resolver.
     */
    protected final NamespaceResolver resolver;

    /**
     * Create a new instance of this class.
     */
    public AbstractRecord(NamespaceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     */
    public void writeQName(QName name) throws JournalException {
        try {
            writeString(NameFormat.format(name, resolver));
        } catch (NoPrefixDeclaredException e) {
            String msg = "Undeclared prefix error while writing name.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writePathElement(Path.PathElement element) throws JournalException {
        writeQName(element.getName());
        writeInt(element.getIndex());
    }

    /**
     * {@inheritDoc}
     */
    public void writePath(Path path) throws JournalException {
        try {
            writeString(PathFormat.format(path, resolver));
        } catch (NoPrefixDeclaredException e) {
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
            CompactNodeTypeDefWriter writer = new CompactNodeTypeDefWriter(sw, resolver, true);
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
    public QName readQName() throws JournalException {
        try {
            return NameFormat.parse(readString(), resolver);
        } catch (UnknownPrefixException e) {
            String msg = "Unknown prefix error while reading name.";
            throw new JournalException(msg, e);
        } catch (IllegalNameException e) {
            String msg = "Illegal name error while reading name.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Path.PathElement readPathElement() throws JournalException {
        try {
            QName name = NameFormat.parse(readString(), resolver);
            int index = readInt();
            if (index != 0) {
                return Path.PathElement.create(name, index);
            } else {
                return Path.PathElement.create(name);
            }
        } catch (UnknownPrefixException e) {
            String msg = "Unknown prefix error while reading path element.";
            throw new JournalException(msg, e);
        } catch (IllegalNameException e) {
            String msg = "Illegal name error while reading path element.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Path readPath() throws JournalException {
        try {
            return PathFormat.parse(readString(), resolver);
        } catch (MalformedPathException e) {
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
                return (NodeId) uuidIndex.get(index);
            }
        } else if (uuidType == UUID_LITERAL) {
            byte[] b = new byte[Constants.UUID_BYTE_LENGTH];
            readFully(b);
            NodeId nodeId = new NodeId(new UUID(b));
            uuidIndex.add(nodeId);
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

            CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(sr, "(internal)");
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
        int index = uuidIndex.indexOf(nodeId);
        if (index == -1) {
            uuidIndex.add(nodeId);
        }
        return index;
    }
}