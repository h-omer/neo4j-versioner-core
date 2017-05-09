# Neo4j Graph Versioner

Neo4j Graph Versioner is made of a series of procedures, meant to be used in order to help developers to manage the Entity-State model, by creating, updating and querying the graph.

## License

Apache License 2.0

## Installation

1. Download the latest [release](https://github.com/h-omer/neo4j-graph-versioner/releases);
2. Put the downloaded jar file into `$NEO4J_HOME/plugins` folder;
3. Start/Restart Neo4j.

## About

Neo4j Graph Versioner has been developed by [Marco Falcier](https://github.com/mfalcier) and [Alberto D'Este](https://github.com/albertodeste).

It's based on the following data model

## Examples

A little example on how you can add a `State` node to a given Entity:

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.update(d, {context:'some details'}, 'Error', 593920000000) YIELD node RETURN node
```

And how to retrieve the current `State`:

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.current.state(d) YIELD node RETURN node
```

## Full documentation

You can find the full codumentation [here](https://h-omer.github.io/neo4j-graph-versioner/).

## Feedback

We would love to hear what do you know about the Graph Versioner, how to improve it and how to fix (we hope not so many! :see_no_evil:) bad things. Say yours in the [issue](https://github.com/h-omer/neo4j-graph-versioner/issues) section.

