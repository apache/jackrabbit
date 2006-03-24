ORM persistence managers for Jackrabbit README
----------------------------------------------

License (see also LICENSE.txt)
------------------------------

Collective work: Copyright 2006 The Apache Software Foundation.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Introduction
------------

This sub-project of Jackrabbit providers two ORM-based persistence manager
implementation. 
- OJB (http://db.apache.org/ojb) -based persistence manager
- Hibernate (http://www.hibernate.org) -based persistence manager

This also means that Jackrabbit can now store all content in a database.

Requirements
------------

This project assumes that you have already successfully compiled and 
installed the parent project Jackrabbit into your maven repository. If 
this is not the case, go back to the root project and launch : 
  maven jar:install
  
For running the full tests, you will require a rather large amount of 
free memory, as the default setup with HSQLDB requires a lot of memory
when handling large BLOB files.

Existing limitations 
--------------------

- Values (including multi-values) are stored in a single database column 
  in this implementation, limiting the length of values to the length
  defines in the SQL schema and the mapping files (there is no hard-coded
  limitation in the Java implementation though).
    
Running
-------

By default the package comes configured to run against an embedded HSQLDB
database. In order for the tests and the benchmark to run, you must first
run :

  maven clean
  
The above resets the database, and clean the whole environment to run the
tests and/or the benchmark. You can then launch 

  maven start.test.server
  
This launches the database in server mode (full embedded mode was not 
possible to implement because it doesn't finalize properly). Note that 
your shell is now blocked while running the database (CTRL+C to stop it)
and you will need a second shell to run the tests/benchmarks

From a second shell, you can now run

  maven
  
This will run the tests. Please note that the tests may take a while 
to run (1 minute on a P4 3.0Ghz).

If you just want to build the project without the tests : 

  maven -Dmaven.test.skip=true
  
Recycling HSQLDB
----------------

If you want to restart a test session, here is the quick procedure to do so:

1. In the shell that launched the database, press CTRL+C
2. In the other shell, type : maven clean (this resets the database to an 
   empty schema by copying a file, so don't do it while HSQLDB is running !)
3. Relaunch the database in it's shell by typing : maven start.test.server
That's it !

Using MySQL
-----------

The ORM persistence managers have also been tested against a MySQL database. 
In order to configure the persistence managers to use MySQL instead of HSQLDB
you need to change the following files, depending on whether you want to use
the OJB or Hibernate implementation : 

For OJB : applications/test/ojb/repository_database.xml
For Hibernate : applications/test/hibernate.properties

This project also contains a database initialization script that contains the
DB schema :

  create_db_mysql.sql
  
Also available is a batch script for Windows, that initializes a MySQL database
called "jackrabbit", and recycles it if it already exists : 

  reset_db_mysql.bat

Enjoy !   
