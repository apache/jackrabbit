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
 * <code>NameFormat</code> formats a {@link QName} using a
 * {@link NamespaceResolver}.
 */
public class NameFormat {

    /**
     * The reqular expression pattern used to validate and parse
     * qualified names.
     * <p>
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
     * Parses the <code>jcrName</code> and returns a new <code>QName</code>. If
     * the passed <code>resolver</code> also an instance of {@link NameCache}
     * then the parsing is first read from the cache.
     *
     * @param jcrName the name to be parsed
     * @param resolver <code>NamespaceResolver</code> use to retrieve the
     * namespace URI from the prefix contained in the given JCR name.
     * @return qName the new <code>QName</code>
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     * JCR-style name.
     */
    public static QName parse(String jcrName, NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {

        if (resolver instanceof NameCache) {
            QName name = ((NameCache) resolver).retrieveName(jcrName);
            if (name == null) {
                name = parseNoCache(jcrName, resolver);
                ((NameCache) resolver).cacheName(jcrName, name);
            }
            return name;
        } else {
            return parseNoCache(jcrName, resolver);
        }
    }

    /**
     * Parses an array of <code>jcrName</code> and returns the respective
     * array of <code>QName</code>. If the passed <code>resolver</code> also an
     * instance of {@link NameCache} then the parsing is first read from the cache.
     *
     * @param jcrNames the array of names to be parsed
     * @param resolver <code>NamespaceResolver</code> use to retrieve the
     * namespace URI from the prefix contained in the given JCR name.
     * @return the new array of <code>QName</code>
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     * JCR-style name.
     */
    public static QName[] parse(String jcrNames[], NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {

        QName[] ret = new QName[jcrNames.length];
        if (resolver instanceof NameCache) {
            for (int i=0; i<ret.length; i++) {
                QName name = ((NameCache) resolver).retrieveName(jcrNames[i]);
                if (name == null) {
                    name = parseNoCache(jcrNames[i], resolver);
                    ((NameCache) resolver).cacheName(jcrNames[i], name);
                }
                ret[i] = name;
            }
        } else {
            for (int i=0; i<ret.length; i++) {
                ret[i] = parseNoCache(jcrNames[i], resolver);
            }
        }
        return ret;
    }

    /**
     * Parses the <code>jcrName</code> and returns a new <code>QName</code>,
     * but does not respect possible caches.
     *
     * @param jcrName the name to be parsed
     * @param resolver <code>NamespaceResolver</code> use to retrieve the
     * namespace URI from the prefix contained in the given JCR name.
     * @return qName the new <code>QName</code>
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     * JCR-style name.
     */
    private static QName parseNoCache(String jcrName, NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {
        String[] parts = parse(jcrName);
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
     * the prefix (or empty string), the second the local name.
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    public static String[] parse(String jcrName) throws IllegalNameException {
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

        return new String[] {prefix, localName};

    }
    /**
     * Checks if <code>jcrName</code> is a valid JCR-style name.
     *
     * @param jcrName the name to be checked
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     * JCR-style name.
     */
    public static void checkFormat(String jcrName) throws IllegalNameException {
        parse(jcrName);
    }

    /**
     * Returns a string representation of the qualified <code>name</code> in the
     * JCR name format. If the passed <code>resolver</code> also an instance of
     * {@link NameCache} then the formatting is first read from the cache.
     *
     * @param qName the qualified name to resolve.
     * @param resolver the namespace resolver.
     * @return JCR the formatted path.
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     * @see #format(QName, NamespaceResolver, StringBuffer)
     */
    public static String format(QName qName, NamespaceResolver resolver)
            throws NoPrefixDeclaredException {

        if (resolver instanceof NameCache) {
            String jcrName = ((NameCache) resolver).retrieveName(qName);
            if (jcrName == null) {
                StringBuffer buf = new StringBuffer();
                formatNoCache(qName, resolver, buf);
                jcrName = buf.toString();
                ((NameCache) resolver).cacheName(jcrName, qName);
            }
            return jcrName;

        } else {
            StringBuffer buf = new StringBuffer();
            formatNoCache(qName, resolver, buf);
            return buf.toString();
        }
    }

    /**
     * Optimized convenience method that returns an array of string
     * representations of the given qualified <code>name</code> in the JCR name
     * format. If the passed <code>resolver</code> also an instance of
     * {@link NameCache} then the formatting is first read from the cache.
     *
     * @param qNames the array of qualified name to resolve.
     * @param resolver the namespace resolver.
     * @return the array of jcr names
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     * @see #format(QName, NamespaceResolver, StringBuffer)
     */
    public static String[] format(QName[] qNames, NamespaceResolver resolver)
            throws NoPrefixDeclaredException {
        String[] ret = new String[qNames.length];
        if (resolver instanceof NameCache) {
            for (int i=0; i<ret.length; i++) {
                String jcrName = ((NameCache) resolver).retrieveName(qNames[i]);
                if (jcrName == null) {
                    StringBuffer buf = new StringBuffer();
                    formatNoCache(qNames[i], resolver, buf);
                    jcrName = buf.toString();
                    ((NameCache) resolver).cacheName(jcrName, qNames[i]);
                }
                ret[i] = jcrName;
            }
        } else {
            for (int i=0; i<ret.length; i++) {
                StringBuffer buf = new StringBuffer();
                formatNoCache(qNames[i], resolver, buf);
                ret[i] = buf.toString();
            }
        }
        return ret;
    }

    /**
     * Returns a string representation of the qualified <code>name</code> in the
     * JCR name format. If the passed <code>resolver</code> also an instance of
     * {@link NameCache} then the formatting is first read from the cache.
     *
     * @param qName the qualified name to resolve.
     * @param resolver the namespace resolver.
     * @param buffer StringBuffer where the prefixed JCR name should be appended to.
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     * @see #format(QName, NamespaceResolver)
     */
    public static void format(QName qName, NamespaceResolver resolver, StringBuffer buffer)
            throws NoPrefixDeclaredException {

        if (resolver instanceof NameCache) {
            String jcrName = ((NameCache) resolver).retrieveName(qName);
            if (jcrName == null) {
                int l = buffer.length();
                formatNoCache(qName, resolver, buffer);
                ((NameCache) resolver).cacheName(buffer.substring(l), qName);
            } else {
                buffer.append(jcrName);
            }
        } else {
            formatNoCache(qName, resolver, buffer);
        }
    }

    /**
     * Returns a string representation of the qualified <code>name</code> in the
     * JCR name format, but does not respect possible caches.
     *
     * @param qName the qualified name to resolve.
     * @param resolver the namespace resolver.
     * @param buffer StringBuffer where the prefixed JCR name should be appended to.
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     *
     * @see #format(QName, NamespaceResolver)
     */
    private static void formatNoCache(QName qName, NamespaceResolver resolver, StringBuffer buffer)
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
