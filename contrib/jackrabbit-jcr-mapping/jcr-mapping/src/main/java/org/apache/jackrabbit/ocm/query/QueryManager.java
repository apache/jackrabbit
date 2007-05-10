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
 * The query manager is used to instantiate query objects and execute query based on the object model. 
 * Internally, this service used the JCR QueryManager
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 *
 */
public interface QueryManager
{
	/**
	 * Create a new empty filter
	 * @param classQuery The class used to search
	 * @return a new instantiated filter object
	 */
	 public Filter createFilter(Class classQuery);
	 
	 /**
	  * Create a new empty query 
	  * @param filter the filter used by the query
	  * @return a new instantiated query object
	  */
     public Query createQuery(Filter filter); 
     
     /**
      * Build a JCR search expression from a Query
      * @param query the query matching to the JCR search expression
      * @return a JCR XPATH search expression 
      */
     public String buildJCRExpression(Query query); 
     
       
}
