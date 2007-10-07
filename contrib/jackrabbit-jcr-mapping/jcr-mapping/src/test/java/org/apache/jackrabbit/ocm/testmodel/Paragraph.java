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
package org.apache.jackrabbit.ocm.testmodel;

import org.apache.jackrabbit.ocm.manager.beanconverter.impl.ParentBeanConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/** 
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * 
 */
@Node(jcrType="ocm:paragraph", discriminator=false)
public class Paragraph
{
    @Field(path=true) private String path;
	@Field(jcrName="ocm:text") private String text;
	
    // The converter ParentBeanConverterImpl can be used to have a simple reference 
	// to the page containing this pararaph (parent node) - cannot be updated
	@Bean(proxy=true, converter=ParentBeanConverterImpl.class) private Page page; 
	
    public String getPath() 
    {
		return path;
	}

	public void setPath(String path) 
	{
		this.path = path;
	}

	public Paragraph()
    {
        this.text = "Default text";
    }

    public Paragraph(String text)
    {
        this.text = text;
    }

    /**
     * @return Returns the text.
     */
    public String getText()
    {
        return text;
    }

    /**
     * @param text The text to set.
     */
    public void setText(String text)
    {
        this.text = text;
    }

	public Page getPage() 
	{
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	
    
    
}
