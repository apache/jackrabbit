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
package org.apache.jackrabbit.taglib.template;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

/**
 * Template for testing purposes.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class SimpleTemplateEngine implements TemplateEngine
{
	private static Logger log = Logger.getLogger(SimpleTemplateEngine.class);

    public void setTemplate(String id)
    {
        // Nothing to do
    }

    public void write(PageContext ctx, Item item)
    {
        try
        {
            if (item instanceof Node)
            {
                ctx.getOut().write(item.getName());
            } else if (item instanceof Property)
            {
                Property prop = (Property) item;
                if (prop.getDefinition().isMultiple())
                {
                    ctx.getOut().write("Multiple: " + prop.getValues().length); 
                } else {
                    String value = prop.getValue().getString();
                    ctx.getOut().write(value);
                }
            } else
            {
                throw new IllegalArgumentException("Unsupported item. "
                        + item.getClass().getName());
            }
        } catch (Exception e)
        {
            log.error(e);
        }
    }

}