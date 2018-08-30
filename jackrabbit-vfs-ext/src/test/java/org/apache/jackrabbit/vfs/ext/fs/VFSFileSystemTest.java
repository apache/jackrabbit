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
package org.apache.jackrabbit.vfs.ext.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.fs.AbstractFileSystemTest;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the VFS file system.
 */
public class VFSFileSystemTest extends AbstractFileSystemTest {

    private static Logger log = LoggerFactory.getLogger(VFSFileSystemTest.class);

    private File tempFolder;

    private VFSFileSystem fileSystem;

    @Override
    protected FileSystem getFileSystem() {
        if (fileSystem == null) {
            try {
                final File tempFile = File.createTempFile("jackrabbit-vfsfs-test", "");
                tempFolder = new File(tempFile.getPath() + ".d");
                tempFile.delete();
                final String baseFolderUri = tempFolder.toURI().toString();

                fileSystem = new VFSFileSystem();
                fileSystem.setBaseFolderUri(baseFolderUri);

                try {
                    fileSystem.init();
                } catch (FileSystemException e) {
                    log.error("Failed to initialize VFS file system.", e);
                    fail("Failed to initialize VFS file system.");
                }
            } catch (IOException e) {
                fail("Failed to clean up temporary folder at " + tempFolder);
            }
        }

        return fileSystem;
    }

    @Override
    protected void setUp() throws Exception {
        final String configProp = System.getProperty("config");

        if (configProp != null && !"".equals(configProp)) {
            try {
                final Properties props = new Properties();
                final File configFile = new File(configProp);

                if (configFile.isFile()) {
                    try (final FileInputStream fis = new FileInputStream(configFile)) {
                        props.load(fis);
                    }
                } else {
                    props.load(getClass().getResourceAsStream(configProp));
                }
            } catch (IOException e) {
                log.error("Failed to load configuration file at {}.", configProp, e);
            }
        }

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (fileSystem != null) {
            fileSystem.close();
        }

        if (tempFolder != null) {
            FileUtils.deleteDirectory(tempFolder);
        }
    }

}
