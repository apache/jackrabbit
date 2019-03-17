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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Utility class for printing
 */
final class PrintHelper {

    /**
     * private constructor
     */
    private PrintHelper() {
        super();
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @param width
     *        the columns width
     * @param text
     *        the text
     */
    public static void printRow(Context ctx, int[] width, String[] text) {
        if (width.length != text.length) {
            throw new IllegalArgumentException(
                "width[] and text[] haven't the same length");
        }

        PrintWriter out = CommandHelper.getOutput(ctx);

        int rows = 1;

        // Calculate rows
        for (int i = 0; i < text.length; i++) {
            int textLength = text[i].length();
            if (textLength == 0) {
                textLength = 1;
            }
            int columnWidth = width[i];
            int neededRows = (int) Math.ceil((double) textLength
                    / (double) columnWidth);
            if (neededRows > rows) {
                rows = neededRows;
            }
        }

        // Write table
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < width.length; column++) {
                for (int pointer = 0; pointer < width[column]; pointer++) {
                    int pos = row * width[column] + pointer;
                    if (pos < text[column].length()) {
                        out.print(text[column].charAt(pos));
                    } else {
                        out.print(' ');
                    }
                }
                out.print(' ');
            }
            out.println();
        }
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @param width
     *        the column width
     * @param separator
     *        the separator chr
     */
    public static void printSeparatorRow(
        Context ctx,
        int[] width,
        char separator) {
        PrintWriter out = CommandHelper.getOutput(ctx);
        for (int i = 0; i < width.length; i++) {
            for (int j = 0; j <= width[i]; j++) {
                if (j < width[i]) {
                    out.print(separator);
                } else {
                    out.print(' ');
                }
            }
        }
        out.println();
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @param width
     *        the column width
     * @param texts
     *        the texts
     * @throws CommandException
     */
    public static void printRow(Context ctx, int[] width, Collection texts)
            throws CommandException {
        String[] text = new String[width.length];
        Iterator iter = texts.iterator();
        int column = 0;
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o == null) {
                text[column] = "";
            } else if (o instanceof String) {
                text[column] = (String) o;
            } else if (o instanceof Collection) {
                StringBuffer sb = new StringBuffer();
                Iterator i = ((Collection) o).iterator();
                while (i.hasNext()) {
                    String str = (String) i.next();
                    int rows = (int) Math.ceil((double) str.length()
                            / (double) width[column]);
                    if (rows == 0) {
                        rows = 1;
                    }
                    sb.append(str);
                    for (int j = 0; j < rows * width[column] - str.length(); j++) {
                        sb.append(' ');
                    }
                }
                text[column] = sb.toString();
            } else {
                throw new CommandException("exception.illegalargument");
            }
            column++;
        }
        printRow(ctx, width, text);
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @param widths
     *        the column width
     * @param texts
     *        the texts
     * @throws CommandException
     */
    public static void printRow(Context ctx, Collection widths, Collection texts)
            throws CommandException {
        printRow(ctx, convertWidth(widths), texts);
    }

    /**
     * @param widths
     *        the column width
     * @return the column width
     * @throws CommandException
     */
    private static int[] convertWidth(Collection widths)
            throws CommandException {
        int[] width = new int[widths.size()];
        int index = 0;
        Iterator iter = widths.iterator();
        while (iter.hasNext()) {
            Integer i = (Integer) iter.next();
            width[index] = i.intValue();
            index++;
        }
        return width;
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @param widths
     *        the columns widths
     * @param separator
     *        the separator char
     * @throws CommandException
     */
    public static void printSeparatorRow(
        Context ctx,
        Collection widths,
        char separator) throws CommandException {
        printSeparatorRow(ctx, convertWidth(widths), separator);
    }

}
