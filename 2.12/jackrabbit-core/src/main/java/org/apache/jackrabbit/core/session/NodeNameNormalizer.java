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
package org.apache.jackrabbit.core.session;

import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class for checking/logging "weird" node names.
 * <p>
 * For now, it only checks that a node name being created uses NFC
 * 
 * @see <a href="http://www.unicode.org/reports/tr15/tr15-23.html">http://www.unicode.org/reports/tr15/tr15-23.html</a>
 */
public class NodeNameNormalizer {

    private static Logger log = LoggerFactory.getLogger(NodeNameNormalizer.class);

    public static void check(Name name) {
        if (log.isDebugEnabled()) {
            String lname = name.getLocalName();
            String normalized = Normalizer.normalize(lname, Form.NFC);
            if (!lname.equals(normalized)) {
                String message = "The new node name '" + dump(lname) + "' is not in Unicode NFC form ('" + dump(normalized) + "').";
                log.debug(message, new Exception("Call chain"));
            }
        }
    }

    private static String dump(String lname) {
        StringBuilder sb = new StringBuilder();
        for (char c : lname.toCharArray()) {
            if (c > ' ' && c < 127) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }
}
