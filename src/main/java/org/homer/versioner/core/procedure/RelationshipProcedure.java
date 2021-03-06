package org.homer.versioner.core.procedure;

import org.apache.commons.lang3.StringUtils;
import org.homer.versioner.core.builders.UpdateBuilder;
import org.homer.versioner.core.core.CoreProcedure;
import org.homer.versioner.core.exception.VersionerCoreException;
import org.homer.versioner.core.output.BooleanOutput;
import org.homer.versioner.core.output.RelationshipOutput;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.homer.versioner.core.Utility.*;

/**
 * RelationshipProcedure class, it contains all the Procedures needed to create versioned relationships between Entities
 */
public class RelationshipProcedure extends CoreProcedure {
    @Procedure(value = "graph.versioner.relationships.createTo", mode = Mode.WRITE)
    @Description("graph.versioner.relationships.createTo(entityA, entitiesB, relProps, date) - Create multiple relationships from entitySource to each of the entityDestinations with the given type and/or properties for the specified date.  The relationship 'versionerLabel' along with properties for each relationship can be passed in via 'relProps' a default label ('LABEL_UNDEFINED') is assigned to relationships that are not supplied with a 'versionerLabel' attribute in the props")
    public Stream<RelationshipOutput> relationshipsCreateTo(
            @Name("entitySource") Node entitySource,
            @Name("entityDestinations") List<Node> entityDestinations,
            @Name(value = "relProps", defaultValue = "[{}]") List<Map<String, Object>> relProps,
            @Name(value = "date", defaultValue = "null") LocalDateTime date) {

        Optional<Node> sourceCurrentState = createNewSourceState(entitySource, defaultToNow(date));
        isEntityOrThrowException(entitySource);
        entityDestinations.sort(Comparator.comparing(Node::getId));

        Stream<RelationshipOutput> relationshipOutputStream = getRelationshipOutputStream(entityDestinations, relProps, sourceCurrentState);
        return relationshipOutputStream;
    }

    @Procedure(value = "graph.versioner.relationships.createFrom", mode = Mode.WRITE)
    @Description("graph.versioner.relationships.createfrom(entitiesA, entityB, relProps, date) - Create multiple relationships from each of the entitySources to the entityDestination with the given type and/or properties for the specified date.  The relationship 'versionerLabel' along with properties for each relationship can be passed in via 'relProps' a default label ('LABEL_UNDEFINED') is assigned to relationships that are not supplied with a 'versionerLabel' attribute in the props")
    public Stream<RelationshipOutput> relationshipsCreateFrom(
            @Name("entitySources") List<Node> entitySources,
            @Name("entityDestination") Node entityDestination,
            @Name(value = "relProps", defaultValue = "[{}]") List<Map<String, Object>> relProps,
            @Name(value = "date", defaultValue = "null") LocalDateTime date) {

        entitySources.sort(Comparator.comparing(Node::getId));
        entitySources.stream().map(node -> {
            log.info("orderedNodes:  " + node.getProperty("id").toString());
            return null;
        });
        Stream<RelationshipOutput> out = null;
        Optional<Node> destinationRNode = getRNode(entityDestination);
        isEntityOrThrowException(entityDestination);
        if (entitySources.size() == relProps.size() && destinationRNode.isPresent()) {
            out = zip(entitySources, relProps).stream().map((item) -> {
                final Node entitySource = item.getLeft();
                log.info("entity source: " + entitySource.getProperty("id").toString());
                Map<String, Object> props = item.getRight();
                final String labelProp = "versionerLabel";
                Map<String, Object> filteredProps = props.entrySet().stream().filter(map -> !map.getKey().equals(labelProp))
                        .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
                String type = props.get(labelProp) instanceof String ? props.get(labelProp).toString() : "LABEL_UNDEFINED";
                Optional<Node> sourceCurrentState = createNewSourceState(entitySource, defaultToNow(date));
                boolean exists = StreamSupport.stream(sourceCurrentState.get().getRelationships(Direction.OUTGOING, RelationshipType.withName(type)).spliterator(), false).anyMatch(relationship -> relationship.getEndNode().getId() == destinationRNode.get().getId());
                if (exists) {
                    return Stream.<RelationshipOutput>empty();
                } else {
                    log.info("creating relationship from: " + sourceCurrentState.get().getId() + "  to: " + destinationRNode.get().getId());
                    return streamOfRelationships(createRelationship(sourceCurrentState.get(), destinationRNode.get(), type, filteredProps));
                }
            }).reduce(Stream::concat).orElseGet(Stream::empty);
        }
        return out;
    }

    private Stream<RelationshipOutput> getRelationshipOutputStream(List<Node> entityDestinations, List<Map<String, Object>> relProps, Optional<Node> sourceCurrentState) {
        return entityDestinations.size() == relProps.size() ? sourceCurrentState.map(node -> zip(entityDestinations, relProps).stream().map((item) -> {
            final Node destinationNode = item.getLeft();
            isEntityOrThrowException(destinationNode);
            Optional<Node> destinationRNode = getRNode(destinationNode);
            Map<String, Object> props = item.getRight();
            final String labelProp = "versionerLabel";
            Map<String, Object> filteredProps = props.entrySet().stream().filter(map -> !map.getKey().equals(labelProp))
                    .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
            if (destinationRNode.isPresent()) {
                String type = props.get(labelProp) instanceof String ? props.get(labelProp).toString() : "LABEL_UNDEFINED";
                streamOfRelationships(createRelationship(node, destinationRNode.get(), type, filteredProps));
            }
            return Stream.<RelationshipOutput>empty();

        }).reduce(Stream::concat).orElseGet(Stream::empty)).orElseGet(Stream::empty) : Stream.empty();
    }


    @Procedure(value = "graph.versioner.relationship.create", mode = Mode.WRITE)
    @Description("graph.versioner.relationship.create(entityA, entityB, type, relProps, date) - Create a relationship from entitySource to entityDestination with the given type and/or properties for the specified date.")
    public Stream<RelationshipOutput> relationshipCreate(
            @Name("entitySource") Node entitySource,
            @Name("entityDestination") Node entityDestination,
            @Name(value = "type") String type,
            @Name(value = "relProps", defaultValue = "{}") Map<String, Object> relProps,
            @Name(value = "date", defaultValue = "null") LocalDateTime date) {

        isEntityOrThrowException(entitySource);
        isEntityOrThrowException(entityDestination);

        Optional<Node> sourceCurrentState = createNewSourceState(entitySource, defaultToNow(date));
        Optional<Node> destinationRNode = getRNode(entityDestination);

        boolean exists = StreamSupport.stream(sourceCurrentState.get().getRelationships(Direction.OUTGOING, RelationshipType.withName(type)).spliterator(), true).anyMatch(relationship -> relationship.getEndNode().getId() == destinationRNode.get().getId());
        if (exists) {
            return null;
        }
        if (sourceCurrentState.isPresent() && destinationRNode.isPresent()) {
            return streamOfRelationships(createRelationship(sourceCurrentState.get(), destinationRNode.get(), type, relProps));
        } else {
            return Stream.empty();
        }
    }

    @Procedure(value = "graph.versioner.relationships.delete", mode = Mode.WRITE)
    @Description("graph.versioner.relationship.delete(entityA, entityB, type, date) - Delete multiple custom type relationship from entitySource's current State to each of the entityDestinations for the specified date.")
    public Stream<BooleanOutput> relationshipsDelete(
            @Name("entitySource") Node entitySource,
            @Name("entityDestinations") List<Node> entityDestinations,
            @Name(value = "type") String type,
            @Name(value = "date", defaultValue = "null") LocalDateTime date) {


        Optional<Node> sourceCurrentState = getCurrentState(entitySource);
        entityDestinations.stream().map((entityDestination) -> {
            return getBooleanOutputStream(entitySource, type, sourceCurrentState, entityDestination, date);
        });
        return Stream.of(new BooleanOutput(Boolean.FALSE));
    }

    private Stream<BooleanOutput> getBooleanOutputStream(@Name("entitySource") Node entitySource, @Name("type") String type, Optional<Node> sourceCurrentState, Node entityDestination, LocalDateTime date) {
        Optional<Node> destinationRNode = getRNode(entityDestination);
        Update updateProcedure = new UpdateBuilder().withLog(log).withTransaction(transaction).build().orElseThrow(() -> new VersionerCoreException("Unable to initialize update procedure"));

        if (sourceCurrentState.isPresent() && destinationRNode.isPresent()) {
            final long destId = destinationRNode.get().getId();
            updateProcedure.update(entitySource, sourceCurrentState.get().getAllProperties(), "", date);
            getCurrentRelationship(entitySource).ifPresent(rel -> rel.getEndNode().getRelationships(Direction.OUTGOING, RelationshipType.withName(type)).forEach(rel2 -> {
                if (rel2.getEndNode().getId() == destId) {
                    rel2.delete();
                }
            }));
            return Stream.of(new BooleanOutput(Boolean.TRUE));
        } else {
            return Stream.of(new BooleanOutput(Boolean.FALSE));
        }
    }

    @Procedure(value = "graph.versioner.relationship.delete", mode = Mode.WRITE)
    @Description("graph.versioner.relationship.delete(entityA, entityB, type, date) - Delete a custom type relationship from entitySource's current State to entityDestination for the specified date.")
    public Stream<BooleanOutput> relationshipDelete(
            @Name("entitySource") Node entitySource,
            @Name("entityDestination") Node entityDestination,
            @Name(value = "type") String type,
            @Name(value = "date", defaultValue = "null") LocalDateTime date) {

        isEntityOrThrowException(entitySource);
        isEntityOrThrowException(entityDestination);
        if (isSystemType(type)) {
            throw new VersionerCoreException("It's not possible to delete a System Relationship like " + type + ".");
        }

        Optional<Node> sourceCurrentState = getCurrentState(entitySource);
        return getBooleanOutputStream(entitySource, type, sourceCurrentState, entityDestination, date);
    }

    static Relationship createRelationship(Node source, Node destination, String type, Map<String, Object> relProps) {

        Relationship rel = source.createRelationshipTo(destination, RelationshipType.withName(type));
        relProps.forEach(rel::setProperty);
        return rel;
    }

    private Optional<Node> getRNode(Node entity) {
        return Optional.ofNullable(entity.getSingleRelationship(RelationshipType.withName("FOR"), Direction.INCOMING))
                .map(Relationship::getStartNode);
    }

    private Optional<Node> createNewSourceState(Node entitySource, LocalDateTime date) {

        Update updateProcedure = new UpdateBuilder().withLog(log).withTransaction(transaction).build().orElseThrow(() -> new VersionerCoreException("Unable to initialize update procedure"));
        return updateProcedure.patch(entitySource, Collections.emptyMap(), StringUtils.EMPTY, date)
                .map(n -> n.node)
                .findFirst();
    }
}
