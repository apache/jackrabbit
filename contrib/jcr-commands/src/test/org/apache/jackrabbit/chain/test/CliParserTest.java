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
package org.apache.jackrabbit.chain.test;

import junit.framework.TestCase;

import org.apache.commons.chain.Command;
import org.apache.jackrabbit.chain.cli.JcrParser;
import org.apache.jackrabbit.chain.cli.JcrParserException;
import org.apache.jackrabbit.chain.command.CurrentNode;
import org.apache.jackrabbit.chain.command.StartJackrabbit;

/**
 * Command line parser tests
 */
public class CliParserTest extends TestCase
{
    
    /**
     * @throws Exception
     */
    public void testParse() throws Exception
    {
        JcrParser parser = new JcrParser();
        String config = "repository.xml";
        String home = "/repository";
        parser.parse("startjackrabbit -config " + config + " -home " + home);
        Command c = parser.getCommand();
        assertTrue(c instanceof StartJackrabbit);
        StartJackrabbit cmd = (StartJackrabbit) c;
        assertTrue(cmd.getConfig().equals(config));
        assertTrue(cmd.getHome().equals(home));
    }

    /**
     * @throws Exception
     */
    public void testCliParserExceptions() throws Exception
    {
        JcrParser parser = new JcrParser();
        try
        {
            parser.parse("startjackrabbit -config ");
            fail();
        } catch (JcrParserException e)
        {
            // Do nothing
        }

    }
    
    /**
     * @throws Exception
     */
    public void testCd() throws Exception
    {
        JcrParser parser = new JcrParser();
        parser.parse("cd foo");
        CurrentNode cmd = (CurrentNode) parser.getCommand() ;
        assertTrue(cmd.getPath().equals("foo")) ;
    }    

}
