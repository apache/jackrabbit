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
package org.apache.jackrabbit.value;

import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.NAME;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.WEAKREFERENCE;

import org.apache.jackrabbit.util.Base64;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.util.TransientFileFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FilterInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The <code>ValueHelper</code> class provides several <code>Value</code>
 * related utility methods.
 */
public class ValueHelper {

    /**
     * empty private constructor
     */
    private ValueHelper() {
    }

    private static final Map<Integer, Set<Integer>> SUPPORTED_CONVERSIONS = new HashMap<Integer, Set<Integer>>();
    static {
        SUPPORTED_CONVERSIONS.put(DATE, immutableSetOf(STRING, BINARY, DOUBLE, DECIMAL, LONG));
        SUPPORTED_CONVERSIONS.put(DOUBLE, immutableSetOf(STRING, BINARY, DECIMAL, DATE, LONG));
        SUPPORTED_CONVERSIONS.put(DECIMAL, immutableSetOf(STRING, BINARY, DOUBLE, DATE, LONG));
        SUPPORTED_CONVERSIONS.put(LONG, immutableSetOf(STRING, BINARY, DECIMAL, DATE, DOUBLE));
        SUPPORTED_CONVERSIONS.put(BOOLEAN, immutableSetOf(STRING, BINARY));
        SUPPORTED_CONVERSIONS.put(NAME, immutableSetOf(STRING, BINARY, PATH, PropertyType.URI));
        SUPPORTED_CONVERSIONS.put(PATH, immutableSetOf(STRING, BINARY, NAME, PropertyType.URI));
        SUPPORTED_CONVERSIONS.put(PropertyType.URI, immutableSetOf(STRING, BINARY, NAME, PATH));
        SUPPORTED_CONVERSIONS.put(REFERENCE, immutableSetOf(STRING, BINARY, WEAKREFERENCE));
        SUPPORTED_CONVERSIONS.put(WEAKREFERENCE, immutableSetOf(STRING, BINARY, REFERENCE));
    }

    private static Set<Integer> immutableSetOf(int... types) {
        Set<Integer> t = new HashSet<Integer>();
        for (int type : types) {
            t.add(type);
        }
        return Collections.unmodifiableSet(t);
    }
    
    public static boolean isSupportedConversion(int fromType, int toType) {
        if (fromType == toType) {
            return true;
        } else if (STRING == fromType || BINARY == fromType) {
            return true;
        } else {
            return SUPPORTED_CONVERSIONS.containsKey(fromType) && SUPPORTED_CONVERSIONS.get(fromType).contains(toType);
        }
    }

    public static void checkSupportedConversion(int fromType, int toType) throws ValueFormatException {
        if (!isSupportedConversion(fromType, toType)) {
            throw new ValueFormatException("Unsupported conversion from '" + PropertyType.nameFromValue(fromType) + "' to '" + PropertyType.nameFromValue(toType) + '\'');
        }
    }

    /**
     * @param srcValue
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value convert(String srcValue, int targetType, ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValue == null) {
            return null;
        } else {
            return factory.createValue(srcValue, targetType);
        }
    }

    /**
     * @param srcValue
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     */
    public static Value convert(InputStream srcValue, int targetType, ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValue == null) {
            return null;
        } else {
            return convert(factory.createValue(srcValue), targetType, factory);
        }
    }

    /**
     * Same as {@link #convert(String[], int, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param srcValues
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(String[] srcValues, int targetType, ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValues == null) {
            return null;
        }
        Value[] newValues = new Value[srcValues.length];
        for (int i = 0; i < srcValues.length; i++) {
            newValues[i] = convert(srcValues[i], targetType, factory);
        }
        return newValues;
    }

    /**
     * @param srcValues
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(InputStream[] srcValues, int targetType,
                                  ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValues == null) {
            return null;
        }
        Value[] newValues = new Value[srcValues.length];
        for (int i = 0; i < srcValues.length; i++) {
            newValues[i] = convert(srcValues[i], targetType, factory);
        }
        return newValues;
    }

    /**
     * @param srcValues
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(Value[] srcValues, int targetType,
                                  ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValues == null) {
            return null;
        }

        Value[] newValues = new Value[srcValues.length];
        int srcValueType = PropertyType.UNDEFINED;
        for (int i = 0; i < srcValues.length; i++) {
            if (srcValues[i] == null) {
                newValues[i] = null;
                continue;
            }
            // check type of values
            if (srcValueType == PropertyType.UNDEFINED) {
                srcValueType = srcValues[i].getType();
            } else if (srcValueType != srcValues[i].getType()) {
                // inhomogeneous types
                String msg = "inhomogeneous type of values";
                throw new ValueFormatException(msg);
            }

            newValues[i] = convert(srcValues[i], targetType, factory);
        }
        return newValues;
    }

    /**
     * Converts the given value to a value of the specified target type.
     * The conversion is performed according to the rules described in
     * "3.6.4 Property Type Conversion" in the JSR 283 specification.
     *
     * @param srcValue
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public static Value convert(Value srcValue, int targetType, ValueFactory factory)
            throws ValueFormatException, IllegalStateException,
            IllegalArgumentException {
        if (srcValue == null) {
            return null;
        }

        Value val;
        int srcType = srcValue.getType();

        if (srcType == targetType) {
            // no conversion needed, return original value
            return srcValue;
        }

        switch (targetType) {
            case PropertyType.STRING:
                // convert to STRING
                try {
                    val = factory.createValue(srcValue.getString());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.BINARY:
                // convert to BINARY
                try {
                    val = factory.createValue(srcValue.getBinary());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.BOOLEAN:
                // convert to BOOLEAN
                try {
                    val = factory.createValue(srcValue.getBoolean());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.DATE:
                // convert to DATE
                try {
                    val = factory.createValue(srcValue.getDate());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.DOUBLE:
                // convert to DOUBLE
                try {
                    val = factory.createValue(srcValue.getDouble());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.LONG:
                // convert to LONG
                try {
                    val = factory.createValue(srcValue.getLong());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.DECIMAL:
                // convert to DECIMAL
                try {
                    val = factory.createValue(srcValue.getDecimal());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.PATH:
                // convert to PATH
                switch (srcType) {
                    case PropertyType.PATH:
                        // no conversion needed, return original value
                        // (redundant code, just here for the sake of clarity)
                        return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                    case PropertyType.NAME: // a name is always also a relative path
                        // try conversion via string
                        String path;
                        try {
                            // get string value
                            path = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to PATH value",
                                    re);
                        }
                        // the following call will throw ValueFormatException
                        // if p is not a valid PATH
                        val = factory.createValue(path, targetType);
                        break;

                    case PropertyType.URI:
                        URI uri;
                        try {
                            uri = URI.create(srcValue.getString());
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to PATH value",
                                    re);
                        }
                        if (uri.isAbsolute()) {
                            // uri contains scheme...
                            throw new ValueFormatException("failed to convert URI value to PATH value");
                        }
                        String p = uri.getPath();

                        if (p.startsWith("./")) {
                            p = p.substring(2);
                        }

                        // the following call will throw ValueFormatException
                        // if p is not a valid PATH
                        val = factory.createValue(p, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.DECIMAL:
                    case PropertyType.LONG:
                    case PropertyType.REFERENCE:
                    case PropertyType.WEAKREFERENCE:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            case PropertyType.NAME:
                // convert to NAME
                switch (srcType) {
                    case PropertyType.NAME:
                        // no conversion needed, return original value
                        // (redundant code, just here for the sake of clarity)
                        return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                    case PropertyType.PATH: // path might be a name (relative path of length 1)
                        // try conversion via string
                        String name;
                        try {
                            // get string value
                            name = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to NAME value",
                                    re);
                        }
                        // the following call will throw ValueFormatException
                        // if p is not a valid NAME
                        val = factory.createValue(name, targetType);
                        break;

                    case PropertyType.URI:
                        URI uri;
                        try {
                            uri = URI.create(srcValue.getString());
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to NAME value",
                                    re);
                        }
                        if (uri.isAbsolute()) {
                            // uri contains scheme...
                            throw new ValueFormatException("failed to convert URI value to NAME value");
                        }
                        String p = uri.getPath();

                        if (p.startsWith("./")) {
                            p = p.substring(2);
                        }

                        // the following call will throw ValueFormatException
                        // if p is not a valid NAME
                        val = factory.createValue(p, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.DECIMAL:
                    case PropertyType.LONG:
                    case PropertyType.REFERENCE:
                    case PropertyType.WEAKREFERENCE:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            case PropertyType.REFERENCE:
                // convert to REFERENCE
                switch (srcType) {
                    case PropertyType.REFERENCE:
                        // no conversion needed, return original value
                        // (redundant code, just here for the sake of clarity)
                        return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                    case PropertyType.WEAKREFERENCE:
                        // try conversion via string
                        String uuid;
                        try {
                            // get string value
                            uuid = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to REFERENCE value", re);
                        }
                        val = factory.createValue(uuid, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.DECIMAL:
                    case PropertyType.PATH:
                    case PropertyType.URI:
                    case PropertyType.NAME:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            case PropertyType.WEAKREFERENCE:
                // convert to WEAKREFERENCE
                switch (srcType) {
                    case PropertyType.WEAKREFERENCE:
                        // no conversion needed, return original value
                        // (redundant code, just here for the sake of clarity)
                        return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                    case PropertyType.REFERENCE:
                        // try conversion via string
                        String uuid;
                        try {
                            // get string value
                            uuid = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to WEAKREFERENCE value", re);
                        }
                        val = factory.createValue(uuid, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.DECIMAL:
                    case PropertyType.URI:
                    case PropertyType.PATH:
                    case PropertyType.NAME:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            case PropertyType.URI:
                // convert to URI
                switch (srcType) {
                    case PropertyType.URI:
                        // no conversion needed, return original value
                        // (redundant code, just here for the sake of clarity)
                        return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                        // try conversion via string
                        String uuid;
                        try {
                            // get string value
                            uuid = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to URI value", re);
                        }
                        val = factory.createValue(uuid, targetType);
                        break;

                    case PropertyType.NAME:
                        String name;
                        try {
                            // get string value
                            name = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to URI value", re);
                        }
                        // prefix name with "./" (jsr 283 spec 3.6.4.8)
                        val = factory.createValue("./" + name, targetType);
                        break;

                    case PropertyType.PATH:
                        String path;
                        try {
                            // get string value
                            path = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to URI value", re);
                        }
                        if (!path.startsWith("/")) {
                            // prefix non-absolute path with "./" (jsr 283 spec 3.6.4.9)
                            path = "./" + path;
                        }
                        val = factory.createValue(path, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.DECIMAL:
                    case PropertyType.REFERENCE:
                    case PropertyType.WEAKREFERENCE:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            default:
                throw new IllegalArgumentException("not a valid type constant: " + targetType);
        }

        return val;
    }

    /**
     *
     * @param srcValue
     * @param factory
     * @throws IllegalStateException
     */
    public static Value copy(Value srcValue, ValueFactory factory)
            throws IllegalStateException {
        if (srcValue == null) {
            return null;
        }

        Value newVal = null;
        try {
            switch (srcValue.getType()) {
                case PropertyType.BINARY:
                    newVal = factory.createValue(srcValue.getStream());
                    break;

                case PropertyType.BOOLEAN:
                    newVal = factory.createValue(srcValue.getBoolean());
                    break;

                case PropertyType.DATE:
                    newVal = factory.createValue(srcValue.getDate());
                    break;

                case PropertyType.DOUBLE:
                    newVal = factory.createValue(srcValue.getDouble());
                    break;

                case PropertyType.LONG:
                    newVal = factory.createValue(srcValue.getLong());
                    break;

                case PropertyType.DECIMAL:
                    newVal = factory.createValue(srcValue.getDecimal());
                    break;

                case PropertyType.PATH:
                case PropertyType.NAME:
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE:
                case PropertyType.URI:
                    newVal = factory.createValue(srcValue.getString(), srcValue.getType());
                    break;

                case PropertyType.STRING:
                    newVal = factory.createValue(srcValue.getString());
                    break;
            }
        } catch (RepositoryException re) {
            // should never get here
        }
        return newVal;
    }

    /**
     * @param srcValues
     * @param factory
     * @throws IllegalStateException
     */
    public static Value[] copy(Value[] srcValues, ValueFactory factory)
            throws IllegalStateException {
        if (srcValues == null) {
            return null;
        }

        Value[] newValues = new Value[srcValues.length];
        for (int i = 0; i < srcValues.length; i++) {
            newValues[i] = copy(srcValues[i], factory);
        }
        return newValues;
    }

    /**
     * Serializes the given value to a <code>String</code>. The serialization
     * format is the same as used by Document &amp; System View XML, i.e.
     * binary values will be Base64-encoded whereas for all others
     * <code>{@link Value#getString()}</code> will be used.
     *
     * @param value        the value to be serialized
     * @param encodeBlanks if <code>true</code> space characters will be encoded
     *                     as <code>"_x0020_"</code> within he output string.
     * @return a string representation of the given value.
     * @throws IllegalStateException if the given value is in an illegal state
     * @throws RepositoryException   if an error occured during the serialization.
     */
    public static String serialize(Value value, boolean encodeBlanks)
            throws IllegalStateException, RepositoryException {
        StringWriter writer = new StringWriter();
        try {
            serialize(value, encodeBlanks, false, writer);
        } catch (IOException ioe) {
            throw new RepositoryException("failed to serialize value",
                    ioe);
        }
        return writer.toString();
    }

    /**
     * Outputs the serialized value to a <code>Writer</code>. The serialization
     * format is the same as used by Document &amp; System View XML, i.e.
     * binary values will be Base64-encoded whereas for all others
     * <code>{@link Value#getString()}</code> will be used for serialization.
     *
     * @param value        the value to be serialized
     * @param encodeBlanks if <code>true</code> space characters will be encoded
     *                     as <code>"_x0020_"</code> within he output string.
     * @param enforceBase64 if <code>true</code>, base64 encoding will always be used
     * @param writer       writer to output the encoded data
     * @throws IllegalStateException if the given value is in an illegal state
     * @throws IOException           if an i/o error occured during the
     *                               serialization
     * @throws RepositoryException   if an error occured during the serialization.
     */
    public static void serialize(Value value, boolean encodeBlanks, boolean enforceBase64,
                                 Writer writer)
            throws IllegalStateException, IOException, RepositoryException {
        if (value.getType() == PropertyType.BINARY) {
            // binary data, base64 encoding required;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain space characters
            InputStream in = value.getStream();
            try {
                Base64.encode(in, writer);
                // no need to close StringWriter
                //writer.close();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        } else {
            String textVal = value.getString();
            if (enforceBase64) {
                byte bytes[] = textVal.getBytes(StandardCharsets.UTF_8);
                Base64.encode(bytes, 0, bytes.length, writer);
            }
            else {
                if (encodeBlanks) {
                    // enocde blanks in string
                    textVal = Text.replace(textVal, " ", "_x0020_");
                }
                writer.write(textVal);
            }
        }
    }

    /**
     * Deserializes the given string to a <code>Value</code> of the given type.
     *
     * @param value        string to be deserialized
     * @param type         type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code>
     *                     character sequences will be decoded to single space
     *                     characters each.
     * @param factory      ValueFactory used to build the <code>Value</code> object.
     * @return the deserialized <code>Value</code>
     * @throws ValueFormatException if the string data is not of the required
     *                              format
     * @throws RepositoryException  if an error occured during the
     *                              deserialization.
     */
    public static Value deserialize(String value, int type, boolean decodeBlanks,
                                    ValueFactory factory)
            throws ValueFormatException, RepositoryException {
        if (type == PropertyType.BINARY) {
            // base64 encoded binary value;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain encoded space characters
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                Base64.decode(value, baos);
                // no need to close ByteArrayOutputStream
                //baos.close();
            } catch (IOException ioe) {
                throw new RepositoryException("failed to decode binary value",
                        ioe);
            }
            // NOTE: for performance reasons the BinaryValue is created directly
            // from the byte-array. This is inconsistent with the other calls,
            // that delegate the value creation to the ValueFactory.
            return new BinaryValue(baos.toByteArray());
        } else {
            if (decodeBlanks) {
                // decode encoded blanks in value
                value = Text.replace(value, "_x0020_", " ");
            }
            return convert(value, type, factory);
        }
    }

    /**
     * Deserializes the string data read from the given reader to a
     * <code>Value</code> of the given type.
     *
     * @param reader       reader for the string data to be deserialized
     * @param type         type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code>
     *                     character sequences will be decoded to single space
     *                     characters each.
     * @param factory      ValueFactory used to build the <code>Value</code> object.
     * @return the deserialized <code>Value</code>
     * @throws IOException          if an i/o error occured during the
     *                              serialization
     * @throws ValueFormatException if the string data is not of the required
     *                              format
     * @throws RepositoryException  if an error occured during the
     *                              deserialization.
     */
    public static Value deserialize(Reader reader, int type,
                                    boolean decodeBlanks, ValueFactory factory)
            throws IOException, ValueFormatException, RepositoryException {
        if (type == PropertyType.BINARY) {
            // base64 encoded binary value;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain encoded space characters

            // decode to temp file
            TransientFileFactory fileFactory = TransientFileFactory.getInstance();
            final File tmpFile = fileFactory.createTransientFile("bin", null, null);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile));
            try {
                Base64.decode(reader, out);
            } finally {
                out.close();
            }

            // create an InputStream that keeps a hard reference to the temp file
            // in order to prevent its automatic deletion once the associated
            // File object is reclaimed by the garbage collector;
            // pass InputStream wrapper to ValueFactory, that creates a BinaryValue.
            return factory.createValue(new FilterInputStream(new FileInputStream(tmpFile)) {

                public void close() throws IOException {
                    in.close();
                    // temp file can now safely be removed
                    tmpFile.delete();
                }
            });
/*
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Base64.decode(reader, baos);
            // no need to close ByteArrayOutputStream
            //baos.close();
            return new BinaryValue(baos.toByteArray());
*/
        } else {
            char[] chunk = new char[8192];
            int read;
            StringBuilder buf = new StringBuilder();
            while ((read = reader.read(chunk)) > -1) {
                buf.append(chunk, 0, read);
            }
            String value = buf.toString();
            if (decodeBlanks) {
                // decode encoded blanks in value
                value = Text.replace(value, "_x0020_", " ");
            }
            return convert(value, type, factory);
        }
    }

    /**
     * Determine the {@link javax.jcr.PropertyType} of the passed values if all are of
     * the same type.
     *
     * @param values array of values of the same type
     * @return  {@link javax.jcr.PropertyType#UNDEFINED} if {@code values} is empty,
     *          {@code values[0].getType()} otherwise.
     * @throws javax.jcr.ValueFormatException  if not all {@code values} are of the same type
     */
    public static int getType(Value[] values) throws ValueFormatException {
        int type = UNDEFINED;
        for (Value value : values) {
            if (value != null) {
                if (type == UNDEFINED) {
                    type = value.getType();
                } else if (value.getType() != type) {
                    throw new ValueFormatException(
                            "All values of a multi-valued property must be of the same type");
                }
            }
        }
        return type;
    }

}
