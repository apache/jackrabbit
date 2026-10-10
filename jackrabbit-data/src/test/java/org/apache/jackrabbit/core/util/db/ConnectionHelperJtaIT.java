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
package org.apache.jackrabbit.core.util.db;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.util.db.ConnectionHelper.JtaContext;

/**
 * Verifies that {@link ConnectionHelper}'s batch-control methods skip the
 * direct JDBC tx-control calls when a managed JTA transaction is active,
 * call them as before when no managed transaction is in progress, and
 * surface a TSR-side rollback-only failure to the caller as a
 * {@link SQLException}.
 *
 * <p>The tests use a {@link CountingConnection} that records whether
 * {@code setAutoCommit}, {@code commit} and {@code rollback} were called,
 * and a hand-rolled {@link DataSource} that hands out a single connection
 * instance per test. The JTA decision is driven by injecting a
 * {@link JtaContext} via {@link ConnectionHelper}'s package-private
 * {@code setJtaContext()} hook, so no JNDI context is needed.
 */
public class ConnectionHelperJtaIT extends TestCase {

    public void testStartBatchSkipsSetAutoCommitUnderManagedTx() throws SQLException {
        CountingConnection con = new CountingConnection();
        ConnectionHelper helper = new ConnectionHelper(new SingletonDataSource(con), false);
        helper.setJtaContext(JtaContext.forTesting(new FakeTsr(0))); // STATUS_ACTIVE

        helper.startBatch();
        try {
            assertEquals("setAutoCommit must NOT be called inside a managed JTA tx",
                    0, con.setAutoCommitCalls.get());
        } finally {
            // endBatch with commit==true is a no-op on the JDBC side under
            // managed JTA -- it must not throw.
            helper.endBatch(true);
        }
        assertEquals("commit must NOT be called inside a managed JTA tx",
                0, con.commitCalls.get());
        assertEquals("rollback must NOT be called inside a managed JTA tx",
                0, con.rollbackCalls.get());
    }

    public void testStartBatchCallsSetAutoCommitOutsideManagedTx() throws SQLException {
        CountingConnection con = new CountingConnection();
        ConnectionHelper helper = new ConnectionHelper(new SingletonDataSource(con), false);
        helper.setJtaContext(JtaContext.forTesting(null)); // no TSR

        helper.startBatch();
        try {
            assertEquals(1, con.setAutoCommitCalls.get());
            assertFalse("setAutoCommit(false) expected", con.lastAutoCommitArg);
        } finally {
            helper.endBatch(true);
        }
        assertEquals("commit() expected when no managed tx and commit=true",
                1, con.commitCalls.get());
    }

    public void testEndBatchRollbackOutsideManagedTxCallsConnectionRollback() throws SQLException {
        CountingConnection con = new CountingConnection();
        ConnectionHelper helper = new ConnectionHelper(new SingletonDataSource(con), false);
        helper.setJtaContext(JtaContext.forTesting(null));

        helper.startBatch();
        helper.endBatch(false);

        assertEquals(1, con.rollbackCalls.get());
        assertEquals(0, con.commitCalls.get());
    }

    public void testEndBatchRollbackUnderManagedTxMarksTransactionRollbackOnly() throws SQLException {
        FakeTsr tsr = new FakeTsr(0); // STATUS_ACTIVE
        CountingConnection con = new CountingConnection();
        ConnectionHelper helper = new ConnectionHelper(new SingletonDataSource(con), false);
        helper.setJtaContext(JtaContext.forTesting(tsr));

        helper.startBatch();
        helper.endBatch(false);

        assertEquals("Connection.rollback must NOT be called under managed JTA",
                0, con.rollbackCalls.get());
        assertEquals("setRollbackOnly() must be called when caller requests rollback",
                1, tsr.rollbackOnlyCalls);
    }

    public void testEndBatchRollbackUnderManagedTxThrowsWhenSetRollbackOnlyFails() throws SQLException {
        FakeTsr tsr = new FakeTsr(0);
        tsr.failOnSetRollbackOnly = true;
        CountingConnection con = new CountingConnection();
        ConnectionHelper helper = new ConnectionHelper(new SingletonDataSource(con), false);
        helper.setJtaContext(JtaContext.forTesting(tsr));

        helper.startBatch();
        try {
            helper.endBatch(false);
            fail("endBatch(false) must throw SQLException when setRollbackOnly() fails");
        } catch (SQLException expected) {
            // expected -- caller's rollback intent must not be lost
        }
        assertEquals("Connection.rollback must NOT be called under managed JTA",
                0, con.rollbackCalls.get());
    }

    public void testGetConnectionSkipsSetAutoCommitUnderManagedTx() throws SQLException {
        CountingConnection con = new CountingConnection();
        con.autoCommitState = false; // simulate the JCA-managed pool's reported state
        ConnectionHelper helper = new ConnectionHelper(new SingletonDataSource(con), false);
        helper.setJtaContext(JtaContext.forTesting(new FakeTsr(0)));

        Connection out = helper.getConnection(false);

        assertSame(con, out);
        assertEquals("setAutoCommit must NOT be called inside a managed JTA tx",
                0, con.setAutoCommitCalls.get());
    }

    public void testGetConnectionRestoresAutoCommitOutsideManagedTx() throws SQLException {
        CountingConnection con = new CountingConnection();
        con.autoCommitState = false;
        ConnectionHelper helper = new ConnectionHelper(new SingletonDataSource(con), false);
        helper.setJtaContext(JtaContext.forTesting(null));

        helper.getConnection(false);

        assertEquals(1, con.setAutoCommitCalls.get());
        assertTrue("setAutoCommit(true) expected (JCR-1013 mitigation)",
                con.lastAutoCommitArg);
    }

    /**
     * Hand-rolled stand-in for the container TSR. The reflective lookup
     * in {@link JtaContext} does not require a real
     * {@code TransactionSynchronizationRegistry} -- it duck-types on
     * method names.
     */
    public static final class FakeTsr {

        private final int status;
        boolean failOnSetRollbackOnly;
        int rollbackOnlyCalls;

        FakeTsr(int status) {
            this.status = status;
        }

        public int getTransactionStatus() {
            return status;
        }

        public void setRollbackOnly() {
            if (failOnSetRollbackOnly) {
                throw new IllegalStateException("forced failure for test");
            }
            rollbackOnlyCalls++;
        }
    }

    /**
     * Minimal {@link DataSource} that hands out the same {@link Connection}
     * for every {@code getConnection()} call. Other methods either return
     * sensible defaults or throw {@link SQLFeatureNotSupportedException}.
     */
    private static final class SingletonDataSource implements DataSource {

        private final Connection connection;

        SingletonDataSource(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }

    /**
     * {@link Connection} stand-in that counts the three tx-control method
     * calls we care about. Other methods are routed to a JDK proxy that
     * returns sensible defaults / no-ops.
     */
    private static final class CountingConnection implements Connection {

        private final AtomicInteger setAutoCommitCalls = new AtomicInteger();
        private final AtomicInteger commitCalls = new AtomicInteger();
        private final AtomicInteger rollbackCalls = new AtomicInteger();
        private boolean autoCommitState = true;
        private boolean lastAutoCommitArg;
        private final Connection delegate;

        CountingConnection() {
            this.delegate = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    new NoopHandler());
        }

        @Override
        public void setAutoCommit(boolean autoCommit) {
            setAutoCommitCalls.incrementAndGet();
            lastAutoCommitArg = autoCommit;
            autoCommitState = autoCommit;
        }

        @Override
        public boolean getAutoCommit() {
            return autoCommitState;
        }

        @Override
        public void commit() {
            commitCalls.incrementAndGet();
        }

        @Override
        public void rollback() {
            rollbackCalls.incrementAndGet();
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        // Delegate every other method to a no-op proxy so this class
        // stays compatible with future Connection method additions.
        @Override
        public java.sql.Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override
        public java.sql.CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override
        public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override
        public java.sql.DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override
        public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override
        public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override
        public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override
        public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override
        public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override
        public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override
        public java.sql.SQLWarning getWarnings() throws SQLException { return null; }
        @Override
        public void clearWarnings() {}
        @Override
        public java.sql.Statement createStatement(int rsType, int rsConcurrency) throws SQLException { return delegate.createStatement(rsType, rsConcurrency); }
        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int rsType, int rsConcurrency) throws SQLException { return delegate.prepareStatement(sql, rsType, rsConcurrency); }
        @Override
        public java.sql.CallableStatement prepareCall(String sql, int rsType, int rsConcurrency) throws SQLException { return delegate.prepareCall(sql, rsType, rsConcurrency); }
        @Override
        public java.util.Map<String, Class<?>> getTypeMap() throws SQLException { return null; }
        @Override
        public void setTypeMap(java.util.Map<String, Class<?>> map) {}
        @Override
        public void setHoldability(int holdability) {}
        @Override
        public int getHoldability() { return 0; }
        @Override
        public java.sql.Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override
        public java.sql.Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override
        public void rollback(java.sql.Savepoint savepoint) throws SQLException { rollbackCalls.incrementAndGet(); }
        @Override
        public void releaseSavepoint(java.sql.Savepoint savepoint) {}
        @Override
        public java.sql.Statement createStatement(int a, int b, int c) throws SQLException { return delegate.createStatement(a, b, c); }
        @Override
        public java.sql.PreparedStatement prepareStatement(String s, int a, int b, int c) throws SQLException { return delegate.prepareStatement(s, a, b, c); }
        @Override
        public java.sql.CallableStatement prepareCall(String s, int a, int b, int c) throws SQLException { return delegate.prepareCall(s, a, b, c); }
        @Override
        public java.sql.PreparedStatement prepareStatement(String s, int a) throws SQLException { return delegate.prepareStatement(s, a); }
        @Override
        public java.sql.PreparedStatement prepareStatement(String s, int[] a) throws SQLException { return delegate.prepareStatement(s, a); }
        @Override
        public java.sql.PreparedStatement prepareStatement(String s, String[] a) throws SQLException { return delegate.prepareStatement(s, a); }
        @Override
        public java.sql.Clob createClob() { return null; }
        @Override
        public java.sql.Blob createBlob() { return null; }
        @Override
        public java.sql.NClob createNClob() { return null; }
        @Override
        public java.sql.SQLXML createSQLXML() { return null; }
        @Override
        public boolean isValid(int timeout) { return true; }
        @Override
        public void setClientInfo(String name, String value) {}
        @Override
        public void setClientInfo(java.util.Properties properties) {}
        @Override
        public String getClientInfo(String name) { return null; }
        @Override
        public java.util.Properties getClientInfo() { return new java.util.Properties(); }
        @Override
        public java.sql.Array createArrayOf(String typeName, Object[] elements) { return null; }
        @Override
        public java.sql.Struct createStruct(String typeName, Object[] attributes) { return null; }
        @Override
        public void setSchema(String schema) {}
        @Override
        public String getSchema() { return null; }
        @Override
        public void abort(java.util.concurrent.Executor executor) {}
        @Override
        public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) {}
        @Override
        public int getNetworkTimeout() { return 0; }
        @Override
        public <T> T unwrap(Class<T> iface) { return null; }
        @Override
        public boolean isWrapperFor(Class<?> iface) { return false; }

        private static final class NoopHandler implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                Class<?> rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class || rt == long.class || rt == short.class || rt == byte.class) return 0;
                if (rt == double.class || rt == float.class) return 0.0;
                if (rt == char.class) return '\0';
                return null;
            }
        }
    }
}
