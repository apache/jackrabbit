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
package org.apache.jackrabbit.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TransientFileFactoryTest extends TestCase {

    private final TransientFileFactory tff = TransientFileFactory.getInstance();

    public void testCreateTransientFile() throws IOException {
        File tmp = tff.createTransientFile("foo", "bar", null);
        String filename = tmp.getName();
        assertTrue(filename.startsWith("foo"));
        assertTrue(filename.endsWith("bar"));
        boolean deleted = tmp.delete();
        assertTrue(deleted);
    }

    public void testCleanup() throws IOException {
        File tmp = tff.createTransientFile("foo", "bar", null);
        String filename = tmp.getName();
        assertTrue(filename.startsWith("foo"));
        assertTrue(filename.endsWith("bar"));
        tff.deleteTransientFiles();
        assertFalse(tmp.exists());
    }

    public void testOutputStreamAfterFileDeletion() throws IOException {
        // verifies that it's possible to write to a FileOutputStream
        // even though "delete" has been called on the File object
        // see testOutputStreamAfterCleanup
        File tmp = File.createTempFile("foo", "bar");
        OutputStream os = new FileOutputStream(tmp);
        tmp.delete();
        os.write(new byte[12345]);
        os.close();
    }

    public void testOutputStreamAfterFactoryCleanup() throws IOException {
        File tmp = tff.createTransientFile("foo", "bar", null);
        OutputStream os = new FileOutputStream(tmp);
        tff.deleteTransientFiles();
        os.write(new byte[12345]);
        os.close();
    }
}