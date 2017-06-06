package org.homer.graph.versioner.core;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;

/**
 * Created by alberto on 6/6/17.
 */
public abstract class CoreProcedure {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

}
