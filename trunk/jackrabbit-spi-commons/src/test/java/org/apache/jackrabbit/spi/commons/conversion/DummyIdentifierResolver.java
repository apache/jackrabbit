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
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * <code>DummyIdentifierResolver</code>...
 */
class DummyIdentifierResolver implements IdentifierResolver {

    private static final PathFactory FACTORY = PathFactoryImpl.getInstance();
    public static final String JCR_PATH = "/a/b/c";

    private final List<String> validIds;
    private final List<String> invalidFormats;
    private final List<String> invalidPaths;
    private final Path path;

    DummyIdentifierResolver() throws RepositoryException {
        path = FACTORY.create(FACTORY.getRootPath(), FACTORY.create("{}a\t{}b\t{}c"), true);
        validIds = new ArrayList<String>();
        validIds.add(UUID.randomUUID().toString());
        validIds.add("a:b");
        validIds.add("a[3]");
        validIds.add("34a[2[");
        validIds.add("34a/]");
        validIds.add(" 3-4a/'\"]");
        validIds.add("/a[3]/b/c:d/");
        validIds.add("{}\"\"\t{}a[3]\t{}b\t{}c:d");

        String invalidID = UUID.randomUUID().toString();
        String invalidIdSegment = "[" + invalidID + "]";
        String validSegment = "[" + validIds.get(0) + "]";
        
        invalidFormats = new ArrayList<String>();
        invalidPaths = new ArrayList<String>();

        for (String validId : validIds) {
            if (!validId.endsWith("]")) {
                invalidFormats.add("[" + validId);
            } else {
                invalidPaths.add("[" + validId);
            }

            if (!validId.startsWith("[")) {
                invalidFormats.add(validId + "]");
            } else {
                invalidPaths.add(validId + "]");
            }
        }
        invalidFormats.add(validSegment + "abc/abc");
        invalidFormats.add(validSegment + "/a/b/c");
        invalidFormats.add("/" + validSegment);
        invalidFormats.add("/a/b/" + validSegment + "/c");
        invalidFormats.add("/a/b/c" + validSegment);
        invalidFormats.add("/" + invalidIdSegment);

        // path starting with [ and ending with ] -> valid format but
        // might be invalid path. 
        invalidPaths.add(validSegment + "/a/b[2]");        
        invalidPaths.add(validSegment + "/" + validSegment);        
        invalidPaths.add(validSegment + "[2]");
        invalidPaths.add(invalidIdSegment);
        invalidPaths.addAll(invalidFormats);
    }

    List<String> getValidIdentifiers() {
        return validIds;
    }

    List<String> getInvalidIdentifierPaths() {
        return invalidPaths;
    }

    List<String> getInvalidIdentifierFormats() {
        return invalidFormats;
    }

    public Path getPath(String identifier) throws MalformedPathException {
        if (validIds.contains(identifier)) {
            return path;
        } else {
            throw new MalformedPathException("Invalid path: identifier '"+ identifier +"' cannot be resolved.");
        }
    }

    public void checkFormat(String identifier) throws MalformedPathException {
        if (validIds.contains(identifier)) {
            return;
        }
        throw new MalformedPathException("Invalid identifier.");
    }
}