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
package io.github.ericmedvet.jgea.core.solver.multifidelity;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.operator.GeneticOperator;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.order.PartiallyOrderedCollection;
import io.github.ericmedvet.jgea.core.problem.MultifidelityQualityBasedProblem;
import io.github.ericmedvet.jgea.core.selector.Selector;
import io.github.ericmedvet.jgea.core.solver.AbstractStandardEvolver;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.MultiFidelityPOCPopulationState;
import io.github.ericmedvet.jgea.core.solver.ProgressBasedStopCondition;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public class ScheduledFidelityStandardEvolver<G, S, Q> extends AbstractStandardEvolver<MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>>, MultifidelityQualityBasedProblem<S, Q>, Individual<G, S, Q>, G, S, Q> {

  private final DoubleUnaryOperator schedule;

  public ScheduledFidelityStandardEvolver(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      int populationSize,
      ProgressBasedStopCondition<? super MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>>> stopCondition,
      Map<GeneticOperator<G>, Double> operators,
      Selector<? super Individual<G, S, Q>> parentSelector,
      Selector<? super Individual<G, S, Q>> unsurvivalSelector,
      int offspringSize,
      boolean overlapping,
      int maxUniquenessAttempts,
      List<PartialComparator<? super Individual<G, S, Q>>> additionalIndividualComparators,
      DoubleUnaryOperator schedule
  ) {
    super(
        solutionMapper,
        genotypeFactory,
        populationSize,
        stopCondition,
        operators,
        parentSelector,
        unsurvivalSelector,
        offspringSize,
        overlapping,
        maxUniquenessAttempts,
        true,
        additionalIndividualComparators
    );
    this.schedule = schedule;
  }

  @Override
  protected MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>> init(
      MultifidelityQualityBasedProblem<S, Q> problem
  ) {
    return MultiFidelityPOCPopulationState.empty(problem, stopCondition());
  }

  @Override
  protected Individual<G, S, Q> mapChildGenotype(
      ChildGenotype<G> childGenotype,
      MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state,
      RandomGenerator random
  ) {
    double fidelity = currentFidelity(state);
    S solution = solutionMapper.apply(childGenotype.genotype());
    Q quality = state.problem().qualityFunction().apply(solution, fidelity);
    return Individual.of(
        childGenotype.id(),
        childGenotype.genotype(),
        solution,
        quality,
        state.nOfIterations(),
        state.nOfIterations(),
        childGenotype.parentIds()
    );
  }

  @Override
  protected Individual<G, S, Q> remapIndividual(
      Individual<G, S, Q> individual,
      MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state,
      RandomGenerator random
  ) {
    double fidelity = currentFidelity(state);
    Q quality = state.problem().qualityFunction().apply(individual.solution(), fidelity);
    return individual.updateQuality(quality, state.nOfIterations());
  }

  private double currentFidelity(
      MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state
  ) {
    double progress = ((ProgressBasedStopCondition<? super MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>>>) stopCondition())
        .progress(
            state
        )
        .rate();
    if (Double.isNaN(progress)) {
      progress = 0;
    }
    return Math.clamp(schedule.applyAsDouble(progress), 0d, 1d);
  }

  @Override
  protected MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>> update(
      MultiFidelityPOCPopulationState<Individual<G, S, Q>, G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state,
      Collection<Individual<G, S, Q>> individuals,
      long nOfNewBirths,
      long nOfNewFitnessEvaluations
  ) {
    return state.updatedWithIteration(
        nOfNewBirths,
        nOfNewFitnessEvaluations,
        Math.max(1d, currentFidelity(state) * nOfNewFitnessEvaluations),
        PartiallyOrderedCollection.from(individuals, partialComparator(state.problem()))
    );
  }
}
