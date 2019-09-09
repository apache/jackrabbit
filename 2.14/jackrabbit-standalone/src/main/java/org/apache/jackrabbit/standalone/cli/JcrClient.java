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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.History;
import jline.SimpleCompletor;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;
import org.apache.jackrabbit.util.ChildrenCollectorFilter;

/**
 * Command line interface client
 */
public class JcrClient {
    /** logger */
    private static Log log = LogFactory.getLog(JcrClient.class);

    /** Resource bundle */
    private ResourceBundle bundle = CommandHelper.getBundle();

    /** exit control variable */
    private boolean exit = false;

    /** Execution context */
    private Context ctx;

    /** run options */
    private Options options;

    /**
     * Constructor
     */
    JcrClient() {
        super();
        ctx = new ContextBase();
        initOptions();
        initContext();
    }

    /**
     * Constructor
     * @param ctx
     *        the <code>Context</code>
     */
    public JcrClient(Context ctx) {
        super();
        this.ctx = ctx;
    }

    /**
     * @param args
     *        the arguments
     */
    public static void main(String[] args) {
        JcrClient client = new JcrClient();
        client.run(args);
    }

    /**
     * Run client
     * @param args
     *        the arguments
     */
    private void run(String[] args) {
        try {
            // parse arguments
            Parser parser = new BasicParser();
            org.apache.commons.cli.CommandLine cl = parser.parse(options, args);

            // Set locale
            this.setLocale(cl);

            // Welcome message
            System.out.println(bundle.getString("word.welcome"));

            // check interactive mode
            if (cl.hasOption("source")) {
                this.runNonInteractive(cl);
            } else {
                this.runInteractive();
            }
        } catch (Exception e) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("jcrclient", options);
            e.printStackTrace();
            return;
        }
    }
    
    /**
     * jline ConsoleReader tab completor that completes on the children of the
     * current jcr node (both nodes and properties).
     * 
     * @author <a href="mailto:alexander(dot)klimetschek(at)mindquarry(dot)com">
     *         Alexander Klimetschek</a>
     *
     */
    private class JcrChildrenCompletor implements Completor {

        public int complete(String buffer, int cursor, List clist) {
            String start = (buffer == null) ? "" : buffer;
            
            Node node;
            try {
                node = CommandHelper.getNode(ctx, ".");
                Collection items = new ArrayList();
                ChildrenCollectorFilter collector = new ChildrenCollectorFilter(
                    "*", items, true, true, 1);
                collector.visit(node);
                for (Object item : items) {
                    String can = ((Item) item).getName();
                    if (can.startsWith(start)) {
                        clist.add(can);
                    }
                }
                
                return 0;
            } catch (CommandException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            
            return -1;
        }
        
    }

    /**
     * Run in interactive mode
     * @throws Exception
     *         if an Exception occurs
     */
    public void runInteractive() throws Exception {
        // built jline console reader with history + tab completion
        ConsoleReader reader = new ConsoleReader();
        reader.setHistory(new History());
        reader.setUseHistory(true);
        
        // get all available commands for command tab completion
        Collection<CommandLine> commands =
            CommandLineFactory.getInstance().getCommandLines();
        List<String> commandNames = new ArrayList<String>();
        for (CommandLine c : commands) {
            commandNames.add(c.getName());
            for (Object alias : c.getAlias()) {
                commandNames.add((String) alias);
            }
        }
        commandNames.add("exit");
        commandNames.add("quit");
        
        // first part is the command, then all arguments will get children tab completion
        reader.addCompletor(new ArgumentCompletor( new Completor[] {
                new SimpleCompletor(commandNames.toArray(new String[] {} )),
                new JcrChildrenCompletor()
        }));
        
        while (!exit) {
            try {
                String input = reader.readLine("[" + this.getPrompt() + "] > ");
                if (input == null) {
                    input = "exit";
                } else {
                    input = input.trim();
                }
                log.debug("running: " + input);
                if (input.equals("exit") || input.equals("quit")) { // exit?
                    exit = true;
                    System.out.println("Good bye...");
                } else if (input.length() > 0) {
                    this.runCommand(input);
                }
            } catch (JcrParserException e) {
                System.out.println(e.getLocalizedMessage());
                System.out.println();
            } catch (Exception e) {
                handleException(reader, e);
            }
        }
    }

    /**
     * Run in non interactive mode
     * @param cl
     *        the <code>CommandLine</code>
     * @throws Exception
     *         if an <code>Exception</code> occurs while running the
     *         <code>Command</code>
     */
    private void runNonInteractive(org.apache.commons.cli.CommandLine cl) throws Exception {
        this.runCommand("source " + cl.getOptionValue("source"));
    }

    /**
     * Parses the input and runs the specified command
     * @param input
     *        the user's input
     * @throws Exception
     *         if an <code>Exception</code> occurs while running the
     *         <code>Command</code>
     */
    void runCommand(String input) throws Exception {
        if (input.startsWith("#") || input.length() == 0) {
            return;
        }

        // Process user input
        JcrParser parser = new JcrParser();
        parser.parse(input);

        // populate ctx
        parser.populateContext(ctx);

        // Execute command
        long start = System.currentTimeMillis();
        parser.getCommand().execute(ctx);
        long elapsed = System.currentTimeMillis() - start;

        // depopulate ctx
        parser.depopulateContext(ctx);

        // Display elapsed timed
        System.out.println();
        System.out.println(bundle.getString("phrase.elapsedtime") + ": "
                + elapsed + " ms.");
        System.out.println();
    }

    /**
     * Handle the Exception. <br>
     * Shows a short message and prompt the user to show the entire stacktrace.
     * @param ex
     *        the <code>Exception</code> to handle
     */
    private void handleException(ConsoleReader cr, Exception ex) {
        System.out.println();
        System.out.println(bundle.getString("exception.occurred"));
        System.out.println();
        System.out.println(bundle.getString("exception") + ": "
                + ex.getClass().getName());
        System.out.println(bundle.getString("word.message") + ": "
                + ex.getLocalizedMessage());
        System.out.println();
        String prompt = bundle.getString("phrase.display.stacktrace")
                + "? [y/n]";

        String str = "";
        int tries = 0;
        while (!str.equals("y") && !str.equals("n") && tries < 3) {
            tries++;

            try {
                str = cr.readLine(prompt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (str.equals("y")) {
            ex.printStackTrace();
        }
    }

    /**
     * Prompt message
     * @return prompt the prompt message
     * @throws RepositoryException
     *         if the current <code>Repository</code> throws a
     *         <code>RepositoryException</code>
     */
    private String getPrompt() throws RepositoryException {

        try {
            CommandHelper.getRepository(ctx);
        } catch (CommandException e) {
            return bundle.getString("phrase.not.connected");
        }

        boolean unsaved = false;
        try {
            unsaved = CommandHelper.getSession(ctx).hasPendingChanges();
        } catch (CommandException e) {
            return bundle.getString("phrase.not.logged.in");
        }

        try {
            Node n = CommandHelper.getCurrentNode(ctx);
            // the current node might be Invalid
            String path;
            try {
                path = n.getPath();
            } catch (InvalidItemStateException e) {
                CommandHelper.setCurrentNode(ctx, CommandHelper.getSession(ctx)
                    .getRootNode());
                path = CommandHelper.getCurrentNode(ctx).getPath();
            }
            if (unsaved) {
                return path + "*";
            } else {
                return path;
            }
        } catch (CommandException e) {
            return bundle.getString("phrase.not.logged.in");
        }

    }

    /**
     * Init allowed CommandLine options
     */
    private void initOptions() {
        options = new Options();
        options.addOption("lang", "code", true, "Language code");
        options.addOption("country", "code", true, "Country code");
        options.addOption("source", "path", true,
            "Script for noninteractive mode");
    }

    /**
     * Sets the default Locale for the given CommandLine
     * @param cl
     *        the CLI <code>CommandLine</code>
     * @throws ParseException
     *         if cl can't be parsed
     */
    private void setLocale(org.apache.commons.cli.CommandLine cl) throws ParseException {
        Locale locale = null;
        if (cl.hasOption("lang") && cl.hasOption("country")) {
            locale = new Locale(cl.getOptionValue("lang"), cl
                .getOptionValue("country"));
        }
        if (cl.hasOption("lang") && !cl.hasOption("country")) {
            locale = new Locale(cl.getOptionValue("lang"));
        }
        if (locale != null) {
            Locale.setDefault(locale);
        }
    }

    /**
     * Init context. <br>
     * Sets the Context Output to the console
     */
    private void initContext() {
        CommandHelper.setOutput(ctx, new PrintWriter(System.out, true));
    }

}
