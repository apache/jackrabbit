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

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.util.XMLChar;

import javax.jcr.NamespaceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>PathFormat</code> formats a {@link Path} using a
 * {@link NamespaceResolver}.
 *
 * @deprecated Use the PathResolver interface from
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class PathFormat {

    /**
     * Pattern used to validate and parse path elements:<p>
     * <ul>
     * <li>group 1 is .
     * <li>group 2 is ..
     * <li>group 3 is namespace prefix incl. delimiter (colon)
     * <li>group 4 is namespace prefix excl. delimiter (colon)
     * <li>group 5 is localName
     * <li>group 6 is index incl. brackets
     * <li>group 7 is index excl. brackets
     * </ul>
     */
    private static final Pattern PATH_ELEMENT_PATTERN =
            Pattern.compile("(\\.)|"
            + "(\\.\\.)|"
            + "(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?"
            + "([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)"
            + "(\\[([1-9]\\d*)\\])?");

    /**
     * Matcher instance as thread-local.
     */
    private static final ThreadLocal PATH_ELEMENT_MATCHER = new ThreadLocal() {
        protected Object initialValue() {
            return PATH_ELEMENT_PATTERN.matcher("dummy");
        }
    };

    /**
     * Parses <code>jcrPath</code> into a qualified path using
     * <code>resolver</code> to convert prefixes into namespace URI's.
     *
     * @param jcrPath the jcr path.
     * @param resolver the namespace resolver.
     * @return qualified path.
     * @throws MalformedPathException if <code>jcrPath</code> is malformed.
     */
    public static Path parse(String jcrPath, NamespaceResolver resolver)
            throws MalformedPathException {
        return parse(null, jcrPath, resolver);
    }

    /**
     * Parses the give <code>jcrPath</code> and returns a <code>Path</code>. If
     * <code>parent</code> is not <code>null</code>, it is prepended to the
     * returned list. If <code>resolver</code> is <code>null</code>, this method
     * only checks the format of the string and returns <code>null</code>.
     *
     * @param parent   the parent path
     * @param jcrPath  the JCR path
     * @param resolver the namespace resolver to get prefixes for namespace
     *                 URI's.
     * @return the fully qualified Path.
     * @throws MalformedPathException if <code>jcrPath</code> is malformed.
     */
    public static Path parse(Path parent, String jcrPath, NamespaceResolver resolver)
            throws MalformedPathException {
        // shortcut
        if ("/".equals(jcrPath)) {
            return Path.ROOT;
        }

        // split path into path elements
        String[] elems = Text.explode(jcrPath, '/', true);
        if (elems.length == 0) {
            throw new MalformedPathException("empty path");
        }

        Path.PathBuilder pathBuilder;
        if (parent != null) {
            // a parent path was specified; the 'jcrPath' argument is assumed
            // to be a relative path
            pathBuilder = new Path.PathBuilder(parent);
        } else {
            pathBuilder = new Path.PathBuilder();
        }

        for (int i = 0; i < elems.length; i++) {
            // validate & parse path element
            String prefix;
            String localName;
            int index;

            String elem = elems[i];
            if (i == 0 && elem.length() == 0) {
                // path is absolute, i.e. the first element is the root element
                if (parent != null) {
                    throw new MalformedPathException("'" + jcrPath + "' is not a relative path");
                }
                pathBuilder.addLast(Path.ROOT_ELEMENT);
                continue;
            }
            if (elem.length() == 0 && i == elems.length - 1) {
                // ignore trailing '/'
                break;
            }
            Matcher matcher = (Matcher) PATH_ELEMENT_MATCHER.get();
            matcher.reset(elem);
            if (matcher.matches()) {
                if (resolver == null) {
                    // check only
                    continue;
                }

                if (matcher.group(1) != null) {
                    // group 1 is .
                    pathBuilder.addLast(Path.CURRENT_ELEMENT);
                } else if (matcher.group(2) != null) {
                    // group 2 is ..
                    pathBuilder.addLast(Path.PARENT_ELEMENT);
                } else {
                    // element is a name

                    // check for prefix (group 3)
                    if (matcher.group(3) != null) {
                        // prefix specified
                        // group 4 is namespace prefix excl. delimiter (colon)
                        prefix = matcher.group(4);
                        // check if the prefix is a valid XML prefix
                        if (!XMLChar.isValidNCName(prefix)) {
                            // illegal syntax for prefix
                            throw new MalformedPathException("'" + jcrPath + "' is not a valid path: '"
                                    + elem + "' specifies an illegal namespace prefix");
                        }
                    } else {
                        // no prefix specified
                        prefix = "";
                    }

                    // group 5 is localName
                    localName = matcher.group(5);

                    // check for index (group 6)
                    if (matcher.group(6) != null) {
                        // index specified
                        // group 7 is index excl. brackets
                        index = Integer.parseInt(matcher.group(7));
                    } else {
                        // no index specified
                        index = org.apache.jackrabbit.name.Path.INDEX_UNDEFINED;
                    }

                    String nsURI;
                    try {
                        nsURI = resolver.getURI(prefix);
                    } catch (NamespaceException nse) {
                        // unknown prefix
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path: '"
                                + elem + "' specifies an unmapped namespace prefix");
                    }

                    if (index == org.apache.jackrabbit.name.Path.INDEX_UNDEFINED) {
                        pathBuilder.addLast(new QName(nsURI, localName));
                    } else {
                        pathBuilder.addLast(new QName(nsURI, localName), index);
                    }
                }
            } else {
                // illegal syntax for path element
                throw new MalformedPathException("'" + jcrPath + "' is not a valid path: '"
                        + elem + "' is not a legal path element");
            }
        }
        if (resolver != null) {
            return pathBuilder.getPath();
        } else {
            return null;
        }
    }

    /**
     * Checks if <code>jcrPath</code> is a valid JCR-style absolute or relative
     * path.
     *
     * @param jcrPath the path to be checked
     * @throws MalformedPathException If <code>jcrPath</code> is not a valid
     *                                JCR-style path.
     */
    public static void checkFormat(String jcrPath) throws MalformedPathException {
        parse(null, jcrPath, null);
    }

    /**
     * Returns a string representation of the qualified <code>path</code> in the
     * JCR path format.
     *
     * @param path the qualified path to resolve.
     * @param resolver the namespace resolver.
     * @return JCR the formatted path.
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     */
    public static String format(Path path, NamespaceResolver resolver)
            throws NoPrefixDeclaredException {
        if (path.denotesRoot()) {
            // shortcut
            return "/";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < path.getLength(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            // name
            format(path.getElement(i), resolver, sb);
        }
        return sb.toString();
    }

    //-----------------------------------------------------------< internal >---

    /**
     * Appends the JCR name representation of this path element to the given
     * string buffer.
     *
     * @param element  the path element to format.
     * @param resolver namespace resolver
     * @param buf      string buffer where the JCR name representation should be
     *                 appended to
     * @throws NoPrefixDeclaredException if the namespace of the path element
     *                                   name can not be resolved
     */
    private static void format(Path.PathElement element,
                               NamespaceResolver resolver, StringBuffer buf)
            throws NoPrefixDeclaredException {
        // name
        NameFormat.format(element.getName(), resolver, buf);
        // index
        int index = element.getIndex();
        /**
         * FIXME the [1] subscript should only be suppressed if the item
         * in question can't have same-name siblings.
         */
        //if (index > 0) {
        if (index > org.apache.jackrabbit.name.Path.INDEX_DEFAULT) {
            buf.append('[');
            buf.append(index);
            buf.append(']');
        }
    }
}
