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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.jackrabbit.core.search.lucene.AbstractIndex;

import java.io.IOException;

/**
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class PersistentIndex extends AbstractIndex {

    private String location;

    PersistentIndex(String location, Analyzer analyzer) throws IOException {
	super(analyzer,
		IndexReader.indexExists(location)
		? FSDirectory.getDirectory(location, false)
		: FSDirectory.getDirectory(location, true));

	this.location = location;

    }

    void mergeIndex(AbstractIndex index) throws IOException {
	this.getIndexWriter().addIndexes(
		new IndexReader[] { index.getIndexReader() });
    }

    Directory getDirectory() throws IOException {
	return FSDirectory.getDirectory(location, false);
    }
}
