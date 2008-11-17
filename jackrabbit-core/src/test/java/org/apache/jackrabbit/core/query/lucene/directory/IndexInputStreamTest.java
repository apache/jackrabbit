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
package org.apache.jackrabbit.core.query.lucene.directory;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Random;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.IndexOutput;

import junit.framework.TestCase;

/**
 * <code>IndexInputStreamTest</code> performs tests on {@link IndexInputStream}.
 */
public class IndexInputStreamTest extends TestCase {

    public void testIndexInputStream() throws IOException {
        checkStream(0, 0);
        checkStream(0, 128);
        checkStream(128, 0);
        checkStream(128, 128);
        checkStream(127, 128);
        checkStream(129, 128);
        checkStream(300, 128);
    }

    private void checkStream(int size, int buffer) throws IOException {
        Random rand = new Random();
        byte[] data = new byte[size];
        rand.nextBytes(data);
        Directory dir = new RAMDirectory();
        IndexOutput out = dir.createOutput("test");
        out.writeBytes(data, data.length);
        out.close();
        InputStream in = new IndexInputStream(dir.openInput("test"));
        if (buffer != 0) {
            in = new BufferedInputStream(in, buffer);
        }
        byte[] buf = new byte[3];
        int len;
        int pos = 0;
        while ((len = in.read(buf)) > -1) {
            for (int i = 0; i < len; i++, pos++) {
                assertEquals(data[pos], buf[i]);
            }
        }
        in.close();
        // assert length
        assertEquals(data.length, pos);
    }
}
