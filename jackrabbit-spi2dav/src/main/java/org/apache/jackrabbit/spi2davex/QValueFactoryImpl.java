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
package org.apache.jackrabbit.spi2davex;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.DavException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * <code>ValueFactoryImpl</code>...
 */
class QValueFactoryImpl implements QValueFactory {

    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();
    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();
    private static final String DEFAULT_ENCODING = "UTF-8";

    private final NamePathResolver resolver;
    private final ValueLoader loader;

    public QValueFactoryImpl() {
        this(null, null);
    }

    QValueFactoryImpl(NamePathResolver resolver, ValueLoader loader) {
        this.resolver = resolver;
        this.loader = loader;
    }

    /**
     * Create a BINARY QValue with the given length and the given uri used
     * to retrieve the value.
     *
     * @param length Length of the binary value.
     * @param uri Uri from which the the binary value can be accessed.
     * @param index The index of the value within the values array.
     * @return a new BINARY QValue.
     */
    QValue create(long length, String uri, int index) {
        if (loader == null) {
            throw new IllegalStateException();
        }
        return new BinaryQValue(length, uri, index);
    }

    /**
     *
     * @param uri The Uri from which the type info can be retrieved.
     * @return the type of the property with the given <code>uri</code>.
     * @throws IOException If an error occurs.
     * @throws RepositoryException If an error occurs.
     */
    int retrieveType(String uri) throws IOException, RepositoryException {
        return loader.loadType(uri);
    }

    //------------------------------------------------------< QValueFactory >---
    /**
     * @see QValueFactory#create(String, int)
     */
    public QValue create(String value, int type) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        try {
            switch (type) {
                case PropertyType.BOOLEAN:
                    return (Boolean.valueOf(value).booleanValue()) ?
                            QValueImpl.TRUE :
                            QValueImpl.FALSE;
                case PropertyType.DATE: {
                        Calendar cal = ISO8601.parse(value);
                        if (cal == null) {
                            throw new ValueFormatException("not a valid date: " + value);
                        }
                        return new DateQValue(cal);
                    }
                case PropertyType.DOUBLE:
                    return new QValueImpl(Double.valueOf(value));
                case PropertyType.LONG:
                    return new QValueImpl(Long.valueOf(value));
                case PropertyType.PATH:
                    return new QValueImpl(PATH_FACTORY.create(value));
                case PropertyType.NAME:
                    return new QValueImpl(NAME_FACTORY.create(value));
                case PropertyType.STRING:
                case PropertyType.REFERENCE:
                    return new QValueImpl(value, type);
                case PropertyType.BINARY:
                    return new BinaryQValue(value.getBytes(DEFAULT_ENCODING));
            }
        } catch (IllegalArgumentException ex) {
            // given String value cannot be converted to Long/Double/Path/Name
            throw new ValueFormatException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new RepositoryException(ex);
        }

        // none of the legal types:
        throw new IllegalArgumentException("illegal type");
    }

    /**
     * @see QValueFactory#create(Calendar)
     */
    public QValue create(Calendar value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        // Calendar is not constant, must create a clone
        return new DateQValue((Calendar) value.clone());
    }

    /**
     * @see QValueFactory#create(double)
     */
    public QValue create(double value) {
        return new QValueImpl(new Double(value));
    }

    /**
     * @see QValueFactory#create(long)
     */
    public QValue create(long value) {
        return new QValueImpl(new Long(value));
    }

    /**
     * @see QValueFactory#create(boolean)
     */
    public QValue create(boolean value) throws RepositoryException {
        return (value) ? QValueImpl.TRUE : QValueImpl.FALSE;
    }

    /**
     * @see QValueFactory#create(Name)
     */
    public QValue create(Name value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValueImpl(value);
    }

    /**
     * @see QValueFactory#create(Path)
     */
    public QValue create(Path value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValueImpl(value);
    }

    /**
     * @see QValueFactory#create(byte[])
     */
    public QValue create(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new BinaryQValue(value);
    }

    /**
     * @see QValueFactory#create(InputStream)
     */
    public QValue create(InputStream value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new BinaryQValue(value);
    }

    /**
     * @see QValueFactory#create(File)
     */
    public QValue create(File value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new BinaryQValue(value);
    }

    /**
     * @see QValueFactory#computeAutoValues(QPropertyDefinition)
     */
    public QValue[] computeAutoValues(QPropertyDefinition propertyDefinition) throws RepositoryException {
        Name nodeType = propertyDefinition.getDeclaringNodeType();
        Name name = propertyDefinition.getName();

        if (NameConstants.NT_HIERARCHYNODE.equals(nodeType) && NameConstants.JCR_CREATED.equals(name)) {
            return new QValue[] { create(Calendar.getInstance()) };
        } else if (NameConstants.NT_RESOURCE.equals(nodeType) && NameConstants.JCR_LASTMODIFIED.equals(name)) {
            return new QValue[] { create(Calendar.getInstance()) };
        } else if (NameConstants.MIX_REFERENCEABLE.equals(nodeType) && NameConstants.JCR_UUID.equals(name)) {
            return new QValue[] { create(UUID.randomUUID().toString(), PropertyType.STRING) };
        } else {
            throw new RepositoryException("createFromDefinition not implemented for: " + name);
        }
    }

    //--------------------------------------------------------< Inner Class >---
    /**
     * <code>QValue</code> implementation for all valid <code>PropertyType</code>s
     * except for BINARY.
     * @see QValueFactoryImpl.BinaryQValue
     */
    private static class QValueImpl implements QValue, Serializable {

        private static final QValue TRUE = new QValueImpl(Boolean.TRUE);
        private static final QValue FALSE = new QValueImpl(Boolean.FALSE);

        private final Object val;
        private final int type;

        private QValueImpl(String value, int type) {
            val = value;
            this.type = type;
        }

        private QValueImpl(Long value) {
            val = value;
            type = PropertyType.LONG;
        }

        private QValueImpl(Double value) {
            val = value;
            type = PropertyType.DOUBLE;
        }

        private QValueImpl(Boolean value) {
            val = value;
            type = PropertyType.BOOLEAN;
        }

        private QValueImpl(Calendar value) {
            val = value;
            this.type = PropertyType.DATE;
        }

        private QValueImpl(Name value) {
            val = value;
            type = PropertyType.NAME;
        }

        private QValueImpl(Path value) {
            val = value;
            type = PropertyType.PATH;
        }

        protected String getQString(int type) throws RepositoryException {
            return getString();
        }
        //---------------------------------------------------------< QValue >---
        /**
         * @see QValue#getType()
         */
        public int getType() {
            return type;
        }

        /**
         * @see QValue#getLength()
         */
        public long getLength() throws RepositoryException {
            return getString().length();
        }

        /**
         * @see QValue#getString()
         */
        public String getString() {
            return val.toString();
        }

        /**
         * @see QValue#getStream()
         */
        public InputStream getStream() throws RepositoryException {
            try {
                // convert via string
                return new ByteArrayInputStream(getString().getBytes(QValueFactoryImpl.DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(QValueFactoryImpl.DEFAULT_ENCODING + " is not supported encoding on this platform", e);
            }
        }

        /**
         * @see QValue#getCalendar()
         */
        public Calendar getCalendar() throws RepositoryException {
            if (val instanceof Calendar) {
                return (Calendar) ((Calendar) val).clone();
            } else if (val instanceof Double) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
                cal.setTimeInMillis(((Double) val).longValue());
                return cal;
            } else if (val instanceof Long) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
                cal.setTimeInMillis(((Long) val).longValue());
                return cal;
            } else {
                String str = getString();
                Calendar cal = ISO8601.parse(str);
                if (cal == null) {
                    int type = getType();
                    if (type == PropertyType.LONG) {
                        cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
                        cal.setTimeInMillis(new Long(str).longValue());
                    } else if (type == PropertyType.DOUBLE) {
                        cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
                        cal.setTimeInMillis(new Double(str).longValue());
                    } else {
                        throw new ValueFormatException("not a date string: " + getString());
                    }
                }
                return cal;
            }
        }

        /**
         * @see QValue#getDouble()
         */
        public double getDouble() throws RepositoryException {
            if (val instanceof Double) {
                return ((Double) val).doubleValue();
            } else if (val instanceof Calendar) {
                return ((Calendar) val).getTimeInMillis();
            } else {
                try {
                    return Double.parseDouble(getString());
                } catch (NumberFormatException ex) {
                    int type = getType();
                    if (type == PropertyType.DATE) {
                        Calendar cal = ISO8601.parse(getString());
                        if (cal != null) {
                            return cal.getTimeInMillis();
                        }
                    }
                    throw new ValueFormatException("not a double: " + getString(), ex);
                }
            }
        }

        /**
         * @see QValue#getLong()
         */
        public long getLong() throws RepositoryException {
            if (val instanceof Long) {
                return ((Long) val).longValue();
            } else if (val instanceof Double) {
                return ((Double) val).longValue();
            } else if (val instanceof Calendar) {
                return ((Calendar) val).getTimeInMillis();
            } else {
                String str = getString();
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException ex) {
                    int type = getType();
                    if (type == PropertyType.DOUBLE) {
                        return new Double(str).longValue();
                    } else if (type == PropertyType.DATE) {
                        Calendar cal = ISO8601.parse(getString());
                        if (cal != null) {
                            return cal.getTimeInMillis();
                        }
                    }
                    throw new ValueFormatException("not a long: " + getString(), ex);
                }
            }
        }

        public boolean getBoolean() throws RepositoryException {
            if (val instanceof Boolean) {
                return ((Boolean) val).booleanValue();
            } else {
                return new Boolean(getString()).booleanValue();
            }
        }

        /**
         * @see QValue#getName()
         */
        public Name getName() throws RepositoryException {
            if (val instanceof Name) {
                return (Name) val;
            } else {
                try {
                    return NAME_FACTORY.create(getString());
                } catch (IllegalArgumentException e) {
                    throw new ValueFormatException("not a valid Name value: " + getString(), e);
                }
            }
        }

        /**
         * @see QValue#getPath()
         */
        public Path getPath() throws RepositoryException {
            if (val instanceof Path) {
                return (Path) val;
            } else {
                try {
                    return PATH_FACTORY.create(getString());
                } catch (IllegalArgumentException e) {
                    throw new ValueFormatException("not a valid Path value: " + getString(), e);
                }
            }
        }

        /**
         * @see QValue#discard()
         */
        public void discard() {
            // nothing to do
        }

        //---------------------------------------------------------< Object >---
        /**
         * Returns the string representation of this internal value.
         *
         * @return string representation of this internal value.
         * @see Object#toString() 
         */
        public String toString() {
            return val.toString();
        }

        /**
         * @see Object#equals(Object)
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof QValueImpl) {
                QValueImpl other = (QValueImpl) obj;
                if (type == other.type && type != PropertyType.UNDEFINED) {
                    return getString().equals(other.getString());
                }
                try {
                    int type = getType();
                    return type == other.getType() && getQString(type).equals(other.getQString(type));
                } catch (RepositoryException e) {
                    // should never get here. return false.
                }
            }
            return false;
        }

        /**
         * @return the hashCode of the internal value object.
         * @see Object#hashCode()
         */
        public int hashCode() {
            return val.hashCode();
        }
    }

    //--------------------------------------------------------< Inner Class >---
    /**
     * Extension for values of type {@link PropertyType#DATE}.
     */
    private class DateQValue extends QValueImpl {

        private final String formattedStr;

        private DateQValue(Calendar value) {
            super(value);
            formattedStr = ISO8601.format(value);
        }

        /**
         * @return The formatted String of the internal Calendar value.
         * @see QValue#getString()
         * @see ISO8601#format(Calendar)
         */
        public String getString() {
            return formattedStr;
        }

        //---------------------------------------------------------< Object >---
        /**
         * @param obj The object to be checked for equality.
         * @return true if the given Object is a <code>DateQValue</code> with an
         * equal String representation.
         * @see Object#equals(Object)
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DateQValue) {
                DateQValue other = (DateQValue) obj;
                return formattedStr.equals(other.formattedStr);
            } else if (obj instanceof QValueImpl) {
                QValueImpl other = (QValueImpl) obj;
                return formattedStr.equals(other.getString()) &&
                       other.getType() == PropertyType.DATE;
            }
            return false;
        }

        /**
         * @return the hashCode of the formatted String of the Calender value.
         * @see Object#hashCode()
         */
        public int hashCode() {
            return formattedStr.hashCode();
        }
    }

    //--------------------------------------------------------< Inner Class >---
    /**
     * <code>BinaryQValue</code> represents a binary <code>Value</code> which is
     * backed by a resource or byte[]. Unlike <code>BinaryValue</code> it has no
     * state, i.e. the <code>getStream()</code> method always returns a fresh
     * <code>InputStream</code> instance.
     */
    private class BinaryQValue implements QValue, Serializable, ValueLoader.Target {
        /**
         * empty array
         */
        private final byte[] EMPTY_BYTE_ARRAY = new byte[0];

        /**
         * max size for keeping tmp data in memory
         */
        private static final int MAX_BUFFER_SIZE = 0x10000;

        /**
         * underlying file
         */
        private transient File file;

        /**
         * flag indicating if this instance represents a <i>temporary</i> value
         * whose dynamically allocated resources can be explicitly freed on
         * {@link #discard()}.
         */
        private transient boolean temp;

        /**
         * Buffer for small-sized data
         */
        private byte[] buffer;

        /**
         * Converted text
         */
        private transient String text = null;

        /**
         * URI to retrieve the value from
         */
        private String uri;
        private long length;
        private int index = -1;
        private boolean initialized = true;

        private BinaryQValue(long length, String uri, int index) {
            this.length = length;
            this.uri = uri;
            this.index = index;
            initialized = false;
        }

        /**
         * Creates a new <code>BinaryQValue</code> instance from an
         * <code>InputStream</code>. The contents of the stream is spooled
         * to a temporary file or to a byte buffer if its size is smaller than
         * {@link #MAX_BUFFER_SIZE}.
         * <p/>
         * The new instance represents a <i>temporary</i> value whose dynamically
         * allocated resources will be freed explicitly on {@link #discard()}.
         *
         * @param in stream to be represented as a <code>BinaryQValue</code> instance
         * @throws IOException if an error occurs while reading from the stream or
         *                     writing to the temporary file
         */
        private BinaryQValue(InputStream in) throws IOException {
            init(in, true);
        }


        /**
         * Creates a new <code>BinaryQValue</code> instance from a
         * <code>byte[]</code> array.
         *
         * @param bytes byte array to be represented as a <code>BinaryQValue</code>
         *              instance
         */
        private BinaryQValue(byte[] bytes) {
            buffer = bytes;
            file = null;
            // this instance is not backed by a temporarily allocated buffer
            temp = false;
        }

        /**
         * Creates a new <code>BinaryQValue</code> instance from a <code>File</code>.
         *
         * @param file file to be represented as a <code>BinaryQValue</code> instance
         * @throws IOException if the file can not be read
         */
        private BinaryQValue(File file) throws IOException {
            String path = file.getCanonicalPath();
            if (!file.isFile()) {
                throw new IOException(path + ": the specified file does not exist");
            }
            if (!file.canRead()) {
                throw new IOException(path + ": the specified file can not be read");
            }
            // this instance is backed by a 'real' file
            this.file = file;
            // this instance is not backed by temporarily allocated resource/buffer
            temp = false;
        }

        /**
         * Creates a new <code>BinaryQValue</code> instance from an
         * <code>InputStream</code>. The contents of the stream is spooled
         * to a temporary file or to a byte buffer if its size is smaller than
         * {@link #MAX_BUFFER_SIZE}.
         * <p/>
         * The <code>temp</code> parameter governs whether dynamically allocated
         * resources will be freed explicitly on {@link #discard()}. Note that any
         * dynamically allocated resources (temp file/buffer) will be freed
         * implicitly once this instance has been gc'ed.
         *
         * @param in stream to be represented as a <code>BinaryQValue</code> instance
         * @param temp flag indicating whether this instance represents a
         *             <i>temporary</i> value whose resources can be explicitly freed
         *             on {@link #discard()}.
         * @throws IOException if an error occurs while reading from the stream or
         *                     writing to the temporary file
         */
        private void init(InputStream in, boolean temp) throws IOException {
            byte[] spoolBuffer = new byte[0x2000];
            int read;
            int len = 0;
            OutputStream out = null;
            File spoolFile = null;
            try {
                while ((read = in.read(spoolBuffer)) > 0) {
                    if (out != null) {
                        // spool to temp file
                        out.write(spoolBuffer, 0, read);
                        len += read;
                    } else if (len + read > BinaryQValue.MAX_BUFFER_SIZE) {
                        // threshold for keeping data in memory exceeded;
                        // create temp file and spool buffer contents
                        TransientFileFactory fileFactory = TransientFileFactory.getInstance();
                        spoolFile = fileFactory.createTransientFile("bin", null, null);
                        out = new FileOutputStream(spoolFile);
                        out.write(buffer, 0, len);
                        out.write(spoolBuffer, 0, read);
                        buffer = null;
                        len += read;
                    } else {
                        // reallocate new buffer and spool old buffer contents
                        if (buffer == null) {
                            buffer = EMPTY_BYTE_ARRAY;
                        }
                        byte[] newBuffer = new byte[len + read];
                        System.arraycopy(buffer, 0, newBuffer, 0, len);
                        System.arraycopy(spoolBuffer, 0, newBuffer, len, read);
                        buffer = newBuffer;
                        len += read;
                    }
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            if (spoolFile == null && buffer == null) {
                // input stream was empty -> initialize an empty binary value
                this.temp = false;
                buffer = EMPTY_BYTE_ARRAY;
            } else {
                // init vars
                file = spoolFile;
                this.temp = temp;
            }
            initialized = true;
        }

        //---------------------------------------------------------< QValue >---
        /**
         * @see QValue#getType()
         */
        public int getType() {
            return PropertyType.BINARY;
        }

        /**
         * Returns the length of this <code>BinaryQValue</code>.
         *
         * @return The length, in bytes, of this <code>BinaryQValue</code>,
         *         or -1L if the length can't be determined.
         * @see QValue#getLength()
         */
        public long getLength() {
            if (file != null) {
                // this instance is backed by a 'real' file
                if (file.exists()) {
                    return file.length();
                } else {
                    return -1;
                }
            } else if (buffer != null) {
                // this instance is backed by an in-memory buffer
                return buffer.length;
            } else {
                // value has not yet been read from the server.
                return length;
            }
        }

        /**
         * @see QValue#getString()
         */
        public String getString() throws RepositoryException {
            if (text == null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    spool(out);
                    byte[] data = out.toByteArray();
                    text = new String(data, QValueFactoryImpl.DEFAULT_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    throw new RepositoryException(QValueFactoryImpl.DEFAULT_ENCODING
                        + " not supported on this platform", e);
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
         * @see QValue#getStream()
         */
        public InputStream getStream() throws RepositoryException {
            // if the value has not yet been loaded -> retrieve it first in
            // order to make sure that either 'file' or 'buffer' is set.
            if (file == null && buffer == null) {
                try {
                    loadBinary();
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
            }

            // always return a 'fresh' stream
            if (file != null) {
                // this instance is backed by a 'real' file
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException fnfe) {
                    throw new RepositoryException("file backing binary value not found",
                        fnfe);
                }
            } else {
                return new ByteArrayInputStream(buffer);
            }
        }

        /**
         * @see QValue#getName()
         */
        public Name getName() throws RepositoryException {
            throw new UnsupportedOperationException();
        }

        /**
         * @see QValue#getCalendar()
         */
        public Calendar getCalendar() throws RepositoryException {
             Calendar cal = ISO8601.parse(getString());
             if (cal == null) {
                 throw new ValueFormatException("not a date string: " + getString());
             } else {
                 return cal;
             }
        }

        /**
         * @see QValue#getDouble()
         */
        public double getDouble() throws RepositoryException {
            try {
                return Double.parseDouble(getString());
            } catch (NumberFormatException ex) {
                throw new ValueFormatException(ex);
            }
        }

        /**
         * @see QValue#getLong()
         */
        public long getLong() throws RepositoryException {
            try {
                return Long.parseLong(getString());
            } catch (NumberFormatException ex) {
                throw new ValueFormatException(ex);
            }
        }

        public boolean getBoolean() throws RepositoryException {
            return new Boolean(getString()).booleanValue();
        }

        /**
         * @see QValue#getPath()
         */
        public Path getPath() throws RepositoryException {
            throw new UnsupportedOperationException();
        }

        /**
         * Frees temporarily allocated resources such as temporary file, buffer, etc.
         * If this <code>BinaryQValue</code> is backed by a persistent resource
         * calling this method will have no effect.
         * @see QValue#discard()
         */
        public void discard() {
            if (!temp) {
                // do nothing if this instance is not backed by temporarily
                // allocated resource/buffer
                return;
            }
            if (file != null) {
                // this instance is backed by a temp file
                file.delete();
            } else if (buffer != null) {
                // this instance is backed by an in-memory buffer
                buffer = EMPTY_BYTE_ARRAY;
            }
        }

        //-----------------------------------------------< java.lang.Object >---
        /**
         * Returns a string representation of this <code>BinaryQValue</code>
         * instance. The string representation of a resource backed value is
         * the path of the underlying resource. If this instance is backed by an
         * in-memory buffer the generic object string representation of the byte
         * array will be used instead.
         *
         * @return A string representation of this <code>BinaryQValue</code> instance.
         */
        public String toString() {
            if (file != null) {
                // this instance is backed by a 'real' file
                return file.toString();
            } else if (buffer != null) {
                // this instance is backed by an in-memory buffer
                return buffer.toString();
            } else {
                return super.toString();
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BinaryQValue) {
                BinaryQValue other = (BinaryQValue) obj;
                // for both the value has not been loaded yet
                if (!initialized) {
                    if (other.uri != null) {
                        return other.uri.equals(uri);
                    } else {
                        // need to load the binary value in order to be able
                        // to compare the 2 values.
                        try {
                            loadBinary();
                        } catch (RepositoryException e) {
                            return false;
                        } catch (IOException e) {
                            return false;
                        }
                    }
                }
                // both have been loaded
                return ((file == null ? other.file == null : file.equals(other.file))
                    && Arrays.equals(buffer, other.buffer));
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

        //----------------------------------------------------------------------
        /**
         * Spools the contents of this <code>BinaryQValue</code> to the given
         * output stream.
         *
         * @param out output stream
         * @throws RepositoryException if the input stream for this
         *                             <code>BinaryQValue</code> could not be obtained
         * @throws IOException         if an error occurs while while spooling
         */
        private void spool(OutputStream out) throws RepositoryException, IOException {
            InputStream in;
            if (file != null) {
                // this instance is backed by a 'real' file
                try {
                    in = new FileInputStream(file);
                } catch (FileNotFoundException fnfe) {
                    throw new RepositoryException("file backing binary value not found",
                        fnfe);
                }
            } else if (buffer != null) {
                // this instance is backed by an in-memory buffer
                in = new ByteArrayInputStream(buffer);
            } else {
                // only uri present:
                loadBinary();
                if (buffer == null) {
                    in = new FileInputStream(file);
                } else {
                    in = new ByteArrayInputStream(buffer);
                }
            }
            try {
                byte[] buffer = new byte[0x2000];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }

        private synchronized void loadBinary() throws RepositoryException, IOException {
            if (uri == null) {
                throw new IllegalStateException();
            }
            loader.loadBinary(uri, index, this);
        }

        //-----------------------------< Serializable >-------------------------
        private void writeObject(ObjectOutputStream out)
                throws IOException {
            out.defaultWriteObject();
            // write hasFile marker
            out.writeBoolean(file != null);
            // then write file if necessary
            if (file != null) {
                byte[] buffer = new byte[4096];
                int bytes;
                InputStream stream = new FileInputStream(file);
                while ((bytes = stream.read(buffer)) >= 0) {
                    // Write a segment of the input stream
                    if (bytes > 0) {
                        // just to ensure that no 0 is written
                        out.writeInt(bytes);
                        out.write(buffer, 0, bytes);
                    }
                }
                // Write the end of stream marker
                out.writeInt(0);
                // close stream
                stream.close();
            }
        }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            boolean hasFile = in.readBoolean();
            if (hasFile) {
                file = File.createTempFile("binary-qvalue", "bin");

                OutputStream out = new FileOutputStream(file);
                byte[] buffer = new byte[4096];
                for (int bytes = in.readInt(); bytes > 0; bytes = in.readInt()) {
                    if (buffer.length < bytes) {
                        buffer = new byte[bytes];
                    }
                    in.readFully(buffer, 0, bytes);
                    out.write(buffer, 0, bytes);
                }
                out.close();
            }
            // deserialized value is always temp
            temp = true;
        }

        //---------------------------------------------------------< Target >---
        public void setStream(InputStream in) throws IOException {
            if (index == -1) {
                init(in, true);
            } else {
                // TODO: improve. jcr-server sends XML for multivalued properties
                try {
                    DocumentBuilder db = DomUtil.BUILDER_FACTORY.newDocumentBuilder();
                    Document doc = db.parse(in);
                    Element prop = DomUtil.getChildElement(doc, ItemResourceConstants.JCR_VALUES.getName(), ItemResourceConstants.JCR_VALUES.getNamespace());
                    DavProperty p = DefaultDavProperty.createFromXml(prop);
                    ValuesProperty vp = new ValuesProperty(p, PropertyType.BINARY, ValueFactoryImpl.getInstance());

                    Value[] jcrVs = vp.getJcrValues();
                    init(jcrVs[index].getStream(), true);
                } catch (RepositoryException e) {
                    throw new IOException(e.getMessage());
                }catch (DavException e) {
                    throw new IOException(e.getMessage());
                } catch (SAXException e) {
                    throw new IOException(e.getMessage());
                } catch (ParserConfigurationException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }
}