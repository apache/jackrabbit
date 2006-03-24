/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.command.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;
import org.apache.jackrabbit.command.CommandException;

/**
 * Import data from the file system. <br>
 * If the given path refers to a file it's imported to a <code>Node</code> of
 * type nt:file under the current working <code>Node<code>.<br>
 * If the given path refers to a folder, the given folder and all the subtree is
 * imported.
 */
public class ImportFileSystem implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(ImportFileSystem.class);

    /** extension to mime type mapping */
    private static Properties mimeTypes;

    static {
        try {
            mimeTypes = new Properties();
            mimeTypes.load(ImportFileSystem.class
                .getResourceAsStream("mimetypes.properties"));
        } catch (Exception e) {
            log.error("unable to load mime types", e);
        }
    }

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
        String mimeType = null;
        String extension = getExtension(file.getName());
        if (extension != null) {
            mimeType = mimeTypes.getProperty(extension);
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        Node fileNode = parentnode.addNode(file.getName(), "nt:file");
        Node resNode = fileNode.addNode("jcr:content", "nt:resource");
        resNode.setProperty("jcr:mimeType", mimeType);
        resNode.setProperty("jcr:encoding", "");
        resNode.setProperty("jcr:data", new FileInputStream(file));
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(file.lastModified());
        resNode.setProperty("jcr:lastModified", lastModified);
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
                Node childnode = parentnode.addNode(direntry.getName(),
                    "nt:folder");
                importFolder(childnode, direntry);
            } else {
                importFile(parentnode, direntry);
            }
        }
    }

    /**
     * @param name
     *        the file name
     * @return the extension for the given file name
     */
    private String getExtension(String name) {
        String ext = null;
        if (name.lastIndexOf('.') != -1) {
            ext = name.substring(name.lastIndexOf('.') + 1);
        }
        return ext;
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
