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
package org.apache.jackrabbit.standalone.cli.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;
import org.apache.jackrabbit.util.ChildrenCollectorFilter;

/**
 * Remove any <code>Item</code> under the given <code>Node</code> that match
 * the given name pattern.
 */
public class RemoveItems implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(RemoveItems.class);

    // ---------------------------- < keys >
    /** path key */
    private String pathKey = "path";

    /** item pattern key */
    private String patternKey = "pattern";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String pattern = (String) ctx.get(this.patternKey);
        String path = (String) ctx.get(this.pathKey);

        Node n = CommandHelper.getNode(ctx, path);

        if (log.isDebugEnabled()) {
            log.debug("removing nodes from " + n.getPath()
                    + " that match pattern " + pattern);
        }

        List children = new ArrayList();
        ChildrenCollectorFilter collector = new ChildrenCollectorFilter(
            pattern, children, true, true, 1);
        collector.visit(n);

        Iterator items = children.iterator();

        while (items.hasNext()) {
            Item item = (Item) items.next();
            if (item.isSame(CommandHelper.getCurrentNode(ctx))
                    && item.getDepth() > 0) {
                CommandHelper.setCurrentNode(ctx, item.getParent());
            }
            item.remove();
        }

        return false;
    }

    /**
     * @return the pattern key
     */
    public String getPatternKey() {
        return patternKey;
    }

    /**
     * @param patternKey
     *        the pattern key to set
     */
    public void setPatternKey(String patternKey) {
        this.patternKey = patternKey;
    }

    /**
     * @return the path key
     */
    public String getPathKey() {
        return pathKey;
    }

    /**
     * @param pathKey
     *        the path key to set
     */
    public void setPathKey(String pathKey) {
        this.pathKey = pathKey;
    }
}
