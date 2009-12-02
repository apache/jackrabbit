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
package org.apache.jackrabbit.core.fs.db;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.fs.AbstractFileSystemTest;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;

/**
 * Tests the Apache Derby file system.
 */
public class DerbyFileSystemTest extends AbstractFileSystemTest {

    private ConnectionFactory conFac;

    private File file;

    protected FileSystem getFileSystem() {
        DerbyFileSystem filesystem = new DerbyFileSystem();
        filesystem.setConnectionFactory(conFac);
        filesystem.setUrl("jdbc:derby:" + file.getPath() + ";create=true");
        return filesystem;
    }

    protected void setUp() throws Exception {
        file = File.createTempFile("jackrabbit", "derbyfs");
        file.delete();
        conFac = new ConnectionFactory();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteDirectory(file);
        conFac.close();
    }
}
