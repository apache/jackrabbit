README - JCR Server
-------------------

Requirements
------------

This project assumes that you have already successfully compiled and 
installed the parent project Jackrabbit and the contrib project jcr-rmi
into your maven repository. If this is not the case, go back to the root
project and launch

  commons:  maven jar:install

which will build and copy Jackrabbit into ~/.maven/repository/org.apache.jackrabbit/jars

Also go to the contrib project jcr-rmi and launch

  maven jar:install

which will build and copy jcr-rmi to ~/.maven/repository/org.apache.jackrabbit/jars

After building all dependencies one can build the actual server webapp

  cd webdav
  maven jar:install

  cd ../server
  maven jar:install

  cd ../client
  maven jar:install

  cd ../webapp
  maven

