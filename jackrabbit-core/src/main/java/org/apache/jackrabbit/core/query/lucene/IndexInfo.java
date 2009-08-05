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

/**
 * <code>IndexInfo</code> implements a single index info, which consists of a
 * index segment name and a generation number.
 */
final class IndexInfo implements Cloneable {

    /**
     * The name of the index segment.
     */
    private final String name;

    /**
     * The generation number.
     */
    private long generation;

    /**
     * Creates a new index info.
     *
     * @param name the name of the index segment.
     * @param generation the generation.
     */
    IndexInfo(String name, long generation) {
        this.name = name;
        this.generation = generation;
    }

    /**
     * @return the name of the index segment.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the generation of this index info.
     */
    public long getGeneration() {
        return generation;
    }

    /**
     * Sets a new generation
     * @param generation
     */
    public void setGeneration(long generation) {
        this.generation = generation;
    }

    public IndexInfo clone() {
        try {
            return (IndexInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            // will never happen, this class is cloneable
            throw new RuntimeException();
        }
    }
}
