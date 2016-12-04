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
package org.apache.jackrabbit.core.fs.local;

import java.io.File;
import java.io.IOException;

import org.apache.jackrabbit.core.fs.AbstractFileSystemTest;
import org.apache.jackrabbit.core.fs.FileSystem;

/**
 * Tests the local file system.
 */
public class LocalFileSystemTest extends AbstractFileSystemTest {

    private String tempDirectory;

    protected FileSystem getFileSystem() {
        LocalFileSystem filesystem = new LocalFileSystem();
        filesystem.setPath(tempDirectory);
        return filesystem;
    }

    protected void setUp() throws Exception {
        File file = File.createTempFile("jackrabbit", "localfs");
        tempDirectory = file.getPath();
        file.delete();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        delete(new File(tempDirectory));
    }

    private void delete(File file) throws IOException {
        File[] files = file.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            delete(files[i]);
        }
        file.delete();
    }

}
