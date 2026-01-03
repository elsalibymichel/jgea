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

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface CBTOProblem<S, C, CQ, Q> extends CaseBasedProblem<S, C, CQ, Q>, TotalOrderQualityBasedProblem<S, Q> {

  static <S, C, CQ, Q> CBTOProblem<S, C, CQ, Q> of(
      Function<List<CQ>, Q> aggregateFunction,
      BiFunction<S, C, CQ> caseFunction,
      IndexedProvider<C> caseProvider,
      IndexedProvider<C> validationCaseProvider,
      Comparator<Q> totalOrderComparator,
      S example,
      String name
  ) {
    record HardCBTOProblem<S, C, CQ, Q>(
        Function<List<CQ>, Q> aggregateFunction,
        BiFunction<S, C, CQ> caseFunction,
        IndexedProvider<C> caseProvider,
        IndexedProvider<C> validationCaseProvider,
        Comparator<Q> totalOrderComparator,
        Optional<S> example,
        String name
    ) implements CBTOProblem<S, C, CQ, Q> {

      @Override
      public String toString() {
        return name();
      }
    }
    return new HardCBTOProblem<>(
        aggregateFunction,
        caseFunction,
        caseProvider,
        validationCaseProvider,
        totalOrderComparator,
        Optional.ofNullable(example),
        name
    );
  }

  @Override
  default <T> CBTOProblem<T, C, CQ, Q> on(Function<T, S> function, T example) {
    return of(
        aggregateFunction(),
        (t, c) -> caseFunction().apply(function.apply(t), c),
        caseProvider(),
        validationCaseProvider(),
        totalOrderComparator(),
        example,
        "%s[on=%s]".formatted(this, function)
    );
  }
}
