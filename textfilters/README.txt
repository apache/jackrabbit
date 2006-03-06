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

