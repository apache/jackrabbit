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

package org.apache.jackrabbit.ocm.query;

/**
 * JCR Query interface
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 *
 */
public interface Query
{
		
	/**
	 * Set the filter to use with this query
	 * @param filter The filter to use
	 */
	public void setFilter(Filter filter);
	
	/**
	 *
	 * @return The filter used for this query
	 */
	public Filter getFilter();
	
	/**
	 * Order the object found (ascending)
	 * @param fieldNameAttribute the name of the field used to sort the search result
	 */
	public void addOrderByAscending(String fieldNameAttribute);
	
	/**
	 * Order the object found (descending)
	 * @param fieldNameAttribute the name of the field used to sort the search result
	 */
	
	public void addOrderByDescending(String fieldNameAttribute);

}
