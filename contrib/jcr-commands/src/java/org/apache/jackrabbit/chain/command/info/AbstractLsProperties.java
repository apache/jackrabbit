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

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * List properties superclass
 */
public abstract class AbstractLsProperties extends AbstractLs
{
    /** name width */
    private int nameWidth = 35;

    /** node type width */
    private int typeWidth = 15;

    /**
     * @inheritDoc
     */
    public final boolean execute(Context ctx) throws Exception
    {
        int[] width = new int[]
        {
                35, longWidth, longWidth, longWidth
        };

        String header[] = new String[]
        {
                bundle.getString("name"), bundle.getString("multiple"),
                bundle.getString("type"), bundle.getString("length")
        };

        PrintHelper.printRow(ctx, width, header);
        PrintHelper.printSeparatorRow(ctx, width, '-');

        int index = 0;
        Iterator iter = getProperties(ctx);

        int maxItems = getMaxItems(ctx) ;
        
        while (iter.hasNext() && index < maxItems)
        {
            Property p = (Property) iter.next();

            long length = 0;

            if (p.getDefinition().isMultiple())
            {
                long[] lengths = p.getLengths();
                for (int i = 0; i < lengths.length; i++)
                {
                    length += lengths[i];
                }
            } else
            {
                length = p.getLength();
            }

            String multiple = Boolean.toString(p.getDefinition().isMultiple());
            if (p.getDefinition().isMultiple())
            {
                multiple += "[" + p.getValues().length + "]";
            }

            String[] row = new String[]
            {
                    p.getName(), multiple,
                    PropertyType.nameFromValue(p.getType()),
                    Long.toString(length)
            };
            PrintHelper.printRow(ctx, width, row);
            index++;
        }

        CtxHelper.getOutput(ctx).println();

        // Write footer
        printFooter(ctx, (PropertyIterator) iter);

        return false;
    }

    /**
     * Subclasses are responsible of collecting the properties to display
     * 
     * @param ctx
     * @return
     */
    protected abstract Iterator getProperties(Context ctx) throws Exception;

}
