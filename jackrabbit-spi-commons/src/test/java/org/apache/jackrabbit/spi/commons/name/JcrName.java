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
 * <code>JcrName</code>...
 */
public final class JcrName {

    public final String jcrName;
    public final String prefix;
    public final String name;

    // create tests
    private static ArrayList list = new ArrayList();
    static {
        // valid names
        list.add(new JcrName("a", "", "a"));
        list.add(new JcrName("name", "", "name"));
        list.add(new JcrName("na me", "", "na me"));
        list.add(new JcrName("prefix:name", "prefix", "name"));
        list.add(new JcrName("prefix:na me", "prefix", "na me"));
        list.add(new JcrName("...", "", "..."));
        list.add(new JcrName(".a.", "", ".a."));

        // valid names since jcr 2.0
        list.add(new JcrName("a'", "", "a'"));                   // single quote
        list.add(new JcrName("'a", "", "'a"));
        list.add(new JcrName("ab'c", "", "ab'c"));
        list.add(new JcrName("prefix:ab'c", "prefix", "ab'c"));

        list.add(new JcrName("a\"", "", "a\""));                 // double quote
        list.add(new JcrName("\"a", "", "\"a"));
        list.add(new JcrName("ab\"c", "", "ab\"c"));
        list.add(new JcrName("prefix:ab\"c", "prefix", "ab\"c"));       

        // expanded names
        list.add(new JcrName("{}a", "", "a"));
        list.add(new JcrName("{}name", "", "name"));
        list.add(new JcrName("{}na me", "", "na me"));
        list.add(new JcrName("{uri:}name", "uri:", "name"));
        list.add(new JcrName("{uri:}na me", "uri:", "na me"));
        list.add(new JcrName("{nouri}name", "", "{nouri}name"));
        list.add(new JcrName("{nouri}na me", "", "{nouri}na me"));
        list.add(new JcrName("{}...", "", "..."));
        list.add(new JcrName("{}.a.", "", ".a."));

        // invalid names
        list.add(new JcrName(":name"));
        list.add(new JcrName("."));
        list.add(new JcrName(".."));
        list.add(new JcrName("pre:"));
        list.add(new JcrName(""));
        list.add(new JcrName(" name"));
        list.add(new JcrName(" prefix: name"));
        list.add(new JcrName("prefix: name"));
        list.add(new JcrName("prefix:name "));
        list.add(new JcrName("pre fix:name"));
        list.add(new JcrName("prefix :name"));

        list.add(new JcrName("name/name"));

        list.add(new JcrName("name[name"));
        list.add(new JcrName("name]name"));
        list.add(new JcrName("name[]"));
        list.add(new JcrName("name[123]"));

        list.add(new JcrName("name*name"));

        list.add(new JcrName("prefix:name:name"));

        list.add(new JcrName("name|name"));
        list.add(new JcrName("|name"));
        list.add(new JcrName("name|"));
        list.add(new JcrName("prefix:name|name"));
    }

    private static JcrName[] tests = (JcrName[]) list.toArray(new JcrName[list.size()]);

    public static JcrName[] getTests() {
        return tests;
    }

    public JcrName(String jcrName) {
        this(jcrName, null, null);
    }

    public JcrName(String jcrName, String prefix, String name) {
        this.jcrName = jcrName;
        this.prefix = prefix;
        this.name = name;
    }

    public boolean isValid() {
        return name!=null;
    }

    public String toString() {
        StringBuffer b = new StringBuffer(jcrName);
        if (isValid()) {
            b.append(",VAL");
        }
        return b.toString();
    }
}