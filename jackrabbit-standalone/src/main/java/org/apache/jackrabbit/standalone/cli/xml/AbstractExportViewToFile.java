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
package org.apache.jackrabbit.standalone.cli.xml;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Export the xml view to a file
 */
public abstract class AbstractExportViewToFile implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(AbstractExportViewToFile.class);

    // ---------------------------- < keys >
    /** from literal */
    protected String srcAbsPathKey = "srcAbsPath";

    /** target file key */
    protected String desFsPathKey = "desFsPath";

    /** overwrite flag key */
    protected String overwriteKey = "overwrite";

    /** skip binary flag key */
    protected String skipBinaryKey = "skipBinary";

    /** no recurse flag key */
    protected String noRecurseKey = "noRecurse";

    /**
     * @return the OutputStream for the given file
     * @throws CommandException
     * @throws IOException
     */
    protected OutputStream getOutputStream(Context ctx)
            throws CommandException, IOException {
        String to = (String) ctx.get(this.desFsPathKey);
        boolean overwrite = Boolean
            .valueOf((String) ctx.get(this.overwriteKey)).booleanValue();
        File f = new File(to);

        if (f.exists() && !overwrite) {
            throw new CommandException("exception.file.exists", new String[] {
                to
            });
        }

        if (!f.exists()) {
            f.createNewFile();
        }

        BufferedOutputStream out = new BufferedOutputStream(
            new FileOutputStream(f));

        return out;
    }

    /**
     * @return the no recurse key
     */
    public String getNoRecurseKey() {
        return noRecurseKey;
    }

    /**
     * @param noRecurseKey
     *        the no recurse key to set
     */
    public void setNoRecurseKey(String noRecurseKey) {
        this.noRecurseKey = noRecurseKey;
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
     * @return the skip binary key
     */
    public String getSkipBinaryKey() {
        return skipBinaryKey;
    }

    /**
     * @param skipBinaryKey
     *        the skip binary key to set
     */
    public void setSkipBinaryKey(String skipBinaryKey) {
        this.skipBinaryKey = skipBinaryKey;
    }

    /**
     * @return the from key
     */
    public String getSrcAbsPathKey() {
        return srcAbsPathKey;
    }

    /**
     * @param fromKey
     *        the from key to set
     */
    public void setSrcAbsPathKey(String fromKey) {
        this.srcAbsPathKey = fromKey;
    }

    /**
     * @return the to key
     */
    public String getDesFsPathKey() {
        return desFsPathKey;
    }

    /**
     * @param toKey
     *        the to key to set
     */
    public void setDesFsPathKey(String toKey) {
        this.desFsPathKey = toKey;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean execute(Context ctx) throws Exception {
        boolean skipBinary = Boolean.valueOf(
            (String) ctx.get(this.skipBinaryKey)).booleanValue();
        boolean noRecurse = Boolean
            .valueOf((String) ctx.get(this.noRecurseKey)).booleanValue();
        String fromStr = (String) ctx.get(this.srcAbsPathKey);
        if (log.isDebugEnabled()) {
            log.debug("exporting view from " + fromStr);
        }
        Node from = CommandHelper.getNode(ctx, fromStr);
        OutputStream out = getOutputStream(ctx);
        exportView(from, out, skipBinary, noRecurse);
        out.close();
        return false;
    }

    /**
     * Export the view to the given OutputStream
     * @param node
     *        the <code>Node</code>
     * @param out
     *        the <code>OutputStream</code>
     * @param skipBinary
     * @param noRecurse
     * @throws RepositoryException
     * @throws IOException
     * @throws PathNotFoundException
     */
    protected abstract void exportView(
        Node node,
        OutputStream out,
        boolean skipBinary,
        boolean noRecurse) throws PathNotFoundException, IOException,
            RepositoryException;
}
