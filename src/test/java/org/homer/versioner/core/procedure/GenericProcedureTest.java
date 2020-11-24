package org.homer.versioner.core.procedure;

import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;

public abstract class GenericProcedureTest {

    protected static Node initEntity(Session session) {

        return session.run("CREATE (:R)-[:FOR]->(e:Entity)-[:CURRENT {date:localdatetime('1988-10-27T00:00:00')}]->(:State) RETURN e").single().get("e").asNode();
    }
}
