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

import org.apache.jackrabbit.core.data.RandomInputStream;
import org.apache.jackrabbit.core.query.AbstractIndexingTest;
import org.apache.jackrabbit.core.query.lucene.LazyTextExtractorField.ParsingTask;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;

public class LazyTextExtractorFieldTest extends AbstractIndexingTest {

    /**
     * @see <a
     *      href="https://issues.apache.org/jira/browse/JCR-3296">JCR-3296</a>
     *      Indexing ignored file types creates some garbage
     */
    public void testEmptyParser() throws Exception {

        InternalValue val = InternalValue
                .create(new RandomInputStream(1, 1024));

        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "application/java-archive");
        metadata.set(Metadata.CONTENT_ENCODING, "UTF-8");

        Parser p = getSearchIndex().getParser();

        ParsingTask task = new ParsingTask(p, val, metadata, Integer.MAX_VALUE) {
            public void setExtractedText(String value) {
                assertEquals("", value);
            }
        };
        task.run();
    }
}
