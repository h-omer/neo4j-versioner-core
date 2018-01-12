package org.homer.versioner.core.procedure;

import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.types.Node;

public abstract class GenericProcedureTest {

    protected static Node initEntity(Session session) {

        return session.run("CREATE (:R)-[:FOR]->(e:Entity)-[:CURRENT {date:593910000000}]->(:State) RETURN e").single().get("e").asNode();
    }
}
