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
package org.apache.jackrabbit.vfs.ext.ds;

import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VFS Test Utilities.
 */
class VFSTestUtils {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(VFSTestUtils.class);

    /**
     * Deletes all the descendant files under the folder.
     * @param folderObject folder object
     * @throws FileSystemException if any file system exception occurs
     * @throws DataStoreException if any file system exception occurs
     */
    static void deleteAllDescendantFiles(FileObject folderObject) throws FileSystemException, DataStoreException {
        List<FileObject> children = VFSUtils.getChildFileOrFolders(folderObject);
        FileType fileType;

        for (FileObject child : children) {
            fileType = child.getType();

            if (fileType == FileType.FILE) {
                boolean deleted = child.delete();

                if (!deleted) {
                    LOG.warn("File not deleted: {}", child.getName().getFriendlyURI());
                }
            } else if (fileType == FileType.FOLDER) {
                deleteAllDescendantFiles(child);
            }
        }
    }

    private VFSTestUtils() {
    }

}
