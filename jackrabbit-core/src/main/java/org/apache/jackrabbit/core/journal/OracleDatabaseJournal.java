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
package org.apache.jackrabbit.core.journal;

import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.CheckSchemaOperation;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.OracleConnectionHelper;

/**
 * It has the following property in addition to those of the DatabaseJournal:
 * <ul>
 * <li><code>tableSpace</code>: the Oracle tablespace to use</li>
 * </ul>
 */
public class OracleDatabaseJournal extends DatabaseJournal {

    /** the variable for the Oracle table space */
    public static final String TABLE_SPACE_VARIABLE =
        "${tableSpace}";

    /** the Oracle table space to use */
    protected String tableSpace = "";

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
        OracleConnectionHelper helper = new OracleConnectionHelper(dataSrc, false);
        helper.init();
        return helper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CheckSchemaOperation createCheckSchemaOperation() {
        return super.createCheckSchemaOperation().addVariableReplacement(
            CheckSchemaOperation.TABLE_SPACE_VARIABLE, tableSpace);
    }

    /**
     * Returns the configured Oracle table space.
     * @return the configured Oracle table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the Oracle table space.
     * @param tableSpace the Oracle table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null && tableSpace.trim().length() > 0) {
            this.tableSpace = "tablespace " + tableSpace.trim();
        } else {
            this.tableSpace = "";
        }
    }
}
