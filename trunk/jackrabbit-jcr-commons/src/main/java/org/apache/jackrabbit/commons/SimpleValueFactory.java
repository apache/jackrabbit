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
package org.apache.jackrabbit.commons;

import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.value.AbstractValueFactory;

/**
 * Simple value factory implementation for use mainly in testing.
 * Complex value types such as names, paths and references are kept
 * just as strings, and no format checks nor any namespace prefix
 * updates are made.
 *
 * @since Apache Jackrabbit 2.3
 */
public class SimpleValueFactory extends AbstractValueFactory {

    @Override
    protected void checkPathFormat(String pathValue)
            throws ValueFormatException {
    }

    @Override
    protected void checkNameFormat(String nameValue)
            throws ValueFormatException {
    }

}
