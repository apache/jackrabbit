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
package org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;


/**
 * An <code>AtomicTypeConverterProvider</code> that registers by default the
 * convertes available in OCM.
 *
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class DefaultAtomicTypeConverterProvider extends AtomicTypeConverterProviderImpl {
    /**
     * No-arg constructor.
     */
    public DefaultAtomicTypeConverterProvider() {
        m_converters= registerDefaultAtomicTypeConverters();
    }

    /**
     * Full constructor.
     *
     * @param converters a map of classes and their associated <code>AtomicTypeConverter</code>
     * classes.
     */
    public DefaultAtomicTypeConverterProvider(Map converters) {
        this();
        m_converters.putAll(converters);
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.AtomicTypeConverterProviderImpl#setAtomicTypeConvertors(java.util.Map)
     */
    public void setAtomicTypeConvertors(Map converters) {
        m_converters.putAll(converters);
    }

    protected Map registerDefaultAtomicTypeConverters() {
        Map converters= new HashMap();

        converters.put(String.class, StringTypeConverterImpl.class);
        converters.put(InputStream.class, BinaryTypeConverterImpl.class);
        converters.put(long.class, LongTypeConverterImpl.class);
        converters.put(Long.class, LongTypeConverterImpl.class);
        converters.put(int.class, IntTypeConverterImpl.class);
        converters.put(Integer.class, IntTypeConverterImpl.class);
        converters.put(double.class, DoubleTypeConverterImpl.class);
        converters.put(Double.class, DoubleTypeConverterImpl.class);
        converters.put(boolean.class, BooleanTypeConverterImpl.class);
        converters.put(Boolean.class, BooleanTypeConverterImpl.class);
        converters.put(Calendar.class, CalendarTypeConverterImpl.class);
        converters.put(GregorianCalendar.class, CalendarTypeConverterImpl.class);
        converters.put(Date.class, UtilDateTypeConverterImpl.class);
        converters.put(byte[].class, ByteArrayTypeConverterImpl.class);
        converters.put(Timestamp.class, TimestampTypeConverterImpl.class);

        return converters;
    }
}
