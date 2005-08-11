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
package org.apache.jackrabbit.chain.cli;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User's Input Parser. <br>
 * 
 * <ul>
 * <li>Lookup the CommandLine.</li>
 * <li>Populate CommandLine</li>
 * <li>Validate CommandLine</li>
 * <li>Get a Chain Command either from the the Catalog or by creating a new
 * Instance.</li>
 * </ul>
 * 
 */
public class JcrParser
{
    /** parser */
    private static Log log = LogFactory.getLog(JcrParser.class);

    static
    {
        try
        {
            ConfigParser parser = new ConfigParser();
            parser.parse(JcrParser.class.getResource("chains.xml"));
        } catch (Exception e)
        {
            e.printStackTrace();
            log.error(e);
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
    public JcrParser()
    {
        super();
    }

    /**
     * Parse the user's input.
     * 
     * @param input
     * @return a Command
     * @throws JcrParserException
     *             if the input is illegal
     * @throws ConfigurationException
     *             if the mapped command can't be mapped to a Commons Chain
     *             Command
     */
    public void parse(String input) throws JcrParserException,
            ConfigurationException
    {
        this.cl = null;
        this.cmd = null;

        // Validate input
        if (input == null || input.length() == 0)
        {
            throw new JcrParserException("parse.input.empty");
        }

        // Extract arguments
        LinkedList args = this.getArguments(input);

        // The first arg is the command name
        String cmdName = (String) args.getFirst();
        args.removeFirst();

        // parse command line
        cl = CommandLineFactory.getInstance().getCommandLine(cmdName);

        // populate with params
        populate(cl, args);

        // populate with params
        validate(cl);

        // Create Chain Command
        createCommand();

    }

    /**
     * If the CommandLine specifies an implementation a new Command instance
     * will created an populated with the given parameters. If the CommandLine
     * doesn't specify an implementation the parser will lookup the Command in
     * the Catalog, and the attributes will be used to populate the Context.
     * 
     * @throws ConfigurationException
     *             if the Command can not be instantiated or found in the
     *             Catalog
     */
    private void createCommand() throws ConfigurationException
    {
        if (cl.getImpl() != null)
        {
            try
            {
                log.debug("create command instance " + cl.getName());
                // Get command
                cmd = (Command) Class.forName(cl.getImpl()).newInstance();

                Map attrs = BeanUtils.describe(cmd);

                Iterator iter = cl.getAllParameters();

                while (iter.hasNext())
                {
                    AbstractParameter param = (AbstractParameter) iter.next();

                    // Command attribute
                    String commandAttr = param.getCommandAttribute();
                    if (commandAttr == null)
                    {
                        commandAttr = param.getName();
                    }

                    // Check that the Command has the attribute
                    if (!attrs.containsKey(commandAttr))
                    {
                        throw new ConfigurationException(
                            "no.attribute.for.name", new String[]
                            {
                                    param.getName(), cmd.getClass().getName()
                            });
                    }

                    BeanUtils.setProperty(cmd, commandAttr, param.getValue());
                }

            } catch (Exception e)
            {
                throw new ConfigurationException("parse.instantiate.command",
                    e, new String[]
                    {
                        cl.getImpl()
                    });
            }
        } else
        {
            log.debug("lookup command " + cl.getName() + "in default catalog");
            cmd = catalog.getCommand(cl.getName());
            if (cmd == null)
            {
                throw new ConfigurationException(
                    "no.command.in.catalog.for.name", new String[]
                    {
                        cl.getName()
                    });
            }
        }

    }

    /**
     * Tokenize user's input
     * 
     * @param input
     * @return
     */
    private LinkedList getArguments(String input)
    {
        LinkedList args = new LinkedList();
        int length = input.length();

        boolean exit = false;
        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;
        int escape = -1;

        StringBuffer arg = new StringBuffer();

        for (int i = 0; i < length; ++i)
        {
            char c = input.charAt(i);

            // end of argument?
            if ((!insideSingleQuote & !insideDoubleQuote & Character
                .isWhitespace(c)))
            {
                if (arg.toString().trim().length() > 0)
                {
                    args.add(arg.toString().trim());
                    arg = new StringBuffer();
                }
                continue;
            }

            if (i == escape)
            { // escaped char
                arg.append(c);
            } else
            { // unescaped char
                switch (c)
                {
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

        if (arg.toString().trim().length() > 0)
        {
            args.add(arg.toString());
        }

        return args;
    }

    /**
     * Populate the context with the attributes needed by the Command
     */
    public void populateContext(Context ctx)
    {
        if (cl.getImpl() == null)
        {
            Iterator iter = cl.getAllParameters();
            while (iter.hasNext())
            {
                AbstractParameter param = (AbstractParameter) iter.next();
                log.debug("add ctx attr: " + param.getName() + "="
                        + param.getValue());
                ctx.put(param.getName(), param.getValue());
            }
        }
    }

    /**
     * Remove context attribute specific to the parsed command
     */
    public void dePopulateContext(Context ctx)
    {
        if (cl.getImpl() == null)
        {
            Iterator iter = cl.getAllParameters();
            while (iter.hasNext())
            {
                AbstractParameter param = (AbstractParameter) iter.next();
                log.debug("remove ctx attr: " + param.getName() + "="
                        + param.getValue());
                ctx.remove(param.getName());
            }
        }
    }

    /**
     * @return the Command
     */
    public Command getCommand()
    {
        return cmd;
    }

    /**
     * Populate the CommandLine with the given parameters
     * 
     * @param cl
     * @param values
     * @throws JcrParserException
     */
    private void populate(CommandLine cl, List valList)
            throws JcrParserException
    {
        String[] values = (String[]) valList
            .toArray(new String[valList.size()]);

        // Command Line parameters
        Map options = cl.getOptions();
        Map flags = cl.getFlags();
        Map clArgs = cl.getArguments();

        // Input arguments
        List args = new ArrayList();

        for (int i = 0; i < values.length; i++)
        {
            String value = values[i];

            if (value.startsWith("-"))
            {
                // option
                if (i + 1 < values.length && !values[i + 1].startsWith("-"))
                {
                    Option opt = (Option) options.get(value.substring(1));
                    if (opt == null)
                    {
                        throw new JcrParserException("no.opt.for.name",
                            new String[]
                            {
                                value.substring(1)
                            });
                    }
                    opt.setValue(values[i + 1]);
                    i++;
                } else
                {
                    // flag
                    Flag flag = (Flag) flags.get(value.substring(1));
                    if (flag == null)
                    {
                        throw new JcrParserException("no.flag.for.name",
                            new String[]
                            {
                                value
                            });
                    }
                    flag.setPresent(true);
                }
            } else
            {
                // collect arguments
                args.add(value);
            }
        }

        // set arguments
        String[] argValues = (String[]) args.toArray(new String[args.size()]);
        for (int j = 0; j < argValues.length; j++)
        {
            Argument arg = (Argument) clArgs.get(new Integer(j));
            if (arg == null)
            {
                throw new JcrParserException("more.arguments.than.expected");
            }
            arg.setValue(argValues[j]);
        }

    }

    /**
     * Validate the CommandLine.
     * 
     * @param cl
     * @throws JcrParserException
     */
    private void validate(CommandLine cl) throws JcrParserException
    {
        Iterator iter = cl.getRequiredParameters();
        while (iter.hasNext())
        {
            AbstractParameter param = (AbstractParameter) iter.next();
            if (param.getValue() == null)
            {
                throw new JcrParserException("missing.paramater", new String[]
                {
                    param.getName()
                });
            }
        }
    }

}
