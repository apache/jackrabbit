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
package org.apache.jackrabbit.core.integration;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.core.query.lucene.SlowQueryHandler;

/**
 * <code>WorkspaceInitTest</code>...
 */
public class WorkspaceInitTest extends AbstractJCRTest {

    protected void setUp() throws Exception {
        super.setUp();
        SlowQueryHandler.setInitializationDelay(10 * 1000);
    }

    protected void tearDown() throws Exception {
        SlowQueryHandler.setInitializationDelay(0);
        super.tearDown();
    }

    public void testIdleTime() throws Exception {
        // simply access the workspace, which will cause
        // initialization of SlowQueryHandler.
        List threads = new ArrayList();
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        getHelper().getSuperuserSession("wsp-init-test").logout();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            t.start();
            threads.add(t);
        }
        for (Iterator it = threads.iterator(); it.hasNext(); ) {
            ((Thread) it.next()).join();
        }
    }
}
