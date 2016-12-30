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
package org.apache.jackrabbit.core.persistence.db;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

/**
 * <code>OraclePersistenceManager</code> is a JDBC-based
 * <code>PersistenceManager</code> for Jackrabbit that persists
 * <code>ItemState</code> and <code>NodeReferences</code> objects in Oracle
 * database using a simple custom serialization format and a
 * very basic non-normalized database schema (in essence tables with one 'key'
 * and one 'data' column).
 * <p>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the FQN name of the JDBC driver class
 * (default: <code>"oracle.jdbc.OracleDriver"</code>)</li>
 * <li><code>schema</code>: type of schema to be used
 * (default: <code>"oracle"</code>)</li>
 * <li><code>url</code>: the database url (e.g.
 * <code>"jdbc:oracle:thin:@[host]:[port]:[sid]"</code>)</li>
 * <li><code>user</code>: the database user</li>
 * <li><code>password</code>: the user's password</li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * <li><code>tableSpace</code>: the tablespace to use</li>
 * <li><code>externalBLOBs</code>: if <code>true</code> (the default) BINARY
 * values (BLOBs) are stored in the local file system;
 * if <code>false</code> BLOBs are stored in the database</li>
 * </ul>
 * See also {@link SimpleDbPersistenceManager}.
 * <p>
 * The following is a fragment from a sample configuration:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.OraclePersistenceManager"&gt;
 *       &lt;param name="url" value="jdbc:oracle:thin:@127.0.0.1:1521:orcl"/&gt;
 *       &lt;param name="user" value="scott"/&gt;
 *       &lt;param name="password" value="tiger"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="tableSpace" value=""/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *  &lt;/PersistenceManager&gt;
 * </pre>
 *
 * @deprecated Please migrate to a bundle persistence manager
 *   (<a href="https://issues.apache.org/jira/browse/JCR-2802">JCR-2802</a>)
 */
@Deprecated
public class OraclePersistenceManager extends SimpleDbPersistenceManager {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(OraclePersistenceManager.class);

    private Class<?> blobClass;
    private Integer durationSessionConstant;
    private Integer modeReadWriteConstant;

    /** the variable for the Oracle table space */
    public static final String TABLE_SPACE_VARIABLE =
        "${tableSpace}";

    /** the Oracle table space to use */
    protected String tableSpace;

    /**
     * Creates a new <code>OraclePersistenceManager</code> instance.
     */
    public OraclePersistenceManager() {
        // preset some attributes to reasonable defaults
        schema = "oracle";
        driver = "oracle.jdbc.OracleDriver";
        schemaObjectPrefix = "";
        user = "";
        password = "";
        initialized = false;
    }

    /**
     * Returns the configured Oracle table space.
     * @return the configured Oracle table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the Oracle table space.
     * @param tableSpace the Oracle table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null) {
            this.tableSpace = tableSpace.trim();
        } else {
            this.tableSpace = null;
        }
    }

    //---------------------------------< SimpleDbPersistenceManager overrides >
    /**
     * {@inheritDoc}
     * <p>
     * Retrieve the <code>oracle.sql.BLOB</code> class via reflection, and
     * initialize the values for the <code>DURATION_SESSION</code> and
     * <code>MODE_READWRITE</code> constants defined there.
     */
    public void init(PMContext context) throws Exception {
        super.init(context);

        if (!externalBLOBs) {
            blobStore = new OracleBLOBStore();
        }

        // initialize oracle.sql.BLOB class & constants

        // use the Connection object for using the exact same
        // class loader that the Oracle driver was loaded with
        blobClass = con.getClass().getClassLoader().loadClass("oracle.sql.BLOB");
        durationSessionConstant =
                new Integer(blobClass.getField("DURATION_SESSION").getInt(null));
        modeReadWriteConstant =
                new Integer(blobClass.getField("MODE_READWRITE").getInt(null));
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void store(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        //boolean update = exists((NodeId) state.getId());
        String sql = (update) ? nodeStateUpdateSQL : nodeStateInsertSQL;

        Blob blob = null;
        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize node state
            Serializer.serialize(state, out);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            executeStmt(sql, new Object[]{blob, state.getNodeId().toString()});

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write node state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            if (blob != null) {
                try {
                    freeTemporaryBlob(blob);
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void store(PropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        //boolean update = exists((PropertyId) state.getId());
        String sql = (update) ? propertyStateUpdateSQL : propertyStateInsertSQL;

        Blob blob = null;
        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize property state
            Serializer.serialize(state, out, blobStore);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            executeStmt(sql, new Object[]{blob, state.getPropertyId().toString()});

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write property state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            if (blob != null) {
                try {
                    freeTemporaryBlob(blob);
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void store(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = existsReferencesTo(refs.getTargetId());
        String sql = (update) ? nodeReferenceUpdateSQL : nodeReferenceInsertSQL;

        Blob blob = null;
        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize references
            Serializer.serialize(refs, out);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            blob = createTemporaryBlob(new ByteArrayInputStream(out.toByteArray()));
            executeStmt(sql, new Object[]{blob, refs.getTargetId().toString()});

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
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to support multiple oracle schemas. Note that
     * schema names in Oracle correspond to the username of the connection.
     * See http://issues.apache.org/jira/browse/JCR-582
     *
     * @throws Exception if an error occurs
     */
    protected void checkSchema() throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        String tableName = schemaObjectPrefix + "NODE";
        if (metaData.storesLowerCaseIdentifiers()) {
            tableName = tableName.toLowerCase();
        } else if (metaData.storesUpperCaseIdentifiers()) {
            tableName = tableName.toUpperCase();
        }
        String userName = metaData.getUserName();

        ResultSet rs = metaData.getTables(null, userName, tableName, null);
        boolean schemaExists;
        try {
            schemaExists = rs.next();
        } finally {
            rs.close();
        }

        if (!schemaExists) {
            // read ddl from resources
            InputStream in = getSchemaDDL();
            if (in == null) {
                String msg = "Configuration error: unknown schema '" + schema + "'";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Statement stmt = con.createStatement();
            try {
                String sql = reader.readLine();
                while (sql != null) {
                    // Skip comments and empty lines
                    if (!sql.startsWith("#") && sql.length() > 0) {
                        // replace prefix variable
                        sql = Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);

                        // set the tablespace if it is defined
                        String tspace;
                        if (tableSpace == null || "".equals(tableSpace)) {
                            tspace = "";
                        } else {
                            tspace = "tablespace " + tableSpace;
                        }
                        sql = Text.replace(sql, TABLE_SPACE_VARIABLE, tspace).trim();

                        // execute sql stmt
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
                // commit the changes
                con.commit();
            } finally {
                IOUtils.closeQuietly(in);
                closeStatement(stmt);
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
        Object blob = createTemporary.invoke(null, new Object[]{
                ConnectionFactory.unwrap(con),
                Boolean.FALSE, durationSessionConstant });
        Method open = blobClass.getMethod("open", new Class[]{Integer.TYPE});
        open.invoke(blob, new Object[]{modeReadWriteConstant});
        Method getBinaryOutputStream =
                blobClass.getMethod("getBinaryOutputStream", new Class[0]);
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

    //--------------------------------------------------------< inner classes >
    class OracleBLOBStore extends DbBLOBStore {
        /**
         * {@inheritDoc}
         */
        public synchronized void put(String blobId, InputStream in, long size)
                throws Exception {
            Statement stmt = executeStmt(blobSelectExistSQL, new Object[]{blobId});
            ResultSet rs = stmt.getResultSet();
            // a BLOB exists if the result has at least one entry
            boolean exists = rs.next();
            closeResultSet(rs);

            Blob blob = null;
            try {
                String sql = (exists) ? blobUpdateSQL : blobInsertSQL;
                blob = createTemporaryBlob(in);
                executeStmt(sql, new Object[]{blob, blobId});
            } finally {
                if (blob != null) {
                    try {
                        freeTemporaryBlob(blob);
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }
}
