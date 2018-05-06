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
package org.apache.jackrabbit.core.query.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import junit.framework.TestCase;

/**
 * <code>IndexInfosTest</code> check if <code>IndexInfos</code> can deal with
 * invalid info files. See also JCR-3299.
 */
public class IndexInfosTest extends TestCase {

    private static final File TEST_DIR = new File(new File("target"), "indexInfosTest");

    private Directory dir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FileUtils.deleteDirectory(TEST_DIR);
        TEST_DIR.mkdirs();
        dir = FSDirectory.open(TEST_DIR);
    }

    @Override
    protected void tearDown() throws Exception {
        dir.close();
        FileUtils.deleteDirectory(TEST_DIR);
        super.tearDown();
    }

    public void testEmptyIndexesFile() throws IOException {
        // creates initial generation of infos
        IndexInfos infos = new IndexInfos(dir, "indexes");
        long initialGeneration = infos.getGeneration();

        // create second generation
        infos.addName("index1", 23432);
        infos.write();

        // replace second generation file with an empty one
        String fileName = infos.getFileName();
        dir.deleteFile(fileName);
        dir.createOutput(fileName).close();

        new IndexHistory(dir, Integer.MAX_VALUE); // must succeed

        // read index infos again
        infos = new IndexInfos(dir, "indexes");
        assertEquals("must read initial generation", initialGeneration, infos.getGeneration());

        // create new generation
        infos.addName("index1", 39854);
        infos.write(); // must succeed
    }
}
