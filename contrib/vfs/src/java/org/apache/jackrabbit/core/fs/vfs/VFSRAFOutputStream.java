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
package org.apache.jackrabbit.core.fs.vfs;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;

/**
 * Wrapper of VFS output stream on a random access file.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
class VFSRAFOutputStream extends RandomAccessOutputStream
{

    private Log log = LogFactory.getLog(VFSRAFOutputStream.class);

    /**
     * The default size of the write buffer in bytes.
     */
    static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * The write buffer.
     */
    private final byte[] buffer;

    /**
     * The underlying <code>RandomAccessContent</code>.
     */
    protected RandomAccessContent rac;

    /**
     * The starting position of the buffer in the code.
     */
    private long bufferStart;

    /**
     * The end of valid data in the buffer.
     */
    private int bufferEnd;

    /**
     * Dummy buffer for {@link #write(int)}.
     */
    private byte[] one = new byte[1];

    /**
     * Constructor
     */
    public VFSRAFOutputStream(RandomAccessContent rac, int size)
    {
        super();
        this.rac = rac;
        this.buffer = new byte[size];
        try
        {
            bufferStart = rac.getFilePointer();
        } catch (IOException e)
        {
            log.error("Unable to get file pointer");
        }
    }

    /**
     * Constructor
     */
    public VFSRAFOutputStream(RandomAccessContent rac)
    {
        this(rac, DEFAULT_BUFFER_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.RandomAccessOutputStream#seek(long)
     */
    public void seek(long position) throws IOException
    {
        flush();
        rac.seek(position);
        bufferStart = position;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {
        one[0] = (byte) b;
        write(one, 0, 1);
    }

    public void close() throws IOException
    {
        flush();
        rac.close();
        rac = null;
    }

    public void flush() throws IOException
    {
        rac.write(buffer, 0, bufferEnd);
        bufferEnd = 0;
        bufferStart = rac.getFilePointer();
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        if (len > buffer.length - bufferEnd)
        {
            flush();
            rac.write(b, off, len);
        } else
        {
            System.arraycopy(b, off, buffer, bufferEnd, len);
            bufferEnd += len;
        }
    }

    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }
}