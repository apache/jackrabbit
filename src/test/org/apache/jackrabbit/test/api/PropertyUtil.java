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
package org.apache.jackrabbit.test.api;

import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.PropertyType;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedInputStream;

/**
 * This class provides various utility methods that are used by the property
 * test cases.
 */
class PropertyUtil {

    private static final Pattern PATH_PATTERN = Pattern.compile("(\\.)|(\\.\\.)|(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)(\\[([1-9]\\d*)\\])?");
    private static final Pattern NAME_PATTERN = Pattern.compile("(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)");

    private static final String dateFormatPattern = "[0-9][0-9][0-9][0-9]-(0[1-9]|1[0-2])-(0[1-9]|1[0-9]||2[0-9]|3[01])T([0-1][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9].[0-9][0-9][0-9](Z|[+-]([0-1][0-9]|2[0-3]):[0-5][0-9])";
    private static final Pattern DATE_PATTERN = Pattern.compile(dateFormatPattern);

    private static final Pattern UUID_PATTERN = Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

    /**
     * Private constructor to disable instantiation.
     */
    private PropertyUtil() {
    }

    /**
     * Traverses a tree below a given node searching for a property with a given
     * type
     *
     * @param node the node to start traverse
     * @param type the property type to search for
     * @return the property found or null if no property is found
     */
    public static Property searchProp(Session session, Node node, int type)
            throws RepositoryException, ValueFormatException {

        Property prop = null;
        int propType = PropertyType.UNDEFINED;
        if (prop == null) {
            for (PropertyIterator props = node.getProperties(); props.hasNext();) {
                Property property = props.nextProperty();
                propType = property.getType();
                if (propType == type) {
                    prop = property;
                    break;
                }
            }
        }
        if (prop == null) {
            for (NodeIterator nodes = node.getNodes(); nodes.hasNext();) {
                Node n = nodes.nextNode();
                prop = searchProp(session, n, type);
                if (prop != null) {
                    break;
                }
            }
        }
        return prop;
    }

    /**
     * Returns the value of a property. If <code>prop</code> is multi valued
     * this method returns the first value.
     *
     * @param prop the property from which to return the value.
     * @return the value of the property.
     */
    public static Value getValue(Property prop) throws RepositoryException {
        Value val;
        if (prop.getDefinition().isMultiple()) {
            Value[] vals = prop.getValues();
            if (vals.length > 0) {
                val = vals[0];
            } else {
                val = null;
            }
        } else {
            val = prop.getValue();
        }
        return val;
    }

    /**
     * checks if the given name follows the NAME syntax rules and if a present
     * prefix is mapped to a registered namespace
     *
     * @param name the string to test
     */
    public static boolean checkNameFormat(String name, Session session) throws RepositoryException {
        if (name == null || name.length() == 0) {
            return false;
        } else {
            NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
            boolean prefixOk = true;
            // validate name element
            Matcher matcher = NAME_PATTERN.matcher(name);
            // validate namespace prefixes if present
            String[] split = name.split(":");
            if (split.length > 1) {
                String prefix = split[0];
                try {
                    nsr.getURI(prefix);
                } catch (NamespaceException nse) {
                    prefixOk = false;
                }
            }
            return matcher.matches() && prefixOk;
        }
    }

    /**
     * Checks if the given path follows the path syntax rules.
     *
     * @param jcrPath the string to test
     */
    public static boolean checkPathFormat(String jcrPath, Session session) throws RepositoryException {
        if (jcrPath == null || jcrPath.length() == 0) {
            return false;
        } else if (jcrPath.equals("/")) {
            return true;
        } else {
            NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
            boolean match = false;
            boolean prefixOk = true;
            // split path into path elements and validate each of them
            String[] elems = jcrPath.split("/", -1);
            for (int i = jcrPath.startsWith("/") ? 1 : 0; i < elems.length; i++) {
                // validate path element
                String elem = elems[i];
                Matcher matcher = PATH_PATTERN.matcher(elem);
                match = matcher.matches();
                if (!match) {
                    break;
                }
                // validate namespace prefixes if present
                String[] split = elem.split(":");
                if (split.length > 1) {
                    String prefix = split[0];
                    try {
                        nsr.getURI(prefix);
                    } catch (NamespaceException nse) {
                        prefixOk = false;
                        break;
                    }
                }
            }
            return match && prefixOk;
        }
    }

    /**
     * Checks if the String is a valid date in string format.
     *
     * @param str the string to test.
     * @return <code>true</code> if <code>str</code> is a valid date format.
     */
    public static boolean isDateFormat(String str) {
        return DATE_PATTERN.matcher(str).matches();
    }

    /**
     * Checks if the String is a UUID.
     *
     * @param str the string to test.
     * @return <code>true</code> if <code>str</code> is a UUID.
     */
    public static boolean isUUID(String str) {
        return UUID_PATTERN.matcher(str).matches();
    }

    /**
     * Counts the number of bytes of a Binary value.
     *
     * @param val the binary value.
     * @return the number of bytes or -1 in case of any exception
     */
    public static long countBytes(Value val) {
        int length = 0;
        try {
            BufferedInputStream bin = new BufferedInputStream(val.getStream());
            while (bin.read() != -1) {
                length++;
            }
            bin.close();
        } catch (Exception e) {
            length = -1;
        }
        return length;
    }

    /**
     * Helper method to test the type received with Value.getType() and
     * Property.getType() .
     */
    public static boolean checkGetType(Property prop, int propType) throws RepositoryException {
        Value val = getValue(prop);
        boolean samePropType = (val.getType() == propType);
        int requiredType = prop.getDefinition().getRequiredType();
        if (requiredType != PropertyType.UNDEFINED) {
            samePropType = (val.getType() == requiredType);
        }
        return samePropType;
    }

    /**
     * Helper method to compare the equality of two values for equality with the
     * fulfilling of the equality conditions. These conditions for the values
     * are to have the same type and the same string representation.
     *
     * @param val1 first value
     * @param val2 second value
     * @return true if the equals method is equivalent to the normative
     *         definition of value equality, false in the other case.
     */
    public static boolean equalValues(Value val1, Value val2) throws RepositoryException {

        boolean isEqual = val1.equals(val2);
        boolean conditions = false;
        try {
            conditions = (val1.getType() == val2.getType())
                    && val1.getString().equals(val2.getString());
        } catch (ValueFormatException vfe) {
            return false;
        }
        return (isEqual == conditions);
    }

    /**
     * Helper method to assure that no property with a null value exist.
     *
     * @param node the node to start the search from.
     * @return <code>true</code> if a null value property is found;
     *         <code>false</code> in the other case.
     */
    public static boolean nullValues(Node node) throws RepositoryException {
        boolean nullValue = false;
        for (PropertyIterator props = node.getProperties(); props.hasNext();) {
            Property property = props.nextProperty();
            if (!property.getDefinition().isMultiple()) {
                nullValue = (property.getValue() == null);
                if (nullValue) {
                    break;
                }
            }
        }

        if (!nullValue) {
            for (NodeIterator nodes = node.getNodes(); nodes.hasNext();) {
                Node n = nodes.nextNode();
                nullValue = nullValues(n);
            }
        }
        return nullValue;
    }

    /**
     * Helper method to find a multivalue property.
     *
     * @param node the node to start the search from.
     * @return a multivalue property or null if not found any.
     */
    public static Property searchMultivalProp(Node node) throws RepositoryException {
        Property multiVal = null;
        for (PropertyIterator props = node.getProperties(); props.hasNext();) {
            Property property = props.nextProperty();
            if (property.getDefinition().isMultiple()) {
                multiVal = property;
                break;
            }
        }

        if (multiVal == null) {
            for (NodeIterator nodes = node.getNodes(); nodes.hasNext();) {
                Node n = nodes.nextNode();
                multiVal = searchMultivalProp(n);
            }
        }
        return multiVal;
    }
}