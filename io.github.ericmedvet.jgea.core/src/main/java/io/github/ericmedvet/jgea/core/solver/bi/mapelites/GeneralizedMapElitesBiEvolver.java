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
package io.github.ericmedvet.jgea.core.solver.bi.mapelites;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.QualityBasedBiProblem;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.SolverException;
import io.github.ericmedvet.jgea.core.solver.bi.AbstractBiEvolver;
import io.github.ericmedvet.jgea.core.solver.mapelites.Archive;
import io.github.ericmedvet.jgea.core.solver.mapelites.MEIndividual;
import io.github.ericmedvet.jgea.core.solver.mapelites.MEPopulationState;
import io.github.ericmedvet.jgea.core.solver.mapelites.MapElites;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class GeneralizedMapElitesBiEvolver<G, S, Q, O> extends AbstractBiEvolver<MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>>, QualityBasedBiProblem<S, O, Q>, MEIndividual<G, S, Q>, G, S, Q, O> {

  protected final int populationSize;
  private final Mutation<G> mutation;
  private final List<MapElites.Descriptor<G, S, Q>> descriptors;
  private final boolean emptyArchive;
  private final BiFunction<MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>>, MEIndividual<G, S, Q>, List<MEIndividual<G, S, Q>>> subsetOfOpponentsGetter;
  private final Function<List<Q>, Q> fitnessAggregator;

  public GeneralizedMapElitesBiEvolver(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      Predicate<? super MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>>> stopCondition,
      Mutation<G> mutation,
      int populationSize,
      List<MapElites.Descriptor<G, S, Q>> descriptors,
      BinaryOperator<Q> fitnessReducer,
      boolean emptyArchive,
      List<PartialComparator<? super MEIndividual<G, S, Q>>> additionalIndividualComparators,
      BiFunction<MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>>, MEIndividual<G, S, Q>, List<MEIndividual<G, S, Q>>> subsetOfOpponentsGetter,
      Function<List<Q>, Q> fitnessAggregator
  ) {
    super(solutionMapper, genotypeFactory, stopCondition, false, fitnessReducer, additionalIndividualComparators);
    this.populationSize = populationSize;
    this.mutation = mutation;
    this.descriptors = descriptors;
    this.emptyArchive = emptyArchive;
    this.subsetOfOpponentsGetter = subsetOfOpponentsGetter;
    this.fitnessAggregator = fitnessAggregator;
  }

  @Override
  public MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>> init(
      QualityBasedBiProblem<S, O, Q> problem,
      RandomGenerator random,
      ExecutorService executor
  ) throws SolverException {
    AtomicLong counter = new AtomicLong(0);
    List<? extends G> genotypes = genotypeFactory.build(populationSize, random);
    // Create individuals with initial quality = null
    List<MEIndividual<G, S, Q>> individuals = genotypes.stream()
        .map(g -> {
          S solution = solutionMapper.apply(g);
          return MEIndividual.from(
              Individual.of(counter.getAndIncrement(), g, solution, null, 0, 0, List.of()),
              descriptors
          );
        })
        .toList();
    MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>> state = MEPopulationState.empty(
        problem,
        stopCondition(),
        descriptors
    )
        .updatedWithIteration(
            populationSize,
            0,
            MEPopulationState.empty(problem, stopCondition(), descriptors)
                .archive()
                .updated(individuals, MEIndividual::bins, partialComparator(problem))
        );
    // For each solution, obtain its subset of opponents and then its fitness
    List<Future<MEIndividual<G, S, Q>>> futures = new ArrayList<>();
    for (MEIndividual<G, S, Q> individual : individuals) {
      futures.add(executor.submit(() -> evaluateIndividual(state, individual, state.nOfIterations())));
    }
    Collection<MEIndividual<G, S, Q>> evaluatedIndividuals = futures.stream()
        .map(future -> {
          try {
            return future.get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
    Archive<MEIndividual<G, S, Q>> updatedArchive = state.archive()
        .updated(evaluatedIndividuals, MEIndividual::bins, partialComparator(problem));
    return state.updatedWithIteration(populationSize, futures.size(), updatedArchive);
  }

  @Override
  public MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>> update(
      RandomGenerator random,
      ExecutorService executor,
      MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>> state
  ) throws SolverException {
    List<MEIndividual<G, S, Q>> individuals = new ArrayList<>(state.archive().asMap().values());
    AtomicLong counter = new AtomicLong(state.nOfBirths());
    List<ChildGenotype<G>> newChildGenotypes = IntStream.range(0, populationSize)
        .mapToObj(j -> Misc.pickRandomly(individuals, random))
        .map(
            p -> new ChildGenotype<>(
                counter.getAndIncrement(),
                mutation.mutate(p.genotype(), random),
                List.of(p.id())
            )
        )
        .toList();
    individuals.addAll(
        newChildGenotypes.stream()
            .map(
                g -> MEIndividual.from(
                    Individual.from(g, solutionMapper, s -> null, state.nOfQualityEvaluations()),
                    descriptors
                )
            )
            .toList()
    );
    List<Future<MEIndividual<G, S, Q>>> futures = new ArrayList<>();
    for (MEIndividual<G, S, Q> individual : individuals) {
      futures.add(executor.submit(() -> evaluateIndividual(state, individual, state.nOfIterations())));
    }
    Collection<MEIndividual<G, S, Q>> newAndOldPopulation = futures.stream()
        .map(future -> {
          try {
            return future.get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
    PartialComparator<? super Individual<?, ?, ?>> updaterComparator = (
        newI,
        existingI
    ) -> newI == existingI ? PartialComparator.PartialComparatorOutcome.BEFORE : PartialComparator.PartialComparatorOutcome.AFTER;
    Archive<MEIndividual<G, S, Q>> archive;
    if (emptyArchive) {
      archive = new Archive<>(state.archive().binUpperBounds());
    } else {
      archive = state.archive().updated(newAndOldPopulation, MEIndividual::bins, updaterComparator);
    }
    archive = archive.updated(newAndOldPopulation, MEIndividual::bins, partialComparator(state.problem()));
    return state.updatedWithIteration(populationSize, futures.size(), archive);
  }

  private MEIndividual<G, S, Q> evaluateIndividual(
      MEPopulationState<G, S, Q, QualityBasedBiProblem<S, O, Q>> state,
      MEIndividual<G, S, Q> individual,
      long iteration
  ) {
    List<MEIndividual<G, S, Q>> opponents = subsetOfOpponentsGetter.apply(state, individual);
    if (opponents.isEmpty()) {
      return individual.updateQuality(individual.quality(), iteration);
    } else {
      List<Q> qualityList = new ArrayList<>();
      for (MEIndividual<G, S, Q> opponent : opponents) {
        O outcome = state.problem().outcomeFunction().apply(individual.solution(), opponent.solution());
        Q computedQuality = state.problem().firstQualityFunction().apply(outcome);
        qualityList.add(computedQuality);
      }
      return individual.updateQuality(fitnessAggregator.apply(qualityList), iteration);
    }
  }
}
