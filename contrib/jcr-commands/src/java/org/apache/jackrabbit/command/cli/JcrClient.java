/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.command.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandException;
import org.apache.jackrabbit.command.CommandHelper;

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
    JcrClient(Context ctx) {
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
            CommandLine cl = parser.parse(options, args);

            // Set locale
            this.setLocale(cl);

            // Welcome message
            System.out.println(bundle.getString("word.welcome"));

            // check interactive mode
            if (cl.hasOption("source")) {
                this.runNonInteractive(cl);
            } else {
                this.runInteractive(cl);
            }
        } catch (Exception e) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("jcrclient", options);
            e.printStackTrace();
            return;
        }
    }

    /**
     * Run in interactive mode
     * @param cl
     *        the <code>CommandLine</code>
     * @throws Exception
     *         if an Exception occurs
     */
    private void runInteractive(CommandLine cl) throws Exception {
        // Prompt command
        while (!exit) {
            try {
                System.out.print(this.getPrompt() + ">");
                // Read input
                BufferedReader br = new BufferedReader(new InputStreamReader(
                    System.in));
                String input = br.readLine();
                log.debug("running: " + input);
                if (input.trim().equals("exit") || input.trim().equals("quit")) { // exit?
                    exit = true;
                    System.out.println("Good bye..");
                } else if (input.trim().length() == 0) {
                    // Do nothing
                } else {
                    this.runCommand(input);
                }
            } catch (JcrParserException e) {
                System.out.println(e.getLocalizedMessage());
                System.out.println();
            } catch (Exception e) {
                handleException(e);
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
    private void runNonInteractive(CommandLine cl) throws Exception {
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
    private void handleException(Exception ex) {
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
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String str = "";
        int tries = 0;
        while (!str.equals("y") && !str.equals("n") && tries < 3) {
            tries++;
            System.out.print(prompt);
            try {
                str = in.readLine();
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

        try {
            CommandHelper.getSession(ctx);
        } catch (CommandException e) {
            return bundle.getString("phrase.not.logged.in");
        }

        try {
            Node n = CommandHelper.getCurrentNode(ctx);
            // the current node might be Invalid
            try {
                return n.getPath();
            } catch (InvalidItemStateException e) {
                CommandHelper.setCurrentNode(ctx, CommandHelper.getSession(ctx)
                    .getRootNode());
                return CommandHelper.getCurrentNode(ctx).getPath();
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
    private void setLocale(CommandLine cl) throws ParseException {
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
