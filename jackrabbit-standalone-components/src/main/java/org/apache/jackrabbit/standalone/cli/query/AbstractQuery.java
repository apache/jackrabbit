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
package org.apache.jackrabbit.standalone.cli.query;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Query the <code>Repository</code> through either SQL or XPATH language.
 */
public abstract class AbstractQuery implements Command {

    // ---------------------------- < keys >

    /** query statement key */
    private String statementKey = "statement";

    /** destination key */
    private String destKey = "collected";

    /**
     * {@inheritDoc}
     */
    public final boolean execute(Context ctx) throws Exception {
        String statement = (String) ctx.get(this.statementKey);
        Session session = CommandHelper.getSession(ctx);
        Query query = session.getWorkspace().getQueryManager().createQuery(
            statement, this.getLanguage());
        QueryResult result = query.execute();
        ctx.put(destKey, result.getNodes());
        return false;
    }

    /**
     * @return the query language
     */
    protected abstract String getLanguage();

    /**
     * @return the statement key
     */
    public String getStatementKey() {
        return statementKey;
    }

    /**
     * @param statementKey
     *        the statement key to set
     */
    public void setStatementKey(String statementKey) {
        this.statementKey = statementKey;
    }

    /**
     * @return the destination key
     */
    public String getDestKey() {
        return destKey;
    }

    /**
     * @param toKey
     *        the destination key to set
     */
    public void setDestKey(String toKey) {
        this.destKey = toKey;
    }
}
