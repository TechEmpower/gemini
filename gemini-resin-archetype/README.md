# Gemini Archetype

Gemini Archetype is the project used to build new Gemini applications via `mvn`.

To test changes to the Archetype in building a new Gemini application:

```
$ cd gemini
$ mvn compile
$ mvn install
```
This will install the Archetype locally and the following will attempt to build a new Gemini application project:

```
$ mvn archetype:generate -DarchetypeGroupId=com.techempower -DarchetypeArtifactId=gemini-resin-archetype -DarchetypeRepository=local -DarchetypeCatalog=local
```