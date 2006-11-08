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
package org.apache.jackrabbit.taglib.filter;

import javax.jcr.Item;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.apache.log4j.Logger;

/**
 * It evaluates any javax.jcr.Item based on a JEXL valid expression which
 * returns a Boolean instance. The javax.jcr.Item is added to the JEXLContext
 * with the name of "item". A valid JEXL expression would be
 * "item.name.equals('MyNodeName')".
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class JEXLItemFilter implements ItemFilter
{
    private static Logger log = Logger.getLogger(JEXLItemFilter.class);

    /** Contex */
    JexlContext jc = JexlHelper.createContext();

    private Expression expression;

    /**
     * Set the expression to evaluate
     */
    public void setExpression(String exp)
    {
        try
        {
            this.expression = ExpressionFactory.createExpression(exp);
        } catch (Exception e)
        {
            log.error("Unable to create expression. " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate a node. <br>
     */
    public boolean evaluate(Object o)
    {
        // Evaluate
        try
        {
            Item item = (Item) o;

            // Clear the context
            jc.getVars().clear();

            // Add nodes to the context
            jc.getVars().put("item", item);

            // Evaluate
            return ((Boolean) this.expression.evaluate(jc)).booleanValue();
        } catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Unable to evalute " + e.getMessage(), e);
            }
            return false;
        }
    }

}