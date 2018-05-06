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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import java.io.File;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

/**
 * <code>ReadWhileSaveTest</code> reads nodes using a new session each time
 * while a large binary is written to the workspace.
 */
public class ReadWhileSaveTest extends AbstractJCRTest {

    public void testReadWhileSave() throws RepositoryException, IOException {
        Thread t = runExpensiveSave();
        long numReads = 0;
        while (t.isAlive()) {
            Session s = getHelper().getSuperuserSession();
            try {
                for (NodeIterator it = s.getRootNode().getNodes(); it.hasNext(); ) {
                    it.nextNode();
                }
                numReads++;
            } finally {
                s.logout();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.println("numReads: " + numReads);
    }

    private Thread runExpensiveSave() throws RepositoryException, IOException {
        // create a temp file with 10 mb random data
        final File tmp = File.createTempFile("garbage", null);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp));
        Random rand = new Random();
        byte[] randomKb = new byte[1024];
        for (int i = 0; i < 1024 * 10; i++) {
            rand.nextBytes(randomKb);
            out.write(randomKb);
        }
        out.close();
        final Session s = getHelper().getSuperuserSession();
        final Node stuff = s.getRootNode().getNode(testPath).addNode("stuff");
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        stuff.setProperty("binary", new BufferedInputStream(new FileInputStream(tmp)));
                        s.save();
                        stuff.getProperty("binary").remove();
                        s.save();
                    }
                } catch (RepositoryException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    s.logout();
                    tmp.delete();
                }
            }
        });
        t.start();
        return t;
    }
}
