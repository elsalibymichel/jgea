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
package io.github.ericmedvet.jgea.core.solver;

import io.github.ericmedvet.jgea.core.order.PartiallyOrderedCollection;
import io.github.ericmedvet.jgea.core.problem.MultifidelityQualityBasedProblem;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

public interface MultiFidelityPOCPopulationState<I extends Individual<G, S, Q>, G, S, Q, P extends MultifidelityQualityBasedProblem<S, Q>> extends POCPopulationState<I, G, S, Q, P> {
  double cumulativeFidelity();

  static <I extends Individual<G, S, Q>, G, S, Q, P extends MultifidelityQualityBasedProblem<S, Q>> MultiFidelityPOCPopulationState<I, G, S, Q, P> of(
      LocalDateTime startingDateTime,
      long elapsedMillis,
      long nOfIterations,
      P problem,
      Predicate<State<?, ?>> stopCondition,
      long nOfBirths,
      long nOfQualityEvaluations,
      double cumulativeFidelity,
      PartiallyOrderedCollection<I> pocPopulation
  ) {
    record HardState<I extends Individual<G, S, Q>, G, S, Q, P extends MultifidelityQualityBasedProblem<S, Q>>(
        LocalDateTime startingDateTime,
        long elapsedMillis,
        long nOfIterations,
        P problem,
        Predicate<State<?, ?>> stopCondition,
        long nOfBirths,
        long nOfQualityEvaluations,
        double cumulativeFidelity,
        PartiallyOrderedCollection<I> pocPopulation
    ) implements MultiFidelityPOCPopulationState<I, G, S, Q, P> {}
    return new HardState<>(
        startingDateTime,
        elapsedMillis,
        nOfIterations,
        problem,
        stopCondition,
        nOfBirths,
        nOfQualityEvaluations,
        cumulativeFidelity,
        pocPopulation
    );
  }

  static <I extends Individual<G, S, Q>, G, S, Q, P extends MultifidelityQualityBasedProblem<S, Q>> MultiFidelityPOCPopulationState<I, G, S, Q, P> empty(
      P problem,
      Predicate<State<?, ?>> stopCondition
  ) {
    return of(LocalDateTime.now(), 0, 0, problem, stopCondition, 0, 0, 0d, PartiallyOrderedCollection.from());
  }

  default MultiFidelityPOCPopulationState<I, G, S, Q, P> updatedWithIteration(
      long nOfNewBirths,
      long nOfNewQualityEvaluations,
      double cumulativeNewFidelity,
      PartiallyOrderedCollection<I> pocPopulation
  ) {
    return of(
        startingDateTime(),
        ChronoUnit.MILLIS.between(startingDateTime(), LocalDateTime.now()),
        nOfIterations() + 1,
        problem(),
        stopCondition(),
        nOfBirths() + nOfNewBirths,
        nOfQualityEvaluations() + nOfNewQualityEvaluations,
        cumulativeFidelity() + cumulativeNewFidelity,
        pocPopulation
    );
  }

  @Override
  default MultiFidelityPOCPopulationState<I, G, S, Q, P> updatedWithProblem(P problem) {
    return of(
        startingDateTime(),
        elapsedMillis(),
        nOfIterations(),
        problem,
        stopCondition(),
        nOfBirths(),
        nOfQualityEvaluations(),
        cumulativeFidelity(),
        pocPopulation()
    );
  }

}
