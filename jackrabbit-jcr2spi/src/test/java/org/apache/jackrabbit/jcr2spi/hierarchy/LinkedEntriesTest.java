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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;

public class LinkedEntriesTest extends TestCase {

    public void testConccurentModification() throws Throwable {
        try {
            internalTestConcurrentModification();
            fail("ConcurrentModificationException expected");
        } catch (ConcurrentModificationException expected) {
        }
    }

    private void internalTestConcurrentModification() throws Throwable {
        ExecutorService executorService = null;
        try {
            executorService = Executors.newFixedThreadPool(100);

            LinkedEntries entries = new LinkedEntries(null, null);
            Collection<Callable<Object>> futures = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                NodeEntry nodeEntry = mock(NodeEntry.class);
                futures.add(() -> entries.getLinkNode(nodeEntry));
                futures.add(() -> {
                    entries.add(nodeEntry);
                    return null;
                });
            }
            for (Future<?> future : executorService.invokeAll(futures)) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw e.getCause();
                }
            }
        } finally {
            executorService.shutdown();
        }
    }
}
