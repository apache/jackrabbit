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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;
import org.apache.tika.Tika;

/**
 * Import data from the file system. <br>
 * If the given path refers to a file it's imported to a <code>Node</code> of
 * type nt:file under the current working <code>Node</code>.<br>
 * If the given path refers to a folder, the given folder and all the subtree is
 * imported.
 */
public class ImportFileSystem implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(ImportFileSystem.class);

    /** Use Apache Tika to detect the media types of imported files */
    private static Tika tika = new Tika();

    // ---------------------------- < keys >

    /** File system path key */
    private String srcFsPathKey = "srcFsPath";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String file = (String) ctx.get(this.srcFsPathKey);
        Node parent = CommandHelper.getCurrentNode(ctx);

        if (log.isDebugEnabled()) {
            log.debug("importing filesystem from " + file + " to node "
                    + parent);
        }

        if (file == null) {
            throw new CommandException("exception.fspath.is.null");
        }

        File f = new File(file);

        if (!f.exists()) {
            throw new CommandException("exception.file.not.found",
                new String[] {
                    file
                });
        }

        if (f.isFile()) {
            this.importFile(parent, f);
        } else {
            Node folder = parent.addNode(f.getName(), "nt:folder");
            this.importFolder(folder, f);
        }

        return false;
    }

    /**
     * Imports a File.
     * @param parentnode
     *        Parent <code>Node</code>
     * @param file
     *        <code>File</code> to be imported
     * @throws RepositoryException
     *         on <code>Repository</code> errors
     * @throws IOException
     *         on io errors
     */

    private void importFile(Node parentnode, File file)
            throws RepositoryException, IOException {
        InputStream stream = new FileInputStream(file);
        try {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(file.lastModified());
            JcrUtils.putFile(
                    parentnode, file.getName(),
                    tika.detect(file), stream, date);
        } finally {
            stream.close();
        }
    }

    /**
     * Import a Folder.
     * @param parentnode
     *        Parent <code>Node</code>
     * @param directory
     *        File system directory to traverse
     * @throws RepositoryException
     *         on repository errors
     * @throws IOException
     *         on io errors
     */
    private void importFolder(Node parentnode, File directory)
            throws RepositoryException, IOException {
        File[] direntries = directory.listFiles();
        for (int i = 0; i < direntries.length; i++) {
            File direntry = direntries[i];
            if (direntry.isDirectory()) {
                Node childnode =
                    JcrUtils.getOrAddFolder(parentnode, direntry.getName());
                importFolder(childnode, direntry);
            } else {
                importFile(parentnode, direntry);
            }
        }
    }

    /**
     * @return the from key
     */
    public String getSrcFsPathKey() {
        return srcFsPathKey;
    }

    /**
     * @param fromKey
     *        the from key to set
     */
    public void setSrcFsPathKey(String fromKey) {
        this.srcFsPathKey = fromKey;
    }
}
