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
package io.github.ericmedvet.jgea.core.solver.bi;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.operator.GeneticOperator;
import io.github.ericmedvet.jgea.core.order.DAGPartiallyOrderedCollection;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.order.PartiallyOrderedCollection;
import io.github.ericmedvet.jgea.core.problem.QualityBasedBiProblem;
import io.github.ericmedvet.jgea.core.selector.Selector;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.POCPopulationState;
import io.github.ericmedvet.jgea.core.solver.SolverException;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.Pair;

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

public class StandardBiEvolver<G, S, Q, O> extends AbstractBiEvolver<POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>>, QualityBasedBiProblem<S, O, Q>, Individual<G, S, Q>, G, S, Q, O> {

  protected final Map<GeneticOperator<G>, Double> operators;
  protected final Selector<? super Individual<G, S, Q>> parentSelector;
  protected final Selector<? super Individual<G, S, Q>> unsurvivalSelector;
  protected final int populationSize;
  protected final int offspringSize;
  protected final boolean overlapping;
  protected final int maxUniquenessAttempts;

  public StandardBiEvolver(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      int populationSize,
      Predicate<? super POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>>> stopCondition,
      Map<GeneticOperator<G>, Double> operators,
      Selector<? super Individual<G, S, Q>> parentSelector,
      Selector<? super Individual<G, S, Q>> unsurvivalSelector,
      int offspringSize,
      boolean overlapping,
      int maxUniquenessAttempts,
      BinaryOperator<Q> fitnessReducer,
      List<PartialComparator<? super Individual<G, S, Q>>> additionalIndividualComparators,
      OpponentsSelector<Individual<G, S, Q>, S, Q, O> opponentsSelector,
      Function<List<Q>, Q> fitnessAggregator
  ) {
    super(solutionMapper, genotypeFactory, stopCondition, false, fitnessReducer, additionalIndividualComparators, opponentsSelector, fitnessAggregator);
    this.operators = operators;
    this.parentSelector = parentSelector;
    this.unsurvivalSelector = unsurvivalSelector;
    this.populationSize = populationSize;
    this.offspringSize = offspringSize;
    this.overlapping = overlapping;
    this.maxUniquenessAttempts = maxUniquenessAttempts;
  }

  protected Collection<ChildGenotype<G>> buildOffspringToMapGenotypes(
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>> state,
      RandomGenerator random
  ) {
    AtomicLong counter = new AtomicLong(state.nOfBirths());
    Collection<ChildGenotype<G>> offspringChildGenotypes = new ArrayList<>();
    Set<G> uniqueOffspringGenotypes = new HashSet<>();
    if (maxUniquenessAttempts > 0) {
      uniqueOffspringGenotypes.addAll(
          state.pocPopulation()
              .all()
              .stream()
              .map(Individual::genotype)
              .toList()
      );
    }
    int attempts = 0;
    while (offspringChildGenotypes.size() < offspringSize) {
      GeneticOperator<G> operator = Misc.pickRandomly(operators, random);
      List<Individual<G, S, Q>> parents = new ArrayList<>(operator.arity());
      for (int j = 0; j < operator.arity(); j++) {
        Individual<G, S, Q> parent = parentSelector.select(state.pocPopulation(), random);
        parents.add(parent);
      }
      List<? extends G> childGenotypes = operator.apply(parents.stream().map(Individual::genotype).toList(), random);
      if (attempts >= maxUniquenessAttempts || childGenotypes.stream().noneMatch(uniqueOffspringGenotypes::contains)) {
        attempts = 0;
        List<Long> parentIds = parents.stream().map(Individual::id).toList();
        childGenotypes.stream()
            .map(g -> new ChildGenotype<G>(counter.getAndIncrement(), g, parentIds))
            .forEach(offspringChildGenotypes::add);
        uniqueOffspringGenotypes.addAll(childGenotypes);
      } else {
        attempts = attempts + 1;
      }
    }
    return offspringChildGenotypes;
  }
  
  @Override
  public POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>> init(
      QualityBasedBiProblem<S, O, Q> problem,
      RandomGenerator random,
      ExecutorService executor
  ) throws SolverException {
    // create initial genotypes and map to individuals with null quality
    AtomicLong counter = new AtomicLong(0);
    Collection<? extends G> genotypes = genotypeFactory.build(populationSize, random);
    Collection<Individual<G, S, Q>> individuals = genotypes.stream()
        .map(
            g -> Individual.<G,S,Q>of(
            counter.getAndIncrement(),
            g,
            solutionMapper.apply(g),
            null,
            0,
            0,
            List.of()
        ))
        .toList();
    // determine matches
    Map<Pair<Long, Long>, Pair<Individual<G, S, Q>, Individual<G, S, Q>>> matches = new HashMap<>();
    BiFunction<Individual<G, S, Q>, Individual<G, S, Q>, Pair<Long, Long>> individualsToIdPair =
        (i1, i2) -> new Pair<>(Math.min(i1.id(), i2.id()), Math.max(i1.id(), i2.id()));
    for (Individual<G, S, Q> individual : individuals) {
      List<Individual<G, S, Q>> opponents = opponentsSelector.select(individuals, individual, problem, random);
      for (Individual<G, S, Q> opponent : opponents) {
        matches.putIfAbsent(individualsToIdPair.apply(individual, opponent), new Pair<>(individual, opponent));
      }
    }
    // execute matches
    List<Future<MatchOutcome<Q>>> futures = new ArrayList<>();
    matches.values().forEach(matchPair ->
        futures.add(executor.submit(() -> {
          Individual<G, S, Q> i1 = matchPair.first();
          Individual<G, S, Q> i2 = matchPair.second();
          O outcome = problem.outcomeFunction().apply(i1.solution(), i2.solution());
          return new MatchOutcome<>(
              i1.id(),
              i2.id(),
              problem.firstQualityFunction().apply(outcome),
              problem.secondQualityFunction().apply(outcome)
          );
        }))
    );
    Collection<MatchOutcome<Q>> matchesOutcomes = futures.stream()
        .map(f -> {
          try {
            return f.get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
    // aggregate fitness
    Map<Long, List<Q>> idsFitnessMap = new HashMap<>();
    matchesOutcomes.forEach(fo -> {
      idsFitnessMap.computeIfAbsent(fo.id1(), k -> new ArrayList<>()).add(fo.f1());
      idsFitnessMap.computeIfAbsent(fo.id2(), k -> new ArrayList<>()).add(fo.f2());
    });
    // update individuals with aggregated quality
    Collection<Individual<G, S, Q>> updatedIndividuals = individuals.stream()
        .map(i -> i.updateQuality(fitnessAggregator.apply(idsFitnessMap.getOrDefault(i.id(), List.of())), 0))
        .toList();
    // build state
    POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>> state =
        POCPopulationState.empty(problem, stopCondition());
    return state.updatedWithIteration(
        populationSize,
        futures.size(),
        PartiallyOrderedCollection.from(updatedIndividuals, partialComparator(problem))
    );
  }
  
  @Override
  public POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>> update(
      RandomGenerator random,
      ExecutorService executor,
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>> state
  ) throws SolverException {
    // create offspring and build extended population
    Collection<ChildGenotype<G>> offspringChildGenotypes = buildOffspringToMapGenotypes(state, random);
    Collection<Individual<G, S, Q>> offspring = offspringChildGenotypes.stream()
        .map(g -> Individual.<G, S, Q>from(g, solutionMapper, s -> null, state.nOfIterations()))
        .toList();
    Collection<Individual<G, S, Q>> individuals = new ArrayList<>();
    individuals.addAll(state.pocPopulation().all());
    individuals.addAll(offspring);
    // determine matches
    Map<Pair<Long, Long>, Pair<Individual<G, S, Q>, Individual<G, S, Q>>> matches = new HashMap<>();
    BiFunction<Individual<G, S, Q>, Individual<G, S, Q>, Pair<Long, Long>> individualsToIdPair =
        (i1, i2) -> new Pair<>(Math.min(i1.id(), i2.id()), Math.max(i1.id(), i2.id()));
    for (Individual<G, S, Q> individual : individuals) {
      List<Individual<G, S, Q>> opponents = opponentsSelector.select(individuals, individual, state.problem(), random);
      for (Individual<G, S, Q> opponent : opponents) {
        matches.putIfAbsent(individualsToIdPair.apply(individual, opponent), new Pair<>(individual, opponent));
      }
    }
    // execute matches
    List<Future<MatchOutcome<Q>>> futures = new ArrayList<>();
    matches.values().forEach(matchPair ->
        futures.add(executor.submit(() -> {
          Individual<G, S, Q> i1 = matchPair.first();
          Individual<G, S, Q> i2 = matchPair.second();
          O outcome = state.problem().outcomeFunction().apply(i1.solution(), i2.solution());
          return new MatchOutcome<>(
              i1.id(),
              i2.id(),
              state.problem().firstQualityFunction().apply(outcome),
              state.problem().secondQualityFunction().apply(outcome)
          );
        }))
    );
    Collection<MatchOutcome<Q>> matchesOutcomes = futures.stream()
        .map(f -> {
          try {
            return f.get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
    // aggregate fitness
    Map<Long, List<Q>> idsFitnessMap = new HashMap<>();
    matchesOutcomes.forEach(fo -> {
      idsFitnessMap.computeIfAbsent(fo.id1(), k -> new ArrayList<>()).add(fo.f1());
      idsFitnessMap.computeIfAbsent(fo.id2(), k -> new ArrayList<>()).add(fo.f2());
    });
    // update individuals with aggregated quality
    Collection<Individual<G, S, Q>> updatedIndividuals = individuals.stream()
        .map(i -> {
          List<Q> qualities = idsFitnessMap.getOrDefault(i.id(), List.of());
          if (qualities.isEmpty()) {
            return i; // Individual did not participate in any match
          }
          Q aggregatedQuality = fitnessAggregator.apply(qualities);
          Q finalQuality = i.quality() != null ?
              fitnessReducer.apply(i.quality(), aggregatedQuality) :
              aggregatedQuality;
          return i.updateQuality(finalQuality, state.nOfIterations());
        })
        .toList();
    // trim population and update state
    Collection<Individual<G, S, Q>> newPopulation = trimPopulation(updatedIndividuals, state, random);
    return state.updatedWithIteration(
        offspring.size(),
        futures.size(),
        PartiallyOrderedCollection.from(newPopulation, partialComparator(state.problem()))
    );
  }

  protected Collection<Individual<G, S, Q>> trimPopulation(
      Collection<Individual<G, S, Q>> population,
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedBiProblem<S, O, Q>> state,
      RandomGenerator random
  ) {
    PartiallyOrderedCollection<Individual<G, S, Q>> orderedPopulation = new DAGPartiallyOrderedCollection<>(
        population,
        partialComparator(state.problem())
    );
    while (orderedPopulation.size() > populationSize) {
      Individual<G, S, Q> toRemoveIndividual = unsurvivalSelector.select(orderedPopulation, random);
      orderedPopulation.remove(toRemoveIndividual);
    }
    return orderedPopulation.all();
  }
}
