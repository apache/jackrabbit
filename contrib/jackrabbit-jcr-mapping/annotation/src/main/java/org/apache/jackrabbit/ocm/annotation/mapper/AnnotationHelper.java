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
package org.apache.jackrabbit.ocm.annotation.mapper;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.ocm.annotation.Node;

/**
 * A helper to simplify handling the common annotations
 * 
 * @author Philip Dodds
 * 
 */
public class AnnotationHelper {

	public static String getJcrNodeName(Class clazz) {
		String name = getJcrNode(clazz).name();
		if ("".equals(name))
			return clazz.getSimpleName();
		else
			return name;
	}

	public static Node getJcrNode(Class annotatedClass) {
		return (Node) annotatedClass.getAnnotation(Node.class);
	}

	public static String getJcrNodeType(Class annotatedClass) {
		return getJcrNode(annotatedClass).prefix() + ":"
				+ (getJcrNodeName(annotatedClass));
	}

	public static QName getNodeType(Class annotatedClass) {
		return new QName(getJcrNode(annotatedClass).namespace(),
				getJcrNodeName(annotatedClass));
	}

}
