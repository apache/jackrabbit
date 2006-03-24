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

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.jackrabbit.command.CommandException;

import junit.framework.TestCase;

public class CliTest extends TestCase
{
	static
	{
		try
		{
			ConfigParser parser = new ConfigParser();
			parser.parse(CommandLine.class.getResource("command.xml"));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/** catalog */
	protected Catalog catalog = CatalogFactoryBase.getInstance().getCatalog();

	public void testCommandExist() throws CommandException
	{
		Iterator iter = CommandLineFactory.getInstance().getCommandLines()
				.iterator();
		while (iter.hasNext())
		{
			CommandLine cl = (CommandLine) iter.next();
			String impl = cl.getImpl();
			if (impl == null)
			{
				impl = cl.getName();
			}
			assertTrue(impl + " not found", catalog.getCommand(impl) != null);
		}
	}

	public void testArgumentsExist() throws CommandException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException
	{
		Iterator iter = CommandLineFactory.getInstance().getCommandLines()
				.iterator();
		while (iter.hasNext())
		{
			CommandLine cl = (CommandLine) iter.next();
			String impl = cl.getImpl();
			if (impl == null)
			{
				impl = cl.getName();
			}
			Command cmd = catalog.getCommand(impl);
			// don't test chain
			if (!(cmd instanceof Chain))
			{
				Map props = BeanUtils.describe(cmd);
				Iterator params = cl.getAllParameters();
				while (params.hasNext())
				{
					AbstractParameter param = (AbstractParameter) params.next();
					if (!props.containsValue(param.getContextKey()))
					{
						fail("Command: " + cl.getName() + ". Param "
								+ param.getContextKey() + " not found");
					}
				}
			}
		}
	}

}
