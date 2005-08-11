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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Exports a node of type nt:file or nt:folder to the given file system path.<br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class ExportFileSystem implements Command
{
    // ---------------------------- < literals >

    /** Node path */
    private String from;

    /** File system path */
    private String to;

    /** Overwrite flag */
    private String overwrite;

    // ---------------------------- < keys >

    /** Node path key */
    private String fromKey;

    /** File system path key */
    private String toKey;

    /** Overwrite flag key */
    private String overwriteKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String from = CtxHelper.getAttr(this.from, this.fromKey, ctx);

        String to = CtxHelper.getAttr(this.to, this.toKey, ctx);

        boolean overwrite = CtxHelper.getBooleanAttr(this.overwrite,
            this.overwriteKey, false, ctx);

        Node node = CtxHelper.getNode(ctx, from);

        File f = new File(to);

        // check if the file exists
        if (f.exists() && !overwrite)
        {
            throw new JcrCommandException("file.exists", new String[]
            {
                to
            });
        }

        // export either a file or a folder
        if (node.isNodeType("nt:file"))
        {
            this.createFile(node, f);
        } else if (node.isNodeType("nt:folder"))
        {
            this.addFolder(node, f);
        } else
        {
            throw new JcrCommandException("not.file.or.folder", new String[]
            {
                node.getPrimaryNodeType().getName()
            });
        }

        return false;
    }

    /**
     * Exports a nt:file to the file system
     * 
     * @param node
     * @param file
     * @throws IOException
     * @throws JcrCommandException
     * @throws ValueFormatException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private void createFile(Node node, File file) throws IOException,
            JcrCommandException, ValueFormatException, PathNotFoundException,
            RepositoryException
    {

        boolean created = file.createNewFile();
        if (!created)
        {
            throw new JcrCommandException("file.not.created", new String[]
            {
                file.getPath()
            });
        }
        BufferedOutputStream out = new BufferedOutputStream(
            new FileOutputStream(file));
        InputStream in = node.getNode("jcr:content").getProperty("jcr:data")
            .getStream();

        int c;

        while ((c = in.read()) != -1)
        {
            out.write(c);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     * Exports a nt:folder and all its children to the file system
     * 
     * @param node
     * @param file
     * @throws JcrCommandException
     * @throws RepositoryException
     * @throws IOException
     */
    private void addFolder(Node node, File file) throws JcrCommandException,
            RepositoryException, IOException
    {
        boolean created = file.mkdir();

        if (!created)
        {
            throw new JcrCommandException("folder.not.created", new String[]
            {
                file.getPath()
            });
        }

        NodeIterator iter = node.getNodes();
        while (iter.hasNext())
        {
            Node child = iter.nextNode();
            // File
            if (child.isNodeType("nt:file"))
            {
                File childFile = new File(file, child.getName());
                createFile(child, childFile);
            } else if (child.isNodeType("nt:folder"))
            {
                File childFolder = new File(file, child.getName());
                addFolder(child, childFolder);
            }
        }
    }

    /**
     * @return file system path
     */
    public String getTo()
    {
        return to;
    }

    /**
     * Sets the file system path
     * 
     * @param to
     */
    public void setTo(String to)
    {
        this.to = to;
    }

    /**
     * @return jcr node path
     */
    public String getFrom()
    {
        return from;
    }

    /**
     * Set the jcr node path
     * 
     * @param from
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
     *            Set the context attribute key for the from attribute.
     */
    public void setFromKey(String fromKey)
    {
        this.fromKey = fromKey;
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
     * @return Returns the toKey.
     */
    public String getToKey()
    {
        return toKey;
    }

    /**
     * @param toKey
     *            Set the context attribute key for the to attribute
     */
    public void setToKey(String toKey)
    {
        this.toKey = toKey;
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
     *            Set the context attribute key for the overwrite attribute
     */
    public void setOverwriteKey(String overwriteKey)
    {
        this.overwriteKey = overwriteKey;
    }
}
