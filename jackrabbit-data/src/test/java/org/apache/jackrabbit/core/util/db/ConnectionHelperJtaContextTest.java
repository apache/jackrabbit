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

import junit.framework.TestCase;

import org.apache.jackrabbit.core.util.db.ConnectionHelper.JtaContext;

/**
 * Verifies the decision logic in {@link ConnectionHelper.JtaContext}:
 * which JTA status values cause direct JDBC tx-control to be skipped,
 * and how reflection / invocation failures fall back. The tests use a
 * {@link FakeTransactionSynchronizationRegistry} as a stand-in for the
 * container-supplied TSR so no JNDI context is needed.
 *
 * <p>JTA-Status values follow the Status interface contract:
 * <ul>
 *   <li>{@code STATUS_ACTIVE = 0}</li>
 *   <li>{@code STATUS_MARKED_ROLLBACK = 1}</li>
 *   <li>{@code STATUS_PREPARED = 2}</li>
 *   <li>{@code STATUS_COMMITTED = 3}</li>
 *   <li>{@code STATUS_ROLLEDBACK = 4}</li>
 *   <li>{@code STATUS_UNKNOWN = 5}</li>
 *   <li>{@code STATUS_NO_TRANSACTION = 6}</li>
 * </ul>
 */
public class ConnectionHelperJtaContextTest extends TestCase {

    public void testNoRegistryMeansNoManagedTransaction() {
        JtaContext ctx = JtaContext.forTesting(null);
        assertFalse(ctx.hasManagedTransaction());
        // markRollbackOnly() must be safe to call without a registry.
        ctx.markRollbackOnly();
    }

    public void testStatusNoTransactionMeansNoManagedTransaction() {
        FakeTransactionSynchronizationRegistry tsr = new FakeTransactionSynchronizationRegistry(6);
        JtaContext ctx = JtaContext.forTesting(tsr);
        assertFalse(ctx.hasManagedTransaction());
    }

    public void testStatusActiveMeansManagedTransaction() {
        FakeTransactionSynchronizationRegistry tsr = new FakeTransactionSynchronizationRegistry(0);
        JtaContext ctx = JtaContext.forTesting(tsr);
        assertTrue(ctx.hasManagedTransaction());
    }

    public void testStatusMarkedRollbackMeansManagedTransaction() {
        FakeTransactionSynchronizationRegistry tsr = new FakeTransactionSynchronizationRegistry(1);
        JtaContext ctx = JtaContext.forTesting(tsr);
        assertTrue(ctx.hasManagedTransaction());
    }

    public void testStatusUnknownMeansManagedTransaction() {
        // STATUS_UNKNOWN means a transaction is associated but its status
        // cannot be determined; treat as managed (fail closed).
        FakeTransactionSynchronizationRegistry tsr = new FakeTransactionSynchronizationRegistry(5);
        JtaContext ctx = JtaContext.forTesting(tsr);
        assertTrue(ctx.hasManagedTransaction());
    }

    public void testStatusInvocationFailureMeansManagedTransaction() {
        // Reflective invocation failures on a present TSR must be treated
        // as managed (fail closed), so callers do not run direct JDBC
        // tx-control on a possibly enrolled connection.
        FakeTransactionSynchronizationRegistry tsr = new FakeTransactionSynchronizationRegistry(0);
        tsr.failOnGetStatus = true;
        JtaContext ctx = JtaContext.forTesting(tsr);
        assertTrue(ctx.hasManagedTransaction());
    }

    public void testMarkRollbackOnlyDelegatesToRegistry() {
        FakeTransactionSynchronizationRegistry tsr = new FakeTransactionSynchronizationRegistry(0);
        JtaContext ctx = JtaContext.forTesting(tsr);
        assertEquals(0, tsr.rollbackOnlyCalls);
        assertTrue(ctx.markRollbackOnly());
        assertEquals(1, tsr.rollbackOnlyCalls);
    }

    public void testMarkRollbackOnlyReportsInvocationFailure() {
        // setRollbackOnly() failures must be visible to the caller so the
        // batch-rollback path can surface them as SQLException; otherwise
        // a requested rollback could vanish while the global transaction
        // still commits.
        FakeTransactionSynchronizationRegistry tsr = new FakeTransactionSynchronizationRegistry(0);
        tsr.failOnSetRollbackOnly = true;
        JtaContext ctx = JtaContext.forTesting(tsr);
        assertFalse(ctx.markRollbackOnly());
    }

    public void testMarkRollbackOnlyReportsAbsenceOfRegistry() {
        JtaContext ctx = JtaContext.forTesting(null);
        assertFalse(ctx.markRollbackOnly());
    }

    public void testMissingGetTransactionStatusMethodMeansManagedTransaction() {
        // TSR present but does not expose getTransactionStatus(); the
        // resolveMethod() reflective lookup returns null and the helper
        // must fall back to fail-closed (treat thread as transaction-
        // associated) so callers do not run direct JDBC tx-control on
        // a possibly enrolled connection.
        JtaContext ctx = JtaContext.forTesting(new TsrWithoutGetTransactionStatus());
        assertTrue(ctx.hasManagedTransaction());
    }

    public void testMarkRollbackOnlyReportsMissingMethod() {
        // TSR present but does not expose setRollbackOnly(); the helper
        // must report failure to the caller (so endBatch(false) can
        // surface it as SQLException) rather than silently no-op.
        JtaContext ctx = JtaContext.forTesting(new TsrWithoutSetRollbackOnly());
        assertFalse(ctx.markRollbackOnly());
    }

    /** TSR stand-in lacking getTransactionStatus(). */
    public static final class TsrWithoutGetTransactionStatus {
        public void setRollbackOnly() {
            // no-op
        }
    }

    /** TSR stand-in lacking setRollbackOnly(). */
    public static final class TsrWithoutSetRollbackOnly {
        public int getTransactionStatus() {
            return 0; // STATUS_ACTIVE
        }
    }

    /**
     * Test stand-in that exposes the two TSR-public methods exercised by
     * {@link JtaContext}. The duck-typed reflective lookup in
     * {@code JtaContext} resolves these by name on the concrete class
     * because the real TSR interface types may not be on the test
     * classpath.
     */
    public static final class FakeTransactionSynchronizationRegistry {

        private final int status;
        boolean failOnGetStatus;
        boolean failOnSetRollbackOnly;
        int rollbackOnlyCalls;

        FakeTransactionSynchronizationRegistry(int status) {
            this.status = status;
        }

        public int getTransactionStatus() {
            if (failOnGetStatus) {
                throw new IllegalStateException("forced failure for test");
            }
            return status;
        }

        public void setRollbackOnly() {
            if (failOnSetRollbackOnly) {
                throw new IllegalStateException("forced failure for test");
            }
            rollbackOnlyCalls++;
        }
    }
}
