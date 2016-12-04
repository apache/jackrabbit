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
package org.apache.jackrabbit.rmi.value;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;

/**
 * Serializable binary.
 */
class SerializableBinary implements Binary, Serializable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7742179594834275853L;

    private static final int BUFFER_SIZE = 64 * 1024;

    private transient long length;

    private transient byte[] data;

    private transient File file;

    /**
     * Creates a binary from the given stream. The stream is not closed.
     *
     * @param stream binary stream
     */
    public SerializableBinary(InputStream stream) throws IOException {
        length = 0;
        data = null;
        file = null;

        OutputStream output = null;
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int n = stream.read(buffer);
            while (n != -1) {
                length += n;
                if (length < buffer.length) {
                    n = stream.read(
                            buffer, (int) length, buffer.length - (int) length);
                } else {
                    if (file == null) {
                        file = File.createTempFile("jackrabbit-jcr-rmi-", null);
                        output = new FileOutputStream(file);
                        output.write(buffer);
                    } else {
                        output.write(buffer, 0, n);
                    }
                    n = stream.read(buffer);
                }
            }
            if (file == null) {
                data = new byte[(int) length];
                System.arraycopy(buffer, 0, data, 0, (int) length);
            }
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    public synchronized int read(byte[] b, long position)
            throws RepositoryException {
        if (position < 0 || position >= length) {
            return -1;
        } else if (data != null) {
            int n = Math.min(b.length, data.length - (int) position);
            System.arraycopy(data, (int) position, b, 0, n);
            return n;
        } else if (file != null) {
            try {
                RandomAccessFile random = new RandomAccessFile(file, "r");
                try {
                    random.seek(position);
                    return random.read(b);
                } finally {
                    random.close();
                }
            } catch (FileNotFoundException e) {
                throw new RepositoryException("Binary file is missing", e);
            } catch (IOException e) {
                throw new RepositoryException("Unable to read the binary", e);
            }
        } else {
            throw new IllegalStateException("This binary has been disposed");
        }
    }

    public synchronized InputStream getStream() throws RepositoryException {
        if (data != null) {
            return new ByteArrayInputStream(data);
        } else if (file != null) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RepositoryException("Binary file is missing", e);
            }
        } else {
            throw new IllegalStateException("This binary has been disposed");
        }
    }

    public long getSize() {
        return length;
    }

    public synchronized void dispose() {
        data = null;
        if (file != null) {
            file.delete();
            file = null;
        }
    }

    private synchronized void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeLong(length);
        if (data != null) {
            stream.write(data);
        } else if (file != null) {
            InputStream input = new FileInputStream(file);
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int n = input.read(buffer);
                while (n != -1) {
                    stream.write(buffer, 0, n);
                    n = input.read(buffer);
                }
            } finally {
                input.close();
            }
        } else {
            throw new IllegalStateException("This binary has been disposed");
        }
    }

    private void readObject(ObjectInputStream stream)
            throws IOException {
        length = stream.readLong();
        if (length <= BUFFER_SIZE) {
            data = new byte[(int) length];
            stream.readFully(data);
            file = null;
        } else {
            data = null;
            file = File.createTempFile("jackrabbit-jcr-rmi-", null);
            OutputStream output = new FileOutputStream(file);
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                long count = 0;
                int n = stream.read(buffer);
                while (n != -1 && count < length) {
                    output.write(buffer, 0, n);
                    count += n;
                    n = stream.read(buffer, 0, Math.min(
                            buffer.length, (int) (length - count)));
                }
            } finally {
                output.close();
            }
        }
    }

    public void finalize() {
        dispose();
    }

}
