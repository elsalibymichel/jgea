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

import io.github.ericmedvet.jnb.datastructure.Utils;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Function;

public interface SimpleMOProblem<S, O> extends MultiObjectiveProblem<S, SequencedMap<String, O>, O> {

  static <S, O> SimpleMOProblem<S, O> of(
      SequencedMap<String, Comparator<O>> comparators,
      Function<S, SequencedMap<String, O>> qualityFunction,
      Function<S, SequencedMap<String, O>> validationQualityFunction,
      S example,
      String name
  ) {
    record HardSimpleMOProblem<S, O>(
        SequencedMap<String, Comparator<O>> comparators,
        Function<S, SequencedMap<String, O>> qualityFunction,
        Function<S, SequencedMap<String, O>> validationQualityFunction,
        Optional<S> example,
        String name
    ) implements SimpleMOProblem<S, O> {
      @Override
      public String toString() {
        return name();
      }
    }
    return new HardSimpleMOProblem<>(
        comparators,
        qualityFunction,
        validationQualityFunction,
        Optional.ofNullable(example),
        name
    );
  }

  SequencedMap<String, Comparator<O>> comparators();

  @Override
  default SequencedMap<String, Objective<SequencedMap<String, O>, O>> objectives() {
    return comparators().entrySet()
        .stream()
        .collect(
            Utils.toSequencedMap(
                Map.Entry::getKey,
                e -> new Objective<>(
                    map -> map.get(e.getKey()),
                    e.getValue()
                )
            )
        );
  }

  default SimpleMOProblem<S, O> toReducedSimpleMOProblem(Set<String> objectiveNames) {
    SequencedMap<String, Comparator<O>> reducedComparators = comparators().keySet()
        .stream()
        .filter(objectiveNames::contains)
        .collect(Utils.toSequencedMap(cn -> comparators().get(cn)));
    return of(reducedComparators, qualityFunction(), validationQualityFunction(), example().orElse(null), toString());
  }
}