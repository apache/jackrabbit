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
package org.apache.jackrabbit.commons.json;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>JSONUtilTest</code>...
 */
public class JsonUtilTest extends TestCase {

    public void testGetJsonString() {
        Map m = new HashMap();
        m.put("abc", "\"abc\"");
        m.put("a \"b\" c", "\"a \\\"b\\\" c\"");
        m.put("a\tb\rc\nd\fe\b", "\"a\\tb\\rc\\nd\\fe\\b\"");
        m.put("\\abc", "\"\\\\abc\"");
        m.put("abc", "\"abc\"");

        // non-printable ascii other than those treated (\t,\r,\n)
        m.put(String.valueOf((char) 7), "\"\\u0007\"");
        m.put(String.valueOf((char) 30), "\"\\u001e\"");

        // chinese
        m.put("\u4e00a\u4e8cb\u4e09c", "\"\u4e00a\u4e8cb\u4e09c\"");
        /* arabic */
        m.put("\u062c\u062f\u064a\u062f", "\"\u062c\u062f\u064a\u062f\"");
        /* Сaзb?c */
        m.put("\u00d1a\u00e7b\u0416c", "\"\u00d1a\u00e7b\u0416c\"");
        // вишь
        // m.put("вишь", "\"\u00e2\u00e8\u00f8\u00fc\"");

        for (Iterator it = m.keySet().iterator(); it.hasNext();) {
            String key = it.next().toString();
            assertEquals(m.get(key).toString(), JsonUtil.getJsonString(key));
        }
    }
}