/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.jackrabbit.core.search.PathQueryNode;

/**
 * 
 */
public class PathFilter {

    private final String basePath;

    private final int type;

    PathFilter(String path, int type) {
        this.type = type;

        switch (type) {
            case PathQueryNode.TYPE_CHILDREN:
            case PathQueryNode.TYPE_DESCENDANT_SELF:
                if (path.length() > 1) {
                    this.basePath = path + "/";
                } else {
                    this.basePath = path;
                }
                break;
            case PathQueryNode.TYPE_EXACT:
                basePath = path;
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public boolean includes(String path) {
        if (!path.startsWith(basePath)) {
            return false;
        }

        switch (type) {
            case PathQueryNode.TYPE_CHILDREN:
                return (path.indexOf('/', basePath.length()) == -1);
            case PathQueryNode.TYPE_DESCENDANT_SELF:
                return path.length() > basePath.length();
            case PathQueryNode.TYPE_EXACT:
                return path.length() == basePath.length();
            default:
                // will never happen, checked in constructor: unknown type
                return false;
        }
    }
}
