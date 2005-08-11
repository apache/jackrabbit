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
 * List items
 * 
 * <p>
 * <ul>
 * <li>name</li>
 * <li>type</li>
 * <li>isNode</li>
 * <li>isNew</li>
 * <li>isModified</li>
 * </ul>
 * </p>
 */
public class LsItems extends AbstractLsItems
{

    /** name pattern key */
    private String patternKey;

    /** name pattern */
    private String pattern;

    /**
     * @return the name pattern
     */
    public String getPatternKey()
    {
        return patternKey;
    }

    /**
     * Sets the name pattern
     * 
     * @param name
     *            pattern
     */
    public void setPatternKey(String pattern)
    {
        this.patternKey = pattern;
    }

    /**
     * @inheritDoc
     */
    protected Iterator getItems(Context ctx) throws JcrCommandException,
            RepositoryException
    {
        String pattern = CtxHelper.getAttr(this.pattern, patternKey, "*", ctx);
        return CtxHelper.getItems(ctx, pattern);
    }

    /**
     * @return Returns the pattern.
     */
    public String getPattern()
    {
        return pattern;
    }

    /**
     * @param pattern
     *            The pattern to set.
     */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }
}
