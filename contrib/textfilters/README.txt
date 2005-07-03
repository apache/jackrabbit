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
classpath. The filters will be automatically loaded 
on startup.

For further information, see the javadocs for:
org.apache.jackrabbit.core.query.TextFilter
org.apache.jackrabbit.core.query.TextFilterService

