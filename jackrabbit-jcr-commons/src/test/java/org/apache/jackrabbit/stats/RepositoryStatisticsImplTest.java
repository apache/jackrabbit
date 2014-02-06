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
package org.apache.jackrabbit.stats;

import java.util.Iterator;
import java.util.Map.Entry;

import junit.framework.TestCase;
import org.apache.jackrabbit.api.stats.TimeSeries;

public class RepositoryStatisticsImplTest extends TestCase {

    private static final int DEFAULT_NUMBER_OF_ELEMENTS = 20;

    public void testDefaultIterator() {
        RepositoryStatisticsImpl repositoryStatistics = new RepositoryStatisticsImpl();

        Iterator<Entry<String, TimeSeries>> iterator = repositoryStatistics.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            iterator.next();
        }
        assertEquals(DEFAULT_NUMBER_OF_ELEMENTS, count);
    }

    public void testIteratorWithSingleCustomType() {
        RepositoryStatisticsImpl repositoryStatistics = new RepositoryStatisticsImpl();
        String type = "customType";
        repositoryStatistics.getCounter(type, false);

        Iterator<Entry<String, TimeSeries>> iterator = repositoryStatistics.iterator();
        int count = 0;
        boolean customTypeExists = false;
        while (iterator.hasNext()) {
            count++;
            Entry<String, TimeSeries> entry = iterator.next();
            if (type.equals(entry.getKey())) {
                customTypeExists = true;
            }
        }
        assertEquals(DEFAULT_NUMBER_OF_ELEMENTS + 1, count);
        assertTrue(customTypeExists);
    }
}
