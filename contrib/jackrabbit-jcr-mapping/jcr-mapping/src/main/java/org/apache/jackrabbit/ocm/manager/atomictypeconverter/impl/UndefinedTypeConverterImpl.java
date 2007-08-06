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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.ocm.exception.IncorrectAtomicTypeException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;

/**
 * 
 * String Type Converter 
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class UndefinedTypeConverterImpl implements AtomicTypeConverter
{
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter#getValue(java.lang.Object)
	 */
	public Value getValue(ValueFactory valueFactory, Object propValue)
	{

		  if (propValue == null)
		  {
			return null;
		  }
		  
          if (propValue.getClass() == String.class)
          {
        	  return valueFactory.createValue((String) propValue);  
          }

          if (propValue.getClass() == InputStream.class)
          {
        	  return valueFactory.createValue((InputStream) propValue);  
          }
	        
          if ((propValue.getClass() == Long.class) ||(propValue.getClass() == Integer.class))
          {
        	  return valueFactory.createValue(((Number) propValue).longValue());  
          }

          if (propValue.getClass() == Double.class )
          {
        	  return valueFactory.createValue(((Double) propValue).doubleValue());  
          }

          if (propValue.getClass() == Boolean.class )
          {
        	  return valueFactory.createValue(((Boolean) propValue).booleanValue());  
          }

          if (propValue.getClass() == Calendar.class )
          {
        	  
        	  return valueFactory.createValue((Calendar) propValue);  
          }

          if (propValue.getClass() == GregorianCalendar.class )
          {
        	  return valueFactory.createValue((GregorianCalendar) propValue);  
          }
          
          if (propValue.getClass() == GregorianCalendar.class )
          {
        	  return valueFactory.createValue((GregorianCalendar) propValue);  
          }
          
          if (propValue.getClass() == Date.class )
          {
        	  Calendar calendar = Calendar.getInstance();
        	  calendar.setTime((Date) propValue);
        	  return valueFactory.createValue(calendar);  
          }

          if (propValue.getClass() == Date.class )
          {
        	  Calendar calendar = Calendar.getInstance();
        	  calendar.setTime((Date) propValue);
        	  return valueFactory.createValue(calendar);  
          }
          
          throw new IncorrectAtomicTypeException("Impossible to convert the value - property type not found");
		
	}

    /**
     * 
     * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter#getObject(javax.jcr.Value)
     */
	public Object getObject(Value value)
    {
		try
		{

			if (value.getType() == PropertyType.STRING )
			{
				return value.getString();	
			}

			if (value.getType() == PropertyType.DATE)
			{
				return value.getDate();	
			}

			if (value.getType() == PropertyType.BINARY)
			{
				return value.getStream();	
			}

			if (value.getType() == PropertyType.DOUBLE)
			{
				return new Double(value.getDouble());	
			}

			if (value.getType() == PropertyType.LONG)
			{
				return new Long(value.getLong());	
			}

			if (value.getType() == PropertyType.BOOLEAN)
			{
				return new Boolean(value.getBoolean());	
			}

			if (value.getType() == PropertyType.NAME)
			{
				return value.getString();	
			}
			
			if (value.getType() == PropertyType.PATH)
			{
				return value.getString();	
			}

			if (value.getType() == PropertyType.REFERENCE)
			{
				return value.getString();	
			}
			
			throw new IncorrectAtomicTypeException("Impossible to create the value object - unsupported class");
			
		}
		catch (RepositoryException e)
		{
			throw new IncorrectAtomicTypeException("Impossible to convert the value : " + value.toString(), e);
		}

	}
	
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter#getStringValue(java.lang.Object)
	 */
	public String getXPathQueryValue(ValueFactory valueFactory, Object object)
	{
		return "'" + object.toString() + "'";
	}
}
