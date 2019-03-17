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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * List items superclass
 */
public abstract class AbstractLsItems extends AbstractLs {
    /** bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** name width */
    private int nameWidth = 30;

    /** node type width */
    private int typeWidth = 15;

    /**
     * @param ctx
     *        the <code>Context</code>
     * @return Iterator containing the Items to list
     * @throws CommandException
     *         if an errors occurs
     * @throws RepositoryException
     *         if the current <code>Repository</code> throws a
     *         <code>RepositoryException</code>
     */
    protected abstract Iterator getItems(Context ctx) throws CommandException,
            RepositoryException;

    /**
     * {@inheritDoc}
     */
    public final boolean execute(Context ctx) throws Exception {
        int nodes = 0;
        int properties = 0;

        // header
        int[] width = new int[] {
                nameWidth, typeWidth, longWidth, longWidth, longWidth
        };
        String[] header = new String[] {
                bundle.getString("word.name"), bundle.getString("word.type"),
                bundle.getString("word.node"), bundle.getString("word.new"),
                bundle.getString("word.modified")
        };

        // print header
        PrintHelper.printRow(ctx, width, header);

        // print separator
        PrintHelper.printSeparatorRow(ctx, width, '-');

        // nodes
        Iterator iter = getItems(ctx);

        int index = 0;

        int maxItems = getMaxItems(ctx);

        // Print nodes
        while (iter.hasNext() && index < maxItems) {
            Item i = (Item) iter.next();

            String type = null;

            // Show name or path
            String name = null;
            if (this.isPath()) {
                name = i.getPath();
            } else {
                name = i.getName();
            }

            if (i.isNode()) {
                nodes++;
                // name
                Node n = (Node) i;
                if (!isPath() && n.getIndex() > 1) {
                    name = n.getName() + "[" + n.getIndex() + "]";
                }
                // type
                type = n.getPrimaryNodeType().getName();
            } else {
                properties++;
                type = PropertyType.nameFromValue(((Property) i).getType());
            }

            PrintHelper.printRow(ctx, width, new String[] {
                    name, type, Boolean.toString(i.isNode()),
                    Boolean.valueOf(i.isNew()).toString(),
                    Boolean.valueOf(i.isModified()).toString()
            });
            index++;
        }

        // Footer
        printFooter(ctx, iter);

        return false;
    }

}
