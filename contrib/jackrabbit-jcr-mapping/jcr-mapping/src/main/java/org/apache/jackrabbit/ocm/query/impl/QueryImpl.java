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

package org.apache.jackrabbit.ocm.query.impl;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;

/**
 * Default Query implementation 
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 *
 */
public class QueryImpl implements Query
{
		
	private Filter filter;
	
    ClassDescriptor classDescriptor;
    
    private ArrayList orderByExpressions = new ArrayList();

	/**
	 * Constructor 
	 * 
	 * @param filter
	 * @param mapper
	 */
	public QueryImpl(Filter filter, Mapper mapper) 
	{				
		this.filter = filter;
		classDescriptor = mapper.getClassDescriptorByClass(filter.getFilterClass());
	}

	/**
	 * @see org.apache.jackrabbit.ocm.query.Query#setFilter(org.apache.jackrabbit.ocm.query.Filter)
	 */
	public void setFilter(Filter filter)
	{
		this.filter = filter;
	}

	/**
	 * @see org.apache.jackrabbit.ocm.query.Query#getFilter()
	 */
	public Filter getFilter()
	{
        return this.filter;
	}

	public void addOrderByDescending(String fieldNameAttribute)
	{
		orderByExpressions.add("@" + this.getJcrFieldName(fieldNameAttribute) + " descending");
	}

	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.query.Query#addOrderByAscending(java.lang.String)
	 */
	public void addOrderByAscending(String fieldNameAttribute)
	{
		orderByExpressions.add("@" + this.getJcrFieldName(fieldNameAttribute) + " ascending");
	}
	
	public String getOrderByExpression()
	{
		
		if (orderByExpressions.size() == 0)
		{
		   return "";	
		}
		
		String orderByExpression = "order by ";
		Iterator iterator   = orderByExpressions.iterator();
		int count=1;
		while (iterator.hasNext())
		{
			   if (count > 1)
			   {
				   orderByExpression += " , ";
			   }
			   orderByExpression+= (String) iterator.next();
			   count++;
		}
		
		return orderByExpression;
	}
	
	
	private String getJcrFieldName(String fieldAttribute)
	{
		
		return classDescriptor.getJcrName(fieldAttribute);
	    	
	}

}
