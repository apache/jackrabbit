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
package org.apache.jackrabbit.jcrlog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Adler32;

/**
 * Input Stream wrapper for user input streams.
 *
 * @author Thomas Mueller
 *
 */
class LogInputStream extends InputStream {
    private IOException throwThis;
    private ByteArrayInputStream in;
    private byte[] data;
    private int size;
    private long adler;
    private boolean logStream;

    public static LogInputStream wrapStream(InputStream in, boolean logData) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bufferSize = 4 * 1024;
        IOException throwThis = null;
        Adler32 adler = new Adler32();
        try {
            byte[] buffer = new byte[bufferSize];
            while (true) {
                int l = in.read(buffer, 0, bufferSize);
                if (l < 0) {
                    break;
                }
                adler.update(buffer, 0, l);
                out.write(buffer, 0, l);
            }
        } catch (IOException e) {
            throwThis = e;
        }
        return new LogInputStream(out.toByteArray(), throwThis, adler
                .getValue(), logData);
    }

    private LogInputStream(byte[] buffer, IOException throwThis, long adler, boolean logStream) {
        this.throwThis = throwThis;
        this.size = buffer.length;
        this.adler = adler;
        this.data = buffer;
        this.logStream = logStream;
        this.in = new ByteArrayInputStream(buffer);
    }

    private void throwOnceIfRequired() throws IOException {
        if (throwThis != null) {
            IOException t = throwThis;
            throwThis = null;
            throw t;
        }
    }

    public int read() throws IOException {
        throwOnceIfRequired();
        return in.read();
    }

    public int available() throws IOException {
        throwOnceIfRequired();
        return in.available();
    }

    public void close() throws IOException {
        throwOnceIfRequired();
        in.close();
    }

    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    public boolean markSupported() {
        return in.markSupported();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        throwOnceIfRequired();
        return in.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        throwOnceIfRequired();
        return in.read(b);
    }

    public void reset() throws IOException {
        throwOnceIfRequired();
        in.reset();
    }

    public long skip(long n) throws IOException {
        throwOnceIfRequired();
        return in.skip(n);
    }


    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("size:");
        buff.append(size);
        buff.append(" adler32:0x");
        buff.append(Long.toHexString(adler));
        if (logStream) {
            buff.append(" data:");
            buff.append(StringUtils.convertBytesToString(data));
        }
        return buff.toString();
    }

}
