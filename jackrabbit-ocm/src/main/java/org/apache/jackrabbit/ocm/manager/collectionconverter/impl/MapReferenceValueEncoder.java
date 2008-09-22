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


public class MapReferenceValueEncoder {

    private final static String PREFIX="OCM:";
    final static String KEY_TOKEN="MAPKEY:";
    final static String REFERENCE_TOKEN="MAPVALUE:";

    public static String decodeKey(String encoded)
    {
        String[] splitted = encoded.split(PREFIX);
        return splitted[1].replaceAll(KEY_TOKEN, "");
    }

    public static String decodeReference(String encoded)
    {
        String[] splitted = encoded.split(PREFIX);
        return splitted[2].replaceAll(REFERENCE_TOKEN, "");
    }
    
    public static String encodeKeyAndReference(String key, String reference)
    {
        return PREFIX+KEY_TOKEN+key+PREFIX+REFERENCE_TOKEN+reference;
    }
}
