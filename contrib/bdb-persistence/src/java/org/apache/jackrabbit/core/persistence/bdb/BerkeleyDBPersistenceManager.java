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
package org.apache.jackrabbit.core.persistence.bdb;

import java.io.File;

import javax.jcr.PropertyType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.persistence.AbstractPersistenceManager;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class BerkeleyDBPersistenceManager extends AbstractPersistenceManager {

    private static Log log = LogFactory.getLog(BerkeleyDBPersistenceManager.class);

    protected static final String ENCODING = "UTF-8";

    private boolean initialized = false;
    private Environment environment;
    private Database database;
    // file system where BLOB data is stored
    private FileSystem blobFS;
    // BLOBStore that manages BLOB data in the file system
    private BLOBStore blobStore;

    private ThreadLocal localTransaction = new ThreadLocal(); // ?? are persistence managers thread-safes ???

    //

    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        // prepare the db directory
        File envDir = new File(context.getHomeDir(), "db");
        if (!envDir.exists()) {
            envDir.mkdirs();
        }

        log.debug("init berkeleyDb environment at " + envDir.getAbsolutePath());

        // create environnement
        EnvironmentConfig config = new EnvironmentConfig();
        config.setAllowCreate(true);
        config.setTransactional(true);
        environment = new Environment(envDir, config);

        // open database
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(false);
        dbConfig.setTransactional(true);
        database = environment.openDatabase(null, "jcrStore", dbConfig);

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

    public void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        log.debug("close berkeleyDb environment");

        // close database
        database.close();

        // close environnement
        environment.close();

        // close BLOB file system
        blobFS.close();
        blobFS = null;
        blobStore = null;

        initialized = false;
    }

    //

    public NodeState load(NodeId id) throws NoSuchItemStateException, ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            NodeStateTupleBinding tupleBinding = new NodeStateTupleBinding(id);
            key.setData(id.toString().getBytes(ENCODING));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            if (operationStatus.equals(OperationStatus.NOTFOUND)) {
                throw new NoSuchItemStateException(id.toString());
            }
            return (NodeState) tupleBinding.entryToObject(value);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    public PropertyState load(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            PropertyStateTupleBinding tupleBinding = new PropertyStateTupleBinding(id, blobStore);
            key.setData(id.toString().getBytes(ENCODING));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            if (operationStatus.equals(OperationStatus.NOTFOUND)) {
                throw new NoSuchItemStateException(id.toString());
            }
            return (PropertyState) tupleBinding.entryToObject(value);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    public NodeReferences load(NodeReferencesId id) throws NoSuchItemStateException, ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            NodeReferencesTupleBinding tupleBinding = new NodeReferencesTupleBinding(id);
            key.setData((id.toString() + ".references").getBytes(ENCODING));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            if (operationStatus.equals(OperationStatus.NOTFOUND)) {
                throw new NoSuchItemStateException(id.toString());
            }
            return (NodeReferences) tupleBinding.entryToObject(value);
        } catch (NoSuchItemStateException e) {
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    public boolean exists(NodeId id) throws ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            key.setData(id.toString().getBytes(ENCODING));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            return operationStatus.equals(OperationStatus.SUCCESS);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    public boolean exists(PropertyId id) throws ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            key.setData(id.toString().getBytes(ENCODING));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            return operationStatus.equals(OperationStatus.SUCCESS);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    public boolean exists(NodeReferencesId targetId) throws ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            key.setData((targetId.toString() + ".references").getBytes(ENCODING));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            return operationStatus.equals(OperationStatus.SUCCESS);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    //

    public synchronized void store(ChangeLog changeLog) throws ItemStateException {
        Transaction transaction = null;
        try {
            transaction = environment.beginTransaction(null, null);
            localTransaction.set(transaction);
            super.store(changeLog);
            transaction.commit();
        } catch (Exception e) {
            try {
                if (transaction != null) {
                    transaction.abort();
                }
            } catch (Exception fe) {
                log.fatal(fe);
            }
            throw new ItemStateException(e.getMessage(), e);
        } finally {
            localTransaction.set(null);
        }
    }

    //

    protected void store(NodeState state) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            NodeStateTupleBinding tupleBinding = new NodeStateTupleBinding();
            key.setData(state.getId().toString().getBytes(ENCODING));
            tupleBinding.objectToEntry(state, value);
            OperationStatus operationStatus = database.put(transaction, key, value);
            if (!operationStatus.equals(OperationStatus.SUCCESS)) {
                throw new ItemStateException(operationStatus.toString());
            }
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    protected void store(PropertyState state) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            PropertyStateTupleBinding tupleBinding = new PropertyStateTupleBinding(blobStore);
            key.setData(state.getId().toString().getBytes(ENCODING));
            tupleBinding.objectToEntry(state, value);
            OperationStatus operationStatus = database.put(transaction, key, value);
            if (!operationStatus.equals(OperationStatus.SUCCESS)) {
                throw new ItemStateException(operationStatus.toString());
            }
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    protected void store(NodeReferences refs) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            NodeReferencesTupleBinding tupleBinding = new NodeReferencesTupleBinding();
            key.setData((refs.getTargetId().toString() + ".references").getBytes(ENCODING));
            tupleBinding.objectToEntry(refs, value);
            OperationStatus operationStatus = database.put(transaction, key, value);
            if (!operationStatus.equals(OperationStatus.SUCCESS)) {
                throw new ItemStateException(operationStatus.toString());
            }
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    protected void destroy(NodeState state) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            key.setData(state.getId().toString().getBytes(ENCODING));
            OperationStatus operationStatus = database.delete(transaction, key);
            if (!operationStatus.equals(OperationStatus.SUCCESS)) {
                throw new ItemStateException(operationStatus.toString());
            }
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    protected void destroy(PropertyState state) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            key.setData(state.getId().toString().getBytes(ENCODING));
            OperationStatus operationStatus = database.delete(transaction, key);
            if (!operationStatus.equals(OperationStatus.SUCCESS)) {
                throw new ItemStateException(operationStatus.toString());
            }

            InternalValue[] values = state.getValues();
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    InternalValue val = values[i];
                    if (val != null) {
                        if (val.getType() == PropertyType.BINARY) {
                            BLOBFileValue blobVal = (BLOBFileValue) val.internalValue();
                            // delete internal resource representation of BLOB value
                            blobVal.delete(true);
                            // also remove from BLOBStore
                            String blobId = blobStore.createId((PropertyId) state.getId(), i);
                            try {
                                blobStore.remove(blobId);
                            } catch (Exception e) {
                                log.warn("failed to remove from BLOBStore: " + blobId, e);
                            }
                        }
                    }
                }
            } 
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    protected void destroy(NodeReferences refs) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            key.setData((refs.getTargetId().toString() + ".references").getBytes(ENCODING));
            OperationStatus operationStatus = database.delete(transaction, key);
            if (!operationStatus.equals(OperationStatus.SUCCESS)) {
                throw new ItemStateException(operationStatus.toString());
            }
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e.getMessage(), e);
        }
    }
}
