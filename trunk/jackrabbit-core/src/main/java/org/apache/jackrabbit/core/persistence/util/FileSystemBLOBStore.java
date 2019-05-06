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
package org.apache.jackrabbit.core.persistence.util;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemPathUtil;
import org.apache.jackrabbit.core.fs.FileSystemResource;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>FileSystemBLOBStore</code> is a <code>ResourceBasedBLOBStore</code>
 * implementation that stores BLOB data in a <code>FileSystem</code>.
 * <p>
 * Note that The DataStore should nowadays be used instead of the BLOBStore.
 */
public class FileSystemBLOBStore implements ResourceBasedBLOBStore {

    /**
     * the file system where the BLOBs are stored
     */
    private final FileSystem fs;

    /**
     * Creates a new <code>FileSystemBLOBStore</code> instance.
     *
     * @param fs file system for storing the BLOB data
     */
    public FileSystemBLOBStore(FileSystem fs) {
        this.fs = fs;
    }

    //------------------------------------------------------------< BLOBStore >
    /**
     * {@inheritDoc}
     */
    public String createId(PropertyId id, int index) {
        // the blobId is an absolute file system path
        StringBuilder sb = new StringBuilder();
        sb.append(FileSystem.SEPARATOR_CHAR);
        char[] chars = id.getParentId().toString().toCharArray();
        int cnt = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '-') {
                continue;
            }
            //if (cnt > 0 && cnt % 4 == 0) {
            if (cnt == 2 || cnt == 4) {
                sb.append(FileSystem.SEPARATOR_CHAR);
            }
            sb.append(chars[i]);
            cnt++;
        }
        sb.append(FileSystem.SEPARATOR_CHAR);
        sb.append(FileSystemPathUtil.escapeName(id.getName().toString()));
        sb.append('.');
        sb.append(index);
        sb.append(".bin");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream get(String blobId) throws Exception {
        return getResource(blobId).getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    public void put(String blobId, InputStream in, long size) throws Exception {
        // the blobId is an absolute file system path
        FileSystemResource internalBlobFile = new FileSystemResource(fs, blobId);
        internalBlobFile.makeParentDirs();
        OutputStream out = internalBlobFile.getOutputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(String blobId) throws Exception {
        // the blobId is an absolute file system path
        FileSystemResource res = new FileSystemResource(fs, blobId);
        if (!res.exists()) {
            return false;
        }
        // delete resource and prune empty parent folders
        res.delete(true);
        return true;
    }

    //-----------------------------------------------< ResourceBasedBLOBStore >
    /**
     * {@inheritDoc}
     */
    public FileSystemResource getResource(String blobId)
            throws Exception {
        // the blobId is an absolute file system path
        return new FileSystemResource(fs, blobId);
    }
}
