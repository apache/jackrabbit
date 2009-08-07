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
package org.apache.jackrabbit.api.jsr283.observation;

/**
 * An <code>EventJournal</code> is an extension of <code>EventIterator</code>
 * that provides the additional method {@link #skipTo(long)}:
 *
 * @since JCR 2.0
 */
public interface EventJournal extends EventIterator {

    /**
     * Skip all elements of the iterator earlier than <code>date</code>.
     * <p/>
     * If an attempt is made to skip past the last element of the iterator,
     * no exception is thrown but the subsequent {@link #nextEvent()} will fail.
     *
     * @param date a date that is represented by the number of milliseconds
     *          since January 1, 1970, 00:00:00 GMT
     */
     public void skipTo(long date);
}
