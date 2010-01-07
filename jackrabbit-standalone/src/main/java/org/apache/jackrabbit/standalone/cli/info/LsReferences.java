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

import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Displays references to the given <code>Node</code>
 */
public class LsReferences implements Command {
    /** bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** path to the <code>Node</code> key */
    private String pathKey = "path";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        Node n = CommandHelper.getNode(ctx, path);

        // header
        int[] width = new int[] {
            60
        };
        String[] header = new String[] {
            bundle.getString("word.path")
        };

        // print header
        PrintHelper.printRow(ctx, width, header);

        // print separator
        PrintHelper.printSeparatorRow(ctx, width, '-');

        PropertyIterator iter = n.getReferences();
        while (iter.hasNext()) {
            Property p = iter.nextProperty();
            // print header
            PrintHelper.printRow(ctx, width, new String[] {
                p.getPath()
            });
        }

        CommandHelper.getOutput(ctx).println();
        CommandHelper.getOutput(ctx).println(
            iter.getSize() + " " + bundle.getString("word.references"));

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
}
