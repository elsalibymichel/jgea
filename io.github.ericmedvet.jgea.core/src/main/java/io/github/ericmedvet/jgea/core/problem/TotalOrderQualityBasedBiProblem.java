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
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface TotalOrderQualityBasedBiProblem<S, O, Q> extends QualityBasedBiProblem<S, O, Q> {

  static <S, O, Q> TotalOrderQualityBasedBiProblem<S, O, Q> from(
      QualityBasedBiProblem<S, O, Q> qbBiProblem,
      Comparator<Q> comparator
  ) {
    return of(
        qbBiProblem.outcomeFunction(),
        qbBiProblem.firstQualityFunction(),
        qbBiProblem.secondQualityFunction(),
        comparator,
        qbBiProblem.example().orElse(null),
        qbBiProblem.toString()
    );
  }

  static <S, O, Q> TotalOrderQualityBasedBiProblem<S, O, Q> of(
      BiFunction<S, S, O> outcomeFunction,
      Function<O, Q> firstQualityFunction,
      Function<O, Q> secondQualityFunction,
      Comparator<Q> totalOrderComparator,
      S example,
      String name
  ) {
    record HardTotalOrderQualityBasedBiProblem<S, O, Q>(
        BiFunction<S, S, O> outcomeFunction,
        Function<O, Q> firstQualityFunction,
        Function<O, Q> secondQualityFunction,
        Comparator<Q> totalOrderComparator,
        Optional<S> example,
        String name
    ) implements TotalOrderQualityBasedBiProblem<S, O, Q> {
      @Override
      public String toString() {
        return name();
      }
    }
    return new HardTotalOrderQualityBasedBiProblem<>(
        outcomeFunction,
        firstQualityFunction,
        secondQualityFunction,
        totalOrderComparator,
        Optional.ofNullable(example),
        name
    );
  }

  @Override
  default <T> TotalOrderQualityBasedBiProblem<T, O, Q> on(Function<T, S> function, T example) {
    return of(
        (t1, t2) -> outcomeFunction().apply(function.apply(t1), function.apply(t2)),
        firstQualityFunction(),
        secondQualityFunction(),
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