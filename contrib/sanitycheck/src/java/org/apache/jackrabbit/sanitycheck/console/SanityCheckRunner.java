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
package org.apache.jackrabbit.sanitycheck.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

import org.apache.jackrabbit.sanitycheck.SanityCheckException;
import org.apache.jackrabbit.sanitycheck.SanityCheckHelper;
import org.apache.jackrabbit.sanitycheck.inconsistency.NodeInconsistency;

/**
 * <p>
 * Command line Sanity Check Runner.
 * </p>
 * <p>
 * Arguments:
 * <ul>
 * <li>-repair prompts to repair the inconsistencies</li>
 * <li>-config=[repository.xml path]</li>
 * <li>-repository=[repository path]</li>
 * </ul>
 * 
 * </p>
 * 
 */
public class SanityCheckRunner
{
    private static final String REPAIR_OPTION = "-repair";

    private static final String CONFIG_OPTION = "-config";

    private static final String REPOSITORY_OPTION = "-repository";

    /** Runner */
    private static SanityCheckRunner runner = new SanityCheckRunner();

    /**
     * 
     * @param args
     * @throws SanityCheckException
     */
    public static void main(String[] args) throws Exception
    {
        runner.run(args);
    }

    /**
     * Run all the SanityChecks for the given workspaces
     * 
     * @param args
     * @throws Exception
     */
    private void run(String[] args) throws Exception
    {
        boolean repair = hasOption(args, REPAIR_OPTION);

        Collection inconsistencies = SanityCheckHelper.getInconsistencies(new File(getConfigPath(args)),
            getRepositoryPath(args));

        // Execute report
        this.report(inconsistencies);

        // Execute repair
        if (repair)
        {
            repair(inconsistencies);
        }

    }

    /**
     * Report the inconsistencies
     * 
     * @param log
     */
    private void report(Collection inconsistencies)
    {
        System.out.println("----- < REPORT STARTS > -----");
        int i = 0;
        Iterator iter = inconsistencies.iterator();
        while (iter.hasNext())
        {
            i++;
            NodeInconsistency inc = (NodeInconsistency) iter.next();
            System.out.println(i + " - " + inc.getDescription());
        }
        System.out.println("----- < REPORT ENDS > -----");
    }

    /**
     * Repair the inconsistencies
     * 
     * @param log
     * @throws SanityCheckException
     */
    private void repair(Collection inconsistencies) throws SanityCheckException
    {
        int i = 0;
        Iterator iter = inconsistencies.iterator();
        while (iter.hasNext())
        {
            i++;
            NodeInconsistency inc = (NodeInconsistency) iter.next();
            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String str = "";
                int tries = 0;
                while (!str.equals("y") && !str.equals("n") && tries < 3)
                {
                    tries++;
                    System.out.print(i + "> " + inc.getDescription() + ".\n do you want to fix it? [y/n] ");
                    str = in.readLine();
                }
                if (str.equals("y"))
                {
                    SanityCheckHelper.repair(inc);
                    System.out.println(i + "> [repaired]");
                } else
                {
                    System.out.println(i + ">  [ignored]");
                }
            } catch (IOException e)
            {
                throw new SanityCheckException("Unable to prompt.", e);
            }
        }
    }

    private String getConfigPath(String[] args)
    {
        String configPath = getArgument(args, CONFIG_OPTION);
        if (configPath == null)
        {
            throw new IllegalArgumentException("Set the config file. "
                    + CONFIG_OPTION + "=[path]");
        }
        File file = new File(configPath);
        if (!file.exists())
        {
            throw new IllegalArgumentException("Config file not found. "
                    + configPath);
        }
        return configPath;
    }

    private String getRepositoryPath(String[] args)
    {
        String repoPath = getArgument(args, REPOSITORY_OPTION);
        if (repoPath == null)
        {
            throw new IllegalArgumentException("Set the repository path. "
                    + REPOSITORY_OPTION + "=[path]");
        }
        File repoFile = new File(repoPath);
        if (!repoFile.exists())
        {
            throw new IllegalArgumentException("Repository path not found. "
                    + repoPath);
        }
        return repoPath;
    }

    /**
     * Gets the argument value
     * 
     * @param args
     * @param key
     * @return
     */
    private String getArgument(String[] args, String key)
    {
        String arg = null;
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith(key + "="))
            {
                arg = args[i].substring(key.length() + 1);
            }
        }
        return arg;
    }

    /**
     * @param args
     * @param option
     * @return true if the option exists
     */
    private boolean hasOption(String[] args, String option)
    {
        boolean retu = false;
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(option))
            {
                retu = true;
            }
        }
        return retu;
    }

}
