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
package org.apache.jackrabbit.core.journal;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * A file record log is a file containing {@link Record}s. Every file record
 * log contains a header with the following physical layout:
 *
 * <blockquote>
 *   <table>
 *     <caption>Physical Record Layout</caption>
 *     <tr style="text-align:center">
 *       <th>{@code Byte 1}</th>
 *       <th>{@code Byte 2}</th>
 *       <th>{@code Byte 3}</th>
 *       <th>{@code Byte 4}</th>
 *     </tr>
 *     <tr>
 *       <td style="text-align:center">{@code 'J'}</td>
 *       <td style="text-align:center">{@code 'L'}</td>
 *       <td style="text-align:center">{@code 'O'}</td>
 *       <td style="text-align:center">{@code 'G'}</td>
 *     </tr>
 *     <tr>
 *       <td style="text-align:center" colspan="2">{@code MAJOR}</td>
 *       <td style="text-align:center" colspan="2">{@code MINOR}</td>
 *     </tr>
 *     <tr>
 *       <td style="text-align:center" colspan="4">{@code START REVISION}</td>
 *     </tr>
 *  </table>
 * </blockquote>
 *
 * After this header, zero or more <code>ReadRecord</code>s follow.
 */
public class FileRecordLog {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(FileRecordLog.class);

    /**
     * Record log signature.
     */
    private static final byte[] SIGNATURE = { 'J', 'L', 'O', 'G' };

    /**
     * Known major version.
     */
    private static final short MAJOR_VERSION = 2;

    /**
     * Known minor version.
     */
    private static final short MINOR_VERSION = 0;

    /**
     * Header size. This is the size of {@link #SIGNATURE}, {@link #MAJOR_VERSION},
     * {@link #MINOR_VERSION} and first revision (8 bytes).
     */
    private static final int HEADER_SIZE = 4 + 2 + 2 + 8;

    /**
     * Underlying file.
     */
    private File logFile;

    /**
     * Flag indicating whether this is a new log.
     */
    private boolean isNew;

    /**
     * Input stream used when seeking a specific record.
     */
    private DataInputStream in;

    /**
     * Last revision that is not in this log.
     */
    private long previousRevision;

    /**
     * Relative position inside this log.
     */
    private long position;

    /**
     * Last revision that is available in this log.
     */
    private long lastRevision;

    /**
     * Major version found in record log.
     */
    private short major;

    /**
     * Minor version found in record log.
     */
    private short minor;

    /**
     * Create a new instance of this class. Opens a record log in read-only mode.
     *
     * @param logFile file containing record log
     * @throws java.io.IOException if an I/O error occurs
     */
    public FileRecordLog(File logFile) throws IOException {
        this.logFile = logFile;

        if (logFile.exists()) {
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(logFile), 128));

            try {
                readHeader(in);
                previousRevision = in.readLong();
                lastRevision = previousRevision + logFile.length() - HEADER_SIZE;
            } finally {
                close(in);
            }
        } else {
            isNew = true;
        }
    }

    /**
     * Initialize this record log by writing a header containing the
     * previous revision.
     */
    public void init(long previousRevision) throws IOException {
        if (isNew) {
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(logFile), 128));

            try {
                writeHeader(out);
                out.writeLong(previousRevision);
            } finally {
                close(out);
            }

            this.previousRevision = previousRevision;
            this.lastRevision = previousRevision;
            isNew = false;
        }
    }

    /**
     * Return a flag indicating whether this record log contains a certain revision.
     *
     * @param revision revision to look for
     * @return <code>true</code> if this record log contain a certain revision;
     *         <code>false</code> otherwise
     */
    public boolean contains(long revision) {
        return (revision >= previousRevision && revision < lastRevision);
    }

    /**
     * Return a flag indicating whether this record log is new.
     *
     * @return <code>true</code> if this record log is new;
     *         <code>false</code> otherwise
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Return a flag indicating whether this record log exceeds a given size.
     */
    public boolean exceeds(long size) {
        return (lastRevision - previousRevision) > size;
    }

    /**
     * Seek an entry. This is an operation that allows the underlying input stream
     * to be sequentially scanned and must therefore not be called twice.
     *
     * @param revision revision to seek
     * @throws java.io.IOException if an I/O error occurs
     */
    public void seek(long revision) throws IOException {
        if (in != null) {
            String msg = "Stream already open: seek() only allowed once.";
            throw new IllegalStateException(msg);
        }
        in = new DataInputStream(new BufferedInputStream(
                new FileInputStream(logFile)));
        skip(revision - previousRevision + HEADER_SIZE);
        position = revision - previousRevision;
    }

    /**
     * Skip exactly <code>n</code> bytes. Throws if less bytes are skipped.
     *
     * @param n bytes to skip
     * @throws java.io.IOException if an I/O error occurs, or less that <code>n</code> bytes
     *                     were skipped.
     */
    private void skip(long n) throws IOException {
        long skiplen = n;
        while (skiplen > 0) {
            long skipped = in.skip(skiplen);
            if (skipped <= 0) {
                break;
            }
            skiplen -= skipped;
        }
        if (skiplen != 0) {
            String msg = "Unable to skip remaining bytes.";
            throw new IOException(msg);
        }
    }

    /**
     * Read the file record at the current seek position.
     *
     * @param resolver namespace resolver
     * @return file record
     * @throws java.io.IOException if an I/O error occurs
     */
    public ReadRecord read(NamespaceResolver resolver, NamePathResolver npResolver) throws IOException {
        String journalId = in.readUTF();
        String producerId = in.readUTF();
        int length = in.readInt();

        position +=
            2 + utfLength(journalId) +  2 + utfLength(producerId) + 4 + length;

        long revision = previousRevision + position;
        return new ReadRecord(journalId, producerId, revision, in, length, resolver, npResolver);
    }

    /**
     * Append a record to this log. Returns the revision following this record.
     *
     * @param journalId journal identifier
     * @param producerId producer identifier
     * @param in record to add
     * @param length record length
     * @throws java.io.IOException if an I/O error occurs
     */
    public long append(String journalId, String producerId, InputStream in, int length)
            throws IOException {

        OutputStream out = new FileOutputStream(logFile, true);

        try {
            DataBuffer buffer = new DataBuffer();
            buffer.writeUTF(journalId);
            buffer.writeUTF(producerId);
            buffer.writeInt(length);
            buffer.copy(out);

            IOUtils.copy(in, out);
            out.flush();

            lastRevision +=
                2 + utfLength(journalId) + 2 + utfLength(producerId)
                + 4 + length;
            return lastRevision;
        } finally {
            close(out);
        }
    }

    /**
     * Return the previous revision. This is the last revision preceding the
     * first revision in this log.
     *
     * @return previous revision
     */
    public long getPreviousRevision() {
        return previousRevision;
    }

    /**
     * Return the last revision. This is the last revision in this log.
     *
     * @return last revision
     */
    public long getLastRevision() {
        return lastRevision;
    }

    /**
     * Close this log.
     */
    public void close() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            String msg = "Error while closing record log: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Read signature and major/minor version of file and verify.
     *
     * @param in input stream
     * @throws java.io.IOException if an I/O error occurs or the file does
     *                     not have a valid header.
     */
    private void readHeader(DataInputStream in) throws IOException {
        byte[] signature = new byte[SIGNATURE.length];
        in.readFully(signature);

        for (int i = 0; i < SIGNATURE.length; i++) {
            if (signature[i] != SIGNATURE[i]) {
                String msg = "Record log '" + logFile.getPath()
                    + "' has wrong signature: " + toHexString(signature);
                throw new IOException(msg);
            }
        }

        major = in.readShort();
        if (major != MAJOR_VERSION) {
            String msg = "Record log '" + logFile.getPath()
                + "' has incompatible major version: " + major;
            throw new IOException(msg);
        }
        minor  = in.readShort();
    }

    /**
     * Write signature and major/minor.
     *
     * @param out input stream
     * @throws java.io.IOException if an I/O error occurs.
     */
    private void writeHeader(DataOutputStream out) throws IOException {
        out.write(SIGNATURE);
        out.writeShort(MAJOR_VERSION);
        out.writeShort(MINOR_VERSION);
    }

    /**
     * Close an input stream, logging a warning if an error occurs.
     */
    private static void close(InputStream in) {
        try {
            in.close();
        } catch (IOException e) {
            String msg = "I/O error while closing input stream.";
            log.warn(msg, e);
        }
    }

    /**
     * Close an output stream, logging a warning if an error occurs.
     */
    private static void close(OutputStream out) {
        try {
            out.close();
        } catch (IOException e) {
            String msg = "I/O error while closing input stream.";
            log.warn(msg, e);
        }
    }

    /**
     * Convert a byte array to its hexadecimal string representation.
     */
    private static String toHexString(byte[] b) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String s = Integer.toHexString(b[i] & 0xff).toUpperCase();
            if (s.length() == 1) {
                buf.append('0');
            }
            buf.append(s);
        }
        return buf.toString();
    }

    /**
     * Return the length of a string when converted to its Java modified
     * UTF-8 encoding, as used by <code>DataInput.readUTF</code> and
     * <code>DataOutput.writeUTF</code>.
     */
    private static int utfLength(String s) {
        char[] ac = s.toCharArray();
        int utflen = 0;

        for (int i = 0; i < ac.length; i++) {
            char c = ac[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }
        return utflen;
    }

    /**
     * A simple helper class that writes to a buffer. The current buffer can
     * be {@link #copy copied} to an output stream.
     */
    private static final class DataBuffer extends DataOutputStream {

        public DataBuffer() {
            super(new ByteArrayOutputStream());
        }

        /**
         * Copies the bytes the are currently held in the buffer to the given
         * output stream.
         *
         * @param out the output stream where the buffered data is written.
         * @throws IOException if an error occurs while writing data to
         *          <code>out</code>.
         */
        public void copy(OutputStream out) throws IOException {
            byte[] buffer = ((ByteArrayOutputStream) super.out).toByteArray();
            out.write(buffer);
        }
    }
}
