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
package org.apache.jackrabbit.standalone.cli;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Input Parser
 */
public class JcrParser {
    /** parser */
    private static Log log = LogFactory.getLog(JcrParser.class);

    static {
        try {
            ConfigParser parser = new ConfigParser();
            parser.parse(JcrParser.class.getResource("command.xml"));
        } catch (Exception e) {
            log.error("Failed to parse command.xml", e);
        }
    }

    /** catalog */
    private Catalog catalog = CatalogFactoryBase.getInstance().getCatalog();

    /** Command */
    private Command cmd;

    /** Command Line */
    private CommandLine cl;

    /**
     * Constructor
     */
    public JcrParser() {
        super();
    }

    /**
     * Parse the user's input.
     * @param input
     *        user's input
     * @throws JcrParserException
     *         if the input is illegal
     * @throws ConfigurationException
     *         if the mapped command can't be mapped to a Commons Chain Command
     */
    public void parse(String input) throws JcrParserException,
            ConfigurationException {
        this.cl = null;
        this.cmd = null;

        // Validate input
        if (input == null || input.length() == 0) {
            throw new JcrParserException("exception.parse.input.empty");
        }

        // Extract arguments
        LinkedList args = this.getArguments(input);

        // The first arg is the command name
        String cmdName = (String) args.getFirst();
        args.removeFirst();

        // Get the command line descriptor
        cl = CommandLineFactory.getInstance().getCommandLine(cmdName);

        // populate with the given params
        populate(cl, args);

        // validate the command line
        validate(cl);

        // Create Chain Command
        String impl = cl.getImpl();
        if (impl == null) {
            impl = cl.getName();
        }
        cmd = catalog.getCommand(impl);

        if (cmd == null) {
            throw new JcrParserException("no chain command for name " + impl);
        }

    }

    /**
     * Tokenize user's input
     * @param input
     *        the user's input
     * @return a <code>List</code> containing the arguments
     */
    private LinkedList getArguments(String input) {
        LinkedList args = new LinkedList();
        int length = input.length();

        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;
        int escape = -1;

        StringBuffer arg = new StringBuffer();

        for (int i = 0; i < length; ++i) {
            char c = input.charAt(i);

            // end of argument?
            if ((!insideSingleQuote && !insideDoubleQuote && Character
                .isWhitespace(c))) {
                if (arg.toString().trim().length() > 0) {
                    args.add(arg.toString().trim());
                    arg = new StringBuffer();
                }
                continue;
            }

            if (i == escape) { // escaped char
                arg.append(c);
            } else { // unescaped char
                switch (c) {
                case '\\':
                    escape = i + 1;
                    break;
                case '"':
                    insideDoubleQuote = !insideDoubleQuote;
                    break;
                case '\'':
                    insideSingleQuote = !insideSingleQuote;
                    break;
                default:
                    arg.append(c);
                    break;
                }
            }
        }

        if (arg.toString().trim().length() > 0) {
            args.add(arg.toString());
        }

        return args;
    }

    /**
     * Populate the <code>Context</code> with the attributes needed by the
     * <code>Command</code>
     * @param ctx
     *        the <code>Context</code>
     */
    public void populateContext(Context ctx) {
        Iterator iter = cl.getAllParameters();
        while (iter.hasNext()) {
            AbstractParameter param = (AbstractParameter) iter.next();
            log.debug("add ctx attr: " + param.getContextKey() + "="
                    + param.getValue());
            ctx.put(param.getContextKey(), param.getValue());
        }
    }

    /**
     * Remove <code>Context</code> attribute specific to the parsed
     * <code>Command</code>
     * @param ctx
     *        the <code>Context</code>
     */
    public void depopulateContext(Context ctx) {
        Iterator iter = cl.getAllParameters();
        while (iter.hasNext()) {
            AbstractParameter param = (AbstractParameter) iter.next();
            String ctxKey = param.getContextKey();
            log.debug("remove ctx attr: " + ctxKey + "=" + param.getValue());
            ctx.remove(ctxKey);
        }
    }

    /**
     * @return the <code>Command</code>
     */
    public Command getCommand() {
        return cmd;
    }

    /**
     * Populate the <code>CommandLine</code> with the given parameters
     * @param cl
     *        the <code>CommandLine</code>
     * @param valList
     *        the arguments
     * @throws JcrParserException
     *         if the user's input is illegal
     */
    private void populate(CommandLine cl, List valList)
            throws JcrParserException {
        String[] values = (String[]) valList
            .toArray(new String[valList.size()]);

        // Command Line parameters
        Map options = cl.getOptions();
        Map flags = cl.getFlags();
        Map clArgs = cl.getArguments();

        // Input arguments
        List args = new ArrayList();

        for (int i = 0; i < values.length; i++) {
            String value = values[i];

            if (value.startsWith("-")) {
                // option
                if (i + 1 < values.length && !values[i + 1].startsWith("-")) {
                    Option opt = (Option) options.get(value.substring(1));
                    if (opt == null) {
                        throw new JcrParserException("exception.no.opt.for.name",
                            new String[] {
                                value.substring(1)
                            });
                    }
                    opt.setValue(values[i + 1]);
                    i++;
                } else {
                    // flag
                    Flag flag = (Flag) flags.get(value.substring(1));
                    if (flag == null) {
                        throw new JcrParserException("exception.no.flag.for.name",
                            new String[] {
                                value
                            });
                    }
                    flag.setPresent(true);
                }
            } else {
                // collect arguments
                args.add(value);
            }
        }

        // set arguments
        String[] argValues = (String[]) args.toArray(new String[args.size()]);
        for (int j = 0; j < argValues.length; j++) {
            Argument arg = (Argument) clArgs.get(new Integer(j));
            if (arg == null) {
                throw new JcrParserException("exception.more.arguments.than.expected");
            }
            arg.setValue(argValues[j]);
        }

    }

    /**
     * Validate the <code>CommandLine</code>
     * @param cl
     *        the <code>CommandLine</code>
     * @throws JcrParserException
     *         if a required parameter is not present in the user's input
     */
    private void validate(CommandLine cl) throws JcrParserException {
        Iterator iter = cl.getRequiredParameters();
        while (iter.hasNext()) {
            AbstractParameter param = (AbstractParameter) iter.next();
            if (param.getValue() == null) {
                throw new JcrParserException("exception.missing.paramater", new String[] {
                    param.getName()
                });
            }
        }
    }

}
