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
package org.apache.jackrabbit.test.api.query;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Test cases for order by queries on date properties.
 *
 * @tck.config testroot path to node that accepts child nodes of type
 *   <code>nodetype</code>
 * @tck.config nodetype name of a node type
 * @tck.config nodename1 name of a child node of type <code>nodetype</code>
 * @tck.config nodename2 name of a child node of type <code>nodetype</code>
 * @tck.config nodename3 name of a child node of type <code>nodetype</code>
 * @tck.config nodename4 name of a child node of type <code>nodetype</code>
 * @tck.config propertyname1 name of a single value calendar property.
 *
 * @test
 * @sources OrderByDateTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.OrderByDateTest
 * @keywords level2
 */
public class OrderByDateTest extends AbstractOrderByTest {

    /**
     * Tests order by queries with calendar properties.
     */
    public void testDateOrder() throws Exception {
        Calendar c1 = Calendar.getInstance();
        c1.set(2000, 4, 20, 14, 35, 14);
        Calendar c2 = Calendar.getInstance();
        c2.set(2000, 5, 20, 14, 35, 14);
        Calendar c3 = Calendar.getInstance();
        c3.set(2000, 4, 20, 14, 35, 13);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new String[]{nodeName3, nodeName1, nodeName2});
    }

    /**
     * Tests order by queries with calendar properties where the calendar
     * values only have a millisecond difference.
     */
    public void testDateOrderMillis() throws Exception {
        Calendar c1 = Calendar.getInstance();
        c1.set(2000, 6, 12, 14, 35, 19);
        c1.set(Calendar.MILLISECOND, 10);
        Calendar c2 = Calendar.getInstance();
        c2.set(2000, 6, 12, 14, 35, 19);
        c2.set(Calendar.MILLISECOND, 9);
        Calendar c3 = Calendar.getInstance();
        c3.set(2000, 6, 12, 14, 35, 19);
        c3.set(Calendar.MILLISECOND, 11);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new String[]{nodeName2, nodeName1, nodeName3});
    }

    /**
     * Tests order by queries with calendar properties where the calendar
     * values have different time zones.
     */
    public void testDateOrderPositiveTimeZone() throws Exception {
        Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        c1.set(2000, 6, 12, 15, 35, 19);
        c1.set(Calendar.MILLISECOND, 10);
        Calendar c2 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c2.set(2000, 6, 12, 14, 35, 19);
        c2.set(Calendar.MILLISECOND, 9);
        Calendar c3 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c3.set(2000, 6, 12, 14, 35, 19);
        c3.set(Calendar.MILLISECOND, 11);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new String[]{nodeName2, nodeName1, nodeName3});
    }

    /**
     * Tests order by queries with calendar properties where the calendar
     * values have different time zones.
     */
    public void testDateOrderNegativeTimeZone() throws Exception {
        Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("GMT-1:00"));
        c1.set(2000, 6, 12, 13, 35, 19);
        c1.set(Calendar.MILLISECOND, 10);
        Calendar c2 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c2.set(2000, 6, 12, 14, 35, 19);
        c2.set(Calendar.MILLISECOND, 9);
        Calendar c3 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c3.set(2000, 6, 12, 14, 35, 19);
        c3.set(Calendar.MILLISECOND, 11);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new String[]{nodeName2, nodeName1, nodeName3});
    }

}
