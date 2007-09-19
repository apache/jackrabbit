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
package org.apache.jackrabbit.core.persistence.bundle.util;

import java.util.HashMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implements a {@link StringIndex} that stores and retrieves the names from a
 * table in a database.
 * <p/>
 * Note that this class is not threadsafe by itself. it needs to be synchronized
 * by the using application.
 * <p/>
 * Due to a bug with oracle that treats empty strings a null values
 * (see JCR-815), all empty strings are replaced by a ' '. since names never
 * start with a space, this it not problematic yet.
 */
public class DbNameIndex implements StringIndex {

    /**
     * The CVS/SVN id
     */
    static final String CVS_ID = "$URL$ $Rev$ $Date$";

    /**
     * The class that manages statement execution and recovery from connection loss.
     */
    protected ConnectionRecoveryManager connectionManager;

    // name index statements
    protected String nameSelectSQL;
    protected String indexSelectSQL;
    protected String nameInsertSQL;
    
    // caches
    private final HashMap string2Index = new HashMap();
    private final HashMap index2String = new HashMap();

    /**
     * Creates a new index that is stored in a db.
     * @param con the jdbc connection
     * @param schemaObjectPrefix the prefix for table names
     * @throws SQLException if the statements cannot be prepared.
     */
    public DbNameIndex(ConnectionRecoveryManager conMgr, String schemaObjectPrefix)
            throws SQLException {
        connectionManager = conMgr;
        init(schemaObjectPrefix);
    }

    /**
     * Inits this index and prepares the statements.
     *
     * @param con the jdbc connection
     * @param schemaObjectPrefix the prefix for table names
     * @throws SQLException if the statements cannot be prepared.
     */
    protected void init(String schemaObjectPrefix)
            throws SQLException {
        nameSelectSQL = "select NAME from " + schemaObjectPrefix + "NAMES where ID = ?";
        indexSelectSQL = "select ID from " + schemaObjectPrefix + "NAMES where NAME = ?";
        nameInsertSQL = "insert into " + schemaObjectPrefix + "NAMES (NAME) values (?)";
    }

    /**
     * Closes this index and releases it's resources.
     */
    public void close() {
        // closing the database resources is done by the owning
        // BundleDbPersistenceManager that created this index
    }

    /**
     * {@inheritDoc}
     */
    public int stringToIndex(String string) {
        // check cache
        Integer index = (Integer) string2Index.get(string);
        if (index == null) {
            String dbString = string.length() == 0 ? " " : string;
            int idx = getIndex(dbString);
            if (idx == -1) {
                idx = insertString(dbString);
            }
            index = new Integer(idx);
            string2Index.put(string, index);
            index2String.put(index, string);
            return idx;
        } else {
            return index.intValue();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String indexToString(int idx) {
        // check cache
        Integer index = new Integer(idx);
        String s = (String) index2String.get(index);
        if (s == null) {
            s = getString(idx);
            if (s == null) {
                throw new IllegalStateException("String empty???");
            }
            if (s.equals(" ")) {
                s = "";
            }
            index2String.put(index, s);
            string2Index.put(s, index);
        }
        return s;
    }

    /**
     * Inserts a string into the database and returns the new index.
     *
     * @param string the string to insert
     * @return the new index.
     */
    protected int insertString(String string) {
        // assert index does not exist
        ResultSet rs = null;
        try {
            Statement stmt = connectionManager.executeStmt(nameInsertSQL, new Object[]{string}, true, 0);
            rs = stmt.getGeneratedKeys();
            if (!rs.next()) {
                return -1;
            } else {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to insert index: " + e);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Retrieves the index from the database for the given string.
     * @param string the string to retrieve the index for
     * @return the index or -1 if not found.
     */
    protected int getIndex(String string) {
        ResultSet rs = null;
        try {
            Statement stmt = connectionManager.executeStmt(indexSelectSQL, new Object[]{string});
            rs = stmt.getResultSet();
            if (!rs.next()) {
                return -1;
            } else {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read index: " + e);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Retrieves the string from the database for the given index.
     * @param index the index to retrieve the string for.
     * @return the string or <code>null</code> if not found.
     */
    protected String getString(int index) {
        ResultSet rs = null;
        try {
            Statement stmt = connectionManager.executeStmt(nameSelectSQL, new Object[]{new Integer(index)});
            rs = stmt.getResultSet();
            if (!rs.next()) {
                return null;
            } else {
                return rs.getString(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read name: " + e);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * Closes the result set
     * @param rs the result set.
     */
    protected void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException se) {
                // ignore
            }
        }
    }
}
