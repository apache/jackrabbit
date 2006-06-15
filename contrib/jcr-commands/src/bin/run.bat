echo off
set CP=./;../classes/
for %%i in (../lib/*.jar) do call cp.bat %%i 
echo on
java -classpath %CP% org.apache.jackrabbit.command.cli.JcrClient