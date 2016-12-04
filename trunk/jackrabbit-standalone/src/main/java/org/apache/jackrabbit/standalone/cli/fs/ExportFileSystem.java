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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Export a <code>Node</code> of type nt:file or nt:folder to the given file
 * system path.
 */
public class ExportFileSystem implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(ExportFileSystem.class);

    // ---------------------------- < keys >

    /** Node path key */
    private String srcJcrPathKey = "srcJcrPath";

    /** File system path key */
    private String destFsPathKey = "destFsPath";

    /** Overwrite flag key */
    private String overwriteKey = "overwrite";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String from = (String) ctx.get(this.srcJcrPathKey);
        String to = (String) ctx.get(this.destFsPathKey);
        boolean overwrite = Boolean
            .valueOf((String) ctx.get(this.overwriteKey)).booleanValue();

        if (log.isDebugEnabled()) {
            log.debug("exporting node at " + from + " to the filesystem (" + to
                    + ") overwrite=" + overwrite);
        }

        Node node = CommandHelper.getNode(ctx, from);

        File f = new File(to);

        // check if the file exists
        if (f.exists() && !overwrite) {
            throw new CommandException("exception.file.exists", new String[] {
                to
            });
        }

        // export either a file or a folder
        if (node.isNodeType("nt:file")) {
            this.createFile(node, f);
        } else if (node.isNodeType("nt:folder")) {
            this.addFolder(node, f);
        } else {
            throw new CommandException("exception.not.file.or.folder",
                new String[] {
                    node.getPrimaryNodeType().getName()
                });
        }

        return false;
    }

    /**
     * Exports an nt:file to the file system
     * @param node
     *        the <code>Node</code>
     * @param file
     *        the <code>File</code>
     * @throws IOException
     *         if an IOException occurs
     * @throws CommandException
     *         if the <code>File</code> can't be created
     * @throws ValueFormatException
     *         if a <code>Value</code> can't be retrieved
     * @throws PathNotFoundException
     *         if the <code>Node</code> can't be found
     * @throws RepositoryException
     *         if the current working <code>Repository</code> throws an
     *         <code>Exception</code>
     */
    private void createFile(Node node, File file) throws IOException,
            CommandException, ValueFormatException, PathNotFoundException,
            RepositoryException {

        boolean created = file.createNewFile();
        if (!created) {
            throw new CommandException("exception.file.not.created",
                new String[] {
                    file.getPath()
                });
        }
        BufferedOutputStream out = new BufferedOutputStream(
            new FileOutputStream(file));
        InputStream in = node.getNode("jcr:content").getProperty("jcr:data")
            .getStream();

        int c;

        while ((c = in.read()) != -1) {
            out.write(c);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     * Exports a nt:folder and all its children to the file system
     * @param node
     *        the <code>Node</code>
     * @param file
     *        <code>File</code>
     * @throws CommandException
     *         if the <code>File</code> can't be created
     * @throws RepositoryException
     *         if the current working <code>Repository</code> throws an
     *         <code>Exception</code>
     * @throws IOException
     *         if an IOException occurs
     */
    private void addFolder(Node node, File file) throws CommandException,
            RepositoryException, IOException {
        boolean created = file.mkdir();

        if (!created) {
            throw new CommandException("exception.folder.not.created",
                new String[] {
                    file.getPath()
                });
        }

        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            // File
            if (child.isNodeType("nt:file")) {
                File childFile = new File(file, child.getName());
                createFile(child, childFile);
            } else if (child.isNodeType("nt:folder")) {
                File childFolder = new File(file, child.getName());
                addFolder(child, childFolder);
            }
        }
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
     * @return the source jcr path key
     */
    public String getSrcJcrPathKey() {
        return srcJcrPathKey;
    }

    /**
     * @param srcJcrPathKey
     *        the source jcr path key to set
     */
    public void setSrcJcrPathKey(String srcJcrPathKey) {
        this.srcJcrPathKey = srcJcrPathKey;
    }

    /**
     * @return the destination file system path key
     */
    public String getDestFsPathKey() {
        return destFsPathKey;
    }

    /**
     * @param toFsPathKey
     *        the destination file system path key to set
     */
    public void setDestFsPathKey(String toFsPathKey) {
        this.destFsPathKey = toFsPathKey;
    }
}
