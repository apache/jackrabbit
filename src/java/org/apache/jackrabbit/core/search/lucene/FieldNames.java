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

/**
 * Defines field names that are used internally to store UUID, etc in the
 * search index.
 */
public class FieldNames {

    /**
     * Name of the field that contains the UUID of the node. Terms are stored
     * but not tokenized.
     */
    public static final String UUID = "_:UUID";

    /**
     * Name of the field that contains the fulltext index including terms
     * from all properties of a node. Terms are tokenized.
     */
    public static final String FULLTEXT = "_:FULLTEXT";

    /**
     * Name of the field that contains the UUID of the parent node. Terms are
     * stored and but not tokenized.
     */
    public static final String PARENT = "_:PARENT";

    /**
     * Name of the field that contains the label of the node. Terms are not
     * tokenized.
     */
    public static final String LABEL = "_:LABEL";
}
