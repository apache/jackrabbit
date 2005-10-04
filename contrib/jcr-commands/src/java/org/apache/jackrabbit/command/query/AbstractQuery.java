/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.apache.jackrabbit.command.query;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Query the repository through either SQL or XPATH.
 */
public abstract class AbstractQuery implements Command
{

    // ---------------------------- < keys >

    /** query statement key */
    private String statementKey = "statement";

    /** destination key */
    private String destKey = "collected";

    /**
     * @inheritDoc
     */
    public final boolean execute(Context ctx) throws Exception
    {
        String statement = (String) ctx.get(this.statementKey) ;
        Session session = CommandHelper.getSession(ctx);
        Query query = session.getWorkspace().getQueryManager()
            .createQuery(statement, this.getLanguage());
        QueryResult result = query.execute();
        ctx.put(destKey, result.getNodes());
        return false;
    }

    /**
     * Query language
     * 
     * @return
     */
    protected abstract String getLanguage();

    /**
     * @return Returns the statementKey.
     */
    public String getStatementKey()
    {
        return statementKey;
    }

    /**
     * @param statementKey
     *            Set the context attribute key for the statement attribute.
     */
    public void setStatementKey(String statementKey)
    {
        this.statementKey = statementKey;
    }

    /**
     * @return Returns the toKey.
     */
    public String getDestKey()
    {
        return destKey;
    }

    /**
     * @param toKey
     *            Set the context attribute key for the to attribute.
     */
    public void setDestKey(String toKey)
    {
        this.destKey = toKey;
    }
}
