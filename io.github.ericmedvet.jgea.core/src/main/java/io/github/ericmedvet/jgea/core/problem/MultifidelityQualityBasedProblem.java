package io.github.ericmedvet.jgea.core.problem;

import java.util.function.Function;

public interface MultifidelityQualityBasedProblem<S, Q> extends QualityBasedProblem<S, Q> {
  interface MultifidelityFunction<T, R> extends Function<T, R> {
    R apply(T t, double fidelity);

    @Override
    default R apply(T t) {
      return apply(t, 1d);
    }

    @Override
    default <V> MultifidelityFunction<V, R> compose(Function<? super V, ? extends T> before) {
      return (v, fidelity) -> apply(before.apply(v), fidelity);
    }

    @Override
    default <V> MultifidelityFunction<T, V> andThen(Function<? super R, ? extends V> after) {
      return (t, fidelity) -> after.apply(apply(t, fidelity));
    }
  }

  @Override
  MultifidelityFunction<S, Q> qualityFunction();
}
