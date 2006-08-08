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
package org.apache.jackrabbit.backup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipException;

/**
 * Represent a backup/restore file. This is where
 * the content should be sent to/fetched from.
 *
 */
public interface BackupIOHandler {
    void close() throws IOException;
    void write(String name, File f) throws IOException;
    void write(String name, ByteArrayOutputStream fos) throws IOException;
    byte[] read(String zipEntry) throws ZipException, IOException;
    public void read(String zipEntry, File myFile) throws ZipException, IOException;
    Enumeration getEntries() throws ZipException, IOException;
}
