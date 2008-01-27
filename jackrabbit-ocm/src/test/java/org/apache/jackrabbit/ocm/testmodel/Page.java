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

import java.util.ArrayList;


import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.NTCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
@Node(jcrType="ocm:page", discriminator=false)
public class Page
{
	@Field(path=true) String path;
    @Field(jcrName="ocm:title") String title;

    @Collection(elementClassName=Paragraph.class, collectionConverter=NTCollectionConverterImpl.class)
    java.util.Collection paragraphs;

    public String getPath()
    {
		return path;
	}
	public void setPath(String path)
	{
		this.path = path;
	}
	/**
     * @return Returns the paragraphs.
     */
    public java.util.Collection getParagraphs()
    {
        return paragraphs;
    }
    /**
     * @param paragraphs The paragraphs to set.
     */
    public void setParagraphs(java.util.Collection paragraphs)
    {
        this.paragraphs = paragraphs;
    }
    /**
     * @return Returns the title.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @param title The title to set.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    public void addParagraph(Paragraph paragraph)
    {
    	if (paragraphs == null)
    	{
    		paragraphs = new ArrayList();
    	}
    	
    	paragraphs.add(paragraph);
    }



}
