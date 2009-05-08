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
package org.apache.jackrabbit.core.nodetype.compact;

/**
 * ParseException
 *
 * @deprecated Use {@link org.apache.jackrabbit.spi.commons.nodetype.compact.ParseException} instead.
 */
public class ParseException extends org.apache.jackrabbit.spi.commons.nodetype.compact.ParseException {

    public ParseException(int lineNumber, int colNumber, String systemId) {
        super(lineNumber, colNumber, systemId);
    }

    public ParseException(String message, int lineNumber, int colNumber, String systemId) {
        super(message, lineNumber, colNumber, systemId);
    }

    public ParseException(String message, Throwable rootCause, int lineNumber, int colNumber, String systemId) {
        super(message, rootCause, lineNumber, colNumber, systemId);
    }

    public ParseException(Throwable rootCause, int lineNumber, int colNumber, String systemId) {
        super(rootCause, lineNumber, colNumber, systemId);
    }
}
