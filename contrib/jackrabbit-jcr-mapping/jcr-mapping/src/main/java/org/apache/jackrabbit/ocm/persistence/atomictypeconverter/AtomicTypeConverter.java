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

package org.apache.jackrabbit.ocm.persistence.atomictypeconverter;

import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * 
 * AtomicTypeConverter interface.
 * 
 * The Object converter used atomic type converters to map atomic fields to JCR Value objects.
 * Amotic fields are primitive java types and their wrapper classes.
 * 
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public interface AtomicTypeConverter
{
	/**
	 * Convert an object into a JCR value.
	 *
     * @param valueFactory The JCR ValueFactory 
	 * @param object The object to convert
	 * @return the corresponding JCR value
	 *  
	 */
	public Value getValue(ValueFactory valueFactory, Object object);

	/**
	 * Convert a jcr property value into an object
	 * 
	 * @param value The JCR property value
	 * @return the corresponding object	
	 */
	public Object getObject(Value value);
	
	/**
	 * Get the string converted value. This is mainly used to build xpath expressions 
	 * 
	 * @param valueFactory The JCR ValueFactory
	 * @param object The object value
	 * @return The string converted value	 
	 */
	public String getXPathQueryValue(ValueFactory valueFactory, Object object);
}