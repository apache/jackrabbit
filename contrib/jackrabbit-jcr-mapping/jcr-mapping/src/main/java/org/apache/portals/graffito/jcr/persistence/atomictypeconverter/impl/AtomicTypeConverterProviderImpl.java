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
package org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.portals.graffito.jcr.exception.IncorrectAtomicTypeException;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverter;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverterProvider;


/**
 * Implementation of {@link AtomicTypeConverterProvider}.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class AtomicTypeConverterProviderImpl implements AtomicTypeConverterProvider {
    protected Map m_converters;
    protected Map m_converterInstances = new HashMap();
    
    /**
     * No-arg constructor.
     */
    public AtomicTypeConverterProviderImpl() {
    }
    
    /**
     * Full constructor.
     * 
     * @param converters a map of classes and their associated <code>AtomicTypeConverter</code>
     * classes.
     */
    public AtomicTypeConverterProviderImpl(Map converters) {
        m_converters= converters;
    }
    
    /**
     * Sets the associations of classes and their <code>AtomicTypeConverter</code>
     * classes.
     * 
     * @param converters <code>Map<Class, Class></code>
     */
    public void setAtomicTypeConvertors(Map converters) {
        m_converters= converters;
    }
    
    /**
     * @see org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverterProvider#getAtomicTypeConverter(java.lang.Class)
     */
    public AtomicTypeConverter getAtomicTypeConverter(Class clazz) {
        AtomicTypeConverter converter= (AtomicTypeConverter) m_converterInstances.get(clazz);
        if(null != converter) {
            return converter;
        }
        Class converterClass= (Class) m_converters.get(clazz);
        if(null == converterClass) {
            throw new IncorrectAtomicTypeException("No registered converter for class '" + clazz + "'");
        }
        
        try {
            converter= (AtomicTypeConverter) converterClass.newInstance();
            m_converterInstances.put(clazz, converter);
        }
        catch(Exception ex) {
            throw new IncorrectAtomicTypeException(
                    "Cannot create converter instance from class '" + clazz + "'", ex);
            
        }
        
        return converter;
    }
    
    /**
     * @see org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverterProvider#getAtomicTypeConverters()
     */
    public Map getAtomicTypeConverters() {
        Map result= new HashMap();
        for(Iterator it= m_converters.keySet().iterator(); it.hasNext(); ) {
            Class clazz= (Class) it.next();
            result.put(clazz, getAtomicTypeConverter(clazz));
        }
        
        return result;
    }
}
