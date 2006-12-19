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
package org.apache.jackrabbit.extractor;

/**
 * Interface for text extractors that need to delegate the extraction
 * of parts of content documents to another text extractor. This interface
 * is usually implemented by extractors of composite multimedia or archive
 * file formats.
 * <p>
 * The configured delegate text extractor is usually a composite extractor
 * that may contain also the delegating extractor, thus it is possible for
 * the extractor to be invoked recursively within a single thread. An
 * implementation should never pass the full content document to the
 * delegate extractor to avoid infinite loops.
 */
public interface DelegatingTextExtractor extends TextExtractor {

    /**
     * Sets the text textractor to which this extractor should delegate
     * any partial text extraction tasks. The given delegate extractor
     * is expected to be able to handle any content types passed to it.
     *
     * @param extractor delegate text extractor
     */
    void setDelegateTextExtractor(TextExtractor extractor);

}
