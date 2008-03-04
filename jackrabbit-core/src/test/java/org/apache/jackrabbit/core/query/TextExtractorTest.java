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
package org.apache.jackrabbit.core.query;

import javax.jcr.Node;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.net.URLConnection;
import java.util.Calendar;

/**
 * <code>TextExtractorTest</code> implements a file / folder import from the
 * local file system.
 */
public class TextExtractorTest extends AbstractQueryTest {

    private static final String TEST_FOLDER = "test-data";

    private int fileCount = 0;

    public void testImport() throws Exception {
        File sourceFolder = new File(TEST_FOLDER);
        // only run if there is test data
        if (!sourceFolder.exists()) {
            return;
        }
        long time = System.currentTimeMillis();
        addContents(sourceFolder,
                testRootNode.addNode(sourceFolder.getName(), "nt:folder"));
        superuser.save();
        time = System.currentTimeMillis() - time;
        log.println("Imported " + fileCount + " files in " + time + " ms.");
    }

    /**
     * Recursively adds files and folders to the workspace.
     */
    private void addContents(File folder, Node n) throws Exception {
        String[] names = folder.list();
        for (int i = 0; i < names.length; i++) {
            File f = new File(folder, names[i]);
            if (f.canRead()) {
                if (f.isDirectory()) {
                    log.println("Added folder: " + f.getAbsolutePath());
                    addContents(f, n.addNode(names[i], "nt:folder"));
                } else {
                    addFile(n, f);
                    log.println("Added file: " + f.getAbsolutePath());
                    // save after 100 files
                    if (++fileCount % 100 == 0) {
                        n.getSession().save();
                    }
                }
            }
        }
    }

    /**
     * Repeatedly update a file in the workspace and force text extraction
     * on it.
     */
    public void testRepeatedUpdate() throws Exception {
        File testFile = new File("test.pdf");
        if (!testFile.exists()) {
            return;
        }
        Node resource = addFile(testRootNode, testFile).getNode("jcr:content");
        superuser.save();
        for (int i = 0; i < 10; i++) {
            // kick start text extractor
            executeXPathQuery(testPath, new Node[]{testRootNode});
            InputStream in = new BufferedInputStream(new FileInputStream(testFile));
            try {
                resource.setProperty("jcr:data", in);
            } finally {
                in.close();
            }
            log.println("updating resource...");
            superuser.save();
        }
    }

    private static Node addFile(Node folder, File f) throws Exception {
        String mimeType = URLConnection.guessContentTypeFromName(f.getName());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        Node file = folder.addNode(f.getName(), "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        try {
            resource.setProperty("jcr:data", in);
            resource.setProperty("jcr:mimeType", mimeType);
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(f.lastModified());
            resource.setProperty("jcr:lastModified", lastModified);
        } finally {
            in.close();
        }
        return file;
    }
}
