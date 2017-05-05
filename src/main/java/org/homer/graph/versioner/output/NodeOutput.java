package org.homer.graph.versioner.output;

import org.neo4j.graphdb.Node;

public class NodeOutput {
    public Node node;

    public NodeOutput(Node node) {
        this.node = node;
    }
}
