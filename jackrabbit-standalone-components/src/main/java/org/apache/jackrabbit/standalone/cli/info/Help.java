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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.cli.HelpFormatter;
import org.apache.jackrabbit.standalone.cli.AbstractParameter;
import org.apache.jackrabbit.standalone.cli.Argument;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;
import org.apache.jackrabbit.standalone.cli.CommandLine;
import org.apache.jackrabbit.standalone.cli.CommandLineFactory;
import org.apache.jackrabbit.standalone.cli.Flag;
import org.apache.jackrabbit.standalone.cli.Option;

/**
 * Show available <code>Command</code>s. If a <code>Command</code> is
 * specified it will show its description, usage and parameters.
 */
public class Help implements Command {
    /** bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** Command factory */
    private CommandLineFactory factory = CommandLineFactory.getInstance();

    /** Help formatter */
    private HelpFormatter hf = new HelpFormatter();

    /** command key */
    private String commandKey = "command";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String command = (String) ctx.get(this.commandKey);
        PrintWriter out = CommandHelper.getOutput(ctx);
        out.println();
        if (command == null) {
            helpAll(ctx);
        } else {
            helpCommand(ctx);
        }
        return false;
    }

    /**
     * Writes help for all the commands
     * @param ctx
     *        the current working <code>Context</code>
     * @throws CommandException
     */
    private void helpAll(Context ctx) throws CommandException {
        PrintWriter out = CommandHelper.getOutput(ctx);
        Collection descriptors = factory.getCommandLines();
        Iterator iter = descriptors.iterator();

        // Tab position
        int tabPos = 20;
        while (iter.hasNext()) {
            CommandLine desc = (CommandLine) iter.next();
            if (desc.getName().length() > tabPos) {
                tabPos = desc.getName().length() + 1;
            }
        }

        iter = descriptors.iterator();
        while (iter.hasNext()) {
            CommandLine desc = (CommandLine) iter.next();
            StringBuffer buf = new StringBuffer(desc.getName());
            buf.setLength(tabPos);
            for (int i = desc.getName().length(); i < buf.length(); i++) {
                buf.setCharAt(i, ' ');
            }
            buf.append(desc.getLocalizedDescription());
            hf.printWrapped(out, 70, tabPos, buf.toString());
        }
    }

    /**
     * Writes detailed help for the given command
     * @param ctx
     *        the current working <code>Context</code>
     * @throws CommandException
     */
    private void helpCommand(Context ctx) throws CommandException {
        PrintWriter out = CommandHelper.getOutput(ctx);

        String cmdName = (String) ctx.get(this.commandKey);

        CommandLine desc = factory.getCommandLine(cmdName);

        out.println(getString(bundle, "word.description") + ": ");
        out.println(desc.getLocalizedDescription());
        out.println();

        // Usage
        out.print(getString(bundle, "word.usage") + ":");
        out.print(desc.getName() + " ");

        // Arguments
        Iterator iter = desc.getArguments().values().iterator();
        while (iter.hasNext()) {
            Argument arg = (Argument) iter.next();
            out.print("<" + arg.getLocalizedArgName() + "> ");
        }

        // Options
        iter = desc.getOptions().values().iterator();
        while (iter.hasNext()) {
            Option arg = (Option) iter.next();
            out.print("-" + arg.getName() + " <" + arg.getLocalizedArgName()
                    + "> ");
        }

        // flags
        iter = desc.getFlags().values().iterator();
        while (iter.hasNext()) {
            Flag arg = (Flag) iter.next();
            out.print("-" + arg.getName() + " ");
        }
        out.println();

        // Alias
        if (desc.getAlias().size() > 0) {
            out.print(getString(bundle, "word.alias") + ":");
            iter = desc.getAlias().iterator();
            while (iter.hasNext()) {
                out.print((String) iter.next() + " ");

            }
            out.println();
        }
        out.println();

        // Arguments details
        if (desc.getArguments().size() > 0) {
            out.println("<" + getString(bundle, "word.arguments") + ">");
            printParam(ctx, desc.getArguments().values());
        }

        // Options details
        if (desc.getOptions().values().size() > 0) {
            out.println();
            out.println("<" + getString(bundle, "word.options") + ">");
            printParam(ctx, desc.getOptions().values());
        }

        // flag details
        if (desc.getFlags().values().size() > 0) {
            out.println();
            out.println("<" + getString(bundle, "word.flags") + ">");
            printParam(ctx, desc.getFlags().values());
        }

    }

    /**
     * @param ctx
     *        the current working <code>Context</code>
     * @param params
     *        the parameters
     */
    private void printParam(Context ctx, Collection params) {
        int[] width = new int[] {
                10, 10, 10, 40
        };

        String[] header = new String[] {
                getString(bundle, "word.name"),
                getString(bundle, "word.argument"),
                getString(bundle, "word.required"),
                getString(bundle, "word.description")
        };

        PrintHelper.printRow(ctx, width, header);
        PrintHelper.printSeparatorRow(ctx, width, '-');

        Iterator iter = params.iterator();
        while (iter.hasNext()) {
            AbstractParameter p = (AbstractParameter) iter.next();
            String[] item = new String[] {
                    p.getName(), p.getLocalizedArgName(),
                    Boolean.toString(p.isRequired()),
                    p.getLocalizedDescription()
            };
            PrintHelper.printRow(ctx, width, item);
        }

    }

    /**
     * @return the command key
     */
    public String getCommandKey() {
        return commandKey;
    }

    /**
     * @param commandKey
     *        the command key to set
     */
    public void setCommandKey(String commandKey) {
        this.commandKey = commandKey;
    }
    
    private String getString(ResourceBundle bundle, String key) {
        try {
            return bundle.getString(key) ;
        } catch (MissingResourceException e) {
            return "not available";
        }
          
    }
}
