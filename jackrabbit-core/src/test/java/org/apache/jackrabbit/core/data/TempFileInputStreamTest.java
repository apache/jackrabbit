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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.core.data.db.TempFileInputStream;
import org.apache.jackrabbit.test.JUnitTest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests the class TempFileInputStream
 */
public class TempFileInputStreamTest extends JUnitTest {

    public void testReadPastEOF() throws IOException {
        File temp = File.createTempFile("test", null);
        TempFileInputStream.writeToFileAndClose(new ByteArrayInputStream(new byte[1]), temp);
        TempFileInputStream in = new TempFileInputStream(temp, false);
        assertEquals(0, in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
        in.close();
    }

    public void testMarkReset() throws IOException {
        File temp = File.createTempFile("test", null);
        TempFileInputStream.writeToFileAndClose(new ByteArrayInputStream(new byte[10]), temp);
        InputStream in = new BufferedInputStream(new TempFileInputStream(temp, false));
        in.mark(100);
        for (int i = 0; i < 10; i++) {
            assertEquals(0, in.read());
        }
        assertEquals(-1, in.read());
        in.reset();
        for (int i = 0; i < 10; i++) {
            assertEquals(0, in.read());
        }
        assertEquals(-1, in.read());
        in.close();
    }

}
