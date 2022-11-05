# tb.parser.c

C Parser based on Eclipse CDT providing:

- C AST to JSON conversion

## Dependencies

- https://github.com/stleary/JSON-java
- Eclipse CDT

## Build

1. Call make.cmd or make.sh for building JSON
2. Deploy using Eclipse creating a "Runnable JAR file" if CParser.java

## Usage

~~~bash
java -jar CParser.jar [Files]
~~~
