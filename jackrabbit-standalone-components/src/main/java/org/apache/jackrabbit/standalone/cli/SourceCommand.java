/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.standalone.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Executes a script from the given file
 */
public class SourceCommand implements Command {
    /** Resource bundle */
    private ResourceBundle bundle = CommandHelper.getBundle();

    /** file */
    private String fileKey = "file";

    /**
     * @return Returns the file.
     */
    public String getFileKey() {
        return fileKey;
    }

    /**
     * @param file
     *        The file to set.
     */
    public void setFileKey(String file) {
        this.fileKey = file;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        File f = new File((String) ctx.get(this.fileKey));
        if (!f.exists()) {
            throw new CommandException("exception.file.not.found",
                new String[] {
                    f.getAbsolutePath()
                });
        }
        // client
        JcrClient client = new JcrClient(ctx);

        BufferedReader in = new BufferedReader(new FileReader(f));
        PrintWriter out = CommandHelper.getOutput(ctx);
        String line = null;
        while ((line = in.readLine()) != null) {
            out.println(bundle.getString("word.running") + ": " + line);
            client.runCommand(line);
        }
        in.close();
        return false;
    }
}
