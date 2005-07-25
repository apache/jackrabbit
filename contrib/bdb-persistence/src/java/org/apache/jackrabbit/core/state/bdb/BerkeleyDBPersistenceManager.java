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
package org.apache.jackrabbit.core.state.bdb;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemPathUtil;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.state.AbstractPersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PMContext;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.name.QName;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class BerkeleyDBPersistenceManager extends AbstractPersistenceManager implements BLOBStore {

    private static Log log = LogFactory.getLog(BerkeleyDBPersistenceManager.class);

    protected static final String ENCODING = "UTF-8";

    private boolean initialized = false;
    private Environment environment;
    private Database database;
    private FileSystem blobFS;
    private ThreadLocal localTransaction = new ThreadLocal(); // ?? are persistence managers thread-safes ???

    //

    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        // prepare the db directory
        File envDir = new File(context.getHomeDir(), "db");
        if (!envDir.exists())
            envDir.mkdir();

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
         * store blob's in local file system in a sub directory
         * of the workspace home directory
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
        blobFS.init();
        this.blobFS = blobFS;

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

        // close blob store
        blobFS.close();
        blobFS = null;

        initialized = false;
    }

    //

    public NodeState load(NodeId id) throws NoSuchItemStateException, ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            NodeStateTupleBinding tupleBinding = new NodeStateTupleBinding(id);
            key.setData(id.toString().getBytes("UTF-8"));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            if (operationStatus.equals(OperationStatus.NOTFOUND))
                throw new NoSuchItemStateException();
            return (NodeState) tupleBinding.entryToObject(value);
        } catch (NoSuchItemStateException e) {
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    public PropertyState load(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            PropertyStateTupleBinding tupleBinding = new PropertyStateTupleBinding(id, this);
            key.setData(id.toString().getBytes("UTF-8"));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            if (operationStatus.equals(OperationStatus.NOTFOUND))
                throw new NoSuchItemStateException();
            return (PropertyState) tupleBinding.entryToObject(value);
        } catch (NoSuchItemStateException e) {
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    public NodeReferences load(NodeReferencesId id) throws NoSuchItemStateException, ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            NodeReferencesTupleBinding tupleBinding = new NodeReferencesTupleBinding(id);
            key.setData((id.toString() + ".references").getBytes("UTF-8"));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            if (operationStatus.equals(OperationStatus.NOTFOUND))
                throw new NoSuchItemStateException();
            return (NodeReferences) tupleBinding.entryToObject(value);
        } catch (NoSuchItemStateException e) {
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    public boolean exists(NodeId id) throws ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            key.setData(id.toString().getBytes("UTF-8"));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            return operationStatus.equals(OperationStatus.SUCCESS);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    public boolean exists(PropertyId id) throws ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            key.setData(id.toString().getBytes("UTF-8"));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            return operationStatus.equals(OperationStatus.SUCCESS);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    public boolean exists(NodeReferencesId targetId) throws ItemStateException {
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            key.setData((targetId.toString() + ".references").getBytes("UTF-8"));
            OperationStatus operationStatus = database.get(null, key, value, LockMode.DEFAULT);
            return operationStatus.equals(OperationStatus.SUCCESS);
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
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
                transaction.abort();
            } catch (Exception fe) {
                log.fatal(fe);
            }
            throw new ItemStateException(e);
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
            key.setData(state.getId().toString().getBytes("UTF-8"));
            tupleBinding.objectToEntry(state, value);
            OperationStatus operationStatus = database.put(transaction, key, value);
            if (!operationStatus.equals(OperationStatus.SUCCESS))
                throw new ItemStateException(operationStatus.toString());
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    protected void store(PropertyState state) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            PropertyStateTupleBinding tupleBinding = new PropertyStateTupleBinding(this);
            key.setData(state.getId().toString().getBytes("UTF-8"));
            tupleBinding.objectToEntry(state, value);
            OperationStatus operationStatus = database.put(transaction, key, value);
            if (!operationStatus.equals(OperationStatus.SUCCESS))
                throw new ItemStateException(operationStatus.toString());
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    protected void store(NodeReferences refs) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            NodeReferencesTupleBinding tupleBinding = new NodeReferencesTupleBinding();
            key.setData((refs.getTargetId().toString() + ".references").getBytes("UTF-8"));
            tupleBinding.objectToEntry(refs, value);
            OperationStatus operationStatus = database.put(transaction, key, value);
            if (!operationStatus.equals(OperationStatus.SUCCESS))
                throw new ItemStateException(operationStatus.toString());
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    protected void destroy(NodeState state) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            key.setData(state.getId().toString().getBytes("UTF-8"));
            OperationStatus operationStatus = database.delete(transaction, key);
            if (!operationStatus.equals(OperationStatus.SUCCESS))
                throw new ItemStateException(operationStatus.toString());
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    protected void destroy(PropertyState state) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            key.setData(state.getId().toString().getBytes("UTF-8"));
            OperationStatus operationStatus = database.delete(transaction, key);
            if (!operationStatus.equals(OperationStatus.SUCCESS))
                throw new ItemStateException(operationStatus.toString());
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    protected void destroy(NodeReferences refs) throws ItemStateException {
        try {
            Transaction transaction = (Transaction) localTransaction.get();
            DatabaseEntry key = new DatabaseEntry();
            key.setData((refs.getTargetId().toString() + ".references").getBytes("UTF-8"));
            OperationStatus operationStatus = database.delete(transaction, key);
            if (!operationStatus.equals(OperationStatus.SUCCESS))
                throw new ItemStateException(operationStatus.toString());
        } catch (Exception e) {
            log.error(e);
            throw new ItemStateException(e);
        }
    }

    // blobs

    public FileSystemResource get(String blobId) throws Exception {
        return new FileSystemResource(blobFS, blobId);
    }

    public String put(PropertyId id, int index, InputStream in, long size) throws Exception {
        String path = buildBlobFilePath(id.getParentUUID(), id.getName(), index);
        OutputStream out = null;
        FileSystemResource internalBlobFile = new FileSystemResource(blobFS, path);
        internalBlobFile.makeParentDirs();
        try {
            out = new BufferedOutputStream(internalBlobFile.getOutputStream());
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            out.close();
        }
        return path;
    }

    public boolean remove(String blobId) throws Exception {
        FileSystemResource res = new FileSystemResource(blobFS, blobId);
        if (!res.exists()) {
            return false;
        }
        // delete resource and prune empty parent folders
        res.delete(true);
        return true;
    }

    private static String buildBlobFilePath(String parentUUID, QName propName, int it) {
        StringBuffer sb = new StringBuffer();
        char[] chars = parentUUID.toCharArray();
        int cnt = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '-') {
                continue;
            }
            if (cnt == 2 || cnt == 4) {
                sb.append(FileSystem.SEPARATOR_CHAR);
            }
            sb.append(chars[i]);
            cnt++;
        }
        return sb.toString() + FileSystem.SEPARATOR + FileSystemPathUtil.escapeName(propName.toString()) + "." + it + ".bin";
    }

}
