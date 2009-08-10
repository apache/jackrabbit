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
package org.apache.jackrabbit.core.value;

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;

/**
 * Represents binary data which is stored in a file system resource.
 */
class BLOBInResource extends BLOBFileValue {

    /**
     * The default logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BLOBInResource.class);

    /**
     * the prefix of the string representation of this value
     */
    private static final String PREFIX = "fsResource:";

    /**
     * underlying file system resource
     */
    private final FileSystemResource fsResource;

    /**
     * the file length
     */
    private final long length;

    /**
     * Creates a new instance from a stream.
     *
     * @param fsResource the file system resource
     * @throws IOException
     */
    private BLOBInResource(FileSystemResource fsResource) throws IOException {
        try {
            if (!fsResource.exists()) {
                throw new IOException(fsResource.getPath()
                        + ": the specified resource does not exist");
            }
            length = fsResource.length();
        } catch (FileSystemException fse) {
            IOException e2 = new IOException(fsResource.getPath()
                    + ": Error while creating value: " + fse.toString());
            e2.initCause(fse);
            throw e2;
        }
        this.fsResource = fsResource;
    }

    /**
     * Creates a new instance from a file system resource.
     *
     * @param fsResource the resource
     */
    static BLOBInResource getInstance(FileSystemResource fsResource) throws IOException {
        return new BLOBInResource(fsResource);
    }

    void delete(boolean pruneEmptyParentDirs) {
        try {
            fsResource.delete(pruneEmptyParentDirs);
        } catch (FileSystemException fse) {
            // ignore
            LOG.warn("Error while deleting BLOBFileValue: " + fse.getMessage());
        }

    }

    public void dispose() {
        // this instance is not backed by temporarily allocated resource/buffer
    }

    BLOBFileValue copy() throws RepositoryException {
        return BLOBInTempFile.getInstance(getStream(), true);
    }

    public long getSize() {
        return length;
    }

    public InputStream getStream() throws RepositoryException {
        try {
            return fsResource.getInputStream();
        } catch (FileSystemException fse) {
            throw new RepositoryException(fsResource.getPath()
                    + ": the specified resource does not exist", fse);
        }
    }

    public String toString() {
        return PREFIX +  fsResource.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BLOBInResource) {
            BLOBInResource other = (BLOBInResource) obj;
            return length == other.length && fsResource.equals(other.fsResource);
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

}
