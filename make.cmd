@echo off
echo Compiling...
mkdir bin
javac external/JSON/src/main/java/org/json/*.java -d bin
echo Creating JAR...W
mkdir libs\json\
cd bin
jar cf ../libs/json/json.jar org/json/*.class
cd ..