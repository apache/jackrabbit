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
package org.apache.jackrabbit.core.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

/**
 * Cluster record representing a node type registration, re-registration or
 * unregistration.
 */
public class NodeTypeRecord extends ClusterRecord {

    /**
     * Operation type: registration.
     */
    public static final int REGISTER = 1;

    /**
     * Operation type: re-registration.
     */
    public static final int REREGISTER = 2;

    /**
     * Operation type: unregistration.
     */
    public static final int UNREGISTER = 3;

    /**
     * Identifier: NODETYPE.
     */
    static final char IDENTIFIER = 'T';

    /**
     * Bit indicating this is a registration operation.
     */
    private static final int NTREG_REGISTER = 0;

    /**
     * Bit indicating this is a reregistration operation.
     */
    private static final int NTREG_REREGISTER = (1 << 30);

    /**
     * Bit indicating this is an unregistration operation.
     */
    private static final int NTREG_UNREGISTER = (1 << 31);

    /**
     * Mask used in node type registration operations.
     */
    private static final int NTREG_MASK = (NTREG_REREGISTER | NTREG_UNREGISTER);

    /**
     * Operation type.
     */
    private int operation;

    /**
     * Collection of node type definitions or node type names.
     */
    private Collection collection;

    /**
     * Create a new instance of this class. Used when serializing a node type
     * registration or unregistration.
     *
     * @param collection collection of node types definitions or node type names
     * @param isRegister <code>true</code> if this is a registration;
     *                   <code>false</code> if this is a unregistration
     * @param record journal record
     */
    public NodeTypeRecord(Collection collection, boolean isRegister, Record record) {
        super(record);

        this.collection = collection;
        this.operation = isRegister ? REGISTER : UNREGISTER;
    }

    /**
     * Create a new instance of this class. Used when serializing a node type
     * re-registration.
     *
     * @param ntDef node type definition
     * @param record journal record
     */
    public NodeTypeRecord(QNodeTypeDefinition ntDef, Record record) {
        super(record);

        this.collection = new ArrayList();
        this.collection.add(ntDef);
        this.operation = REREGISTER;
    }

    /**
     * Create a new instance of this class. Used when deseralizing a node type
     * registration, re-registration or unregistration.
     *
     * @param record journal record
     */
    NodeTypeRecord(Record record) {
        super(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRead() throws JournalException {
        int size = record.readInt();
        int opcode = size & NTREG_MASK;
        size &= ~NTREG_MASK;

        switch (opcode) {
        case NTREG_REGISTER:
            operation = REGISTER;
            collection = new HashSet();
            for (int i = 0; i < size; i++) {
                collection.add(record.readNodeTypeDef());
            }
            break;
        case NTREG_REREGISTER:
            operation = REREGISTER;
            collection = new HashSet();
            collection.add(record.readNodeTypeDef());
            break;
        case NTREG_UNREGISTER:
            operation = UNREGISTER;
            collection = new HashSet();
            for (int i = 0; i < size; i++) {
                collection.add(record.readQName());
            }
            break;
        default:
            String msg = "Unknown opcode: " + opcode;
            throw new JournalException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doWrite() throws JournalException {
        record.writeChar(IDENTIFIER);

        int size = collection.size();
        size |= getBitMask();
        record.writeInt(size);

        Iterator iter = collection.iterator();
        while (iter.hasNext()) {
            if (operation == UNREGISTER) {
                record.writeQName((Name) iter.next());
            } else {
                record.writeNodeTypeDef((QNodeTypeDefinition) iter.next());
            }
        }
    }

    /**
     * Return the bit mask associated with an operation type.
     *
     * @return bit mask
     */
    private int getBitMask() {
        switch (operation) {
        case REGISTER:
            return NTREG_REGISTER;
        case UNREGISTER:
            return NTREG_UNREGISTER;
        case REREGISTER:
            return NTREG_REREGISTER;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(ClusterRecordProcessor processor) {
        processor.process(this);
    }

    /**
     * Return the operation type.
     * @return <code>REGISTER</code>, <code>REREGISTER</code> or
     *         <code>UNREGISTER</code>
     */
    public int getOperation() {
        return operation;
    }

    /**
     * Return the collection of node type definitions or node type names.
     *
     * @return unmodifiable collection
     */
    public Collection getCollection() {
        return Collections.unmodifiableCollection(collection);
    }
}
