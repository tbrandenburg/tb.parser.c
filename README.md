# tb.parser.c

C Parser based on Eclipse CDT providing:

- C AST to JSON conversion

## Dependencies

- https://github.com/stleary/JSON-java
- Eclipse CDT

## Build

1. Call make.cmd or make.sh for building JSON
2. Deploy using Eclipse creating a "Runnable JAR file" of CParser.java

## Usage

~~~bash
AST to console:
  java -jar CParser.jar [Files]
Start py4j gateway to CParser instance:
  java -jar CParser.jar
~~~
