/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.chain.command.info;

import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Lists collected properties.<br> 
 * This Command looks for an Iterator under the given
 * context variable and lists its properties.
 */
public class LsCollectedProperties extends AbstractLsProperties
{
    /** Context variable that holds the Iterator */
    private String fromKey = "collected";

    /**
     * @return the context variable
     */
    public String getFromKey()
    {
        return fromKey;
    }

    /**
     * Sets the context variable
     * @param context variable name
     */
    public void setFromKey(String contextVariable)
    {
        this.fromKey = contextVariable;
    }

    /**
     * @inheritDoc
     */
    protected Iterator getProperties(Context ctx) throws JcrCommandException,
            RepositoryException
    {
        // show the path
        this.setPath(true);
        Object o = ctx.get(this.fromKey);
        if (o == null || !(o instanceof Iterator))
        {
            throw new JcrInfoCommandException(
                "illegalargument.no.iterator.under", new String[]
                {
                    fromKey
                });
        }
        return (Iterator) o;
    }

}
