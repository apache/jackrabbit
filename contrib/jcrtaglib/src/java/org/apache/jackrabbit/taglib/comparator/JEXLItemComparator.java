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
package org.apache.jackrabbit.taglib.comparator;

import javax.jcr.Item;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.apache.log4j.Logger;

/**
 * It compares any javax.jcr.Item based on a JEXL valid expression wich returns
 * a comparable instance. The javax.jcr.Item is added to the JEXLContext with
 * the name of "item". A valid JEXL expression would be "item.name".
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class JEXLItemComparator implements ItemComparator
{
    private static Logger log = Logger.getLogger(JEXLItemComparator.class);

    /** Context */
    JexlContext jc = JexlHelper.createContext();

    /** Expression to evaluate */
    private Expression expression;

    /** Sort order */
    private boolean ascending;

    public int compare(Object o1, Object o2)
    {
        // Cast nodes
        Item i1 = (Item) o1;
        Item i2 = (Item) o2;
        try
        {
            Comparable c1 = this.evaluate(i1);
            Comparable c2 = this.evaluate(i2);
            if (this.ascending)
            {
                return c1.compareTo(c2);
            } else
            {
                return c2.compareTo(c1);
            }
        } catch (Exception e)
        {
            log.error("Unable to evaluate expression. " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Expression to evaluate
     */
    public void setExpression(String exp)
    {
        try
        {
            this.expression = ExpressionFactory.createExpression(exp);
        } catch (Exception e)
        {
            log.error("Unable to create expression from String " + exp + ". "
                    + e.getMessage(), e);
        }
    }

    /**
     * Sort order
     */
    public void setAscending(boolean asc)
    {
        this.ascending = asc;
    }

    /**
     * Evaluate the expression for the given node
     * 
     * @param node
     * @return @throws
     *         Exception
     */
    private Comparable evaluate(Item item) throws Exception
    {
        // Clear the context
        jc.getVars().clear();

        // Add nodes to the context
        jc.getVars().put("item", item);

        // Evaluate
        return (Comparable) expression.evaluate(jc);
    }

}