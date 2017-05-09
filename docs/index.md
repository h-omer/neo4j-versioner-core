# Neo4j Graph Versioner Documentation

Neo4j Graph Versioner is made of a series of procedures, meant to be used in order to help developers to manage the Entity-State model, by creating, updating and querying the graph.

## License

Apache License 2.0

## Installation

1. Download the latest [release](https://github.com/h-omer/neo4j-graph-versioner/releases);
2. Put the downloaded jar file into `$NEO4J_HOME/plugins` folder;
3. Start/Restart Neo4j.

# About

Neo4j Graph Versioner has been developed by [Marco Falcier](https://github.com/mfalcier) and [Alberto D'Este](https://github.com/albertodeste).

## Data Model

The current data model uses two kind of nodes: the Entity nodes, created by the user through a given Label and the `State` node, managed by the Graph Versioner.
The `State` node can be saw as the set of mutable properties which regards the Entity, which instead has got only immutable properties.
There are 3 different relationships:
* `(:Entity)-[:CURRENT {date: 123456789}]-(:State)`, representing the current Entity `State`;
* `(:Entity)-[:HAS_STATE {startDate: 123456788, endDate: 123456789}]-(State)`, representing an Entity `State`, it will have an endDate only if the `State` node is not the current one;
* `(newerState:State)-[:PREVIOUS {date: 123456788}]->(older:State)`, representing the previous `State` of the indexed one.

This is how the data model looks like:

![Data Model](https://raw.githubusercontent.com/h-omer/neo4j-graph-versioner/master/docs/images/data-model.png)

## Use cases

You can find some examples and use cases in the repository [wiki](https://github.com/h-omer/neo4j-graph-versioner/wiki).

# Procedures Reference

Neo4j procedure documentation can also be found using `CALL dbms.procedures()`.

## Procedure CheatSheet

Here you can find a "compressed" list of all the procedures:

Legend
* *Optional parameter*
* **Node/Path**

name | parameters | return values | description
---- | ---------- | ------------- | -----------
[graph.versioner.init](#init) | entityLabel, *{key:value,...}*, *{key:value,...}*, *additionalLabel*, *date* | **node** | Create an Entity node with an optional initial State.
[graph.versioner.update](#update) | **entity**, {key:value,...}, *additionalLabel*, *date* | **node** | Add a new State to the given Entity.
[graph.versioner.patch](#patch) | **entity**, {key:value,...}, *additionalLabel*, *date* | **node** | Add a new State to the given Entity, starting from the previous one. It will update all the properties, not labels.
[graph.versioner.get.current.path](#get-current-path) | **entity** | **path** | Get a the current Path (Entity, State and rels) for the given Entity.
[graph.versioner.get.current.state](#get-current-state) | **entity** | **node** | Get the current State node for the given Entity.
[graph.versioner.get.all](#get-all) | **entity** | **path** | Get all the State nodes for the given Entity.


## init

This procedure is used in order to initialize an Entity node, with an optional initial State. 
If a Map of `State` properties is given, it will also create a `State` node, with both `HAS_STATE` and `CURRENT` relationships. 
If date is given, `date` and `startDate` will be initialized with that value.

### Details

#### Name

`graph.versioner.init`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entityLabel` | mandatory | The name of the entity Label.
`{key:value,...}` | optional | A Map representing the Entity immutable properties.
`{key:value,...}` | optional | A Map representing the `State` properties.
`additionalLabel` | optional | The name of an additional Label to the new State.
`date` | optional | The time-in-millis value of a given date, used instead of the current one.

#### Return value

name | type 
---- | ----
node | Node 

### Example call

```cypher
CALL graph.versioner.init('Person', {ssn: 123456789, name: 'Marco'}, {address: 'Via Roma 11'})
```

## update

This procedure is used in order to update a status of an existing Entity node. It will create a new `State` node, deleting the previous `CURRENT` relationship, creating a new one to the new created node with the current date (or the optional one, if given); then it update the last `HAS_STATE` relationship adding the current/given date as the `endDate` and creating a new `HAS_STATE` relationship with `startDate` as the current/given date. It will also create a new relationship between the new and the last `State` called `PREVIOUS`, with the old date as a property.
If the Entity node has no `State`, it will create a new `State` node, with both `HAS_STATE` and `CURRENT` relationships. 

### Details

#### Name

`graph.versioner.update`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`{key:value,...}` | mandatory | A Map representing the `State` properties.
`additionalLabel` | optional | The name of an additional Label to the new State.
`date` | optional | The time-in-millis value of a given date, used instead of the current one.

#### Return value

name | type 
---- | ----
node | Node 


### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.update(d, {context:'some details'}, 'Error', 593920000000) YIELD node RETURN node
```

## patch

This procedure is used in order to patch the current status of an existing Entity node, updating/creating the given properties, mantaining the oldest and untouched one. It will create a new `State` node, deleting the previous `CURRENT` relationship, creating a new one to the new created node with the current date (or the optional one, if given); then it update the last `HAS_STATE` relationship adding the current/given date as the `endDate` and creating a new `HAS_STATE` relationship with `startDate` as the current/given date. It will also create a new relationship between the new and the last `State` called `PREVIOUS`, with the old date as a property.
If the Entity node has no `State`, it will create a new `State` node, with both `HAS_STATE` and `CURRENT` relationships. 

### Details

#### Name

`graph.versioner.patch`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`{key:value,...}` | mandatory | A Map representing the `State` properties.
`additionalLabel` | optional | The name of an additional Label to the new State.
`date` | optional | The time-in-millis value of a given date, used instead of the current one.

#### Return value

name | type 
---- | ----
node | Node 

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.patch(d, {warnings: 'some warnings'}, 'Warning', 593920000000) YIELD node RETURN node
```

## get current path

This procedure is used to retrieve the current path: by a given Entity node, it will return a path formed by the Entity node, the `State` node and both `HAS_STATE` and `CURRENT` relationships.

This is how the returned path looks like:

![Get Current Path](https://raw.githubusercontent.com/h-omer/neo4j-graph-versioner/master/docs/images/get-path.png)

### Details

#### Name

`graph.versioner.get.current.path`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.


#### Return value

name | type 
---- | ----
path | Path

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.current.path(d) YIELD path RETURN path
```

## get current state

This procedure is used to retrieve the current `State` node: by a given Entity node.

### Details

#### Name

`graph.versioner.get.current.state`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.

#### Return value

name | type 
---- | ----
node | Node 

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.current.state(d) YIELD node RETURN node
```

## get all

This procedure is used to retrieve all the `State` nodes and `PREVIOUS` relationships in a path, by a given Entity node.

Here is how the returned path looks like:

![Get All](https://raw.githubusercontent.com/h-omer/neo4j-graph-versioner/master/docs/images/get-all.png)

### Details

#### Name

`graph.versioner.get.all`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.

#### Return value

name | type 
---- | ----
path | Path

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.all(d) YIELD path RETURN path
```

# Feedback

We would love to know what do you think about the Graph Versioner, how to improve it and how to fix (we hope not so many! :see_no_evil:) bad things. Say yours in the [issue](https://github.com/h-omer/neo4j-graph-versioner/issues) section.