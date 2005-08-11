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
package org.apache.jackrabbit.chain.command.query;

import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Query the repository through either SQL or XPATH. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public abstract class AbstractQuery implements Command
{
    // ---------------------------- < literals >

    /** query statement */
    private String statement;

    // ---------------------------- < keys >

    /** query statement key */
    private String statementKey;

    /** destination key */
    private String toKey;

    /**
     * @inheritDoc
     */
    public final boolean execute(Context ctx) throws Exception
    {
        String statement = CtxHelper.getAttr(this.statement,
            this.statementKey, ctx);

        Session session = CtxHelper.getSession(ctx);
        javax.jcr.query.Query query = session.getWorkspace().getQueryManager()
            .createQuery(statement, this.getLanguage());

        QueryResult result = query.execute();

        ctx.put(toKey, result.getNodes());

        return false;
    }

    /**
     * Query language
     * 
     * @return
     */
    protected abstract String getLanguage();

    /**
     * @return the query statement
     */
    public String getStatement()
    {
        return statement;
    }

    /**
     * Sets the query statement
     * 
     * @param statement
     */
    public void setStatement(String statement)
    {
        this.statement = statement;
    }

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
    public String getToKey()
    {
        return toKey;
    }

    /**
     * @param toKey
     *            Set the context attribute key for the to attribute.
     */
    public void setToKey(String toKey)
    {
        this.toKey = toKey;
    }
}
