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

import org.apache.jackrabbit.core.fs.AbstractFileSystemTest;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;

/**
 * Tests the creation of an Oracle file system with different tablespace and index tablespace.
 *
 * @author Edouard Hue <edouard.hue@gmail.com>
 */
public class OracleFileSystemTest extends AbstractFileSystemTest {
    private ConnectionFactory connectionFactory;

    @Override
    protected FileSystem getFileSystem() throws Exception {
        connectionFactory = new ConnectionFactory();
        final OracleFileSystem fs = new OracleFileSystem();
        fs.setConnectionFactory(connectionFactory);
        fs.setUrl(System.getProperty("tests.oracle.url"));
        fs.setUser(System.getProperty("tests.oracle.user"));
        fs.setPassword(System.getProperty("tests.oracle.password"));
        fs.setDriver(System.getProperty("tests.oracle.driver", "oracle.jdbc.driver.OracleDriver"));
        fs.setSchemaObjectPrefix(System.getProperty("tests.oracle.schemaObjectPrefix", ""));
        fs.setTablespace(System.getProperty("tests.oracle.tablespace"));
        fs.setIndexTablespace(System.getProperty("tests.oracle.indexTablespace"));
        fs.setSchema("oracle");
        return fs;
    }
}
