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

import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;

/**
 * <code>FieldSelectors</code> contains commonly used field selectors.
 */
public class FieldSelectors {

    /**
     * Do not instantiate.
     */
    private FieldSelectors() {
    }

    @SuppressWarnings("serial")
    public static final FieldSelector UUID = new FieldSelector() {
        /**
         * Only accepts {@link FieldNames#UUID}.
         *
         * @param fieldName the field name to check.
         * @return result.
         */
        public FieldSelectorResult accept(String fieldName) {
            if (FieldNames.UUID == fieldName) {
                return FieldSelectorResult.LOAD_AND_BREAK;
            } else {
                return FieldSelectorResult.NO_LOAD;
            }
        }
    };

    @SuppressWarnings("serial")
    public static final FieldSelector UUID_AND_PARENT = new FieldSelector() {
        /**
         * Accepts {@link FieldNames#UUID} and {@link FieldNames#PARENT}.
         *
         * @param fieldName the field name to check.
         * @return result.
         */
        public FieldSelectorResult accept(String fieldName) {
            if (FieldNames.UUID == fieldName) {
                return FieldSelectorResult.LOAD;
            } else if (FieldNames.PARENT == fieldName) {
                return FieldSelectorResult.LOAD;
            } else {
                return FieldSelectorResult.NO_LOAD;
            }
        }
    };
}
