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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.jackrabbit.test.JUnitTest;

/**
 * Tests the LazyFileInputStream class.
 */
public class LazyFileInputStreamTest extends JUnitTest {
    
    private static final String TEST_FILE = "target/test.txt";
    
    private File file = new File(TEST_FILE);

    public void setUp() {
        // Create the test directory
        new File(TEST_FILE).getParentFile().mkdirs();
    }
    
    public void tearDown() {
        new File(TEST_FILE).delete();
    }
    
    private void createFile() throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        out.write(new byte[1]);
        out.close();
    }    
    
    public void test() throws IOException {
        
        createFile();
        
        // test exception if file doesn't exist
        try {
            LazyFileInputStream in = new LazyFileInputStream(file.getAbsolutePath() + "XX");
            in.close();
            fail();
        } catch (IOException e) {
            // expected
        }
        
        // test open / close (without reading)
        LazyFileInputStream in = new LazyFileInputStream(file);
        in.close();
        
        // test reading too much and closing too much
        in = new LazyFileInputStream(file);
        assertEquals(0, in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
        in.close();
        in.close();
        in.close();
        assertEquals(-1, in.read());
        
        // test with file name
        in = new LazyFileInputStream(file.getAbsolutePath());
        assertEquals(1, in.available());
        assertEquals(0, in.read());
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
        assertEquals(0, in.available());
        in.close();
        
        // test markSupported, mark, and reset
        in = new LazyFileInputStream(file);
        assertFalse(in.markSupported());
        in.mark(1);
        assertEquals(0, in.read());
        try {
            in.reset();
            fail();
        } catch (IOException e) {
            // expected
        }
        assertEquals(-1, in.read());
        in.close();
        
        // test read(byte[])
        in = new LazyFileInputStream(file);
        byte[] test = new byte[2];
        assertEquals(1, in.read(test));
        in.close();        
        
        // test read(byte[],int,int)
        in = new LazyFileInputStream(file);
        assertEquals(1, in.read(test, 0, 2));
        in.close();        

        // test skip
        in = new LazyFileInputStream(file);
        assertEquals(2, in.skip(2));
        assertEquals(-1, in.read(test));
        assertEquals(0, in.skip(2));
        in.close();        

        // test with the file descriptor
        RandomAccessFile ra = new RandomAccessFile(file, "r");
        in = new LazyFileInputStream(ra.getFD());
        assertEquals(0, in.read());
        assertEquals(-1, in.read());
        in.close();
        ra.close();
        
        // test that the file is not opened before reading
        in = new LazyFileInputStream(file);
        // this should fail in Windows if the file was opened
        file.delete();
        
        createFile();
        
        // test that the file is closed after reading the last byte
        in = new LazyFileInputStream(file);
        assertEquals(0, in.read());
        assertEquals(-1, in.read());
        // this should fail in Windows if the file was opened
        file.delete();
        
    }

}
