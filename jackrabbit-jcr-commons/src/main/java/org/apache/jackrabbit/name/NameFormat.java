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
package org.apache.jackrabbit.name;

import org.apache.jackrabbit.util.XMLChar;

import javax.jcr.NamespaceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>NameFormat</code> provides methods for formatting and parsing names.
 *
 * @deprecated Use the NameResolver interface from 
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class NameFormat {

    /**
     * The regular expression pattern used to validate and parse
     * qualified names.
     * <p/>
     * The pattern contains the following groups:
     * <ul>
     * <li>group 1 is namespace prefix incl. delimiter (colon)
     * <li>group 2 is namespace prefix excl. delimiter (colon)
     * <li>group 3 is localName
     * </ul>
     */
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?"
            + "([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)");

    /**
     * Matcher instance as thread-local.
     */
    private static final ThreadLocal NAME_MATCHER = new ThreadLocal() {
        protected Object initialValue() {
            return NAME_PATTERN.matcher("dummy");
        }
    };

    /**
     * Converts the <code>jcrName</code> to its corresponding <code>QName</code>.
     * If the <code>resolver</code> passed is also an instance of
     * {@link NameCache} then this method first attempts to find the
     * corresponding <code>QName</code> in the cache. If it cannot be found then
     * the <code>jcrName</code> is parsed and the corresponding
     * <code>QName</code> constructed.
     *
     * @param jcrName  the JCR-style name to be parsed
     * @param resolver <code>NamespaceResolver</code> used for resolving
     *                 prefixes into namespace URIs
     * @return the resulting <code>QName</code>
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    public static QName parse(String jcrName, NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {

        if (resolver instanceof NameCache) {
            QName name = ((NameCache) resolver).retrieveName(jcrName);
            if (name == null) {
                name = parseIgnoreCache(jcrName, resolver);
                ((NameCache) resolver).cacheName(jcrName, name);
            }
            return name;
        } else {
            return parseIgnoreCache(jcrName, resolver);
        }
    }

    /**
     * Converts each JCR-style name in the passed array to its corresponding
     * <code>QName</code> and returns the resulting <code>QName</code> array.
     * If the <code>resolver</code> passed is also an instance of
     * {@link NameCache} then this method first attempts to find the
     * corresponding <code>QName</code> in the cache. If it cannot be found then
     * the <code>jcrName</code> is parsed and the corresponding
     * <code>QName</code> constructed.
     *
     * @param jcrNames the array of JCR-style names to be parsed
     * @param resolver <code>NamespaceResolver</code> used for resolving
     *                 prefixes into namespace URIs
     * @return the resulting <code>QName</code> array
     * @throws IllegalNameException If any of the passed names is not a valid
     *                              JCR-style name.
     */
    public static QName[] parse(String jcrNames[], NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {

        QName[] ret = new QName[jcrNames.length];
        if (resolver instanceof NameCache) {
            for (int i = 0; i < ret.length; i++) {
                QName name = ((NameCache) resolver).retrieveName(jcrNames[i]);
                if (name == null) {
                    name = parseIgnoreCache(jcrNames[i], resolver);
                    ((NameCache) resolver).cacheName(jcrNames[i], name);
                }
                ret[i] = name;
            }
        } else {
            for (int i = 0; i < ret.length; i++) {
                ret[i] = parseIgnoreCache(jcrNames[i], resolver);
            }
        }
        return ret;
    }

    /**
     * Checks if <code>jcrName</code> is a valid JCR-style name.
     *
     * @param jcrName the name to be checked
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    public static void checkFormat(String jcrName) throws IllegalNameException {
        doParse(jcrName);
    }

    /**
     * Formats the given <code>QName</code> to produce a string representation,
     * i.e. JCR-style name. If the <code>resolver</code> passed is also an
     * instance of {@link NameCache} then this method first attempts to find the
     * corresponding JCR-style name in the cache. If it cannot be found then
     * a new string representation is constructed.
     *
     * @param qName    the <code>QName</code> to format
     * @param resolver <code>NamespaceResolver</code> used for resolving
     *                 namespace URIs into prefixes
     * @return the string representation (JCR-style name) of the given
     *         <code>QName</code>
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     * @see #format(QName, NamespaceResolver, StringBuffer)
     */
    public static String format(QName qName, NamespaceResolver resolver)
            throws NoPrefixDeclaredException {

        if (resolver instanceof NameCache) {
            String jcrName = ((NameCache) resolver).retrieveName(qName);
            if (jcrName == null) {
                StringBuffer buf = new StringBuffer();
                formatIgnoreCache(qName, resolver, buf);
                jcrName = buf.toString();
                ((NameCache) resolver).cacheName(jcrName, qName);
            }
            return jcrName;

        } else {
            StringBuffer buf = new StringBuffer();
            formatIgnoreCache(qName, resolver, buf);
            return buf.toString();
        }
    }

    /**
     * Same as {@link #format(QName, NamespaceResolver)} except that this
     * method takes an array of <code>QName</code>s and returns an array of
     * corresponding string representations.
     *
     * @param qNames   the array <code>QName</code>s to format
     * @param resolver <code>NamespaceResolver</code> used for resolving
     *                 namespace URIs into prefixes
     * @return the array of corresponding string representations
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     * @see #format(QName, NamespaceResolver)
     */
    public static String[] format(QName[] qNames, NamespaceResolver resolver)
            throws NoPrefixDeclaredException {
        String[] ret = new String[qNames.length];
        if (resolver instanceof NameCache) {
            for (int i = 0; i < ret.length; i++) {
                String jcrName = ((NameCache) resolver).retrieveName(qNames[i]);
                if (jcrName == null) {
                    StringBuffer buf = new StringBuffer();
                    formatIgnoreCache(qNames[i], resolver, buf);
                    jcrName = buf.toString();
                    ((NameCache) resolver).cacheName(jcrName, qNames[i]);
                }
                ret[i] = jcrName;
            }
        } else {
            for (int i = 0; i < ret.length; i++) {
                StringBuffer buf = new StringBuffer();
                formatIgnoreCache(qNames[i], resolver, buf);
                ret[i] = buf.toString();
            }
        }
        return ret;
    }

    /**
     * Same as {@link #format(QName, NamespaceResolver)} except that this
     * method appends the JCR-style name to the given <code>buffer</code> rather
     * than returning it directly.
     *
     * @param qName    the <code>QName</code> to format
     * @param resolver <code>NamespaceResolver</code> used for resolving
     *                 namespace URIs into prefixes
     * @param buffer   StringBuffer where the string representation should be
     *                 appended to
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     */
    public static void format(QName qName, NamespaceResolver resolver,
                              StringBuffer buffer)
            throws NoPrefixDeclaredException {

        if (resolver instanceof NameCache) {
            String jcrName = ((NameCache) resolver).retrieveName(qName);
            if (jcrName == null) {
                int l = buffer.length();
                formatIgnoreCache(qName, resolver, buffer);
                ((NameCache) resolver).cacheName(buffer.substring(l), qName);
            } else {
                buffer.append(jcrName);
            }
        } else {
            formatIgnoreCache(qName, resolver, buffer);
        }
    }

    //-------------------------------------------------------< implementation >
    /**
     * Converts the <code>jcrName</code> to its corresponding <code>QName</code>.
     * <p/>
     * Note that unlike {@link #parse(String, NamespaceResolver)} this method
     * always constructs a new <code>QName</code>, ignoring potential caching
     * capabilities of the passed <code>resolver</code>.
     *
     * @param jcrName  the JCR-style name to be parsed
     * @param resolver <code>NamespaceResolver</code> used for resolving
     *                 prefixes into namespace URIs
     * @return the resulting <code>QName</code>
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     * @see #parse(String, NamespaceResolver)
     */
    private static QName parseIgnoreCache(String jcrName,
                                          NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {
        String[] parts = doParse(jcrName);
        String uri;
        try {
            uri = resolver.getURI(parts[0]);
        } catch (NamespaceException nse) {
            throw new UnknownPrefixException(parts[0]);
        }
        return new QName(uri, parts[1]);
    }

    /**
     * Parses the <code>jcrName</code> and returns an array of two strings:
     * the first array element contains the prefix (or empty string),
     * the second the local name.
     *
     * @param jcrName the name to be parsed
     * @return An array holding two strings: the first array element contains
     *         the prefix (or empty string), the second the local name.
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    private static String[] doParse(String jcrName) throws IllegalNameException {
        if (jcrName == null || jcrName.length() == 0) {
            throw new IllegalNameException("empty name");
        }

        if (".".equals(jcrName) || "..".equals(jcrName)) {
            // illegal syntax for name
            throw new IllegalNameException("'" + jcrName + "' is not a valid name");
        }

        String prefix;
        String localName;

        Matcher matcher = (Matcher) NAME_MATCHER.get();
        matcher.reset(jcrName);
        if (matcher.matches()) {
            // check for prefix (group 1)
            if (matcher.group(1) != null) {
                // prefix specified
                // group 2 is namespace prefix excl. delimiter (colon)
                prefix = matcher.group(2);
                // check if the prefix is a valid XML prefix
                if (!XMLChar.isValidNCName(prefix)) {
                    // illegal syntax for prefix
                    throw new IllegalNameException("'" + jcrName
                            + "' is not a valid name: illegal prefix");
                }
            } else {
                // no prefix specified
                prefix = "";
            }

            // group 3 is localName
            localName = matcher.group(3);
        } else {
            // illegal syntax for name
            throw new IllegalNameException("'" + jcrName + "' is not a valid name");
        }

        return new String[]{prefix, localName};
    }

    /**
     * Formats the given <code>QName</code> to produce a string representation,
     * i.e. JCR-style name.
     * <p/>
     * Note that unlike {@link #format(QName, NamespaceResolver)} this method
     * always constructs a new <code>String</code>, ignoring potential caching
     * capabilities of the passed <code>resolver</code>.
     *
     * @param qName    the <code>QName</code> to format
     * @param resolver <code>NamespaceResolver</code> used for resolving
     *                 namespace URIs into prefixes
     * @param buffer   StringBuffer where the prefixed JCR name should be
     *                 appended to
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     * @see #format(QName, NamespaceResolver)
     */
    private static void formatIgnoreCache(QName qName,
                                          NamespaceResolver resolver,
                                          StringBuffer buffer)
            throws NoPrefixDeclaredException {
        // prefix
        String prefix;
        try {
            prefix = resolver.getPrefix(qName.getNamespaceURI());
        } catch (NamespaceException nse) {
            throw new NoPrefixDeclaredException("no prefix declared for URI: "
                    + qName.getNamespaceURI());
        }
        if (prefix.length() == 0) {
            // default prefix (empty string)
        } else {
            buffer.append(prefix);
            buffer.append(':');
        }
        // name
        buffer.append(qName.getLocalName());
    }
}
