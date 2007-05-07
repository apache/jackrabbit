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
package org.apache.portals.graffito.jcr.persistence.atomictypeconverter;


import java.util.Map;

/**
 * This interface defines a provider for accessing
 * {@link org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverter}
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public interface AtomicTypeConverterProvider {
    /**
     * Retrieves the <code>AtomicTypeConverter</code> associated with a class.
     * 
     * @param clazz a class
     * @return the corresponding <code>AtomicTypeConverter</code> or <tt>null</tt>
     * if the class has no <code>AtomicTypeConverter</code> associated
     */
    AtomicTypeConverter getAtomicTypeConverter(Class clazz);

    /**
     * Returns a map of all registered <code>AtomicTypeConverter<code>s.
     * 
     * @return <code>Map<Class, AtomicTypeConverter></code>
     */
    Map getAtomicTypeConverters();

}