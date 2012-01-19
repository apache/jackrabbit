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

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;

import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.xml.ClonedInputSource;
import org.xml.sax.InputSource;

/**
 * Record for propagating workspace modifications across the cluster. Currently
 * only workspace creation is propagated because workspace deletion is not yet
 * implemented.
 */
public class WorkspaceRecord extends ClusterRecord {

    /**
     * Identifier: NAMESPACE.
     */
    static final char IDENTIFIER = 'W';

    /**
     * Subtype for determining workspace action.
     */
    public static final int CREATE_WORKSPACE_ACTION_TYPE = 1;

    /**
     * Base workspace action
     */
    public abstract static class Action {
        abstract int getType();

        abstract void write(Record record) throws JournalException;

        abstract void read(Record record) throws JournalException;
    }

    /**
     * Action for workspace creation.
     */
    static final class CreateWorkspaceAction extends Action {
        private InputSource inputSource;
        private char[] charArray;
        private byte[] byteArray;

        @Override
        int getType() {
            return CREATE_WORKSPACE_ACTION_TYPE;
        }

        CreateWorkspaceAction() {

        }

        CreateWorkspaceAction(ClonedInputSource inputSource) {
            this.inputSource = inputSource;
            this.charArray = inputSource.getCharacterArray();
            this.byteArray = inputSource.getByteArray();
        }

        @Override
        void write(Record record) throws JournalException {
            // store the input source
            record.writeString(inputSource.getEncoding());
            record.writeString(inputSource.getPublicId());
            record.writeString(inputSource.getSystemId());

            // save character array if present
            if (charArray != null) {
                record.writeBoolean(true);
                record.writeString(new String(charArray));
            } else {
                record.writeBoolean(false);
            }

            // save the bytearray if present
            if (byteArray != null) {
                record.writeBoolean(true);
                record.writeInt(byteArray.length);
                record.write(byteArray);
            } else {
                record.writeBoolean(false);
            }
        }

        @Override
        void read(Record record) throws JournalException {
            // restore the input source
            inputSource = new InputSource();
            inputSource.setEncoding(record.readString());
            inputSource.setPublicId(record.readString());
            inputSource.setSystemId(record.readString());

            if (record.readBoolean()) {
                charArray = record.readString().toCharArray();
                inputSource.setCharacterStream(new CharArrayReader(charArray));
            }
            if (record.readBoolean()) {
                final int size = record.readInt();
                byteArray = new byte[size];
                record.readFully(byteArray);
                inputSource.setByteStream(new ByteArrayInputStream(byteArray));
            }
        }

        public InputSource getInputSource() {
            return inputSource;
        }
    }

    // current action
    private Action action;

    /**
     * Creates a new {@link WorkspaceRecord} for create workspace action.
     *
     * @param workspace
     *            workspace name
     * @param inputSource
     *            input source with configuration for the workspace
     * @param record
     *            journal record
     */
    protected WorkspaceRecord(String workspace, ClonedInputSource inputSource,
            Record record) {
        super(record, workspace);

        action = new CreateWorkspaceAction(inputSource);
    }

    /**
     * Creates a new empty {@link WorkspaceRecord}.
     *
     * @param record
     */
    protected WorkspaceRecord(Record record) {
        super(record);
    }

    @Override
    protected void doRead() throws JournalException {

        workspace = record.readString();

        // determine type
        int action = record.readInt();

        if (action == CREATE_WORKSPACE_ACTION_TYPE) {
            this.action = new CreateWorkspaceAction();
        }

        if (this.action != null) {
            this.action.read(record);
        } else {
            throw new JournalException("Unknown workspace action type");
        }
    }

    @Override
    protected void doWrite() throws JournalException {

        record.writeChar(IDENTIFIER);

        record.writeString(workspace);

        // store the type
        record.writeInt(getActionType());

        if (action != null) {
            action.write(record);
        } else {
            throw new JournalException("Can not write empty workspace action");
        }
    }

    public int getActionType() {
        return action != null ? action.getType() : -1;
    }

    public CreateWorkspaceAction getCreateWorkspaceAction() {
        return (CreateWorkspaceAction) action;
    }

    @Override
    public void process(ClusterRecordProcessor processor) {
        processor.process(this);
    }

}
