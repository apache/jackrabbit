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

import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandException;

/**
 * Lists collected <code>Item</code>s.<br>
 * This <code>Command</code> looks for an <code>Iterator</code> under the
 * given <code>Context</code> variable and lists its <code>Item</code>s.
 */
public class LsCollectedItems extends AbstractLsItems {
    /** Context variable that holds the Iterator */
    private String fromKey = "collected";

    /**
     * @return the context variable
     */
    public String getFromKey() {
        return fromKey;
    }

    /**
     * Sets the context variable
     * @param from
     *        from key to set
     */
    public void setFromKey(String from) {
        this.fromKey = from;
    }

    /**
     * {@inheritDoc}
     */
    protected Iterator getItems(Context ctx) throws CommandException,
            RepositoryException {
        // Always show the path
        this.setPath(true);
        Object o = ctx.get(this.fromKey);
        if (o == null || !(o instanceof Iterator)) {
            throw new JcrInfoCommandException(
                "illegalargument.no.iterator.under", new String[] {
                    fromKey
                });
        }
        return (Iterator) o;
    }
}
