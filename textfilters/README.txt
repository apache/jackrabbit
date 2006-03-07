TextFilters allow Jackrabbit to extract text from binary
properties for indexing purposes.

Apache Jackrabbit is an effort undergoing incubation at the
Apache Software Foundation. Incubation is required of all newly
accepted projects until a further review indicates that the
infrastructure, communications, and decision making process
have stabilized in a manner consistent with other successful
ASF projects. While incubation status is not necessarily a
reflection of the completeness or stability of the code, it
does indicate that the project has yet to be fully endorsed
by the ASF.  The incubation status is recorded at

   http://incubator.apache.org/projects/jackrabbit.html

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

Copyright 2004-2005 The Apache Software Foundation or its licensors,
                    as applicable.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

