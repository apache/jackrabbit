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

import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.Oracle10R1ConnectionHelper;

/**
 * This class adds special blob handling which is needed for Oracle DBs up to version 10R1.
 */
public class Oracle9FileSystem extends OracleFileSystem {

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
        Oracle10R1ConnectionHelper helper = new Oracle10R1ConnectionHelper(dataSrc, false);
        helper.init();
        return helper;
    }
}
