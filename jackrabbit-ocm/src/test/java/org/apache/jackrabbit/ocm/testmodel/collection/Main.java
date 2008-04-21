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
package org.apache.jackrabbit.ocm.testmodel.collection;

import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;


/**
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * @version $Id: Exp $
 */
@Node
public class Main
{
	@Field(path=true) private String path;

    @Field private String text;
    @Collection (elementClassName=Element.class)
    private HashMapElement hashMapElement;

    @Collection private Map<String, Element> map;


    // 3 ways to implements a collection :

    // inherit from ManageableCollection
    @Collection (elementClassName=Element.class)
    private ArrayListElement arrayListElement;

    // standard collection with Type - no need to specify the elementClassName
    @Collection private List<Element> list;

    // Custom List
    @Collection
    private CustomList customList;


    public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	/**
     * @return Returns the elements.
     */
    public HashMapElement getHashMapElement()
    {
        return hashMapElement;
    }
    /**
     * @param elements The elements to set.
     */
    public void setHashMapElement(HashMapElement hashMap)
    {
        this.hashMapElement = hashMap;
    }

    public ArrayListElement getArrayListElement()
    {
		return arrayListElement;
	}

    public void setArrayListElement(ArrayListElement list)
    {
		this.arrayListElement = list;
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

	public Map<String, Element> getMap()
	{
		return map;
	}

	public void setMap(Map<String, Element> map)
	{
		this.map = map;
	}

	public List<Element> getList() {
		return list;
	}
	public void setList(List<Element> list) {
		this.list = list;
	}
	public CustomList getCustomList() {
		return customList;
	}
	public void setCustomList(CustomList customList) {
		this.customList = customList;
	}


}
