/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

/**
 */
class VolatileIndex extends AbstractIndex {

    VolatileIndex(Analyzer analyzer) throws IOException {
        super(analyzer, new RAMDirectory());
    }

    long size() throws IOException {
        Directory dir = getDirectory();
        String[] files = dir.list();
        long size = 0;
        for (int i = 0; i < files.length; i++) {
            size += dir.fileLength(files[i]);
        }
        return size;
    }
}
