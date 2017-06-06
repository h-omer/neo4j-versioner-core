package org.homer.graph.versioner.builders;

import org.homer.graph.versioner.procedure.Get;

import java.util.Optional;

/**
 * Created by alberto on 6/6/17.
 */
public class GetBuilder {
    public Optional<Get> build(){
        return Optional.of(new Get());
    }
}
