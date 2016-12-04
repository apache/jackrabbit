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
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.util.Random;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.IndexInput;

import junit.framework.TestCase;

/**
 * <code>IndexOutputStreamTest</code> performs tests on {@link IndexOutputStream}.
 */
public class IndexOutputStreamTest extends TestCase {

    public void testIndexOutputStream() throws IOException {
        checkStream(0, 0);
        checkStream(0, 128);
        checkStream(128, 0);
        checkStream(127, 128);
        checkStream(128, 128);
        checkStream(129, 128);
        checkStream(300, 128);
    }

    private void checkStream(int size, int buffer) throws IOException {
        Random rand = new Random();
        byte[] data = new byte[size];
        rand.nextBytes(data);
        Directory dir = new RAMDirectory();
        OutputStream out = new IndexOutputStream(dir.createOutput("test"));
        if (buffer != 0) {
            out = new BufferedOutputStream(out, buffer);
        }
        out.write(data);
        out.close();

        byte[] buf = new byte[3];
        int pos = 0;
        IndexInput in = dir.openInput("test");
        for (;;) {
            int len = (int) Math.min(buf.length, in.length() - pos);
            in.readBytes(buf, 0, len);
            for (int i = 0; i < len; i++, pos++) {
                assertEquals(data[pos], buf[i]);
            }
            if (len == 0) {
                // EOF
                break;
            }
        }
        in.close();

        // assert length
        assertEquals(data.length, pos);
    }
}
