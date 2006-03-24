/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.taglib.size;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 * StorageCalculator implementations are responsible of calculating the size
 * that uses a given Node or Property.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public interface SizeCalculator
{
    int BITS = 0;
    
    int BYTES = 1;

    int KILOBYTES = 2;

    int MEGABYTES = 3;

    int GIGABYTES = 4;

    /**
     * Sets the unit
     * 
     * @param unit
     */
    void setUnit(int unit);

    /**
     * Calculate the size of the given node.
     * 
     * @param node
     * @return size
     */
    double getSize(Node node);

    /**
     * Calculate the size of the given property.
     * 
     * @param property
     * @return size
     */
    double getSize(Property property);

}