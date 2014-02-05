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
package org.apache.jackrabbit.aws.ext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.LocalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testcase to test local cache.
 */
public class TestLocalCache extends TestCase {

    private static final String CACHE_DIR = "target/cache";

    private static final String TEMP_DIR = "target/temp";

    private static final Logger LOG = LoggerFactory.getLogger(TestLocalCache.class);

    @Override
    protected void setUp() {
        try {
            File cachedir = new File(CACHE_DIR);
            if (cachedir.exists()) {
                FileUtils.deleteQuietly(cachedir);
            }
            cachedir.mkdirs();

            File tempdir = new File(TEMP_DIR);
            if (tempdir.exists()) {
                FileUtils.deleteQuietly(tempdir);
            }
            tempdir.mkdirs();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail();
        }
    }

    @Override
    protected void tearDown() throws IOException {
        File cachedir = new File(CACHE_DIR);
        if (cachedir.exists()) {
            FileUtils.deleteQuietly(cachedir);
        }

        File tempdir = new File(TEMP_DIR);
        if (tempdir.exists()) {
            FileUtils.deleteQuietly(tempdir);
        }
    }

    /**
     * Test to validate store retrieve in cache.
     */
    public void testStoreRetrieve() {
        try {
            LocalCache cache = new LocalCache(CACHE_DIR, TEMP_DIR, 400, 0.95,
                0.70);
            Random random = new Random(12345);
            byte[] data = new byte[100];
            Map<String, byte[]> byteMap = new HashMap<String, byte[]>();
            random.nextBytes(data);
            byteMap.put("a1", data);

            data = new byte[100];
            random.nextBytes(data);
            byteMap.put("a2", data);

            data = new byte[100];
            random.nextBytes(data);
            byteMap.put("a3", data);

            cache.store("a1", new ByteArrayInputStream(byteMap.get("a1")));
            cache.store("a2", new ByteArrayInputStream(byteMap.get("a2")));
            cache.store("a3", new ByteArrayInputStream(byteMap.get("a3")));
            assertEquals(new ByteArrayInputStream(byteMap.get("a1")),
                cache.getIfStored("a1"));
            assertEquals(new ByteArrayInputStream(byteMap.get("a2")),
                cache.getIfStored("a2"));
            assertEquals(new ByteArrayInputStream(byteMap.get("a3")),
                cache.getIfStored("a3"));
        } catch (Exception e) {
            LOG.error("error:", e);
            fail();
        }

    }

    /**
     * Test to verify cache's purging if cache current size exceeds
     * cachePurgeTrigFactor * size.
     */
    public void testAutoPurge() {
        try {

            LocalCache cache = new LocalCache(CACHE_DIR, TEMP_DIR, 400, 0.95,
                0.70);
            Random random = new Random(12345);
            byte[] data = new byte[100];
            Map<String, byte[]> byteMap = new HashMap<String, byte[]>();
            random.nextBytes(data);
            byteMap.put("a1", data);

            data = new byte[100];
            random.nextBytes(data);
            byteMap.put("a2", data);

            data = new byte[100];
            random.nextBytes(data);
            byteMap.put("a3", data);

            data = new byte[100];
            random.nextBytes(data);
            byteMap.put("a4", data);

            cache.store("a1", new ByteArrayInputStream(byteMap.get("a1")));
            cache.store("a2", new ByteArrayInputStream(byteMap.get("a2")));
            cache.store("a3", new ByteArrayInputStream(byteMap.get("a3")));
            assertEquals(new ByteArrayInputStream(byteMap.get("a1")),
                cache.getIfStored("a1"));
            assertEquals(new ByteArrayInputStream(byteMap.get("a2")),
                cache.getIfStored("a2"));
            assertEquals(new ByteArrayInputStream(byteMap.get("a3")),
                cache.getIfStored("a3"));

            data = new byte[90];
            random.nextBytes(data);
            byteMap.put("a4", data);
            // storing a4 should purge cache
            cache.store("a4", new ByteArrayInputStream(byteMap.get("a4")));
            Thread.sleep(1000);

            assertNull("a1 should be null", cache.getIfStored("a1"));
            assertNull("a2 should be null", cache.getIfStored("a2"));
            assertEquals(new ByteArrayInputStream(byteMap.get("a3")),
                cache.getIfStored("a3"));
            assertEquals(new ByteArrayInputStream(byteMap.get("a4")),
                cache.getIfStored("a4"));
            data = new byte[100];
            random.nextBytes(data);
            byteMap.put("a5", data);
            cache.store("a5", new ByteArrayInputStream(byteMap.get("a5")));
            assertEquals(new ByteArrayInputStream(byteMap.get("a3")),
                cache.getIfStored("a3"));
        } catch (Exception e) {
            LOG.error("error:", e);
            fail();
        }
    }
    
    /**
     * Assert two inputstream
     */
    protected void assertEquals(InputStream a, InputStream b)
            throws IOException {
        while (true) {
            int ai = a.read();
            int bi = b.read();
            assertEquals(ai, bi);
            if (ai < 0) {
                break;
            }
        }
    }

}
