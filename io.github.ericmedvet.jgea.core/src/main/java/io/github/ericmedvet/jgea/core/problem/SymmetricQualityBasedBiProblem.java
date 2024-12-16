package io.github.ericmedvet.jgea.core.problem;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface SymmetricQualityBasedBiProblem<S, O, Q> extends QualityBasedBiProblem<S, O, Q> {
  UnaryOperator<Q> symmetryFunction();

  @Override
  default Function<O, Q> secondQualityFunction() {
    return o -> symmetryFunction().apply(firstQualityFunction().apply(o));
  }
}