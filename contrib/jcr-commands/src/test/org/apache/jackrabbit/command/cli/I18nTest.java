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

import java.util.Iterator;
import java.util.MissingResourceException;

import org.apache.jackrabbit.command.CommandException;

import junit.framework.TestCase;

/**
 * I18n tests
 */
public class I18nTest extends TestCase
{

    public void testCommandLabels() throws CommandException
    {
        Iterator iter = CommandLineFactory.getInstance().getCommandLines()
            .iterator();
        while (iter.hasNext())
        {
            CommandLine cl = (CommandLine) iter.next();

            Iterator params = cl.getAllParameters();
            while (params.hasNext())
            {
                AbstractParameter param = (AbstractParameter) params.next();
                try
                {
                    param.getLocalizedArgName();
                } catch (MissingResourceException e)
                {
                    fail("no arg name for command " + cl.getName() + ". "
                            + param.getName() + ". " + e.getMessage());
                }
                try
                {
                    param.getLocalizedDescription();
                } catch (MissingResourceException e)
                {
                    fail("no description for argument " + param.getName()
                            + " in command " + cl.getName() + "."
                            + e.getMessage());
                }
            }
        }
    }

    public void testCommandNames() throws CommandException
    {
        Iterator iter = CommandLineFactory.getInstance().getCommandLines()
            .iterator();
        while (iter.hasNext())
        {
            CommandLine cl = (CommandLine) iter.next();
            try
            {
                cl.getLocalizedDescription();
            } catch (MissingResourceException e)
            {
                fail("no description for command " + cl.getName() + ". "
                        + e.getMessage());
            }
        }
    }

}
