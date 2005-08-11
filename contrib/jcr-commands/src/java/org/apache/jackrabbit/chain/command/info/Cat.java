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
package org.apache.jackrabbit.chain.command.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Displays the content of a property or a node of type nt:file or nt:resource
 */
public class Cat implements Command
{
    /** property name */
    private String path;

    /** index. [optional] argument to display multivalue properties */
    private int index;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        Item item = CtxHelper.getItem(ctx, path);
        if (item.isNode())
        {
            printNode(ctx, (Node) item);
        } else
        {
            printProperty(ctx, (Property) item);
        }
        return false;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * 
     * @param ctx
     * @param n
     * @throws PathNotFoundException
     * @throws JcrCommandException
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void printNode(Context ctx, Node n) throws PathNotFoundException,
            JcrCommandException, RepositoryException, IllegalStateException,
            IOException
    {
        if (n.isNodeType("nt:file"))
        {
            printValue(ctx, n.getNode("jcr:content").getProperty("jcr:data")
                .getValue());
        } else if (n.isNodeType("nt:resource"))
        {
            printValue(ctx, n.getProperty("jcr:data").getValue());
        } else
        {
            throw new JcrCommandException("cat.unsupported.type", new String[]
            {
                n.getPrimaryNodeType().getName()
            });
        }
    }

    /**
     * 
     * @param ctx
     * @param p
     * @throws JcrCommandException
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     * @throws IOException
     */
    private void printProperty(Context ctx, Property p)
            throws JcrCommandException, ValueFormatException,
            IllegalStateException, RepositoryException, IOException
    {
        if (p.getDefinition().isMultiple())
        {
            printValue(ctx, p.getValues()[index]);
        } else
        {
            printValue(ctx, p.getValue());
        }
    }

    /**
     * Read the value
     * 
     * @param ctx
     * @param value
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     * @throws IOException
     */
    private void printValue(Context ctx, Value value)
            throws ValueFormatException, IllegalStateException,
            RepositoryException, IOException
    {
        PrintWriter out = CtxHelper.getOutput(ctx);
        out.println();
        BufferedReader in = new BufferedReader(new StringReader(value
            .getString()));
        String str = null;
        while ((str = in.readLine()) != null)
        {
            out.println(str);
        }
    }

    /**
     * @return Returns the index.
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * @param index
     *            The index to set.
     */
    public void setIndex(int index)
    {
        this.index = index;
    }
}
