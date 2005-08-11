Overview
-----------------

This project contains:

1. JMeter plugin for running Commons Chain commands.
2. JMeter function backed by Commons Jexl. See javadocs. 

Dependencies not included in JMeter
-----------------
Add the following libraries to /lib

- commons-chain 
- commons-beanutils
- commons-jexl
- commons-collections. Jackrabbit uses a newer version of commons Collections.
  Replace the jar included in JMeter with collections 3.1

 
Installation
-----------------
 - Build the jar file and place it under /lib/ext.
 - Build a jar file with your custom Chain commands
 - Add the following properties to /org/apache/jmeter/resources/messages.properties
   in order to see the Chain labels correctly.
   1 - chain_config_title=Chain configuration
   2 - chain_request=Chain request

