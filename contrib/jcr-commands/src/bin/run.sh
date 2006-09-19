for i in ../lib/*.jar
do
   # if the directory is empty, then it will return the input string
   # this is stupid, so case for it
   if [ "$i" != "../lib/*.jar" ] ; then
     if [ -z "$LOCALCLASSPATH" ] ; then
       LOCALCLASSPATH=$i
     else
       LOCALCLASSPATH="$i":"$LOCALCLASSPATH"
     fi
   fi
 done

java -classpath ../classes/:$LOCALCLASSPATH org.apache.jackrabbit.command.cli.JcrClient
