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
package org.apache.jackrabbit.spi.commons.conversion;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

import javax.jcr.NamespaceException;

/**
 * <code>PathParser</code> formats a {@link Path} using a
 * {@link NameResolver} and a {@link PathFactory}.
 */
public class PathParser {

    // constants for parser
    private static final int STATE_PREFIX_START = 0;
    private static final int STATE_PREFIX = 1;
    private static final int STATE_NAME_START = 2;
    private static final int STATE_NAME = 3;
    private static final int STATE_INDEX = 4;
    private static final int STATE_INDEX_END = 5;
    private static final int STATE_DOT = 6;
    private static final int STATE_DOTDOT = 7;

    /**
     * Parses <code>jcrPath</code> into a qualified path using
     * <code>resolver</code> to convert prefixes into namespace URI's.
     *
     * @param jcrPath the jcr path.
     * @param resolver the namespace resolver.
     * @param factory
     * @return qualified path.
     * @throws MalformedPathException If the <code>jcrPath</code> is malformed.
     * @throws IllegalNameException if any of the jcrNames is malformed.
     * @throws NamespaceException If an unresolvable prefix is encountered.
     */
    public static Path parse(String jcrPath, NameResolver resolver, PathFactory factory)
            throws MalformedPathException, IllegalNameException, NamespaceException {
        return parse(null, jcrPath, resolver, factory);
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
     * @param factory
     * @return the fully qualified Path.
     * @throws MalformedPathException If the <code>jcrPath</code> is malformed.
     * @throws IllegalNameException if any of the jcrNames is malformed.
     * @throws NamespaceException If an unresolvable prefix is encountered.
     */
    public static Path parse(Path parent, String jcrPath,
                             NameResolver resolver,
                             PathFactory factory) throws MalformedPathException, IllegalNameException, NamespaceException {
        final char EOF = (char) -1;

        // check for length
        int len = jcrPath == null ? 0 : jcrPath.length();

        // shortcut
        if (len == 1 && jcrPath.charAt(0) == '/') {
            return factory.getRootPath();
        }

        if (len == 0) {
            throw new MalformedPathException("empty path");
        }

        // check if absolute path
        PathBuilder builder = new PathBuilder(factory);
        int pos = 0;
        if (jcrPath.charAt(0) == '/') {
            if (parent != null) {
                throw new MalformedPathException("'" + jcrPath + "' is not a relative path");
            }
            builder.addRoot();
            pos++;
        }

        // add master if present
        if (parent != null) {
            builder.addAll(parent.getElements());
        }

        // parse the path
        int state = STATE_PREFIX_START;
        int lastPos = pos;
        String name = null;
        int index = Path.INDEX_UNDEFINED;
        boolean wasSlash = false;

        while (pos <= len) {
            char c = pos == len ? EOF : jcrPath.charAt(pos);
            pos++;
            // special check for whitespace
            if (c != ' ' && Character.isWhitespace(c)) {
                c = '\t';
            }
            switch (c) {
                case '/':
                case EOF:
                    if (state == STATE_PREFIX_START && c != EOF) {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. double slash '//' not allowed.");
                    }
                    if (state == STATE_PREFIX
                            || state == STATE_NAME
                            || state == STATE_INDEX_END) {

                        // eof pathelement
                        if (name == null) {
                            if (wasSlash) {
                                throw new MalformedPathException("'" + jcrPath + "' is not a valid path: Trailing slashes not allowed in prefixes and names.");
                            }
                            name = jcrPath.substring(lastPos, pos - 1);
                        }

                        // only add element if resolver not null. otherwise this
                        // is just a check for valid format.
                        if (resolver != null) {
                            Name qName = resolver.getQName(name);
                            builder.addLast(qName, index);
                        }
                        state = STATE_PREFIX_START;
                        lastPos = pos;
                        name = null;
                        index = Path.INDEX_UNDEFINED;
                    } else if (state == STATE_DOT) {
                        builder.addLast(factory.getCurrentElement());
                        lastPos = pos;
                        state = STATE_PREFIX_START;
                    } else if (state == STATE_DOTDOT) {
                        builder.addLast(factory.getParentElement());
                        lastPos = pos;
                        state = STATE_PREFIX_START;
                    } else if (state == STATE_PREFIX_START && c == EOF) {
                        // ignore trailing slash
                    } else {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not a valid name character.");
                    }
                    break;

                case '.':
                    if (state == STATE_PREFIX_START) {
                        state = STATE_DOT;
                    } else if (state == STATE_DOT) {
                        state = STATE_DOTDOT;
                    } else if (state == STATE_DOTDOT) {
                        state = STATE_PREFIX;
                    } else if (state == STATE_INDEX_END) {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not valid after index. '/' expected.");
                    }
                    break;

                case ':':
                    if (state == STATE_PREFIX_START) {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. Prefix must not be empty");
                    } else if (state == STATE_PREFIX) {
                        if (wasSlash) {
                            throw new MalformedPathException("'" + jcrPath + "' is not a valid path: Trailing slashes not allowed in prefixes and names.");
                        }
                        state = STATE_NAME_START;
                        // don't reset the lastPos/pos since prefix+name are passed together to the NameResolver
                    } else {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not valid name character");
                    }
                    break;

                case '[':
                    if (state == STATE_PREFIX || state == STATE_NAME) {
                        if (wasSlash) {
                            throw new MalformedPathException("'" + jcrPath + "' is not a valid path: Trailing slashes not allowed in prefixes and names.");
                        }
                        state = STATE_INDEX;
                        name = jcrPath.substring(lastPos, pos - 1);
                        lastPos = pos;
                    } else {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not a valid name character.");
                    }
                    break;

                case ']':
                    if (state == STATE_INDEX) {
                        try {
                            index = Integer.parseInt(jcrPath.substring(lastPos, pos - 1));
                        } catch (NumberFormatException e) {
                            throw new MalformedPathException("'" + jcrPath + "' is not a valid path. NumberFormatException in index: " + jcrPath.substring(lastPos, pos - 1));
                        }
                        if (index < Path.INDEX_DEFAULT) {
                            throw new MalformedPathException("'" + jcrPath + "' is not a valid path. Index number invalid: " + index);
                        }
                        state = STATE_INDEX_END;
                    } else {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not a valid name character.");
                    }
                    break;

                case ' ':
                    if (state == STATE_PREFIX_START || state == STATE_NAME_START) {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not valid name start");
                    } else if (state == STATE_INDEX_END) {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not valid after index. '/' expected.");
                    } else if (state == STATE_DOT || state == STATE_DOTDOT) {
                        state = STATE_PREFIX;
                    } else if (state == STATE_INDEX_END) {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not valid after index. '/' expected.");
                    }
                    break;

                case '\t':
                    throw new MalformedPathException("'" + jcrPath + "' is not a valid path. Whitespace not a allowed in name.");

                case '*':
                case '\'':
                case '\"':
                    throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not a valid name character.");

                default:
                    if (state == STATE_PREFIX_START || state == STATE_DOT || state == STATE_DOTDOT) {
                        state = STATE_PREFIX;
                    } else if (state == STATE_NAME_START) {
                        state = STATE_NAME;
                    } else if (state == STATE_INDEX_END) {
                        throw new MalformedPathException("'" + jcrPath + "' is not a valid path. '" + c + "' not valid after index. '/' expected.");
                    }
            }
            wasSlash = c == ' ';
        }

        if (resolver == null) {
            // this was only for checking the format
            return null;
        } else {
            return builder.getPath();
        }
    }

    /**
     * Check the format of the given jcr path. Note, the neither name nor
     * namespace validation (resolution of prefix to URI) is performed and
     * therefore will not be detected.
     *
     * @param jcrPath
     * @throws MalformedPathException If the <code>jcrPath</code> is malformed.
     */
    public static void checkFormat(String jcrPath) throws MalformedPathException {
        try {
            // since no path is created -> use default factory
            parse(jcrPath, null, PathFactoryImpl.getInstance());
        } catch (NamespaceException e) {
            // will never occur
        } catch (IllegalNameException e) {
            // will never occur
        }
    }
}
