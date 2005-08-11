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
package org.apache.jackrabbit.chain.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Executes a CLI script from the given file.
 */
public class SourceCommand implements Command
{
    /** Resource bundle */
    private ResourceBundle bundle = ResourceBundle.getBundle(this.getClass()
        .getPackage().getName()
            + ".resources");

    /** file */
    private String file;

    /** cli parser */
    JcrParser parser = new JcrParser();

    /**
     * @return Returns the file.
     */
    public String getFile()
    {
        return file;
    }

    /**
     * @param file
     *            The file to set.
     */
    public void setFile(String file)
    {
        this.file = file;
    }

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        File f = new File(file);
        if (!f.exists())
        {
            throw new JcrCommandException("file.not.found", new String[]
            {
                file
            });
        }
        BufferedReader in = new BufferedReader(new FileReader(f));
        PrintWriter out = CtxHelper.getOutput(ctx);
        String line = null;
        while ((line = in.readLine()) != null)
        {
            out.println(bundle.getString("running") + ": " + line);

            parser.parse(line);
            // populate ctx
            parser.populateContext(ctx);

            // Execute command
            long start = System.currentTimeMillis();
            parser.getCommand().execute(ctx);
            long elapsed = System.currentTimeMillis() - start;

            // depopulate ctx
            parser.dePopulateContext(ctx);

            out
                .println("   " + bundle.getString("in") + ": " + elapsed
                        + " ms");
        }
        in.close();
        return false;
    }
}
