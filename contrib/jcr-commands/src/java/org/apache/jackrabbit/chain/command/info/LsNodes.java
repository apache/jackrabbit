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
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Lists the nodes under the current working node that match the given pattern.
 */
public class LsNodes extends AbstractLsNodes
{
    /** name pattern */
    private String pattern;

    /** name pattern key */
    private String patternKey;

    /**
     * @return name pattern
     */
    public String getPattern()
    {
        return pattern;
    }

    /**
     * Sets the name pattern
     * 
     * @param pattern
     */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @inheritDoc
     */
    protected Iterator getNodes(Context ctx) throws JcrCommandException,
            RepositoryException
    {
        String pattern = CtxHelper.getAttr(this.pattern, patternKey, "*", ctx);
        return CtxHelper.getNodes(ctx, pattern);
    }

    /**
     * @return Returns the patternKey.
     */
    public String getPatternKey()
    {
        return patternKey;
    }

    /**
     * @param patternKey
     *            The patternKey to set.
     */
    public void setPatternKey(String patternKey)
    {
        this.patternKey = patternKey;
    }
}
