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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Displays references to the given Node
 */
public class LsReferences extends AbstractInfoCommand
{
    /** path to the node */
    private String path;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        Node n = CtxHelper.getNode(ctx, path);

        // header
        int[] width = new int[]
        {
            60
        };
        String[] header = new String[]
        {
            bundle.getString("path")
        };

        // print header
        PrintHelper.printRow(ctx, width, header);

        // print separator
        PrintHelper.printSeparatorRow(ctx, width, '-');

        PropertyIterator iter = n.getReferences();
        while (iter.hasNext())
        {
            Property p = iter.nextProperty();
            // print header
            PrintHelper.printRow(ctx, width, new String[]
            {
                p.getPath()
            });
        }

        CtxHelper.getOutput(ctx).println();
        CtxHelper.getOutput(ctx).println(
            iter.getSize() + " " + bundle.getString("references"));

        return false;
    }

    /**
     * @return Returns the path.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * @param path
     *            The path to set.
     */
    public void setPath(String path)
    {
        this.path = path;
    }
}
