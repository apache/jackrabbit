
WordNet Synonym Provider implementation
=======================================

This contribution requires some manual steps to build the
jar file:

1) Download the lucene-2.0.0.zip distribution, e.g. from here:
   http://mirror.switch.ch/mirror/apache/dist/lucene/java/archive/lucene-2.0.0.zip
   
2) Extract the file lucene-2.0.0/contrib/memory/lucene-memory-2.0.0.jar

3) Install the jar file using the following command:
   
   mvn install:install-file -DgroupId=org.apache.lucene -DartifactId=lucene-memory \
    -Dversion=2.0.0 -Dpackaging=jar -Dfile=lucene-memory-2.0.0.jar

4) Download the WordNet 2.0 prolog release from here:
   http://www.cogsci.princeton.edu/2.0/WNprolog-2.0.tar.gz
   
5) Extract the file wn_s.pl to:
   src/main/java/org/apache/jackrabbit/core/query/wordnet
   

Now you can build the WordNet Synonym Provider:

mvn install


To use this contribution with a jackrabbit installation, copy the
created jar file and the downloaded lucene-memory-2.0.0.jar into the
classpath where jackrabbit is installed and add the following
parameter to the search configuration:

<param name="synonymProviderClass" value="org.apache.jackrabbit.core.query.wordnet.WordNetSynonyms"/>
