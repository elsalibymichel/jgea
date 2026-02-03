/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2026 Eric Medvet
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

public interface QualityBasedProblem<S, Q> extends Problem<S>, Function<S, Q> {

  @Override
  default Q apply(S s) {
    return qualityFunction().apply(s);
  }

  @Override
  default PartialComparatorOutcome compare(S s1, S s2) {
    Function<S, Q> qualityFunction = qualityFunction();
    return qualityComparator()
        .compare(qualityFunction.apply(s1), qualityFunction.apply(s2));
  }

  static <S, Q> QualityBasedProblem<S, Q> of(
      PartialComparator<Q> qualityComparator,
      Function<S, Q> qualityFunction,
      Function<S, Q> validationQualityFunction,
      S example,
      String name
  ) {
    record HardQualityBasedProblem<S, Q>(
        PartialComparator<Q> qualityComparator,
        Function<S, Q> qualityFunction,
        Function<S, Q> validationQualityFunction,
        Optional<S> example,
        String name
    ) implements QualityBasedProblem<S, Q> {
      @Override
      public String toString() {
        return name();
      }

    }
    return new HardQualityBasedProblem<>(
        qualityComparator,
        qualityFunction,
        validationQualityFunction,
        Optional.ofNullable(example),
        name
    );
  }

  @Override
  default <T> QualityBasedProblem<T, Q> on(Function<T, S> function, T example) {
    return of(
        qualityComparator(),
        qualityFunction().compose(function),
        validationQualityFunction().compose(function),
        example,
        "%s[on=%s]".formatted(this, function)
    );
  }

  PartialComparator<Q> qualityComparator();

  Function<S, Q> qualityFunction();

  default Function<S, Q> validationQualityFunction() {
    return qualityFunction();
  }
}
