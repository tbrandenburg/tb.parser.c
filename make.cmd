@echo off
cd external/JSON/src/main/java/
echo Compiling...
javac org/json/*.java
echo Creating JAR...
jar cf ../../../json.jar org/json/*.class
cd ../../../../../