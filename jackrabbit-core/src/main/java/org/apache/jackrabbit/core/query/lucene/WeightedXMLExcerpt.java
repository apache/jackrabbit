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
 * <code>WeightedXMLExcerpt</code> creates an XML excerpt of a matching node. In
 * contrast to {@link DefaultXMLExcerpt} this implementation weights fragments
 * based on the proximity of highlighted terms. Highlighted terms that are
 * adjacent have a higher weight. In addition, the more highlighted terms, the
 * higher the weight.
 * <br>
 * E.g. if you search for 'jackrabbit' and 'query' you may get the following
 * result for a node:
 * <pre>
 * &lt;excerpt&gt;
 *     &lt;fragment&gt;&lt;highlight&gt;Jackrabbit&lt;/highlight&gt; implements both the mandatory XPath and optional SQL &lt;highlight&gt;query&lt;/highlight&gt; syntax.&lt;/fragment&gt;
 *     &lt;fragment&gt;Before parsing the XPath &lt;highlight&gt;query&lt;/highlight&gt; in &lt;highlight&gt;Jackrabbit&lt;/highlight&gt;, the statement is surrounded&lt;/fragment&gt;
 * &lt;/excerpt&gt;
 * </pre>
 *
 * @see WeightedHighlighter
 */
public class WeightedXMLExcerpt extends AbstractExcerpt {

    /**
     * {@inheritDoc}
     */
    protected String createExcerpt(TermPositionVector tpv,
                                   String text,
                                   int maxFragments,
                                   int maxFragmentSize)
            throws IOException {
        return WeightedHighlighter.highlight(tpv, getQueryTerms(), text,
                maxFragments, maxFragmentSize / 2);
    }
}
