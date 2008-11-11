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



import java.util.Map;

import javax.jcr.ValueFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * {@link org.apache.jackrabbit.ocm.query.Filter}
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * @author <a href="mailto:the_mindstorm[at]evolva[dot]ro">Alex Popescu</a>
 */
public class FilterImpl implements Filter {
    private final static Log log = LogFactory.getLog(FilterImpl.class);

    private Class claszz;
    private String scope = "";
    private String nodeName = "*";
    private String jcrExpression = "";

    private ClassDescriptor classDescriptor;
    private Map atomicTypeConverters;
    private ValueFactory valueFactory;

    /**
     * Constructor
     *
     * @param classDescriptor
     * @param atomicTypeConverters
     * @param clazz
     */
    public FilterImpl(ClassDescriptor classDescriptor, Map atomicTypeConverters, Class clazz, ValueFactory valueFactory) {
        this.claszz = clazz;
        this.atomicTypeConverters = atomicTypeConverters;
        this.classDescriptor = classDescriptor;
        this.valueFactory = valueFactory;
    }

    public Object clone(){
    	return new FilterImpl(classDescriptor,atomicTypeConverters,claszz,valueFactory);
    }
    /**
     *
     * @see org.apache.jackrabbit.ocm.query.Filter#getFilterClass()
     */
    public Class getFilterClass() {
        return claszz;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#setScope(java.lang.String)
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#getScope()
     */
    public String getScope() {
        return this.scope;
    }

    public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	/**
     * @see org.apache.jackrabbit.ocm.query.Filter#addContains(java.lang.String, java.lang.String)
     */
    public Filter addContains(String scope, String fullTextSearch) {
        String jcrExpression = null;
        if (scope.equals(".")) {
            jcrExpression = "jcr:contains(., '" + fullTextSearch + "')";
        }
        else {
            jcrExpression = "jcr:contains(@" + this.getJcrFieldName(scope) + ", '" + fullTextSearch
                + "')";
        }

        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addBetween(java.lang.String, java.lang.Object, java.lang.Object)
     */
    public Filter addBetween(String fieldAttributeName, Object value1, Object value2) {
        String jcrExpression = "( @" + this.getJcrFieldName(fieldAttributeName) + " >= "
            + this.getStringValue(fieldAttributeName, value1)
            + " and @" + this.getJcrFieldName(fieldAttributeName) + " <= "
            + this.getStringValue(fieldAttributeName, value2) + ")";

        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addEqualTo(java.lang.String, java.lang.Object)
     */
    public Filter addEqualTo(String fieldAttributeName, Object value) {
        String jcrExpression = "@" + this.getJcrFieldName(fieldAttributeName) + " = "
            + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addGreaterOrEqualThan(java.lang.String, java.lang.Object)
     */
    public Filter addGreaterOrEqualThan(String fieldAttributeName, Object value) {
        String jcrExpression = "@" + this.getJcrFieldName(fieldAttributeName) + " >= "
            + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addGreaterThan(java.lang.String, java.lang.Object)
     */
    public Filter addGreaterThan(String fieldAttributeName, Object value) {
        String jcrExpression = "@" + this.getJcrFieldName(fieldAttributeName) + " > "
            + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addLessOrEqualThan(java.lang.String, java.lang.Object)
     */
    public Filter addLessOrEqualThan(String fieldAttributeName, Object value) {
        String jcrExpression = "@" + this.getJcrFieldName(fieldAttributeName) + " <= "
            + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addLessOrEqualThan(java.lang.String, java.lang.Object)
     */
    public Filter addLessThan(String fieldAttributeName, Object value) {
        String jcrExpression = "@" + this.getJcrFieldName(fieldAttributeName) + " < "
            + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addLike(java.lang.String, java.lang.Object)
     */
    public Filter addLike(String fieldAttributeName, Object value) {
        String jcrExpression = "jcr:like(" + "@" + this.getJcrFieldName(fieldAttributeName) + ", '"
            + value + "')";
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addNotEqualTo(java.lang.String, java.lang.Object)
     */
    public Filter addNotEqualTo(String fieldAttributeName, Object value) {
        String jcrExpression = "@" + this.getJcrFieldName(fieldAttributeName) + " != "
            + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addNotNull(java.lang.String)
     */
    public Filter addNotNull(String fieldAttributeName) {
        String jcrExpression = "@" + this.getJcrFieldName(fieldAttributeName);
        addExpression(jcrExpression);

        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addIsNull(java.lang.String)
     */
    public Filter addIsNull(String fieldAttributeName) {
        String jcrExpression = "not(@" + this.getJcrFieldName(fieldAttributeName) + ")";
        addExpression(jcrExpression);

        return this;
    }

    public Filter addOrFilter(String fieldAttributeName, String[] valueList) {
        String jcrExpression = "";
        for(Object object: valueList){
        jcrExpression =	"@" + this.getJcrFieldName(fieldAttributeName) + " = "
        + this.getStringValue(fieldAttributeName, object);
        orExpression(jcrExpression);
        }
        addExpression(jcrExpression);
        return this;
    }
    
    
    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addOrFilter(org.apache.jackrabbit.ocm.query.Filter)
     */
    public Filter addOrFilter(Filter filter) {
        FilterImpl theFilter = (FilterImpl) filter;
        if (theFilter.getJcrExpression() != null && theFilter.getJcrExpression().length() > 0)
        {
    	   if ( null == jcrExpression || "".equals(jcrExpression) )
    	   {
    		   jcrExpression =    ((FilterImpl) filter).getJcrExpression() ;
    	   }
    	   else
    	   {
    	         jcrExpression =   "(" + jcrExpression + ")  or ( "  +  ((FilterImpl) filter).getJcrExpression() + ")";
    	   }
        }
        return this;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Filter#addAndFilter(Filter)
     */
    public Filter addAndFilter(Filter filter) {
        FilterImpl theFilter = (FilterImpl) filter;
        if (theFilter.getJcrExpression() != null && theFilter.getJcrExpression().length() > 0)
        {
     	   if ( null == jcrExpression || "".equals(jcrExpression) )
    	   {
    		   jcrExpression =    ((FilterImpl) filter).getJcrExpression() ;
    	   }
    	   else
    	   {
    	         jcrExpression =   "(" + jcrExpression + ") and  ( "  +  ((FilterImpl) filter).getJcrExpression() + ")";
    	   }
        }
        return this;
    }


    public Filter addJCRExpression(String jcrExpression) {
       addExpression(jcrExpression);

        return this;
    }

    public Filter orJCRExpression(String jcrExpression) {
        orExpression(jcrExpression);

         return this;
     }
   
    
    private String getJcrFieldName(String fieldAttribute) {
        String jcrFieldName = classDescriptor.getJcrName(fieldAttribute);
        if (jcrFieldName == null) {
            log.error("Impossible to find the jcrFieldName for the attribute :" + fieldAttribute);
        }

        return jcrFieldName;

    }

    private String getStringValue(String fieldName, Object value) {
    	FieldDescriptor fieldDescriptor = classDescriptor.getFieldDescriptor(fieldName);
    	AtomicTypeConverter atomicTypeConverter = null ;
    	// if the attribute is a simple field (primitive data type or wrapper class)
    	if (fieldDescriptor != null)
    	{
	    	String fieldConverterName = fieldDescriptor.getConverter();

	    	// if a field converter is set in the mapping, use this one
	    	if ( fieldConverterName != null )
	    	{
	    		atomicTypeConverter = (AtomicTypeConverter) ReflectionUtils.newInstance(fieldConverterName);
	    	}
	    	// else use a default converter in function of the attribute type
	    	else
	    	{
	    		atomicTypeConverter = (AtomicTypeConverter) atomicTypeConverters.get(value.getClass());
	    	}
    	}
    	// else it could be a collection (for example, it is a multivalue property)
    	else
    	{
    		atomicTypeConverter = (AtomicTypeConverter) atomicTypeConverters.get(value.getClass());
    	}
        return atomicTypeConverter.getXPathQueryValue(valueFactory, value);
    }

    public String getJcrExpression() {
    	     return this.jcrExpression;
    }

    private void addExpression(String jcrExpression) {

    	     if (this.jcrExpression.length() >0) {
              	this.jcrExpression += " and ";
        }
        this.jcrExpression += jcrExpression ;
    }

    private void orExpression(String jcrExpression) {

	     if (this.jcrExpression.length() >0) {
         	this.jcrExpression += " or ";
	     }
	     this.jcrExpression += jcrExpression ;
    }
    
    
	public String toString() {
		return getJcrExpression();
	}


}