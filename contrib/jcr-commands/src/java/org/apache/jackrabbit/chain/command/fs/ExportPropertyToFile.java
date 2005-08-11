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
package org.apache.jackrabbit.chain.command.fs;

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
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Exports the property value to the file system. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class ExportPropertyToFile implements Command
{
    // ---------------------------- < literals >

    /** property name */
    private String name;

    /** value index */
    private String index;

    /** target file */
    private String to;

    /** overwrite the target file if necessary */
    private String overwrite;

    // ---------------------------- < keys >

    /** property name */
    private String nameKey;

    /** value index */
    private String indexKey;

    /** target file */
    private String toKey;

    /** overwrite the target file if necessary */
    private String overwriteKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String name = CtxHelper.getAttr(this.name, this.nameKey, ctx);

        int index = CtxHelper.getIntAttr(this.index, this.indexKey, 0,
            ctx);

        Node n = CtxHelper.getCurrentNode(ctx);
        Property p = n.getProperty(name);
        if (p.getDefinition().isMultiple())
        {
            exportValue(ctx, p.getValues()[index]);
        } else
        {
            exportValue(ctx, p.getValue());
        }
        return false;
    }

    /**
     * Export th given value to a File
     * 
     * @param ctx
     * @param value
     * @throws JcrCommandException
     * @throws IOException
     * @throws RepositoryException
     * @throws IllegalStateException
     */
    private void exportValue(Context ctx, Value value)
            throws JcrCommandException, IOException, IllegalStateException,
            RepositoryException
    {
        String to = CtxHelper.getAttr(this.to, this.toKey, ctx) ;
        boolean overwrite = CtxHelper.getBooleanAttr(this.overwrite, this.overwriteKey, false, ctx) ; 

        File file = new File(to);

        // Check if there's a file at the given target path
        if (file.exists() && !overwrite)
        {
            throw new JcrCommandException("file.exists", new String[]
            {
                to
            });
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
     * @return Returns the index.
     */
    public String getIndex()
    {
        return index;
    }

    /**
     * @param index
     *            The index to set.
     */
    public void setIndex(String index)
    {
        this.index = index;
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
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
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
     * @return Returns the overwrite.
     */
    public String getOverwrite()
    {
        return overwrite;
    }

    /**
     * @param overwrite
     *            The overwrite to set.
     */
    public void setOverwrite(String overwrite)
    {
        this.overwrite = overwrite;
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

    /**
     * @return Returns the to.
     */
    public String getTo()
    {
        return to;
    }

    /**
     * @param to
     *            The to to set.
     */
    public void setTo(String to)
    {
        this.to = to;
    }

    /**
     * @return Returns the toKey.
     */
    public String getToKey()
    {
        return toKey;
    }

    /**
     * @param toKey
     *            Set the context attribute key for the to attribute.
     */
    public void setToKey(String toKey)
    {
        this.toKey = toKey;
    }
}
