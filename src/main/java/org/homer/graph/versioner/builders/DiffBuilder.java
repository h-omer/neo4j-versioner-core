package org.homer.graph.versioner.builders;

import org.homer.graph.versioner.procedure.Diff;

import java.util.Optional;

/**
 * Created by alberto on 6/6/17.
 */
public class DiffBuilder {
    public Optional<Diff> build(){
        return Optional.of(new Diff());
    }
}
