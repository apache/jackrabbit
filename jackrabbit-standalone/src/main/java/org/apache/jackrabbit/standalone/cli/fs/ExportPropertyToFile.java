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
package org.apache.jackrabbit.standalone.cli.fs;

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
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Exports a <code>Property</code> <code>Value</code> of the current working
 * <code>Node</code> to the file system.
 */
public class ExportPropertyToFile implements Command {
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
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String name = (String) ctx.get(this.nameKey);
        Integer index = (Integer) ctx.get(this.indexKey);
        String to = (String) ctx.get(this.destFsPathKey);

        Node n = CommandHelper.getCurrentNode(ctx);

        if (log.isDebugEnabled()) {
            log.debug("exporting property value from " + n.getPath() + "/"
                    + name + " to the filesystem: " + to);
        }

        Property p = n.getProperty(name);
        if (p.getDefinition().isMultiple()) {
            exportValue(ctx, p.getValues()[index.intValue()], to);
        } else {
            exportValue(ctx, p.getValue(), to);
        }
        return false;
    }

    /**
     * Export th given value to a File
     * @param ctx
     *        the <code>Context</code>
     * @param value
     *        the <code>Value</code>
     * @param to
     *        the target file system path
     * @throws CommandException
     *         if the <code>File</code> already exists
     * @throws IOException
     *         if an <code>IOException</code> occurs
     * @throws RepositoryException
     *         if the current working <code>Repository</code> throws an
     *         <code>Exception</code>
     */
    private void exportValue(Context ctx, Value value, String to)
            throws CommandException, IOException, RepositoryException {
        boolean overwrite = Boolean
            .valueOf((String) ctx.get(this.overwriteKey)).booleanValue();

        File file = new File(to);

        // Check if there's a file at the given target path
        if (file.exists() && !overwrite) {
            throw new CommandException("exception.file.exists", new String[] {
                to
            });
        }

        // If it doesn't exists create the file
        if (!file.exists()) {
            file.createNewFile();
        }

        if (value.getType() == PropertyType.BINARY) {
            InputStream in = value.getStream();
            BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(file));
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            in.close();
            out.flush();
            out.close();
        } else {
            Reader in = new StringReader(value.getString());
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            in.close();
            out.flush();
            out.close();
        }
    }

    /**
     * @return the index key
     */
    public String getIndexKey() {
        return indexKey;
    }

    /**
     * @param indexKey
     *        the index key to set
     */
    public void setIndexKey(String indexKey) {
        this.indexKey = indexKey;
    }

    /**
     * @return the name key
     */
    public String getNameKey() {
        return nameKey;
    }

    /**
     * @param nameKey
     *        the name key to set
     */
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    /**
     * @return the overwrite key
     */
    public String getOverwriteKey() {
        return overwriteKey;
    }

    /**
     * @param overwriteKey
     *        the overwrite key to set
     */
    public void setOverwriteKey(String overwriteKey) {
        this.overwriteKey = overwriteKey;
    }

    /**
     * @return the destination file system path key
     */
    public String getDestFsPathKey() {
        return destFsPathKey;
    }

    /**
     * @param destFsPathKey
     *        the destination file system path key to set
     */
    public void setDestFsPathKey(String destFsPathKey) {
        this.destFsPathKey = destFsPathKey;
    }
}
