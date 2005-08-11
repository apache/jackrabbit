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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.chain.JcrCommandException;
import org.xml.sax.SAXException;

/**
 * Command line factory.
 */
public class CommandLineFactory
{
    /** logger */
    private static Log log = LogFactory.getLog(CommandLineFactory.class);

    /** file name */
    private static String COMMAND_LINE_FILE = "command-line.xml";

    /** rules file name */
    private static String COMMAND_LINE_RULES_FILE = "command-line-rules.xml";

    /** singleton */
    private static CommandLineFactory singleton;

    /** command cache */
    private Map cache = new TreeMap();

    /** alias cache */
    private Map alias = new HashMap();

    /**
     * private Constructor
     */
    private CommandLineFactory()
    {
        super();
    }

    /**
     * @return CliCommandFactory singleton
     */
    public static CommandLineFactory getInstance()
    {
        if (singleton == null)
        {
            try
            {
                CommandLineFactory factory = new CommandLineFactory();
                factory.init();
                singleton = factory;
            } catch (Exception e)
            {
                log.error("unable to init CommandLineFactory", e);
                e.printStackTrace();
            }
        }
        return singleton;
    }

    /**
     * @return all registered commands
     * @throws JcrCommandException
     */
    public Collection getCommandLines() throws JcrCommandException
    {
        List cls = new ArrayList();
        Iterator iter = cache.values().iterator();
        while (iter.hasNext())
        {
            CommandLine cl = (CommandLine) iter.next();
            cls.add(cl.clone());
        }
        return cls;
    }

    /**
     * @param name
     * @return a new Command Line Instance for the given command name
     * @throws JcrParserException
     */
    public CommandLine getCommandLine(String name) throws JcrParserException
    {
        log.debug("lookup command " + name);
        // get Command line
        CommandLine original = (CommandLine) cache.get(name);

        if (original == null)
        {
            log.debug("lookup alias " + name);
            original = (CommandLine) alias.get(name);
        }

        if (original == null)
        {
            log.warn("command not found " + name);
            throw new JcrParserException("no.command.for.name", new String[]
            {
                name
            });
        }

        // Return a clone
        return (CommandLine) original.clone();
    }

    /**
     * Parse configuration file
     * 
     * @throws IOException
     * @throws SAXException
     * @throws ConfigurationException
     */
    private void init() throws IOException, SAXException,
            ConfigurationException
    {
        // Configure Digester from XML ruleset
        URL rulesFile = getClass().getResource(COMMAND_LINE_RULES_FILE);
        URL clFile = getClass().getResource(COMMAND_LINE_FILE);

        // init digester
        Digester digester = DigesterLoader.createDigester(rulesFile);

        // Push empty List onto Digester's Stack
        List cls = new ArrayList();
        digester.push(cls);

        // Parse the XML document
        InputStream input = clFile.openStream();
        digester.parse(input);
        input.close();

        // Add to cache
        Iterator iter = cls.iterator();
        while (iter.hasNext())
        {
            CommandLine cl = (CommandLine) iter.next();
            cache.put(cl.getName(), cl);
            // Add to alias cache
            Iterator aliasIt = cl.getAlias().iterator();
            while (aliasIt.hasNext())
            {
                String aliasName = (String) aliasIt.next();
                if (alias.containsKey(aliasName))
                {
                    throw new ConfigurationException("alias.already.in.use",
                        new String[]
                        {
                                aliasName, cl.getName()
                        });
                }
                alias.put(aliasName, cl);
            }
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        // TODO: remove this
        Collection c = CommandLineFactory.getInstance().getCommandLines();
        System.out.println(c.size());
    }

}
