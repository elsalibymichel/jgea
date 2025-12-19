/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.order.PartialComparator;
import java.util.Optional;
import java.util.function.Function;

public interface MultifidelityQualityBasedProblem<S, Q> extends QualityBasedProblem<S, Q> {
  static <S, Q> MultifidelityQualityBasedProblem<S, Q> of(
      PartialComparator<Q> qualityComparator,
      MultifidelityFunction<S, Q> qualityFunction,
      Function<S, Q> validationQualityFunction,
      S example,
      String name
  ) {
    record HardMultifidelityQualityBasedProblem<S, Q>(
        PartialComparator<Q> qualityComparator,
        MultifidelityFunction<S, Q> qualityFunction,
        Function<S, Q> validationQualityFunction,
        Optional<S> example,
        String name
    ) implements MultifidelityQualityBasedProblem<S, Q> {
      @Override
      public String toString() {
        return name();
      }

    }
    return new HardMultifidelityQualityBasedProblem<>(
        qualityComparator,
        qualityFunction,
        validationQualityFunction,
        Optional.ofNullable(example),
        name
    );
  }

  @Override
  MultifidelityFunction<S, Q> qualityFunction();

  interface MultifidelityFunction<T, R> extends Function<T, R> {
    @Override
    default <V> MultifidelityFunction<T, V> andThen(Function<? super R, ? extends V> after) {
      return (t, fidelity) -> after.apply(apply(t, fidelity));
    }

    @Override
    default R apply(T t) {
      return apply(t, 1d);
    }

    R apply(T t, double fidelity);

    @Override
    default <V> MultifidelityFunction<V, R> compose(Function<? super V, ? extends T> before) {
      return (v, fidelity) -> apply(before.apply(v), fidelity);
    }
  }

}