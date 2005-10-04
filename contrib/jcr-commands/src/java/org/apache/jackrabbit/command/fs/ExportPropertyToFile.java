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
package org.apache.jackrabbit.command.fs;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandException;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Exports a property value of the current working node to the file system.
 */
public class ExportPropertyToFile implements Command
{
	/** logger */
	private static Log log = LogFactory.getLog(ExportPropertyToFile.class);

	// ---------------------------- < keys >

	/** property name */
	private String nameKey = "name";

	/** value index */
	private String indexKey = "index";

	/** target file */
	private String destFsPathKey = "destFsPath";

	/** overwrite the target file if necessary */
	private String overwriteKey = "overwrite";

	/**
	 * @inheritDoc
	 */
	public boolean execute(Context ctx) throws Exception
	{
		String name = (String) ctx.get(this.nameKey);
		Integer index = (Integer) ctx.get(this.indexKey);
		String to = (String) ctx.get(this.destFsPathKey);

		Node n = CommandHelper.getCurrentNode(ctx);

		if (log.isDebugEnabled())
		{
			log.debug("exporting property value from " + n.getPath() + "/"
					+ name + " to the filesystem: " + to);
		}

		Property p = n.getProperty(name);
		if (p.getDefinition().isMultiple())
		{
			exportValue(ctx, p.getValues()[index.intValue()], to);
		} else
		{
			exportValue(ctx, p.getValue(), to);
		}
		return false;
	}

	/**
	 * Export th given value to a File
	 * 
	 * @param ctx
	 * @param value
	 * @throws CommandException
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws IllegalStateException
	 */
	private void exportValue(Context ctx, Value value, String to)
			throws CommandException, IOException, IllegalStateException,
			RepositoryException
	{
		boolean overwrite = Boolean
				.valueOf((String) ctx.get(this.overwriteKey)).booleanValue();

		File file = new File(to);

		// Check if there's a file at the given target path
		if (file.exists() && !overwrite)
		{
			throw new CommandException("exception.file.exists", new String[]
			{ to });
		}

		// If it doesn't exists create the file
		if (!file.exists())
		{
			file.createNewFile();
		}

		if (value.getType() == PropertyType.BINARY)
		{
			InputStream in = value.getStream();
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(file));
			int c;
			while ((c = in.read()) != -1)
			{
				out.write(c);
			}
			in.close();
			out.flush();
			out.close();
		} else
		{
			Reader in = new StringReader(value.getString());
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			int c;
			while ((c = in.read()) != -1)
			{
				out.write(c);
			}
			in.close();
			out.flush();
			out.close();
		}
	}

	/**
	 * @return Returns the indexKey.
	 */
	public String getIndexKey()
	{
		return indexKey;
	}

	/**
	 * @param indexKey
	 *            Set the context attribute key for the index attribute.
	 */
	public void setIndexKey(String indexKey)
	{
		this.indexKey = indexKey;
	}

	/**
	 * @return Returns the nameKey.
	 */
	public String getNameKey()
	{
		return nameKey;
	}

	/**
	 * @param nameKey
	 *            Set the context attribute key for the name attribute.
	 */
	public void setNameKey(String nameKey)
	{
		this.nameKey = nameKey;
	}

	/**
	 * @return Returns the overwriteKey.
	 */
	public String getOverwriteKey()
	{
		return overwriteKey;
	}

	/**
	 * @param overwriteKey
	 *            Set the context attribute key for the overwrite attribute.
	 */
	public void setOverwriteKey(String overwriteKey)
	{
		this.overwriteKey = overwriteKey;
	}

	public String getDestFsPathKey()
	{
		return destFsPathKey;
	}

	public void setDestFsPathKey(String destFsPathKey)
	{
		this.destFsPathKey = destFsPathKey;
	}
}
