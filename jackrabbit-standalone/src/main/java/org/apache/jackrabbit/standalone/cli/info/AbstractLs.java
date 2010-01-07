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
import java.util.ResourceBundle;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Ls superclass
 */
public abstract class AbstractLs implements Command {
    /** bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** long width */
    protected int longWidth = 9;

    /** max items to list */
    private int defaultMaxItems = 100;

    /** max number of items */
    private String maxItemsKey = "maxItems";

    /** show path flag */
    private boolean path;

    /**
     * Print the footer
     * @param ctx
     *        the <code>Context</code>
     * @param iter
     *        the <code>Iterator</code>
     */
    protected void printFooter(Context ctx, Iterator iter) {
        CommandHelper.getOutput(ctx).println();
        CommandHelper.getOutput(ctx).println(bundle.getString("word.total"));
        if (iter instanceof NodeIterator) {
            printFooter(ctx, (NodeIterator) iter);
        } else if (iter instanceof PropertyIterator) {
            printFooter(ctx, (PropertyIterator) iter);
        }
    }

    /**
     * Print footer
     * @param ctx
     *        the <code>Context</code>
     * @param iter
     *        the <code>Iterator</code>
     */
    private void printFooter(Context ctx, NodeIterator iter) {
        CommandHelper.getOutput(ctx).println(
            iter.getSize() + " " + bundle.getString("word.nodes"));
    }

    /**
     * Print footer
     * @param ctx
     *        the <code>Context</code>
     * @param iter
     *        the <code>Iterator</code>
     */
    private void printFooter(Context ctx, PropertyIterator iter) {
        CommandHelper.getOutput(ctx).println(
            iter.getSize() + " " + bundle.getString("word.properties"));
    }

    /**
     * @return the default max number of <code>Items</code> s to show
     */
    public int getDefaultMaxItems() {
        return defaultMaxItems;
    }

    /**
     * @param maxItems
     *        the default max number of <code>Items</code> s to set
     */
    public void setDefaultMaxItems(int maxItems) {
        this.defaultMaxItems = maxItems;
    }

    /**
     * @return the path
     */
    public boolean isPath() {
        return path;
    }

    /**
     * @param path
     *        the path to set
     */
    public void setPath(boolean path) {
        this.path = path;
    }

    /**
     * @return the max number of items key
     */
    public String getMaxItemsKey() {
        return maxItemsKey;
    }

    /**
     * @param maxItemsKey
     *        the max number of items key to set
     */
    public void setMaxItemsKey(String maxItemsKey) {
        this.maxItemsKey = maxItemsKey;
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @return the max number of <code>Item</code> s to show
     */
    protected int getMaxItems(Context ctx) {
        String maxItems = (String) ctx.get(this.maxItemsKey);
        if (maxItems == null) {
            maxItems = new Integer(this.defaultMaxItems).toString();
        }
        return Integer.valueOf(maxItems).intValue();
    }

}
