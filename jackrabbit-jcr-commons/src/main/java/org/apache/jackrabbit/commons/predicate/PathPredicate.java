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
package org.apache.jackrabbit.commons.predicate;

import java.util.regex.Pattern;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

/**
 * The path filter provides hierarchical filtering.
 *
 */
public class PathPredicate implements Predicate {

    /**
     * the internal regex pattern
     */
    protected final Pattern regex;

    /**
     * Creates a new default path filter
     * <pre>
     * | Pattern | Matches
     * | /foo    | exactly "/foo"
     * | /foo.*  | all paths starting with "foo."
     * | foo.*   | all files starting with "foo."
     * | /foo/*  | all direct children of /foo
     * | /foo/** | all children of /foo
     * </pre>
     * @param pattern the pattern
     */
    public PathPredicate(String pattern) {
        String suffix = "";
        String prefix = "";
        if (pattern.endsWith("/**")) {
            suffix = "/.*";
            pattern = pattern.substring(0, pattern.length() - 3);
        } else if (pattern.endsWith("*")) {
            suffix = "[^/]*$";
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        if (pattern.charAt(0) != '/') {
            prefix = "^.*/";
        }
        pattern = prefix + pattern.replaceAll("\\.", "\\\\.") + suffix;
        regex = Pattern.compile(pattern);
    }

    /**
     * @see Predicate#evaluate(java.lang.Object)
     */
    public boolean evaluate(Object item) {
        if ( item instanceof Item ) {
            try {
                return regex.matcher(((Item)item).getPath()).matches();
            } catch (RepositoryException re) {
                return false;
            }
        }
        return false;
    }
}