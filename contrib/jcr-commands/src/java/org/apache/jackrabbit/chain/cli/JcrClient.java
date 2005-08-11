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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

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
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Command line interface client
 */
public class JcrClient
{
    private static Log log = LogFactory.getLog(JcrClient.class);

    /** Resource bundle */
    private ResourceBundle bundle = ResourceBundle.getBundle(this.getClass()
        .getPackage().getName()
            + ".resources");

    /** exit control variable */
    private boolean exit = false;

    /** Execution context */
    private Context ctx = new ContextBase();

    /** run options */
    private Options options;

    /**
     * Constructor
     */
    private JcrClient()
    {
        super();
        initOptions();
        initContext();
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        JcrClient client = new JcrClient();
        client.run(args);
    }

    /**
     * Run client
     * 
     * @param args
     */
    private void run(String[] args)
    {
        try
        {
            // parse arguments
            Parser parser = new BasicParser();
            CommandLine cl = parser.parse(options, args);

            // Set locale
            this.setLocale(cl);

            // Welcome message
            System.out.println(bundle.getString("welcome"));
        } catch (Exception e)
        {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("jcrclient", options);
            e.printStackTrace();
            return;
        }

        // Prompt command
        while (!exit)
        {
            try
            {
                System.out.print(this.getPrompt() + ">");
                // Read input
                BufferedReader br = new BufferedReader(new InputStreamReader(
                    System.in));
                String input = br.readLine();
                if (input.trim().equals("exit") || input.trim().equals("quit"))
                { // exit?
                    exit = true;
                    System.out.println("Good bye..");
                } else if (input.trim().length() == 0)
                {
                    // Do nothing
                } else
                {
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
                    parser.dePopulateContext(ctx);

                    // Display elapsed timed
                    System.out.println();
                    System.out.println(bundle.getString("elapsedtime") + ": "
                            + elapsed + " ms.");
                    System.out.println();
                }
            } catch (JcrParserException e)
            {
                System.out.println(e.getLocalizedMessage());
                System.out.println();
            } catch (Exception e)
            {
                handleException(e);
            }
        }

    }

    /**
     * Handle the Exception. <br>
     * Shows a short message and prompt the user to show the entire stacktrace.
     * 
     * @param e
     */
    private void handleException(Exception ex)
    {
        System.out.println();
        System.out.println(bundle.getString("exception.occurred"));
        System.out.println();
        System.out.println(bundle.getString("exception") + ": "
                + ex.getClass().getName());
        System.out.println(bundle.getString("message") + ": "
                + ex.getLocalizedMessage());
        System.out.println();
        String prompt = bundle.getString("prompt.display.stacktrace");
        int counter = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String str = "";
        int tries = 0;
        while (!str.equals("y") && !str.equals("n") && tries < 3)
        {
            tries++;
            System.out.print(prompt);
            try
            {
                str = in.readLine();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        if (str.equals("y"))
        {
            ex.printStackTrace();
        }
    }

    /**
     * Prompt message
     * 
     * @return prompt
     * @throws Exception
     */
    private String getPrompt() throws Exception
    {

        Repository r = CtxHelper.getRepository(ctx);
        if (r == null)
        {
            return bundle.getString("not.connected");
        }

        Session s = CtxHelper.getSession(ctx);
        if (s == null)
        {
            return bundle.getString("not.logged.in");
        }

        Node n = CtxHelper.getCurrentNode(ctx);

        return n.getPath();
    }

    /**
     * Init allowed CommandLine options
     */
    private void initOptions()
    {
        options = new Options();
        options.addOption("lang", "code", true, "Language code");
        options.addOption("country", "code", true, "Country code");
    }

    /**
     * Sets the default Locale for the given CommandLine
     * 
     * @param cl
     * @throws ParseException
     */
    private void setLocale(CommandLine cl) throws ParseException
    {
        Locale locale = null;
        if (cl.hasOption("lang") && cl.hasOption("country"))
        {
            locale = new Locale(cl.getOptionValue("lang"), cl
                .getOptionValue("country"));
        }
        if (cl.hasOption("lang") && !cl.hasOption("country"))
        {
            locale = new Locale(cl.getOptionValue("lang"));
        }
        if (locale != null)
        {
            Locale.setDefault(locale);
        }
    }

    /**
     * Init context. <br>
     * Sets the Context Output to the console
     */
    private void initContext()
    {
        CtxHelper.setOutput(ctx, new PrintWriter(System.out, true));
    }

}
