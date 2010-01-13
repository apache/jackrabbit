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
package org.apache.jackrabbit.core.persistence.bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <code>OracleLegacyPersistenceManager</code> provides support for Oracle jdbc
 * drivers prior to version 10 which require special handling of BLOB data.
 * <p/>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/>
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/>
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/>
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="oracle.jdbc.OracleDriverr"/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value="jdbc:oracle:thin:@127.0.0.1:1521:xe"/>
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/>
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="oracle"/>
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value="${wsp.name}_"/>
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/>
 * </ul>
 */
public class Oracle9PersistenceManager extends OraclePersistenceManager {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(Oracle9PersistenceManager.class);

    private Class< ? > blobClass;
    private Integer duractionSessionConstant;
    private Integer modeReadWriteConstant;

    public Oracle9PersistenceManager() {
    }

    //-----------------------------------< OraclePersistenceManager overrides >
    /**
     * {@inheritDoc}
     * <p/>
     * Retrieve the <code>oracle.sql.BLOB</code> class via reflection, and
     * initialize the values for the <code>DURATION_SESSION</code> and
     * <code>MODE_READWRITE</code> constants defined there.
     *
     * @see oracle.sql.BLOB#DURATION_SESSION
     * @see oracle.sql.BLOB#MODE_READWRITE
     */
    public void init(PMContext context) throws Exception {
        super.init(context);

        // initialize oracle.sql.BLOB class & constants

        // use the Connection object for using the exact same
        // class loader that the Oracle driver was loaded with
        blobClass = connectionManager.getConnection().getClass().getClassLoader().loadClass("oracle.sql.BLOB");
        duractionSessionConstant =
                new Integer(blobClass.getField("DURATION_SESSION").getInt(null));
        modeReadWriteConstant =
                new Integer(blobClass.getField("MODE_READWRITE").getInt(null));
    }

    /**
     * @inheritDoc
     */
    protected BundleDbPersistenceManager.CloseableBLOBStore createDBBlobStore(PMContext context) throws Exception {
        return new OracleBLOBStore();
    }

    /**
     * @inheritDoc
     */
    protected synchronized void storeBundle(NodePropBundle bundle)
            throws ItemStateException {
        Blob blob = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            DataOutputStream dout = new DataOutputStream(out);
            binding.writeBundle(dout, bundle);
            dout.close();

            String sql = bundle.isNew() ? bundleInsertSQL : bundleUpdateSQL;
            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            Object[] params = createParams(bundle.getId(), blob, true);
            connectionManager.executeStmt(sql, params);
        } catch (Exception e) {
            String msg = "failed to write bundle: " + bundle.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            if (blob != null) {
                try {
                    freeTemporaryBlob(blob);
                } catch (Exception e1) {
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public synchronized void store(NodeReferences refs)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        Blob blob = null;
        try {
            // check if insert or update
            boolean update = existsReferencesTo(refs.getTargetId());
            String sql = (update) ? nodeReferenceUpdateSQL : nodeReferenceInsertSQL;

            ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize references
            Serializer.serialize(refs, out);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the preparedStatement

            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            Object[] params = createParams(refs.getTargetId(), blob, true);
            connectionManager.executeStmt(sql, params);

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write " + refs;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            if (blob != null) {
                try {
                    freeTemporaryBlob(blob);
                } catch (Exception e1) {
                }
            }
        }
    }

    //----------------------------------------< oracle-specific blob handling >
    /**
     * Creates a temporary oracle.sql.BLOB instance via reflection and spools
     * the contents of the specified stream.
     */
    protected Blob createTemporaryBlob(InputStream in) throws Exception {
        /*
        BLOB blob = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
        blob.open(BLOB.MODE_READWRITE);
        OutputStream out = blob.getBinaryOutputStream();
        ...
        out.flush();
        out.close();
        blob.close();
        return blob;
        */
        Method createTemporary = blobClass.getMethod("createTemporary",
                new Class[]{Connection.class, Boolean.TYPE, Integer.TYPE});
        Object blob = createTemporary.invoke(null,
                new Object[]{connectionManager.getConnection(), Boolean.FALSE, duractionSessionConstant});
        Method open = blobClass.getMethod("open", new Class[]{Integer.TYPE});
        open.invoke(blob, new Object[]{modeReadWriteConstant});
        Method getBinaryOutputStream = blobClass.getMethod("getBinaryOutputStream", new Class[0]);
        OutputStream out = (OutputStream) getBinaryOutputStream.invoke(blob);
        try {
            IOUtils.copy(in, out);
        } finally {
            try {
                out.flush();
            } catch (IOException ioe) {
            }
            out.close();
        }
        Method close = blobClass.getMethod("close", new Class[0]);
        close.invoke(blob);
        return (Blob) blob;
    }

    /**
     * Frees a temporary oracle.sql.BLOB instance via reflection.
     */
    protected void freeTemporaryBlob(Object blob) throws Exception {
        // blob.freeTemporary();
        Method freeTemporary = blobClass.getMethod("freeTemporary", new Class[0]);
        freeTemporary.invoke(blob);
    }

    /**
     * A blob store specially for Oracle 9.
     */
    class OracleBLOBStore extends DbBlobStore {

        public OracleBLOBStore() throws SQLException {
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void put(String blobId, InputStream in, long size)
                throws Exception {

            Blob blob = null;
            try {
                Statement stmt = connectionManager.executeStmt(blobSelectExistSQL, new Object[]{blobId});
                ResultSet rs = stmt.getResultSet();
                // a BLOB exists if the result has at least one entry
                boolean exists = rs.next();
                closeResultSet(rs);

                String sql = (exists) ? blobUpdateSQL : blobInsertSQL;
                blob = createTemporaryBlob(in);
                connectionManager.executeStmt(sql, new Object[]{blob, blobId});
            } finally {
                if (blob != null) {
                    try {
                        freeTemporaryBlob(blob);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}
