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
package org.apache.jackrabbit.chain.command;

import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.ContextHelper;

/**
 * Query the repository through either SQL or XPATH
 */
public abstract class AbstractQuery implements Command
{

    /** query statement */
    private String statement;

    /** target */
    private String target = "result";

    public boolean execute(Context ctx) throws Exception
    {
        Session session = ContextHelper.getSession(ctx);
        javax.jcr.query.Query query = session.getWorkspace().getQueryManager()
            .createQuery(this.statement, this.getLanguage());
        QueryResult result = query.execute();
        ctx.put(target, result.getNodes());
        return false;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }
    
    /**
     * Query language
     * @return
     */
    protected abstract String getLanguage();
    
    

    public String getStatement()
    {
        return statement;
    }
    
    public void setStatement(String statement)
    {
        this.statement = statement;
    }
}
