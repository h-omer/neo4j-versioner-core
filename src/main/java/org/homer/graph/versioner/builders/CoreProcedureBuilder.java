package org.homer.graph.versioner.builders;

import org.homer.graph.versioner.core.CoreProcedure;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.util.Optional;

/**
 * Created by alberto on 6/6/17.
 */
public abstract class CoreProcedureBuilder<T extends CoreProcedure> {

    private Class<T> clazz;

    private GraphDatabaseService db;
    private Log log;

    public CoreProcedureBuilder(Class<T> clazz) {
        this.clazz = clazz;
    }

    public CoreProcedureBuilder<T> withDb(GraphDatabaseService db) {
        this.db = db;
        return this;
    }

    public CoreProcedureBuilder<T> withLog(Log log) {
        this.log = log;
        return this;
    }

    public abstract Optional<T> build();

    Optional<T> instantiate() {
        T instance;
        try {
            instance = clazz.newInstance();
            instance.db = db;
            instance.log = log;
        } catch (InstantiationException | IllegalAccessException e) {
           instance = null;
           log.error(e.getMessage());
        }
        return Optional.ofNullable(instance);
    }

}
