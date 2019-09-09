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

import java.io.File;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.AbstractConcurrencyTest;

/**
 * <code>LockableFileRevisionTest</code> checks is concurrently locking and
 * unlocking a LockableFileRevision works.
 */
public class LockableFileRevisionTest extends AbstractConcurrencyTest {

    private LockableFileRevision rev;

    private File tmp;

    protected void setUp() throws Exception {
        super.setUp();
        tmp = File.createTempFile("test", "lock");
        rev = new LockableFileRevision(tmp);
    }

    protected void tearDown() throws Exception {
        tmp.delete();
        super.tearDown();
    }

    public void testConcurrentLocking() throws Exception {
        runTask(new Task() {
            public void execute(Session session, Node test) throws
                    RepositoryException {
                for (int i = 0; i < 1000; i++) {
                    try {
                        rev.lock(true);
                        rev.unlock();
                    } catch (Exception e) {
                        throw new RepositoryException(e);
                    }
                }
            }
        }, 50);
    }
}
