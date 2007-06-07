Overview
-----------------
JCR-commands contains a jcr command line client. It's 
backed by a set of Apache Commons Chain commands that 
perform operations on any JCR compliant repository. 

For more information about using Commons Chain please refer 
to http://jakarta.apache.org/commons/chain/

Building
------------------
mvn clean install assembly:assembly

USING
------------------
  unzip the file target/jcr-commands.zip and type
  > cd bin
  > call run 

Help on commands:
  Type help and you'll get the full list of available commands.
  Type help [command name] to get a detailed description of the given command.

Starting jackrabbit:
  In the following example the config file is under 
  /repository/repository.xml and the repository home 
  is under /repository.

  > startjackrabbit /repository/repository.xml /repository
  > login [username] [password]
  
Connecting to a remote JCR-RMI server through JNDI  
--------------------------------------------------
1. Edit the file bin/jndi.properties
2. Copy client JARs to /lib. e.g. /jboss/client/jbossall-client.jar
3. run the command line
4. type the following command:
> jndi [jndi address]

Benchmarking JCR
------------------
You will find a plugin for jmeter under "/jmeter-chain" that 
will allow you to add commons chain commands to 
jmeter in order to prepare custom test plans.

IMPORTANT:
Jmeter uses an older version of Commons-Collection. 
Remember to replace it with a 3.x version.

INSTRUCTIONS:
1 - Install Jmeter.
    see http://jakarta.apache.org/jmeter/.
2 - build jmeter-chain-xx.jar and place it under "lib/ext" [1][2]. 
3 - build jcr-commands-xx.jar and place it under "lib/ext". [2].
4 - put jcr-commands and jmeter-chain dependencies under 
    /lib [2]
5 - Put your jcr implementation and all its dependencies 
    under "/lib" [2]

You'll find simple test plan examples under 
"/benchmarking".

[1] See /jmeter-chain/README.txt
[2] The path is relative to the JMeter installation folder.
   