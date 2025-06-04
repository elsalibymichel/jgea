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
package io.github.ericmedvet.jgea.core;

import io.github.ericmedvet.jgea.core.operator.GeneticOperator;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.selector.Selector;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.POCPopulationState;
import io.github.ericmedvet.jgea.core.solver.SolverException;
import io.github.ericmedvet.jgea.core.solver.StandardEvolver;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

public class ElitistGeneticAlgorithm<G, S, Q> extends StandardEvolver<G, S, Q> {
  private static final Logger L = Logger.getLogger(ElitistGeneticAlgorithm.class.getName());

  private final double eliteRate;

  public ElitistGeneticAlgorithm(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      int populationSize,
      Predicate<? super POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>>> stopCondition,
      Map<GeneticOperator<G>, Double> operators,
      Selector<? super Individual<G, S, Q>> parentSelector,
      double eliteRate,
      int maxUniquenessAttempts,
      boolean remap,
      List<PartialComparator<? super Individual<G, S, Q>>> additionalIndividualComparators
  ) {
    super(
        solutionMapper,
        genotypeFactory,
        populationSize,
        stopCondition,
        operators,
        parentSelector,
        null,
        populationSize - (int) Math.ceil((double) populationSize * eliteRate),
        false,
        maxUniquenessAttempts,
        remap,
        additionalIndividualComparators
    );
    this.eliteRate = eliteRate;
  }

  @Override
  public POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> update(
      RandomGenerator random,
      ExecutorService executor,
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> state
  ) throws SolverException {
    Collection<ChildGenotype<G>> offspringChildGenotypes = buildOffspringToMapGenotypes(state, random);
    int nOfNewBirths = offspringChildGenotypes.size();
    L.fine(String.format("Offspring built: %d genotypes", nOfNewBirths));
    // build elite
    List<Individual<G, S, Q>> elite = state.pocPopulation()
        .fronts()
        .stream()
        .flatMap(Collection::stream)
        .limit((long) Math.ceil((double) populationSize * eliteRate))
        .toList();
    L.fine(String.format("Elite built: %d individuals", elite.size()));
    Collection<Individual<G, S, Q>> newPopulation = mapAll(
        offspringChildGenotypes,
        this::mapChildGenotype,
        elite,
        this::remapIndividual,
        state,
        random,
        executor
    );
    L.fine(String.format("Offspring merged with parents: %d individuals", newPopulation.size()));
    newPopulation = trimPopulation(newPopulation, state, random);
    L.fine(String.format("Offspring trimmed: %d individuals", newPopulation.size()));
    return update(
        state,
        newPopulation,
        nOfNewBirths,
        nOfNewBirths + (remap ? state.pocPopulation().size() : 0)
    );
  }
}
