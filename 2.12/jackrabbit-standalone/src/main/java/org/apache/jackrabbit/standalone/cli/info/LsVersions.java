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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * List the <code>Version</code> s in the <code>VersionHistory</code>.
 */
public class LsVersions implements Command {
    /** bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** path to the node */
    private String pathKey = "path";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        Node n = CommandHelper.getNode(ctx, path);

        // header
        int[] width = new int[] {
                20, 50
        };
        String[] header = new String[] {
                bundle.getString("word.version"),
                bundle.getString("word.labels")
        };
        // print header
        PrintHelper.printRow(ctx, width, header);
        // print separator
        PrintHelper.printSeparatorRow(ctx, width, '-');
        VersionIterator iter = n.getVersionHistory().getAllVersions();
        while (iter.hasNext()) {
            Version v = iter.nextVersion();
            Collection row = new ArrayList();
            row.add(v.getName());
            row.add(Arrays.asList(n.getVersionHistory().getVersionLabels(v)));
            PrintHelper.printRow(ctx, width, row);
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
     * @param pathKey
     *        the path key to set
     */
    public void setPathKey(String pathKey) {
        this.pathKey = pathKey;
    }

}
