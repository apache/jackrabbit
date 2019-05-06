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
package org.apache.jackrabbit.core.query.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import junit.framework.TestCase;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.junit.Test;

public class UtilTest extends TestCase {

    public void testComparableContract(){
        //The test data needs to be greater than 32 in length to trigger TimSort
        //The data was obtained by running multiple runs with random sequence
        //and then reverse engineered from it :)
        Integer[] data = {null,25,21,5,null,23,10,19,10,null,null,10,24,null,10,null,7,11,
                null,7,null,14,26,0,6,19,null,5,null,4,28,19,5,28,18,14,12,16,14,15};
        List<Value[]> testData = createValueArrayList(data);
        Collections.sort(testData, new ValueArrayComparator());
    }

    private static List<Value[]> createValueArrayList(Integer[] data){
        List<Value[]> result = new ArrayList<Value[]>(data.length);
        for(Integer i : data){
            Value[] r = null;
            if(i != null){
                r = new Value[]{ValueFactoryImpl.getInstance().createValue(i.longValue())};
            }
            result.add(r);
        }
        return result;
    }

    private static class ValueArrayComparator implements Comparator<Value[]> {
        @Override
        public int compare(Value[] a, Value[] b) {
            try {
                return Util.compare(a, b);
            } catch (RepositoryException e) {
                throw new RuntimeException("Unable to compare values "
                        + Arrays.toString(a) + " and " + Arrays.toString(b), e);
            }
        }
    }
}
