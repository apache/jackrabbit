/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.util;

import org.apache.jackrabbit.core.IllegalNameException;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.QName;

import javax.jcr.*;

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

    /**
     * @param srcValue
     * @param targetType
     * @return
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public static Value convert(String srcValue, int targetType)
            throws ValueFormatException, IllegalStateException, IllegalArgumentException {
        return convert(new StringValue(srcValue), targetType);
    }

    /**
     * @param srcValue
     * @param targetType
     * @return
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public static Value convert(Value srcValue, int targetType)
            throws ValueFormatException, IllegalStateException, IllegalArgumentException {
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
                    val = new StringValue(srcValue.getString());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.BINARY:
                // convert to BINARY
                try {
                    val = new BinaryValue(srcValue.getStream());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.BOOLEAN:
                // convert to BOOLEAN
                try {
                    val = new BooleanValue(srcValue.getBoolean());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.DATE:
                // convert to DATE
                try {
                    val = new DateValue(srcValue.getDate());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.DOUBLE:
                // convert to DOUBLE
                try {
                    val = new DoubleValue(srcValue.getDouble());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.LONG:
                // convert to LONG
                try {
                    val = new LongValue(srcValue.getLong());
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
                    case PropertyType.NAME:	// a name is always also a relative path
                        // try conversion via string
                        String path;
                        try {
                            // get string value
                            path = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to PATH value", re);
                        }
                        try {
                            // check path format
                            Path.checkFormat(path);
                        } catch (MalformedPathException mpe) {
                            throw new ValueFormatException("source value " + path + " does not represent a valid path", mpe);
                        }
                        val = PathValue.valueOf(path);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.REFERENCE:
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
                    case PropertyType.PATH:	// path might be a name (relative, onle element long path)
                        // try conversion via string
                        String name;
                        try {
                            // get string value
                            name = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to NAME value", re);
                        }
                        try {
                            // check name format
                            QName.checkFormat(name);
                        } catch (IllegalNameException ine) {
                            throw new ValueFormatException("source value " + name + " does not represent a valid name", ine);
                        }
                        val = NameValue.valueOf(name);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.REFERENCE:
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
                        // try conversion via string
                        String uuid;
                        try {
                            // get string value
                            uuid = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to REFERENCE value", re);
                        }
                        val = ReferenceValue.valueOf(uuid);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.PATH:
                    case PropertyType.NAME:
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
     * @param srcVal
     * @return
     * @throws IllegalStateException
     */
    public static Value copy(Value srcVal) throws IllegalStateException {
        Value newVal = null;
        try {
            switch (srcVal.getType()) {
                case PropertyType.BINARY:
                    newVal = new BinaryValue(srcVal.getStream());
                    break;

                case PropertyType.BOOLEAN:
                    newVal = new BooleanValue(srcVal.getBoolean());
                    break;

                case PropertyType.DATE:
                    newVal = new DateValue(srcVal.getDate());
                    break;

                case PropertyType.DOUBLE:
                    newVal = new DoubleValue(srcVal.getDouble());
                    break;

                case PropertyType.LONG:
                    newVal = new LongValue(srcVal.getLong());
                    break;

                case PropertyType.PATH:
                    newVal = PathValue.valueOf(srcVal.getString());
                    break;

                case PropertyType.NAME:
                    newVal = NameValue.valueOf(srcVal.getString());
                    break;

                case PropertyType.REFERENCE:
                    newVal = ReferenceValue.valueOf(srcVal.getString());
                    break;

                case PropertyType.STRING:
                    newVal = new StringValue(srcVal.getString());
                    break;
            }
        } catch (RepositoryException re) {
            // should never get here
        }
        return newVal;
    }

    /**
     * @param srcVal
     * @return
     * @throws IllegalStateException
     */
    public static Value[] copy(Value[] srcVal) throws IllegalStateException {
        if (srcVal == null) {
            return null;
        }
        Value[] newVal = new Value[srcVal.length];
        for (int i = 0; i < srcVal.length; i++) {
            newVal[i] = copy(srcVal[i]);
        }
        return newVal;
    }
}
