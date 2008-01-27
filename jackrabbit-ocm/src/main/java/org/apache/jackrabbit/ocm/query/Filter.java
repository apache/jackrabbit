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
 *
 * JCR Filter interface.
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 *
 */
public interface Filter
{
    /**
     * Set the filter scope. The scope is an Node path specifying where to search in the content tree.
     * For example,
     * /mynode/mysecondnode/', the search engine will search on child objects in the /mynode/mysecondnode
     * /mynode/mysecondnode//', the search engine will search on desncendant objects in the /mynode/mysecondnode (the complete subnode tree)
     *
     * @param scope The filter scope
     *
     */
    void setScope(String scope);


    /**
     * Get the filter scope.
     *
     * @return The filter scope
     */
    String getScope();


    /**
     * Set the node name used to build the jcr search expression.
     *
     * @param nodeName
     */
    void setNodeName(String nodeName);

    /**
     * Get the node name used in the jcr expression
     * @return
     */
    String getNodeName();


    /**
     * Search content based on a fullTextSearch.
     * Depending on the full text search engine, you can also filter on properties.
     *
     * @param scope either a a jcr node or propserty. If a node is used, all properties of this node are searche (following the internal index
     * @param fullTextSearch The full text search string
     */
    Filter addContains(String scope, String fullTextSearch);

	Filter addBetween(String arg0, Object arg1, Object arg2);

	Filter addEqualTo(String arg0, Object arg1);

	Filter addGreaterOrEqualThan(String arg0, Object arg1);

	Filter addGreaterThan(String arg0, Object arg1);

	Filter addLessOrEqualThan(String arg0, Object arg1);
	
	Filter addLessThan(String arg0, Object arg1);

	Filter addLike(String arg0, Object arg1);

	Filter addNotEqualTo(String arg0, Object arg1);

	Filter addNotNull(String arg0);

	Filter addIsNull(String arg0);
	
	Filter addOrFilter(Filter arg0);

	Filter addAndFilter(Filter filter);
	
	Filter addJCRExpression(String jcrExpression);
	
    Class getFilterClass();
	

}
