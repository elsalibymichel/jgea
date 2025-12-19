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

import io.github.ericmedvet.jgea.core.order.ParetoDominance;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.MultifidelityQualityBasedProblem.MultifidelityFunction;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.Function;

public interface BBMOProblem<S, B, BQ, O> extends MultiObjectiveProblem<S, BehaviorBasedProblem.Outcome<B, BQ>, O>, BehaviorBasedProblem<S, B, BQ> {
  static <S, B, BQ, O> BBMOProblem<S, B, BQ, O> of(
      SequencedMap<String, Objective<BQ, O>> behaviorQualityObjectives,
      Function<? super S, ? extends B> behaviorFunction,
      Function<? super B, ? extends BQ> behaviorQualityFunction,
      S example,
      String name
  ) {
    record HardBBMOProblem<S, B, BQ, O>(
        SequencedMap<String, Objective<BQ, O>> behaviorQualityObjectives,
        Function<? super S, ? extends B> behaviorFunction,
        Function<? super B, ? extends BQ> behaviorQualityFunction,
        Optional<S> example,
        String name
    ) implements BBMOProblem<S, B, BQ, O> {
      @Override
      public String toString() {
        return name();
      }
    }
    record HardMFBBMOProblem<S, B, BQ, O>(
        SequencedMap<String, Objective<BQ, O>> behaviorQualityObjectives,
        MultifidelityFunction<? super S, ? extends B> behaviorFunction,
        Function<? super B, ? extends BQ> behaviorQualityFunction,
        Optional<S> example,
        String name
    ) implements MFBBMOProblem<S, B, BQ, O> {
      @Override
      public String toString() {
        return name();
      }
    }
    return switch (behaviorFunction) {
      case MultifidelityFunction<? super S, ? extends B> mfBehaviorFunction -> new HardMFBBMOProblem<>(
          behaviorQualityObjectives,
          mfBehaviorFunction,
          behaviorQualityFunction,
          Optional.ofNullable(example),
          name
      );
      default -> new HardBBMOProblem<>(
          behaviorQualityObjectives,
          behaviorFunction,
          behaviorQualityFunction,
          Optional.ofNullable(example),
          name
      );
    };
  }

  @Override
  default PartialComparator<BQ> behaviorQualityComparator() {
    return new ParetoDominance<>(behaviorQualityObjectives().values().stream().map(Objective::comparator).toList())
        .comparing(
            bq -> behaviorQualityObjectives().values().stream().map(obj -> (O) obj.function().apply(bq)).toList()
        );
  }

  SequencedMap<String, Objective<BQ, O>> behaviorQualityObjectives();

  @Override
  default SequencedMap<String, Objective<Outcome<B, BQ>, O>> objectives() {
    return Misc.sequencedTransformValues(
        behaviorQualityObjectives(),
        obj -> new Objective<>(o -> obj.function().apply(o.behaviorQuality()), obj.comparator())
    );
  }

  @Override
  default <T> MultiObjectiveProblem<T, Outcome<B, BQ>, O> on(Function<T, S> function, T example) {
    return of(
        behaviorQualityObjectives(),
        behaviorFunction().compose(function),
        behaviorQualityFunction(),
        example,
        "%s[on=%s]".formatted(this, function)
    );
  }

  @Override
  default PartialComparator<Outcome<B, BQ>> qualityComparator() {
    return behaviorQualityComparator().comparing(Outcome::behaviorQuality);
  }

}