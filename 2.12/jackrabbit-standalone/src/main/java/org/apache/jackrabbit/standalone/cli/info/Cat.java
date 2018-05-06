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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Display the content of a <code>Property</code> or a <code>Node</code> of
 * type nt:file or nt:resource.
 */
public class Cat implements Command {
    /** property name */
    private String pathKey = "path";

    /** index. [optional] argument to display multivalue properties */
    private String indexKey = "index";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        Item item = CommandHelper.getItem(ctx, path);
        if (item.isNode()) {
            printNode(ctx, (Node) item);
        } else {
            printProperty(ctx, (Property) item);
        }
        return false;
    }

    /**
     * @return the path key
     */
    public String getPathKey() {
        return pathKey;
    }

    /**
     * @param path
     *        the path key to set
     */
    public void setPathKey(String path) {
        this.pathKey = path;
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @param n
     *        the <code>Node</code>
     * @throws PathNotFoundException
     * @throws CommandException
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void printNode(Context ctx, Node n) throws PathNotFoundException,
            CommandException, RepositoryException, IllegalStateException,
            IOException {
        if (n.isNodeType("nt:file")) {
            printValue(ctx, n.getNode("jcr:content").getProperty("jcr:data")
                .getValue());
        } else if (n.isNodeType("nt:resource")) {
            printValue(ctx, n.getProperty("jcr:data").getValue());
        } else {
            throw new CommandException("exception.cat.unsupported.type",
                new String[] {
                    n.getPrimaryNodeType().getName()
                });
        }
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @param p
     *        the <code>Property</code>
     * @throws CommandException
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     * @throws IOException
     */
    private void printProperty(Context ctx, Property p)
            throws CommandException, ValueFormatException,
            IllegalStateException, RepositoryException, IOException {
        String indexStr = (String) ctx.get(this.indexKey);
        int index = 0;
        if (indexStr != null) {
            index = Integer.parseInt(indexStr);
        }
        if (p.getDefinition().isMultiple()) {
            printValue(ctx, p.getValues()[index]);
        } else {
            printValue(ctx, p.getValue());
        }
    }

    /**
     * Read the value
     * @param ctx
     *        the <code>Context</code>
     * @param value
     *        the <code>Value</code>
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     * @throws IOException
     */
    private void printValue(Context ctx, Value value)
            throws ValueFormatException, IllegalStateException,
            RepositoryException, IOException {
        PrintWriter out = CommandHelper.getOutput(ctx);
        out.println();
        BufferedReader in = new BufferedReader(new StringReader(value
            .getString()));
        String str = null;
        while ((str = in.readLine()) != null) {
            out.println(str);
        }
    }

    /**
     * @return the index key
     */
    public String getIndexKey() {
        return indexKey;
    }

    /**
     * @param indexKey
     *        the index key to set
     */
    public void setIndexKey(String indexKey) {
        this.indexKey = indexKey;
    }
}
