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

import java.util.ArrayList;

/**
 * <code>PathParserTest</code>...
 */
public final class JcrPath {

    private static final int ABS = 1;
    private static final int NOR = 2;
    private static final int VAL = 4;

    public final String path;
    public final String normalizedPath;
    public final int flags;

    // create tests
    private static ArrayList list = new ArrayList();
    static {
        // absolute paths
        list.add(new JcrPath("/", NOR|VAL));
        list.add(new JcrPath("/", NOR|VAL));
        list.add(new JcrPath("/", NOR|VAL));
        list.add(new JcrPath("/a/b/c", NOR|VAL));
        list.add(new JcrPath("/prefix:name/prefix:name", NOR|VAL));
        list.add(new JcrPath("/name[2]/name[2]", NOR|VAL));
        list.add(new JcrPath("/prefix:name[2]/prefix:name[2]", NOR|VAL));
        list.add(new JcrPath("/a/b/c/", "/a/b/c", NOR|VAL));
        list.add(new JcrPath("/a/..../", "/a/....", NOR|VAL));
        list.add(new JcrPath("/a/b:.a./", "/a/b:.a.", NOR|VAL));

        // ... containing special characters allowed since JCR 2.0
        list.add(new JcrPath("/name's[2]/'name[2]", NOR|VAL));
        list.add(new JcrPath("/\"name\"[2]/name[2]", NOR|VAL));

        // relative paths
        list.add(new JcrPath("a/b/c/", "a/b/c", NOR|VAL));
        list.add(new JcrPath("a/b/c", NOR|VAL));
        list.add(new JcrPath("prefix:name/prefix:name", NOR|VAL));
        list.add(new JcrPath("name[2]/name[2]", NOR|VAL));
        list.add(new JcrPath("prefix:name[2]/prefix:name[2]", NOR|VAL));
        list.add(new JcrPath(".a./.b.", NOR|VAL));
        list.add(new JcrPath(".../...", NOR|VAL));

        // invalid paths
        list.add(new JcrPath(""));
        list.add(new JcrPath("//"));
        list.add(new JcrPath("/a//b"));
        list.add(new JcrPath(" /a/b/c/"));
        list.add(new JcrPath("/a/b/c/ "));
        list.add(new JcrPath("/:name/prefix:name"));
        list.add(new JcrPath("/prefix:name "));
        list.add(new JcrPath("/prefix: name"));
        list.add(new JcrPath("/ prefix:name"));
        list.add(new JcrPath("/prefix : name"));
        list.add(new JcrPath("/name[0]/name[2]"));
        list.add(new JcrPath("/prefix:name[2]foo/prefix:name[2]"));
        list.add(new JcrPath(":name/prefix:name"));
        list.add(new JcrPath("name[0]/name[2]"));
        list.add(new JcrPath("prefix:name[2]foo/prefix:name[2]"));
        list.add(new JcrPath("/..", "/..", 0));
        list.add(new JcrPath("/a/b/../../..", "/a/b/../../..", 0));
        list.add(new JcrPath("/a/b/../../../c", "/a/b/../../../c", 0));

        list.add(new JcrPath("/prefix:*name"));
        list.add(new JcrPath("/prefix:n*ame"));
        list.add(new JcrPath("/prefix:|name"));
        list.add(new JcrPath("/prefix:n|ame"));

        list.add(new JcrPath("/name[2]\""));
        list.add(new JcrPath("/name[2]\"/name"));
        list.add(new JcrPath("/name[2]'"));
        list.add(new JcrPath("/name[2]'/name"));

        // normalized, relative paths
        list.add(new JcrPath(".", ".", NOR|VAL));
        list.add(new JcrPath("..", "..", NOR|VAL));
        list.add(new JcrPath("../..", "../..", NOR|VAL));
        list.add(new JcrPath("../../a/b", "../../a/b", NOR|VAL));
        list.add(new JcrPath("../a", "../a",NOR|VAL));        

        // non-normalized paths
        list.add(new JcrPath("/a/../b", "/b", VAL));
        list.add(new JcrPath("/a/../b/./c/d/..", "/b/c", VAL));
        list.add(new JcrPath("./../.", "..", VAL));
        list.add(new JcrPath("/a/./b", "/a/b", VAL));
        list.add(new JcrPath("/a/b/../..", "/", VAL));
        list.add(new JcrPath("/a/b/c/../d/..././f", "/a/b/d/.../f", VAL));
        list.add(new JcrPath("../a/b/../../../../f", "../../../f", VAL));
        list.add(new JcrPath("a/../..", "..", VAL));
        list.add(new JcrPath("../../a/.", "../../a", VAL));

        // other non-normalized, relative paths
        list.add(new JcrPath("./.", ".", VAL));
        list.add(new JcrPath("./a", "a", VAL));
        list.add(new JcrPath("a/..", ".", VAL));
        list.add(new JcrPath("../a/..", "..", VAL));
        list.add(new JcrPath("../a/.", "../a", VAL));
        list.add(new JcrPath("a/./b", "a/b", VAL));
    }

    /**
     * creates an invalid path test
     * @param path
     */
    public JcrPath(String path) {
        this(path, null, 0);
    }

    /**
     * @param path
     * @param flags
     */
    public JcrPath(String path, int flags) {
        this(path, null, flags);
    }

    public JcrPath(String path, String normalizedPath, int flags) {
        this.path = path;
        this.normalizedPath = normalizedPath;
        this.flags = flags | ((path.length() > 0 && path.charAt(0)=='/') ? ABS : 0);
    }

    public static JcrPath[] getTests() {
        return (JcrPath[]) list.toArray(new JcrPath[list.size()]);
    }

    public boolean isAbsolute() {
        return (flags&ABS) > 0;
    }

    public boolean isNormalized() {
        return (flags&NOR) > 0;
    }

    public boolean isValid() {
        return (flags&VAL) > 0;
    }

    public String toString() {
        StringBuffer b = new StringBuffer(path);
        if (normalizedPath!=null) {
            b.append(" -> ").append(normalizedPath);
        }
        if (isAbsolute()) {
            b.append(",ABS");
        }
        if (isNormalized()) {
            b.append(",NOR");
        }
        if (isValid()) {
            b.append(",VAL");
        }
        return b.toString();
    }
}
