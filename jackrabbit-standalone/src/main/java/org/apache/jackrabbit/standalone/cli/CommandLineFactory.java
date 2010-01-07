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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;
import org.xml.sax.SAXException;

/**
 * Command line factory
 */
public class CommandLineFactory {
    /** logger */
    private static Log log = LogFactory.getLog(CommandLineFactory.class);

    /** resource bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** file name */
    private static final String COMMAND_LINE_FILE = "command-line.xml";

    /** rules file name */
    private static final String COMMAND_LINE_RULES_FILE = "command-line-rules.xml";

    /** singleton */
    private static CommandLineFactory singleton;

    /** command cache */
    private Map cache = new TreeMap();

    /** alias cache */
    private Map alias = new HashMap();

    /**
     * private constructor
     */
    private CommandLineFactory() {
        super();
    }

    /**
     * @return singleton
     */
    public static CommandLineFactory getInstance() {
        if (singleton == null) {
            try {
                CommandLineFactory factory = new CommandLineFactory();
                factory.init();
                singleton = factory;
            } catch (Exception e) {
                log.error(bundle.getString("exception.unabletoinit"), e);
                e.printStackTrace();
            }
        }
        return singleton;
    }

    /**
     * @return all registered <code>Command</code> s
     */
    public Collection getCommandLines() {
        List cls = new ArrayList();
        Iterator iter = cache.values().iterator();
        while (iter.hasNext()) {
            CommandLine cl = (CommandLine) iter.next();
            cls.add(cl.clone());
        }
        return cls;
    }

    /**
     * Get the <code>Command</code> for the given name
     * @param name
     *        the <code>Command</code> name
     * @return a new Command Line Instance for the given command name
     * @throws JcrParserException
     *         if there's no <code>Command</code> for the given name
     */
    public CommandLine getCommandLine(String name) throws JcrParserException {
        log.debug("lookup command " + name);
        // get Command line
        CommandLine original = (CommandLine) cache.get(name);

        if (original == null) {
            log.debug("lookup alias " + name);
            original = (CommandLine) alias.get(name);
        }

        if (original == null) {
            log.warn("command not found " + name);
            throw new JcrParserException("exception.no.command.for.name",
                new String[] {
                    name
                });
        }

        // Return a clone
        return (CommandLine) original.clone();
    }

    /**
     * parses the configuration file
     * @throws ConfigurationException
     *         an <code>Exception</code> occurs while parsing
     */
    private void init() throws ConfigurationException {
        try {
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
            while (iter.hasNext()) {
                CommandLine cl = (CommandLine) iter.next();
                cache.put(cl.getName(), cl);
                // Add to alias cache
                Iterator aliasIt = cl.getAlias().iterator();
                while (aliasIt.hasNext()) {
                    String aliasName = (String) aliasIt.next();
                    if (alias.containsKey(aliasName)) {
                        throw new ConfigurationException(
                            "exception.alias.already.in.use", new String[] {
                                    aliasName, cl.getName()
                            });
                    }
                    alias.put(aliasName, cl);
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException(e.getLocalizedMessage());
        } catch (SAXException e) {
            throw new ConfigurationException(e.getLocalizedMessage());
        }
    }

}
