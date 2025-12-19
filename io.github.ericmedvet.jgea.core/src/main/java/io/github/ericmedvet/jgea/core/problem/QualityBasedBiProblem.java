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
import java.util.function.BiFunction;
import java.util.function.Function;

public interface QualityBasedBiProblem<S, O, Q> extends QualityBasedProblem<S, Q> {

  static <S, O, Q> QualityBasedBiProblem<S, O, Q> of(
      PartialComparator<Q> qualityComparator,
      BiFunction<S, S, O> outcomeFunction,
      Function<O, Q> firstQualityFunction,
      Function<O, Q> secondQualityFunction,
      S example,
      String name
  ) {
    record HardQualityBasedBiProblem<S, O, Q>(
        PartialComparator<Q> qualityComparator,
        BiFunction<S, S, O> outcomeFunction,
        Function<O, Q> firstQualityFunction,
        Function<O, Q> secondQualityFunction,
        Optional<S> example,
        String name
    ) implements QualityBasedBiProblem<S, O, Q> {

      @Override
      public String toString() {
        return name();
      }
    }
    return new HardQualityBasedBiProblem<>(
        qualityComparator,
        outcomeFunction,
        firstQualityFunction,
        secondQualityFunction,
        Optional.ofNullable(example),
        name
    );
  }

  Function<O, Q> firstQualityFunction();

  @Override
  default <T> QualityBasedBiProblem<T, O, Q> on(Function<T, S> function, T example) {
    return of(
        qualityComparator(),
        (t1, t2) -> outcomeFunction().apply(function.apply(t1), function.apply(t2)),
        firstQualityFunction(),
        secondQualityFunction(),
        example,
        "%s[on=%s]".formatted(this, function)
    );
  }

  BiFunction<S, S, O> outcomeFunction();

  @Override
  default Function<S, Q> qualityFunction() {
    return s -> firstQualityFunction().apply(outcomeFunction().apply(s, s));
  }

  Function<O, Q> secondQualityFunction();
}