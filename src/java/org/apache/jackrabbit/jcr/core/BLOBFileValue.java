/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.jcr.core;

import org.apache.jackrabbit.jcr.fs.FileSystemException;
import org.apache.jackrabbit.jcr.fs.FileSystemResource;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.util.ISO8601;
import java.io.*;
import java.util.Calendar;

/**
 * <code>BLOBFileValue</code> represents a binary <code>Value</code> that
 * is backed by a file. Unlike <code>javax.jcr.BinaryValue</code> it has no
 * state, i.e. the <code>getStream()</code> method always returns a fresh
 * <code>InputStream</code> instance.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.16 $
 */
public class BLOBFileValue implements Value {

    public static final int TYPE = PropertyType.BINARY;

    protected static final String DEFAULT_ENCODING = "UTF-8";

    private final boolean tmpFile;

    private final File file;
    private final FileSystemResource fsResource;

    private String text = null;

    /**
     * Creates a new <code>BLOBFileValue</code> instance from an
     * <code>InputStream</code>. The contents of the stream is spooled
     * to a temporary file.
     *
     * @param in stream to be represented as a <code>BLOBFileValue</code> instance
     * @throws IOException if an error occurs while reading from the stream or
     *                     writing to the temporary file
     */
    public BLOBFileValue(InputStream in) throws IOException {
	// create temp file
	file = File.createTempFile("bin", null);
	file.deleteOnExit();
	// this instance is backed by a 'real' file; set virtual fs resource to null
	fsResource = null;

	// spool stream to temp file
	FileOutputStream out = new FileOutputStream(file);
	try {
	    byte[] buffer = new byte[8192];
	    int read = 0;
	    while ((read = in.read(buffer)) > 0) {
		out.write(buffer, 0, read);
	    }
	} finally {
	    out.close();
	    in.close();
	}

	tmpFile = true;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a <code>File</code>.
     *
     * @param file file to be represented as a <code>BLOBFileValue</code> instance
     * @throws IOException if the file can not be read
     */
    public BLOBFileValue(File file) throws IOException {
	String path = file.getCanonicalPath();
	if (!file.isFile()) {
	    throw new IOException(path + ": the specified file does not exist");
	}
	if (!file.canRead()) {
	    throw new IOException(path + ": the specified file can not be read");
	}
	this.file = file;
	// this instance is backed by a 'real' file; set virtual fs resource to null
	fsResource = null;

	tmpFile = false;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a resource in the
     * virtual file system.
     *
     * @param fsResource resource in virtual file system
     * @throws IOException if the resource can not be read
     */
    public BLOBFileValue(FileSystemResource fsResource) throws IOException {
	try {
	    if (!fsResource.exists()) {
		throw new IOException(fsResource.getPath() + ": the specified resource does not exist");
	    }
	} catch (FileSystemException fse) {
	    throw new IOException(fsResource.getPath() + ": the specified resource does not exist");
	}
	// this instance is backed by a resource in the virtual file system
	this.fsResource = fsResource;
	// set 'real' file to null
	file = null;

	tmpFile = false;
    }

    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof BLOBFileValue) {
	    BLOBFileValue other = (BLOBFileValue) obj;
	    return ((file == null ? other.file == null : file.equals(other.file)) &&
		    (fsResource == null ? other.fsResource == null : fsResource.equals(other.fsResource)));
	}
	return false;
    }

    /**
     * Returns the length of this <code>BLOBFileValue</code>.
     *
     * @return The length, in bytes, of this <code>BLOBFileValue</code>,
     *         or -1L if the length can't be determined.
     */
    public long getLength() {
	if (file != null) {
	    // this instance is backed by a 'real' file
	    if (file.exists()) {
		return file.length();
	    } else {
		return -1;
	    }
	} else {
	    // this instance is backed by a resource in the virtual file system
	    try {
		return fsResource.length();
	    } catch (FileSystemException fse) {
		return -1;
	    }
	}
    }

    /**
     * Returns <code>true</code> it this <code>BLOBFileValue</code> is backed
     * by a temporary file.
     *
     * @return <code>true</code> it this <code>BLOBFileValue</code> is backed
     *         by a temporary file.
     */
    public boolean isTempFile() {
	return tmpFile;
    }

    /**
     * Deletes the file backing this <code>BLOBFileValue</code>.
     */
    public void delete() {
	if (file != null) {
	    // this instance is backed by a 'real' file
	    file.delete();
	} else {
	    // this instance is backed by a resource in the virtual file system
	    try {
		fsResource.delete();
	    } catch (FileSystemException fse) {
		// ignore
	    }
	}
    }

    /**
     * Spools the contents of this <code>BLOBFileValue</code> to the given
     * output stream.
     *
     * @param out output stream
     * @throws RepositoryException if the input stream for this
     *                             <code>BLOBFileValue</code> could not be obtained
     * @throws IOException         if an error occurs while while spooling
     */
    public void spool(OutputStream out) throws RepositoryException, IOException {
	InputStream in;
	if (file != null) {
	    // this instance is backed by a 'real' file
	    try {
		in = new FileInputStream(file);
	    } catch (FileNotFoundException fnfe) {
		throw new RepositoryException("file backing binary value not found", fnfe);
	    }
	} else {
	    // this instance is backed by a resource in the virtual file system
	    try {
		in = fsResource.getInputStream();
	    } catch (FileSystemException fse) {
		throw new RepositoryException(fsResource.getPath() + ": the specified resource does not exist", fse);
	    }
	}
	try {
	    byte[] buffer = new byte[8192];
	    int read = 0;
	    while ((read = in.read(buffer)) > 0) {
		out.write(buffer, 0, read);
	    }
	} finally {
	    try {
		in.close();
	    } catch (IOException ioe) {
	    }
	}
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Returns the path string of the backing file.
     *
     * @return The path string of the backing file.
     */
    public String toString() {
	if (file != null) {
	    // this instance is backed by a 'real' file
	    return file.toString();
	} else {
	    // this instance is backed by a resource in the virtual file system
	    return fsResource.toString();
	}
    }

    //----------------------------------------------------------------< Value >
    /**
     * @see Value#getType
     */
    public int getType() {
	return TYPE;
    }

    /**
     * @see Value#getString
     */
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException {
	if (text == null) {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    try {
		spool(out);
		byte[] data = out.toByteArray();
		text = new String(data, DEFAULT_ENCODING);
	    } catch (UnsupportedEncodingException e) {
		throw new RepositoryException(DEFAULT_ENCODING + " not supported on this platform", e);
	    } catch (IOException e) {
		throw new ValueFormatException("conversion from stream to string failed", e);
	    } finally {
		try {
		    out.close();
		} catch (IOException e) {
		    // ignore
		}
	    }
	}
	return text;
    }

    /**
     * @see Value#getStream
     */
    public InputStream getStream() throws ValueFormatException, IllegalStateException, RepositoryException {
	// always return a 'fresh' stream
	if (file != null) {
	    // this instance is backed by a 'real' file
	    try {
		return new FileInputStream(file);
	    } catch (FileNotFoundException fnfe) {
		throw new RepositoryException("file backing binary value not found", fnfe);
	    }
	} else {
	    // this instance is backed by a resource in the virtual file system
	    try {
		return fsResource.getInputStream();
	    } catch (FileSystemException fse) {
		throw new RepositoryException(fsResource.getPath() + ": the specified resource does not exist", fse);
	    }
	}
    }

    /**
     * @see Value#getDouble
     */
    public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException {
	try {
	    return Double.parseDouble(getString());
	} catch (NumberFormatException e) {
	    throw new ValueFormatException("conversion to double failed", e);
	}
    }

    /**
     * @see Value#getDate
     */
    public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException {
	Calendar cal = ISO8601.parse(getString());
	if (cal != null) {
	    return cal;
	} else {
	    throw new ValueFormatException("not a valid date format");
	}
    }

    /**
     * @see Value#getLong
     */
    public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException {
	try {
	    return Long.parseLong(getString());
	} catch (NumberFormatException e) {
	    throw new ValueFormatException("conversion to long failed", e);
	}
    }

    /**
     * @see Value#getBoolean
     */
    public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException {
	return Boolean.valueOf(getString()).booleanValue();
    }
}
