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
package org.apache.jackrabbit.standalone.cli.info;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * List items
 * <ul>
 * <li>name</li>
 * <li>type</li>
 * <li>isNode</li>
 * <li>isNew</li>
 * <li>isModified</li>
 * </ul>
 */
public class LsItems extends AbstractLsItems {

    /** name pattern key */
    private String patternKey = "pattern";

    /**
     * @return the name pattern
     */
    public String getPatternKey() {
        return patternKey;
    }

    /**
     * @param pattern
     *        the pattern
     */
    public void setPatternKey(String pattern) {
        this.patternKey = pattern;
    }

    /**
     * {@inheritDoc}
     */
    protected Iterator getItems(Context ctx) throws CommandException,
            RepositoryException {
        String pattern = (String) ctx.get(this.patternKey);
        Node n = CommandHelper.getCurrentNode(ctx);
        return CommandHelper.getItems(ctx, n, pattern);
    }
}
