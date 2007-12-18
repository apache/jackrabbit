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
package org.apache.jackrabbit.spi.commons.name;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import junit.framework.TestCase;

import java.util.Vector;

/**
 * <code>PathBuilderTest</code>...
 */
public class PathBuilderTest extends TestCase {

    private final PathFactory factory = PathFactoryImpl.getInstance();
    private final NamespaceResolver nsResolver = new NamespaceResolver() {
        public String getURI(String prefix) {
            return prefix;
        }
        public String getPrefix(String uri) {
            return uri;
        }
    };
    private final PathResolver pathResolver;

    private JcrPath[] tests = JcrPath.getTests();

    public PathBuilderTest() {
        NameResolver nameResolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
        pathResolver = new ParsingPathResolver(factory, nameResolver);
    }

    public void testBuilder() throws Exception {
        for (int i=0; i<tests.length; i++) {
            JcrPath t = tests[i];
            if (t.isValid()) {
                if (t.normalizedPath==null) {
                    // check just creation
                    Path p = build(t.path, false);
                    assertEquals("\"" + t.path + "\".create(false)", t.path,  pathResolver.getJCRPath(p));
                    assertEquals("\"" + t.path + "\".isNormalized()", t.isNormalized(), p.isNormalized());
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                } else {
                    // check with normalization
                    Path p = build(t.path, true);
                    assertEquals("\"" + t.path + "\".create(true)", t.normalizedPath, pathResolver.getJCRPath(p));
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                }
            }
        }
    }

    public void testBuilderReverse() throws Exception {
        for (int i=0; i<tests.length; i++) {
            JcrPath t = tests[i];
            if (t.isValid()) {
                if (t.normalizedPath==null) {
                    // check just creation
                    Path p = buildReverse(t.path, false);
                    assertEquals("\"" + t.path + "\".create(false)", t.path,  pathResolver.getJCRPath(p));
                    assertEquals("\"" + t.path + "\".isNormalized()", t.isNormalized(), p.isNormalized());
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                } else {
                    // check with normalization
                    Path p = buildReverse(t.path, true);
                    assertEquals("\"" + t.path + "\".create(true)", t.normalizedPath, pathResolver.getJCRPath(p));
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                }
            }
        }
    }

    private Path build(String path, boolean normalize)
            throws Exception {
        PathBuilder builder = new PathBuilder();
        String[] elems = explode(path, '/', false);
        if (path.startsWith("/")) {
            builder.addRoot();
        }
        for (int i=0; i<elems.length; i++) {
            int pos = elems[i].indexOf('[');
            String elem;
            Name name;
            int index;
            if (pos<0) {
                elem = elems[i];
                index = -1;
            } else {
                index = Integer.parseInt(elems[i].substring(pos+1, elems[i].length()-1));
                elem = elems[i].substring(0, pos);
            }
            if (".".equals(elem)) {
                builder.addLast(factory.getCurrentElement());
            } else if ("..".equals(elems[i])) {
                builder.addLast(factory.getParentElement());
            } else {
                name = NameParser.parse(elem, nsResolver, NameFactoryImpl.getInstance());
                if (index < 0) {
                    builder.addLast(name);
                } else {
                    builder.addLast(name, index);
                }
            }
        }
        return normalize ? builder.getPath().getNormalizedPath() : builder.getPath();
    }

    private Path buildReverse(String path, boolean normalize)
            throws Exception {
        PathBuilder builder = new PathBuilder();
        String[] elems = explode(path, '/', false);
        for (int i=elems.length-1; i>=0; i--) {
            int pos = elems[i].indexOf('[');
            String elem;
            Name name;
            int index;
            if (pos<0) {
                elem = elems[i];
                index = -1;
            } else {
                index = Integer.parseInt(elems[i].substring(pos+1, elems[i].length()-1));
                elem = elems[i].substring(0, pos);
            }
            if (".".equals(elem)) {
                builder.addFirst(factory.getCurrentElement());
            } else if ("..".equals(elems[i])) {
                builder.addFirst(factory.getParentElement());
            } else {
                name = NameParser.parse(elem, nsResolver, NameFactoryImpl.getInstance());
                if (index < 0) {
                    builder.addFirst(name);
                } else {
                    builder.addFirst(name, index);
                }
            }
        }
        if (path.startsWith("/")) {
            builder.addRoot();
        }
        return normalize ? builder.getPath().getNormalizedPath() : builder.getPath();
    }

    /**
     * returns an array of strings decomposed of the original string, split at
     * every occurance of 'ch'.
     * @param str the string to decompose
     * @param ch the character to use a split pattern
     * @param respectEmpty if <code>true</code>, empty elements are generated
     * @return an array of strings
     */
    private static String[] explode(String str, int ch, boolean respectEmpty) {
        if (str == null) {
            return new String[0];
        }

        Vector strings = new Vector();
        int pos     = 0;
        int lastpos = 0;

        // add snipples
        while ((pos = str.indexOf(ch, lastpos)) >= 0) {
            if (pos-lastpos>0 || respectEmpty)
                strings.add(str.substring(lastpos, pos));
            lastpos = pos+1;
        }
        // add rest
        if (lastpos < str.length()) {
            strings.add(str.substring(lastpos));
        } else if (respectEmpty && lastpos==str.length()) {
            strings.add("");
        }

        // return stringarray
        return (String[]) strings.toArray(new String[strings.size()]);
    }
}