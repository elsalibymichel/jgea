package io.github.ericmedvet.jgea.core.problem;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface QualityBasedBiProblem<S, O, Q> extends QualityBasedProblem<S, Q> {

    BiFunction<S, S, O> outcomeFunction();

    Function<O, Q> firstQualityFunction();

    Function<O, Q> secondQualityFunction();

    @Override
    default Function<S, Q> qualityFunction() {
        return s -> firstQualityFunction().apply(outcomeFunction().apply(s, s));
    }
}
