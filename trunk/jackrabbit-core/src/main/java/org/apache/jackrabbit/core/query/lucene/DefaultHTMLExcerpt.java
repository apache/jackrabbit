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

import org.apache.lucene.index.TermPositionVector;

import java.io.IOException;

/**
 * <code>DefaultHTMLExcerpt</code> creates a HTML excerpt with the following
 * format:
 * <pre>
 * &lt;div&gt;
 *     &lt;span&gt;&lt;strong&gt;Jackrabbit&lt;/strong&gt; implements both the mandatory XPath and optional SQL &lt;strong&gt;query&lt;/strong&gt; syntax.&lt;/span&gt;
 *     &lt;span&gt;Before parsing the XPath &lt;strong&gt;query&lt;/strong&gt; in &lt;strong&gt;Jackrabbit&lt;/strong&gt;, the statement is surrounded&lt;/span&gt;
 * &lt;/div&gt;
 * </pre>
 */
public class DefaultHTMLExcerpt extends AbstractExcerpt {

    /**
     * {@inheritDoc}
     */
    protected String createExcerpt(TermPositionVector tpv,
                                   String text,
                                   int maxFragments,
                                   int maxFragmentSize) throws IOException {
        return DefaultHighlighter.highlight(tpv, getQueryTerms(), text,
                "<div>", "</div>", "<span>", "</span>", "<strong>", "</strong>",
                maxFragments, maxFragmentSize / 2);
    }
}
