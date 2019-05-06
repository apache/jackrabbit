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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.store.Directory;

import java.util.List;
import java.io.IOException;

/**
 * <code>IndexDeletionPolicyImpl</code>...
 */
public class IndexDeletionPolicyImpl implements IndexDeletionPolicy {

    private static final String SEGMENTS = "segments";

    private final PersistentIndex index;

    private final long maxAge;

    public IndexDeletionPolicyImpl(PersistentIndex index, long maxAge)
            throws IOException {
        this.index = index;
        this.maxAge = maxAge;
        // read current generation
        readCurrentGeneration();
    }

    public void onInit(List<? extends IndexCommit> commits) throws IOException {
        checkCommits(commits);
    }

    public void onCommit(List<? extends IndexCommit> commits) throws IOException {
        checkCommits(commits);

        // report back current generation
        IndexCommit current = commits.get(commits.size() - 1);
        String name = current.getSegmentsFileName();
        if (name.equals(SEGMENTS)) {
            index.setCurrentGeneration(0);
        } else {
            index.setCurrentGeneration(
                    Long.parseLong(name.substring(SEGMENTS.length() + 1),
                            Character.MAX_RADIX));
        }
    }

    //-------------------------------< internal >-------------------------------

    private void checkCommits(List<? extends IndexCommit> commits) throws IOException {
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < commits.size() - 1; i++) {
            IndexCommit ic = commits.get(i);
            long lastModified = index.getDirectory().fileModified(ic.getSegmentsFileName());
            if (currentTime - lastModified > maxAge) {
                ic.delete();
            } else {
                // following commits are younger, no need to check
                break;
            }
        }
    }

    void readCurrentGeneration() throws IOException {
        Directory dir = index.getDirectory();
        String[] names = dir.listAll();
        long max = 0;
        if (names != null) {
            for (String name : names) {
                long gen = -1;
                if (name.startsWith(SEGMENTS)) {
                    if (name.length() == SEGMENTS.length()) {
                        gen = 0;
                    } else if (name.charAt(SEGMENTS.length()) == '_') {
                        gen = Long.parseLong(name.substring(SEGMENTS.length() + 1), Character.MAX_RADIX);
                    }
                }
                if (gen > max) {
                    max = gen;
                }
            }
        }
        index.setCurrentGeneration(max);
    }
}
