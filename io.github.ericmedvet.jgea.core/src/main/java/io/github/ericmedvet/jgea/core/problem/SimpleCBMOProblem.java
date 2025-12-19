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

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface SimpleCBMOProblem<S, C, CQ, O> extends CBMOProblem<S, C, CQ, SequencedMap<String, O>, O>, SimpleMOProblem<S, O> {

  static <S, C, CQ, O> SimpleCBMOProblem<S, C, CQ, O> of(
      SequencedMap<String, Objective<List<CQ>, O>> aggregateObjectives,
      BiFunction<S, C, CQ> caseFunction,
      IndexedProvider<C> caseProvider,
      IndexedProvider<C> validationCaseProvider,
      S example,
      String name
  ) {
    record HardSimpleCBMOProblem<S, C, CQ, O>(
        SequencedMap<String, Objective<List<CQ>, O>> aggregateObjectives,
        BiFunction<S, C, CQ> caseFunction,
        IndexedProvider<C> caseProvider,
        IndexedProvider<C> validationCaseProvider,
        Optional<S> example,
        String name
    ) implements SimpleCBMOProblem<S, C, CQ, O> {

      @Override
      public String toString() {
        return name();
      }
    }
    return new HardSimpleCBMOProblem<>(
        aggregateObjectives,
        caseFunction,
        caseProvider,
        validationCaseProvider,
        Optional.ofNullable(example),
        name
    );
  }

  @Override
  default Function<List<CQ>, SequencedMap<String, O>> aggregateFunction() {
    return NamedFunction.from(
        cqs -> Misc.sequencedTransformValues(
            aggregateObjectives(),
            obj -> obj.function().apply(cqs)
        ),
        "(%s)".formatted(
            aggregateObjectives().values()
                .stream()
                .map(obj -> obj.function().toString())
                .collect(
                    Collectors.joining(",")
                )
        )
    );
  }

  SequencedMap<String, Objective<List<CQ>, O>> aggregateObjectives();

  @Override
  default SequencedMap<String, Comparator<O>> comparators() {
    return Misc.sequencedTransformValues(aggregateObjectives(), Objective::comparator);
  }

}