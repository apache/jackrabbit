=====================================
Welcome to Jackrabbit Text Extractors
=====================================

This is the Text Extractors component of the Apache Jackrabbit project.
This component contains extractor classes that allow Jackrabbit to
extract text content from binary properties for full text indexing.
The following file formats and MIME types are currently supported:

    * Microsoft Word
      [org.apache.jackrabbit.extractor.MsWordTextExtractor]
      * application/vnd.ms-word
      * application/msword

    * Microsoft Excel
      [org.apache.jackrabbit.extractor.MsExcelTextExtractor]
      * application/vnd.ms-excel

    * Microsoft PowerPoint
      [org.apache.jackrabbit.extractor.MsPowerPointTextExtractor] 
      * application/vnd.ms-powerpoint
      * application/mspowerpoint

    * Portable Document Format (PDF)
      [org.apache.jackrabbit.extractor.PdfTextExtractor]
      * application/pdf

    * OpenOffice.org
      [org.apache.jackrabbit.extractor.OpenOfficeTextExtractor]
      * application/vnd.oasis.opendocument.database
      * application/vnd.oasis.opendocument.formula
      * application/vnd.oasis.opendocument.graphics
      * application/vnd.oasis.opendocument.presentation
      * application/vnd.oasis.opendocument.spreadsheet
      * application/vnd.oasis.opendocument.text

    * Rich Text Format (RTF)
      [org.apache.jackrabbit.extractor.RTFTextExtractor]
      * application/rtf

    * HyperText Markup Language (HTML)
      [org.apache.jackrabbit.extractor.HTMLTextExtractor]
      * text/html

    * Extensible Markup Language (XML)
      [org.apache.jackrabbit.extractor.XMLTextExtractor]
      * text/xml

To use these text extractors with the Jackrabbit Core:

   1) add the jackrabbit-text-extractors jar file and the dependencies defined
      in the Maven POM in the Jackrabbit classpath, and
   2) add the fully qualified class names listed above in the "textFilterClasses"
      parameter of the "SearchIndex" configuration element of a Jackrabbit
      workspace configuration file (workspace.xml).
