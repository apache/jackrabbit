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
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * List properties superclass
 */
public abstract class AbstractLsProperties extends AbstractLs {

    /** bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** length of length field */
    private static final int LENGTH_LENGTH = 8;

    /**
     * {@inheritDoc}
     */
    public final boolean execute(Context ctx) throws Exception {
        int[] width = new int[] {
                30, longWidth, longWidth, LENGTH_LENGTH, 18
        };

        String header[] = new String[] {
                bundle.getString("word.name"),
                bundle.getString("word.multiple"),
                bundle.getString("word.type"), bundle.getString("word.length"),
                bundle.getString("word.preview")
        };

        PrintHelper.printRow(ctx, width, header);
        PrintHelper.printSeparatorRow(ctx, width, '-');

        int index = 0;
        Iterator iter = getProperties(ctx);

        int maxItems = getMaxItems(ctx);

        while (iter.hasNext() && index < maxItems) {
            Property p = (Property) iter.next();

            long length = 0;

            if (p.getDefinition().isMultiple()) {
                long[] lengths = p.getLengths();
                for (int i = 0; i < lengths.length; i++) {
                    length += lengths[i];
                }
            } else {
                length = p.getLength();
            }

            String multiple = Boolean.toString(p.getDefinition().isMultiple());
            if (p.getDefinition().isMultiple()) {
                multiple += "[" + p.getValues().length + "]";
            }

            Collection row = new ArrayList();
            row.add(p.getName());
            row.add(multiple);
            row.add(PropertyType.nameFromValue(p.getType()));
            row.add(Long.toString(length));
            // preview
            if (p.getDefinition().isMultiple()) {
                row.add(this.getMultiplePreview(p));
            } else {
                row.add(this.getPreview(p));
            }

            PrintHelper.printRow(ctx, width, row);
            index++;
        }

        CommandHelper.getOutput(ctx).println();

        // Write footer
        printFooter(ctx, iter);

        return false;
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @return collected <code>Property</code> s to display
     * @throws Exception
     *         if the <code>Property</code> s can't be retrieved
     */
    protected abstract Iterator getProperties(Context ctx) throws Exception;

    /**
     * @param property
     * @return the first 50 characters of single value properties
     * @throws RepositoryException
     */
    private String getPreview(Property p) throws RepositoryException {
        String value = p.getValue().getString();
        return value.substring(0, Math.min(value.length(), 50));
    }

    /**
     * @param property
     * @return a <code>Collection</code> in which element contains the first
     *         50 characters of the <code>Value</code>'s string
     *         representation
     * @throws RepositoryException
     * @throws ValueFormatException
     */
    private Collection getMultiplePreview(Property p)
            throws ValueFormatException, RepositoryException {
        Collection c = new ArrayList();
        Value[] values = p.getValues();
        for (int i = 0; i < values.length; i++) {
            try {
                String value = values[i].getString();
                c.add(value.substring(0, Math.min(value.length(), 50)));
            } catch (ValueFormatException e) {
                c.add(bundle.getString("phrase.notavailable"));
            }
        }
        return c;
    }
}
