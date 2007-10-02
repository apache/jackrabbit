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
package org.apache.jackrabbit.ocm.testmodel.auto;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;


@Node(isInterface=true, jcrType="ocm:cmsobject")
public interface CmsObject {

	public String getName();

	public void setName(String name);

	public String getPath();

	public void setPath(String path);

	public Folder  getParentFolder();

	public void setParentFolder(Folder parentFolder);

}
