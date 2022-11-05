#!/bin/sh
cd external/JSON/src/main/java/
echo Compiling...
javac org/json/*.java
echo Creating JAR...
jar cf ../../../bin/json.jar org/json/*.class
cd ../../../../../