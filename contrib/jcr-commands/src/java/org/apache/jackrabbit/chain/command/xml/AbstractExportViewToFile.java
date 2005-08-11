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
package org.apache.jackrabbit.chain.command.xml;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Export the xml view to a file<br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public abstract class AbstractExportViewToFile implements Command
{

    // ---------------------------- < literals >
    /** target file literal */
    protected String from;

    /** target file literal */
    protected String to;

    /** overwrite flag literal */
    protected String overwrite;

    /** skip binary flag literal */
    protected String skipBinary;

    /** no recurse flag literal */
    protected String noRecurse;

    // ---------------------------- < keys >
    /** target file literal */
    protected String fromKey;

    /** target file key */
    protected String toKey;

    /** overwrite flag key */
    protected String overwriteKey;

    /** skip binary flag key */
    protected String skipBinaryKey;

    /** no recurse flag key */
    protected String noRecurseKey;

    /**
     * @return the OutputStream for the given file
     * @throws JcrCommandException
     * @throws IOException
     */
    protected OutputStream getOutputStream(Context ctx)
            throws JcrCommandException, IOException
    {

        String file = CtxHelper.getAttr(this.to, this.toKey, ctx);

        boolean overwrite = CtxHelper.getBooleanAttr(this.overwrite,
            this.overwriteKey, false, ctx);

        File f = new File(file);

        if (f.exists() && !overwrite)
        {
            throw new JcrCommandException("file.exists", new String[]
            {
                file
            });
        }

        if (!f.exists())
        {
            f.createNewFile();
        }

        BufferedOutputStream out = new BufferedOutputStream(
            new FileOutputStream(f));

        return out;
    }

    /**
     * @return Returns the noRecurse.
     */
    public String getNoRecurse()
    {
        return noRecurse;
    }

    /**
     * @param noRecurse
     *            The noRecurse to set.
     */
    public void setNoRecurse(String noRecurse)
    {
        this.noRecurse = noRecurse;
    }

    /**
     * @return Returns the noRecurseKey.
     */
    public String getNoRecurseKey()
    {
        return noRecurseKey;
    }

    /**
     * @param noRecurseKey
     *            Set the context attribute key for the noRecurse attribute.
     */
    public void setNoRecurseKey(String noRecurseKey)
    {
        this.noRecurseKey = noRecurseKey;
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
     * @return Returns the skipBinary.
     */
    public String getSkipBinary()
    {
        return skipBinary;
    }

    /**
     * @param skipBinary
     *            The skipBinary to set.
     */
    public void setSkipBinary(String skipBinary)
    {
        this.skipBinary = skipBinary;
    }

    /**
     * @return Returns the skipBinaryKey.
     */
    public String getSkipBinaryKey()
    {
        return skipBinaryKey;
    }

    /**
     * @param skipBinaryKey
     *            Set the context attribute key for the skipBinary attribute.
     */
    public void setSkipBinaryKey(String skipBinaryKey)
    {
        this.skipBinaryKey = skipBinaryKey;
    }

    /**
     * @return Returns the from.
     */
    public String getFrom()
    {
        return from;
    }

    /**
     * @param from
     *            The from to set.
     */
    public void setFrom(String from)
    {
        this.from = from;
    }

    /**
     * @return Returns the fromKey.
     */
    public String getFromKey()
    {
        return fromKey;
    }

    /**
     * @param fromKey
     *            The fromKey to set.
     */
    public void setFromKey(String fromKey)
    {
        this.fromKey = fromKey;
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
     *            The toKey to set.
     */
    public void setToKey(String toKey)
    {
        this.toKey = toKey;
    }
}
