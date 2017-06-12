package org.homer.versioner.core.output;

import org.neo4j.graphdb.Node;

import java.util.Optional;

public class NodeOutput {
    public Node node;

    public NodeOutput(Node node) {
        this.node = node;
    }

    public NodeOutput(Optional<Node> node) {
        this.node = node.orElse(null);
    }
}
