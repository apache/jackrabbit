1- download and deploy dojo webapp 
   download from
   http://www.ibiblio.org/maven2/org/apache/geronimo/applications/geronimo-dojo/1.2-beta/
   rename to dojo.war
   and deploy in a webapp server

2. package and deploy jcr-navigator

3. Add a user to the container with the role "user"
   e.g. in tomcat add the following lines to TOMCAT_HOME/conf/tomcat-users.xml
  <role rolename="user"/>
  <user username="jackrabbit" password="jackrabbit" roles="user"/>

 4. Start tomcat and login to the navigator app with the user "jackrabbit" and the password "jackrabbit"