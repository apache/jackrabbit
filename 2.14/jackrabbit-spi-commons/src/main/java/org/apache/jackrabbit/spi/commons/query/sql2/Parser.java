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
package org.apache.jackrabbit.spi.commons.query.sql2;

import javax.jcr.ValueFactory;
import javax.jcr.query.qom.QueryObjectModelFactory;

/**
 * The SQL2 parser can convert a JCR-SQL2 query to a QueryObjectModel.
 *
 * @deprecated use {@link org.apache.jackrabbit.commons.query.sql2.Parser}
 *             instead.
 */
public class Parser extends org.apache.jackrabbit.commons.query.sql2.Parser {


    /**
     * Create a new parser. A parser can be re-used, but it is not thread safe.
     *
     * @param factory      the query object model factory
     * @param valueFactory the value factory
     */
    public Parser(QueryObjectModelFactory factory, ValueFactory valueFactory) {
        super(factory, valueFactory);
    }
}
