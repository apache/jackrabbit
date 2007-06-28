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
package org.apache.jackrabbit.ocm.manager.cache.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;

/**
*
* This is a simple cache implementation that can be used per retrieve requests.
* This avoids to load duplicated object instance.
* 
* @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
* 
*/
public class RequestObjectCacheImpl implements ObjectCache 
{

	private Map alreadyCachedObjects = new HashMap();
	
	public void cache(String path, Object object) 
	{		
		alreadyCachedObjects.put(path, object);
	}
	
	public void clear() 
	{
		alreadyCachedObjects.clear();
	}
	
	public boolean isCached(String path)
	{		
	     return alreadyCachedObjects.containsKey(path);
	}
	
	public Object getObject(String path)
	{
		return alreadyCachedObjects.get(path);
	}

}
