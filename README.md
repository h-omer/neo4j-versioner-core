# Neo4j Versioner Core

Neo4j Versioner Core is a collection of procedures, aimed to help developers to manage the Entity-State model, by creating, updating and querying the graph.

## License

Apache License 2.0

## Installation

1. Download the latest [release](https://github.com/h-omer/neo4j-versioner-core/releases);
2. Put the downloaded jar file into `$NEO4J_HOME/plugins` folder;
3. Start/Restart Neo4j.

## About

Neo4j Versioner Core has been developed by [Alberto D'Este](https://github.com/albertodeste) and [Marco Falcier](https://github.com/mfalcier).

It's based on the following data model: 

![Data Model](https://raw.githubusercontent.com/h-omer/neo4j-versioner-core/master/docs/images/es-data-model.png)

## Examples

A little example on how you can add a `State` node to a given Entity:

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.update(d, {context:'some details'}, 'Error', localdatetime('1988-10-27T02:46:40')) YIELD node RETURN node
```

And how to retrieve the current `State`:

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.current.state(d) YIELD node RETURN node
```

If you want to use Neo4j Versioner Core procedures on your procedures/functions you simply create a new instance:

```java
Optional<Init> result = new InitBuilder().withDb(db).withLog(log).build();
result.ifPresent(a -> a.init("EntityLabel", entityProps, stateProps, additionalLabel, date));
```

## Full documentation

From version 2.0.0 you can also version relationships: too see how, see the full documentation [here](https://h-omer.github.io/neo4j-versioner-core/).

## Feedback

We would appreciate your feedback about our Versioner Core, how to improve and fix (we hope not so many! :see_no_evil:) any bad things. Say yours in the [issue](https://github.com/h-omer/neo4j-versioner-core/issues) section.

## Buy us a coffee :coffee:

This project is developed during our free time, and our free time is mostly during evening/night! So coffee is really helpful during our sessions :sweat_smile:. If you want to help us with that, you can buy us some :coffee: thanks to [PayPal](https://www.paypal.me/mfalcier/2)!
