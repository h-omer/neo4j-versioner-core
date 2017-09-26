# Neo4j Versioner Core Documentation

Neo4j Versioner Core is a collection of procedures, aimed to help developers to manage the Entity-State model, by creating, updating and querying the graph.

## License

Apache License 2.0

## Installation

1. Download the latest [release](https://github.com/h-omer/neo4j-versioner-core/releases);
2. Put the downloaded jar file into `$NEO4J_HOME/plugins` folder;
3. Start/Restart Neo4j.

# About

Neo4j Versioner Core has been developed by [Alberto D'Este](https://github.com/albertodeste) and [Marco Falcier](https://github.com/mfalcier).

## Data Model

The current data model uses two kind of nodes: the Entity nodes, created by the user through a given Label and the `State` nodes, managed by the Graph Versioner.
The `State` node can be seen as the set of mutable properties which regards the Entity, which possesses only immutable properties.
There are 4 different relationships:
* `(:Entity)-[:CURRENT {date: 123456789}]-(:State)`, representing the current Entity `State`;
* `(:Entity)-[:HAS_STATE {startDate: 123456788, endDate: 123456789}]-(State)`, representing an Entity `State`, it will have an endDate only if the `State` node is not the current one;
* `(newerState:State)-[:PREVIOUS {date: 123456788}]->(older:State)`, representing the previous `State` of the indexed one.
* `(rollbackedState:State)-[:ROLLBACK]->(older:State)`, representing that one `State` has been rolled back to a previous one. 

This is how the data model looks like:

![Data Model](https://raw.githubusercontent.com/h-omer/neo4j-versioner-core/master/docs/images/data-model.png)

## Use cases

You can find some examples and use cases in the repository [wiki](https://github.com/h-omer/neo4j-versioner-core/wiki) (work in progress!).

# Procedures Reference

Neo4j procedure documentation can also be found using `CALL dbms.procedures()`.

## How to call Core Versioner Procedures on your procedures/functions

If you want to use Neo4j Versioner Core procedures on your procedures/functions you simply create a new instance:

```java
Optional<Init> result = new InitBuilder().withDb(db).withLog(log).build();
result.ifPresent(a -> a.init("EntityLabel", entityProps, stateProps, additionalLabel, date));
```

### Maven users

Add the following repository and dependency to your `pom.xml` file

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.h-omer</groupId>
    <artifactId>neo4j-versioner-core</artifactId>
    <version>1.3.0</version>
    <scope>provided</scope>
</dependency>
```

### Gradle users

Add the following repository and dependency to your `build.gradle` file

```
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    compile 'com.github.h-omer:neo4j-versioner-core:1.3.0'
}
```

Neo4j Versioner Core must be a provided dependency on your project and it must be installed on your neo4j instance.

## Procedure CheatSheet

Here you can find a "compressed" list of all the procedures:

Legend
* *Optional parameter*
* **Node/Path**

name | parameters | return values | description
---- | ---------- | ------------- | -----------
[graph.versioner.init](#init) | entityLabel, *{key:value,...}*, *{key:value,...}*, *additionalLabel*, *date* | **node** | Create an Entity node with an optional initial State.
[graph.versioner.update](#update) | **entity**, *{key:value,...}*, *additionalLabel*, *date* | **node** | Add a new State to the given Entity.
[graph.versioner.patch](#patch) | **entity**, *{key:value,...}*, *additionalLabel*, *date* | **node** | Add a new State to the given Entity, starting from the previous one. It will update all the properties, not labels.
[graph.versioner.patch.from](#patch-from) | **entity**, **state**, *date* | **node** | Add a new State to the given Entity, starting from the given one. It will update all the properties, not labels.
[graph.versioner.get.current.path](#get-current-path) | **entity** | **path** | Get a the current path (Entity, State and rels) for the given Entity.
[graph.versioner.get.current.state](#get-current-state) | **entity** | **node** | Get the current State node for the given Entity.
[graph.versioner.get.all](#get-all) | **entity** | **path** | Get an Entity State path for the given Entity.
[graph.versioner.get.by.label](#get-by-label) | **entity**, label | **node** | Get State nodes with the given label, by the given Entity node.
[graph.versioner.get.by.date](#get-by-date) | **entity**, date | **node** | Get State node by the given Entity node, created at the given date.
[graph.versioner.get.nth.state](#get-nth-state) | **entity**, nth | **node** | Get the nth State node for the given Entity.
[graph.versioner.rollback](#rollback) | **entity**, *date* | **node** | Rollback the current State to the first available one.
[graph.versioner.rollback.to](#rollback-to) | **entity**, **state**, *date* | **node** | Rollback the current State to the given one.
[graph.versioner.rollback.nth](#rollback-nth) | **entity**, nth, *date* | **node** | Rollback the given Entity to the nth previous State.
[graph.versioner.diff](#diff) | **stateFrom**, **stateTo** | diff | Get a list of differences that must be applied to stateFrom in order to convert it into stateTo.
[graph.versioner.diff.from.previous](#diff-from-previous) | **state** | diff | Get a list of differences that must be applied to the previous statusof the given one in order to become the given state.
[graph.versioner.diff.from.current](#diff-from-current) | **state** | diff | Get a list of differences that must be applied to the given state in order to become the current entity state.

## init

This procedure is used in order to initialize an Entity node, with an initial State. 
If a Map of `State` properties is given, a `State` node will be created, with both `HAS_STATE` and `CURRENT` relationships; otherwise, a `State` node without properties will be created. 
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
CALL graph.versioner.init('Person', {ssn: 123456789, name: 'Marco'}, {address: 'Via Roma 11'}) YIELD node RETURN node
```

## update

This procedure is used in order to update a status of an existing Entity node. It will create a new `State` node, deleting the previous `CURRENT` relationship, creating a new one to the new created node with the current date (or the optional one, if given); then it update the last `HAS_STATE` relationship adding the current/given date as the `endDate` and creating a new `HAS_STATE` relationship with `startDate` as the current/given date. It will also create a new relationship between the new and the last `State` called `PREVIOUS`, with the old date as a property.
If the Entity node has no `State`, it will create a new `State` node, with both `HAS_STATE` and `CURRENT` relationships.
If no properties are passed, a new `State` node will be created, without properties.

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

This procedure is used in order to patch the current status of an existing Entity node, updating/creating the given properties, maintaining the oldest and untouched one. It will create a new `State` node, deleting the previous `CURRENT` relationship, creating a new one to the new created node with the current date (or the optional one, if given); then it update the last `HAS_STATE` relationship adding the current/given date as the `endDate` and creating a new `HAS_STATE` relationship with `startDate` as the current/given date. It will also create a new relationship between the new and the last `State` called `PREVIOUS`, with the old date as a property.
If the Entity node has no `State`, it will create a new `State` node, with both `HAS_STATE` and `CURRENT` relationships. 
If no properties are passed, a copy of the `CURRENT` node will be created as the new `State`.

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

## patch from

This procedure is used in order to patch the current `State` of an existing Entity node, updating/creating the properties using the those one the given `State, maintaining the oldest and untouched one. It will create a new `State` node, deleting the previous `CURRENT` relationship, creating a new one to the new created node with the current date (or the optional one, if given); then it update the last `HAS_STATE` relationship adding the current/given date as the `endDate` and creating a new `HAS_STATE` relationship with `startDate` as the current/given date. It will also create a new relationship between the new and the last `State` called `PREVIOUS`, with the old date as a property.
If the given `State` is not related with the given Entity, an error will occur.`
### Details

#### Name`

`graph.versioner.patch.from`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`state` | mandatory | The `State` used to patch the current state from.
`date` | optional | The time-in-millis value of a given date, used instead of the current one.

#### Return value

name | type 
---- | ----
node | Node 

### Example call

```cypher
MATCH (d:Device)-[:HAS_STATE->(s:State {code:2}) WITH d,s CALL graph.versioner.patch.from(d, s, 593920000000) YIELD node RETURN node
```

## get current path

This procedure is used to retrieve the current path: by a given Entity node, it will return a path formed by the Entity node, the `State` node ant the `CURRENT` relationship.

This is how the returned path looks like:

![Get Current Path](https://raw.githubusercontent.com/h-omer/neo4j-versioner-core/master/docs/images/get-current-path.png)

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

This procedure is used to retrieve all Entity's history in a path, including the Entity node, all the `State` nodes, `CURRENT` and `PREVIOUS` relationships.

Here is how the returned path looks like:

![Get All](https://raw.githubusercontent.com/h-omer/neo4j-versioner-core/master/docs/images/get-all.png)

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

## get by label

This procedure is used to retrieve all Entity `State` nodes, that have the given Label.


### Details

#### Name

`graph.versioner.get.by.label`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`label` | mandatory | The additional `State` nodes label.

#### Return value

name | type 
---- | ----
node | node

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.by.label(d, 'Error') YIELD node RETURN node
```

## get by date

This procedure is used to retrieve a specific Entity `State` node, that has been created at the given date.


### Details

#### Name

`graph.versioner.get.by.date`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`date` | mandatory | The time-in-millis value of a given date.

#### Return value

name | type 
---- | ----
node | node

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.by.date(d, 593920000000) YIELD node RETURN node
```

## get nth state

This procedure is used to retrieve the nth Entity `State` node.


### Details

#### Name

`graph.versioner.get.nth.state`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`nth` | mandatory | A number representing the nth `State` that must be returned.

#### Return value

name | type 
---- | ----
node | node

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.get.nth.state(d, 3) YIELD node RETURN node
```

## rollback

This procedure is used to rollback the current Entity `State` node, to the first available one. 
The first available `State` node, is the first previous node, without an existing `ROLLBACK` relationship.
If only one current `State` is available, `null` will be returned. If `date` is given, that value will be used instead of the current one.


### Details

#### Name

`graph.versioner.rollback`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`date` | optional | The time-in-millis value of a given date, used instead of the current one.

#### Return value

name | type 
---- | ----
node | node

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.rollback(d, 593920000000) YIELD node RETURN node
```

## rollback to

This procedure is used to rollback the current Entity `State` node, to the given one. 
If the given `State` is the current one, or if it already has a `CURRENT` relationship, `null` will be returned. 
If `date` is given, that value will be used instead of the current one.
If the given `State` is not related with the given Entity, an error will occur.`

### Details

#### Name

`graph.versioner.rollback.to`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`state` | mandatory | The State node to rollback to.
`date` | optional | The time-in-millis value of a given date, used instead of the current one.

#### Return value

name | type 
---- | ----
node | node

### Example call

```cypher
MATCH (d:Device)-[:HAS_STATE]->(s:State {code:2}) WITH d, s CALL graph.versioner.rollback.to(d, s, 593920000000) YIELD node RETURN node
```

## rollback nth

This procedure is used to rollback the current Entity `State` node, to the nth one. 
If the nth value is 0, `null` will be returned. 
If `date` is given, that value will be used instead of the current one.


### Details

#### Name

`graph.versioner.rollback.nth`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`entity` | mandatory | The entity node to operate with.
`nth` | mandatory | A number representing the nth `State` to rollback to.
`date` | optional | The time-in-millis value of a given date, used instead of the current one.

#### Return value

name | type 
---- | ----
node | node

### Example call

```cypher
MATCH (d:Device) WITH d CALL graph.versioner.rollback.nth(d, 3, 593920000000) YIELD node RETURN node
```

## diff

This procedure will offer a list of operation needed in order to obtain a given `State` node, from another given one.
It will return all the properties of both nodes, coupled with an operation.
Those possible operations are:
* **REMOVE** - if the property should be removed
* **ADD** - if the property is a new one
* **UPDATE** - if the property has changed

### Details

#### Name

`graph.versioner.diff`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`stateFrom` | mandatory | The starting State node for the comparison.
`stateTo` | mandatory | The ending State node for the comparison.

#### Return value

name | type 
---- | ----
diff | diff

### Example call

```cypher
MATCH (stateFrom:State {code:2}), (stateTo:State {code:3}) WITH stateFrom, stateTo CALL graph.versioner.diff(stateFrom, stateTo) YIELD diff RETURN diff
```

## diff from previous

This procedure will offer a list of operation needed in order to obtain a given `State` node, from its previous one.
It will return all the properties of both nodes, coupled with an operation (see [diff](#diff)] procedure for more information).


### Details

#### Name

`graph.versioner.diff.from.previous`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`state` | mandatory | The ending State node for the comparison.

#### Return value

name | type 
---- | ----
diff | diff

### Example call

```cypher
MATCH (s:State {code:2}) WITH s CALL graph.versioner.diff.from.previous(s) YIELD diff RETURN diff
```

## diff from current

This procedure will offer a list of operation needed in order to obtain a given `State` node, from the current one.
It will return all the properties of both nodes, coupled with an operation (see [diff](#diff)] procedure for more information).


### Details

#### Name

`graph.versioner.diff.from.current`

#### Parameters

name | necessity | detail 
---- | --------- | ------
`state` | mandatory | The starting State node for the comparison.

#### Return value

name | type 
---- | ----
diff | diff

### Example call

```cypher
MATCH (s:State {code:2}) WITH s CALL graph.versioner.diff.from.current(s) YIELD diff RETURN diff
```

# Feedback

We would appreciate your feedback about our Versioner Core, how to improve and fix (we hope not so many!) any bad things. Say yours in the [issue](https://github.com/h-omer/neo4j-versioner-core/issues) section.