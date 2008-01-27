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
package org.apache.jackrabbit.core.journal;

/**
 * Listener interface on a journal that gets called back for records that should be consumed.
 */
public interface RecordConsumer {

    /**
     * Return the unique identifier of the records this consumer
     * will be able to handle.
     *
     * @return unique identifier
     */
    String getId();

    /**
     * Return the revision this consumer has last seen.
     *
     * @return revision
     */
    long getRevision();

    /**
     * Consume a record.
     *
     * @param  record record to consume
     */
    void consume(Record record);

    /**
     * Set the revision this consumer has last seen.
     *
     * @param revision revision
     */
    void setRevision(long revision);

}
