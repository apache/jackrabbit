TextFilters allow Jackrabbit to extract text from binary
properties for indexing purposes.

This project contains TextFilter implementations for the 
following binary formats:

1. MsExcel
2. MsPowerPoint
3. MsWord
4. Pdf

How to register in jackrabbit?
Build the jar file and place it in the Jackrabbit 
classpath together with the dependencies of these text
filters.
Configure them in the SearchIndex element of the workspace.xml

Sample:

...
  <SearchIndex class="org.apache.jackrabbit.core.query.lucene.SearchIndex">
    <param name="path" value="${wsp.home}/index" />
    <param name="textFilterClasses" value="org.apache.jackrabbit.core.query.MsExcelTextFilter,org.apache.jackrabbit.core.query.MsPowerPointTextFilter,org.apache.jackrabbit.core.query.MsWordTextFilter,org.apache.jackrabbit.core.query.PdfTextFilter,org.apache.jackrabbit.core.query.HTMLTextFilter,org.apache.jackrabbit.core.query.XMLTextFilter,org.apache.jackrabbit.core.query.RTFTextFilter,org.apache.jackrabbit.core.query.OpenOfficeTextFilter" />
  </SearchIndex>
...

For further information, see the javadocs for:
org.apache.jackrabbit.core.query.TextFilter

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2006 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
