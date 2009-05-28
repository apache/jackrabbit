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

import java.math.BigDecimal;

/**
 * The <code>DecimalField</code> class is a utility to convert <code>javas.math.BigDecimal</code>
 * values into <code>String</code> values that are lexicographically ordered
 * according to the decimal value.
 *
 * TODO lexicographical string representation of BigDecimal
 * see e.g. http://www.mail-archive.com/java-user@lucene.apache.org/msg23632.html
 */
public class DecimalField {

    private DecimalField() {
    }

    public static String decimalToString(BigDecimal value) {
        // TODO implement
        throw new UnsupportedOperationException("JCR-1609: new Property Types");
    }

    public static BigDecimal stringToDecimal(String value) {
        // TODO implement
        throw new UnsupportedOperationException("JCR-1609: new Property Types");
    }
}
