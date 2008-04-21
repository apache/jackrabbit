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
package org.apache.jackrabbit.ocm.manager.collectionconverter.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableMap;

/**
 *
 * {@link ManageableCollection} ArrayList implementation
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class ManageableMapImpl implements ManageableMap
{

	private Map map;


	public ManageableMapImpl(Map map) {
		super();
		this.map = map;
	}

	/**
	 *
	 * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#addObject(java.lang.Object)
	 */
    public void addObject(Object key, Object object)
    {
    	map.put(key, object);

    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#getIterator()
     */
    public Iterator getIterator()
    {
        return map.values().iterator();
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#getSize()
     */
	public int getSize()
	{

		return map.size();
	}


	public Map getObjects() {

		return map;
	}



}
