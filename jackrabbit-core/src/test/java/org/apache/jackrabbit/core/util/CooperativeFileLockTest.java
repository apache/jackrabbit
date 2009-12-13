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
package org.apache.jackrabbit.core.util;

import java.io.File;
import java.io.IOException;

import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

/**
 * Tests the cooperative file lock mechanism.
 */
public class CooperativeFileLockTest extends TestCase {

    private static final String TEST_DIRECTORY = "target/tmp/testCooperativeFileLock";

    public void tearDown() throws IOException {
        setUp();
    }

    public void setUp() throws IOException {
        FileUtils.deleteQuietly(new File(TEST_DIRECTORY));
    }

    public void testFileLock() throws RepositoryException {
        int testRuns = 1;
        for (int i = 0; i < testRuns; i++) {
            new File(TEST_DIRECTORY).mkdirs();
            CooperativeFileLock l1 = new CooperativeFileLock();
            l1.init(TEST_DIRECTORY);
            l1.acquire();
            CooperativeFileLock l2 = new CooperativeFileLock();
            l2.init(TEST_DIRECTORY);
            try {
                l2.acquire();
                fail();
            } catch (Exception e) {
                // expected
            }
            l1.release();
            l2.acquire();
            l2.release();
        }
    }

}
