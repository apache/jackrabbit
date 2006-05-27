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

package org.apache.jackrabbit.core.query.test;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.TextFilter;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.QName;

import java.io.File;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

public class AbstractTextFilterTest {

    public void showResult(File file, TextFilter filter) throws Exception {
        PropertyId id = new PropertyId(null, new QName("", ""));
        PropertyState state = new PropertyState(id, 1, true);

        InternalValue value = InternalValue.create(file);
        state.setValues(new InternalValue[]{value});

        Map fields = filter.doFilter(state, System.getProperty("encoding"));
        for (Iterator it = fields.keySet().iterator(); it.hasNext();) {
            String field = (String) it.next();
            Reader r = (Reader) fields.get(field);
            System.out.println("---------------");
            System.out.println("Field: " + field);
            int i;
            while ((i = r.read()) != -1) {
                System.out.print((char) i);
            }
            r.close();
            System.out.println("");
        }
    }

}
