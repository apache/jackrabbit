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
package org.apache.jackrabbit.core.id;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import javax.jcr.RepositoryException;

/**
 * A factory for creating new node ids.
 */
public class NodeIdFactory {

    public final static String SEQUENTIAL_NODE_ID = "jackrabbit.sequentialNodeId";

    private final static String NODE_ID_FILE = "nodeId.properties";
    private final static String NODE_ID_FILE_TEMP = "nodeId.properties.temp";
    private final static String MSB = "msb";
    private final static String NEXT_LSB = "nextLsb";
    private final static int DEFAULT_CACHE_SIZE = 128;

    private final String repositoryHome;

    private boolean createRandom;
    private long msb;
    private long nextLsb;
    private long storedLsb;
    private int cacheSize = DEFAULT_CACHE_SIZE;

    public NodeIdFactory(String repositoryHome) {
        this.repositoryHome = repositoryHome;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void open() throws RepositoryException {
        String seq = System.getProperty(SEQUENTIAL_NODE_ID);
        if (seq == null) {
            createRandom = true;
            return;
        }
        try {
            File n = new File(repositoryHome, NODE_ID_FILE);
            if (!n.exists()) {
                File temp = new File(repositoryHome, NODE_ID_FILE_TEMP);
                if (temp.exists()) {
                    temp.renameTo(n);
                } else {
                    n.getParentFile().mkdirs();
                    n.createNewFile();
                }
            }
            Properties p = new Properties();
            FileInputStream in = new FileInputStream(n);
            try {
                p.load(in);
            } finally {
                in.close();
            }
            String defaultMsb = "", defaultLsb = "0";
            int index = seq.indexOf("/");
            if (index >= 0) {
                defaultMsb = seq.substring(0, index);
                defaultLsb = seq.substring(index + 1);
            }
            String m = p.getProperty(MSB, defaultMsb);
            if (m.length() == 0) {
                msb = UUID.randomUUID().getMostSignificantBits();
                // ensure it doesn't conflict with version 1-5 UUIDs
                msb &= ~0xf000;
            } else {
                if (m.length() == 16) {
                    msb = (Long.parseLong(m.substring(0, 8), 16) << 32) |
                        Long.parseLong(m.substring(8), 16);
                } else {
                    msb = Long.parseLong(m, 16);
                }
            }
            storedLsb = nextLsb = Long.parseLong(p.getProperty(NEXT_LSB, defaultLsb), 16);
        } catch (Exception e) {
            throw new RepositoryException("Could not open node id factory", e);
        }
    }

    public void close() throws RepositoryException {
        if (!createRandom) {
            store(nextLsb);
        }
    }

    private void store(long lsb) throws RepositoryException {
        this.storedLsb = lsb;
        Properties p = new Properties();
        p.setProperty(MSB, Long.toHexString(msb));
        p.setProperty(NEXT_LSB, Long.toHexString(lsb));
        try {
            File temp = new File(repositoryHome, NODE_ID_FILE_TEMP);
            FileOutputStream out = new FileOutputStream(temp);
            try {
                p.store(out, null);
            } finally {
                out.close();
            }
            File n = new File(repositoryHome, NODE_ID_FILE);
            n.delete();
            temp.renameTo(n);
        } catch (IOException e) {
            throw new RepositoryException("Could not store next node id", e);
        }
    }

    public NodeId newNodeId() throws RepositoryException {
        if (createRandom) {
            return NodeId.randomId();
        }
        long lsb = nextLsb++;
        if (lsb >= storedLsb) {
            store(lsb + cacheSize);
        }
        return new NodeId(msb, lsb);
    }

}
