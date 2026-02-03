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
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public interface TotalOrderQualityBasedProblem<S, Q> extends QualityBasedProblem<S, Q> {

  static <S, Q> TotalOrderQualityBasedProblem<S, Q> from(
      QualityBasedProblem<S, Q> qbProblem,
      Comparator<Q> comparator
  ) {
    return of(
        qbProblem.qualityFunction(),
        qbProblem.validationQualityFunction(),
        comparator,
        qbProblem.example().orElse(null),
        qbProblem.toString()
    );
  }

  static <S, Q> TotalOrderQualityBasedProblem<S, Q> of(
      Function<S, Q> qualityFunction,
      Function<S, Q> validationQualityFunction,
      Comparator<Q> totalOrderComparator,
      S example,
      String name
  ) {
    if (qualityFunction instanceof MultifidelityQualityBasedProblem.MultifidelityFunction<S, Q> multifidelityQualityFunction) {
      record HardTOMFQProblem<S, Q>(
          MultifidelityFunction<S, Q> qualityFunction,
          Function<S, Q> validationQualityFunction,
          Comparator<Q> totalOrderComparator,
          Optional<S> example,
          String name
      ) implements TotalOrderQualityBasedProblem<S, Q>, MultifidelityQualityBasedProblem<S, Q> {

        @Override
        public String toString() {
          return name();
        }
      }
      return new HardTOMFQProblem<>(
          multifidelityQualityFunction,
          validationQualityFunction,
          totalOrderComparator,
          Optional.ofNullable(example),
          name
      );
    }
    record HardTOQProblem<S, Q>(
        Function<S, Q> qualityFunction,
        Function<S, Q> validationQualityFunction,
        Comparator<Q> totalOrderComparator,
        Optional<S> example,
        String name
    ) implements TotalOrderQualityBasedProblem<S, Q> {

      @Override
      public String toString() {
        return name();
      }

    }
    return new HardTOQProblem<>(
        qualityFunction,
        validationQualityFunction,
        totalOrderComparator,
        Optional.ofNullable(example),
        name
    );
  }

  @Override
  default <T> TotalOrderQualityBasedProblem<T, Q> on(Function<T, S> function, T example) {
    return of(
        qualityFunction().compose(function),
        validationQualityFunction().compose(function),
        totalOrderComparator(),
        example,
        "%s[on=%s]".formatted(this, function)
    );
  }

  @Override
  default PartialComparator<Q> qualityComparator() {
    return PartialComparator.from(totalOrderComparator());
  }

  Comparator<Q> totalOrderComparator();
}
