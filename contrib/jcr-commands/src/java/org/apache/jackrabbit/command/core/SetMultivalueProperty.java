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
package org.apache.jackrabbit.command.core;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Set a multivalue property to the current working node. <br>
 * The default regular expression is ",".
 */
public class SetMultivalueProperty extends AbstractSetProperty
{
	/** logger */
	private static Log log = LogFactory.getLog(SetMultivalueProperty.class);

	// ---------------------------- < keys >

	/** regular expression key */
	private String regExpKey = "regExp";

	/**
	 * @inheritDoc
	 */
	public boolean execute(Context ctx) throws Exception
	{
		String regExp = (String) ctx.get(this.regExpKey);
		String value = (String) ctx.get(this.valueKey);
		String name = (String) ctx.get(this.nameKey);
		String type = (String) ctx.get(this.typeKey);
		String parent = (String) ctx.get(this.parentPathKey);

		Node node = CommandHelper.getNode(ctx, parent);

		if (log.isDebugEnabled())
		{
			log.debug("setting multivalue property from node at "
					+ node.getPath() + ". regexp=" + regExp + " value=" + value
					+ " property=" + name);
		}

		String[] values = value.split(regExp);

		if (type == null)
		{
			node.setProperty(name, values);
		} else
		{
			node.setProperty(name, values, PropertyType.valueFromName(type));
		}

		return false;
	}

	/**
	 * @return Returns the regExpKey.
	 */
	public String getRegExpKey()
	{
		return regExpKey;
	}

	/**
	 * @param regExpKey
	 *            Set the context attribute key for the regExp attribute.
	 */
	public void setRegExpKey(String regExpKey)
	{
		this.regExpKey = regExpKey;
	}
}
