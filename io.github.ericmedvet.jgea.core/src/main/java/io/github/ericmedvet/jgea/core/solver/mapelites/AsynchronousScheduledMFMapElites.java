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
package io.github.ericmedvet.jgea.core.solver.mapelites;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.listener.Listener;
import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.MultifidelityQualityBasedProblem;
import io.github.ericmedvet.jgea.core.solver.AbstractPopulationBasedIterativeSolver;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.ProgressBasedStopCondition;
import io.github.ericmedvet.jgea.core.solver.SolverException;
import io.github.ericmedvet.jgea.core.solver.mapelites.MapElites.Descriptor;
import io.github.ericmedvet.jgea.core.solver.mapelites.MapElites.Descriptor.Coordinate;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class AsynchronousScheduledMFMapElites<G, S, Q> extends AbstractPopulationBasedIterativeSolver<MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>, MultifidelityQualityBasedProblem<S, Q>, MEIndividual<G, S, Q>, G, S, Q> {

  private final Mutation<G> mutation;
  private final List<Descriptor<G, S, Q>> descriptors;
  private final DoubleUnaryOperator schedule;
  private final int nOfBirthsForIteration;

  public AsynchronousScheduledMFMapElites(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      ProgressBasedStopCondition<? super MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>> stopCondition,
      List<PartialComparator<? super MEIndividual<G, S, Q>>> additionalIndividualComparators,
      Mutation<G> mutation,
      List<Descriptor<G, S, Q>> descriptors,
      DoubleUnaryOperator schedule,
      int nOfBirthsForIteration
  ) {
    super(solutionMapper, genotypeFactory, stopCondition, true, additionalIndividualComparators);
    this.mutation = mutation;
    this.descriptors = descriptors;
    this.schedule = schedule;
    this.nOfBirthsForIteration = nOfBirthsForIteration;
  }

  private MEIndividual<G, S, Q> buildIndividual(
      long id,
      Collection<Long> parentIds,
      G genotype,
      S solution,
      List<Coordinate> coordinates,
      MultifidelityQualityBasedProblem.MultifidelityFunction<S, Q> qualityFunction,
      long nOfIterations,
      Map<List<Integer>, Long> nOfEvaluationsMap,
      Map<List<Integer>, Double> currentFidelityMap,
      Map<List<Integer>, Double> cumulativeFidelityMap
  ) {
    List<Integer> bins = coordinates.stream().map(Coordinate::bin).toList();
    double currentFidelity = currentFidelityMap.get(bins);
    Q quality = qualityFunction.apply(solution, currentFidelity);
    nOfEvaluationsMap.compute(bins, (k, v) -> Objects.requireNonNullElse(v, 0L) + 1);
    cumulativeFidelityMap.put(bins, cumulativeFidelityMap.getOrDefault(bins, 0d) + currentFidelity);
    return MEIndividual.of(
        id,
        genotype,
        solution,
        quality,
        nOfIterations,
        nOfIterations,
        parentIds,
        coordinates
    );
  }

  private MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> buildState(
      Map<List<Integer>, MEIndividual<G, S, Q>> individualMap,
      Map<List<Integer>, Long> nOfEvaluationsMap,
      Map<List<Integer>, Double> fidelityMap,
      Map<List<Integer>, Double> cumulativeFidelityMap,
      AtomicLong nOfBirths,
      AtomicLong nOfIterations,
      LocalDateTime startingDateTime,
      MultifidelityQualityBasedProblem<S, Q> problem
  ) {
    long currentNOfBirths = nOfBirths.get();
    Archive<MultiFidelityMEPopulationState.LocalState> localStateArchive = new Archive<>(
        descriptors.stream()
            .map(Descriptor::nOfBins)
            .toList()
    );
    nOfEvaluationsMap.forEach(
        (bins, nOfEvaluations) -> localStateArchive.asMap()
            .put(
                bins,
                new MultiFidelityMEPopulationState.LocalState(
                    nOfEvaluations,
                    fidelityMap.getOrDefault(bins, 0d),
                    cumulativeFidelityMap.getOrDefault(bins, 0d)
                )
            )
    );
    return MultiFidelityMEPopulationState.of(
        startingDateTime,
        ChronoUnit.MILLIS.between(startingDateTime, LocalDateTime.now()),
        nOfIterations.get(),
        problem,
        stopCondition(),
        currentNOfBirths,
        currentNOfBirths,
        descriptors,
        new Archive<>(descriptors.stream().map(Descriptor::nOfBins).toList(), individualMap),
        localStateArchive
    );
  }

  private List<Coordinate> getCoordinates(
      long id,
      Collection<Long> parentIds,
      G genotype,
      S solution,
      long nOfIterations
  ) {
    return descriptors.stream()
        .map(
            d -> d.coordinate(
                Individual.of(
                    id,
                    genotype,
                    solution,
                    null,
                    nOfIterations,
                    nOfIterations,
                    parentIds
                )
            )
        )
        .toList();
  }

  @Override
  public MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> init(
      MultifidelityQualityBasedProblem<S, Q> problem,
      RandomGenerator random,
      Executor executor
  ) throws SolverException {
    throw new UnsupportedOperationException("This solver is not actually iterative");
  }

  @Override
  public MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> update(
      RandomGenerator random,
      Executor executor,
      MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state
  ) throws SolverException {
    throw new UnsupportedOperationException("This solver is not actually iterative");
  }

  @Override
  public Collection<S> solve(
      MultifidelityQualityBasedProblem<S, Q> problem,
      RandomGenerator random,
      Executor executor,
      Listener<? super MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>> listener
  ) throws SolverException {
    // init maps and counters
    LocalDateTime startingDateTime = LocalDateTime.now();
    AtomicLong nOfBirths = new AtomicLong(0);
    AtomicLong nOfIterations = new AtomicLong(0);
    AtomicLong lastIterationNOfBirths = new AtomicLong(0);
    AtomicInteger nOfRunning = new AtomicInteger(0);
    AtomicReference<Double> progressRate = new AtomicReference<>(0d);
    AtomicBoolean stopped = new AtomicBoolean(false);
    Map<List<Integer>, MEIndividual<G, S, Q>> individualMap = new ConcurrentHashMap<>();
    Map<List<Integer>, Long> nOfEvaluationsMap = new ConcurrentHashMap<>();
    Map<List<Integer>, Double> currentFidelityMap = new ConcurrentHashMap<>();
    Map<List<Integer>, Double> cumulativeFidelityMap = new ConcurrentHashMap<>();
    List<List<Integer>> allCoorditantes = Misc.cartesian(
        descriptors.stream()
            .map(
                d -> IntStream.range(0, d.nOfBins())
                    .boxed()
                    .toList()
            )
            .toList()
    );
    allCoorditantes.forEach(bins -> {
      nOfEvaluationsMap.put(bins, 0L);
      currentFidelityMap.put(bins, 0d);
      cumulativeFidelityMap.put(bins, 0d);
    });
    int capacity = allCoorditantes.size();
    // build seed individual
    long seedId = nOfBirths.getAndIncrement();
    G seedGenotype = genotypeFactory.build(1, random).getFirst();
    S seedSolution = solutionMapper.apply(seedGenotype);
    MEIndividual<G, S, Q> seedIndividual = buildIndividual(
        seedId,
        List.of(),
        seedGenotype,
        seedSolution,
        getCoordinates(seedId, List.of(), seedGenotype, seedSolution, nOfIterations.get()),
        problem.qualityFunction(),
        nOfIterations.get(),
        nOfEvaluationsMap,
        currentFidelityMap,
        cumulativeFidelityMap
    );
    individualMap.put(seedIndividual.bins(), seedIndividual);
    MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state = buildState(
        individualMap,
        nOfEvaluationsMap,
        currentFidelityMap,
        cumulativeFidelityMap,
        nOfBirths,
        nOfIterations,
        startingDateTime,
        problem
    );
    listener.listen(state);
    // "iterate", ie, start capacity concurrent tasks
    IntStream.range(0, capacity)
        .forEach(
            i -> executor.execute(
                variationRunnable(
                    individualMap,
                    nOfEvaluationsMap,
                    currentFidelityMap,
                    cumulativeFidelityMap,
                    nOfBirths,
                    nOfIterations,
                    lastIterationNOfBirths,
                    progressRate,
                    nOfRunning,
                    stopped,
                    startingDateTime,
                    problem,
                    random,
                    executor,
                    listener
                )
            )
        );
    // wait for no more runnables to go
    while (nOfRunning.get() > 0) {
      try {
        synchronized (nOfRunning) {
          nOfRunning.wait();
        }
      } catch (InterruptedException e) {
        // ignore
      }
    }
    listener.done();
    return extractSolutions(
        problem,
        random,
        executor,
        buildState(
            individualMap,
            nOfEvaluationsMap,
            currentFidelityMap,
            cumulativeFidelityMap,
            nOfBirths,
            nOfIterations,
            startingDateTime,
            problem
        )
    );
  }

  private double localFidelity(
      List<Integer> bins,
      AtomicReference<Double> progressRate,
      Map<List<Integer>, Long> nOfEvaluationsMap
  ) {
    long maxNOfEvaluations = nOfEvaluationsMap.values().stream().mapToLong(Long::longValue).max().orElse(1);
    double localNOfEvalsRate = (double) nOfEvaluationsMap.get(bins) / (double) maxNOfEvaluations;
    double globalProgressRate = progressRate.get();
    double localProgressRate = (1 - globalProgressRate) * localNOfEvalsRate + globalProgressRate * globalProgressRate;
    return schedule.applyAsDouble(localProgressRate);
  }

  private Runnable variationRunnable(
      Map<List<Integer>, MEIndividual<G, S, Q>> individualMap,
      Map<List<Integer>, Long> nOfEvaluationsMap,
      Map<List<Integer>, Double> currentFidelityMap,
      Map<List<Integer>, Double> cumulativeFidelityMap,
      AtomicLong nOfBirths,
      AtomicLong nOfIterations,
      AtomicLong lastIterationNOfBirths,
      AtomicReference<Double> progressRate,
      AtomicInteger nOfRunning,
      AtomicBoolean stopped,
      LocalDateTime startingDateTime,
      MultifidelityQualityBasedProblem<S, Q> problem,
      RandomGenerator random,
      Executor executor,
      Listener<? super MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>> listener
  ) {
    // increase counter of "pending" tasks
    nOfRunning.incrementAndGet();
    // create task
    return () -> {
      try {
        // find cell with lower number of variations
        MEIndividual<G, S, Q> parent = Misc.pickRandomly(individualMap.values(), random);
        // create new individual
        long nowNOfIterations = nOfIterations.get();
        long childId = nOfBirths.getAndIncrement();
        G childGenotype = mutation.mutate(parent.genotype(), random);
        S childSolution = solutionMapper.apply(childGenotype);
        List<Long> childParentIds = List.of(parent.id());
        MEIndividual<G, S, Q> child = buildIndividual(
            childId,
            childParentIds,
            childGenotype,
            childSolution,
            getCoordinates(childId, childParentIds, childGenotype, childSolution, nowNOfIterations),
            problem.qualityFunction(),
            nowNOfIterations,
            nOfEvaluationsMap,
            currentFidelityMap,
            cumulativeFidelityMap
        );
        MEIndividual<G, S, Q> existingIndividual = individualMap.get(child.bins());
        if (existingIndividual == null) {
          individualMap.put(child.bins(), child);
        } else {
          if (problem.qualityComparator()
              .compare(child.quality(), existingIndividual.quality())
              .equals(
                  PartialComparator.PartialComparatorOutcome.BEFORE
              )) {
            // update local fidelity
            double newLocalFidelity = localFidelity(child.bins(), progressRate, nOfEvaluationsMap);
            currentFidelityMap.put(child.bins(), newLocalFidelity);
            // update child
            buildIndividual(
                child.id(),
                child.parentIds(),
                child.genotype(),
                child.solution(),
                child.coordinates(),
                problem.qualityFunction(),
                nowNOfIterations,
                nOfEvaluationsMap,
                currentFidelityMap,
                cumulativeFidelityMap
            );
            individualMap.put(child.bins(), child);
          }
        }
        // send "global" state
        if (nOfBirths.get() - lastIterationNOfBirths.get() >= nOfBirthsForIteration) {
          nOfIterations.incrementAndGet();
          lastIterationNOfBirths.set(nOfBirths.get());
          MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state = buildState(
              individualMap,
              nOfEvaluationsMap,
              currentFidelityMap,
              cumulativeFidelityMap,
              nOfBirths,
              nOfIterations,
              startingDateTime,
              problem
          );
          listener.listen(state);
          progressRate.set(
              ((ProgressBasedStopCondition<? super MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>>) stopCondition())
                  .progress(state)
                  .rate()
          );
          if (terminate(random, executor, state)) {
            stopped.set(true);
          }
        }
        // start new variationRunnable on this cell
        if (!stopped.get()) {
          executor.execute(
              variationRunnable(
                  individualMap,
                  nOfEvaluationsMap,
                  currentFidelityMap,
                  cumulativeFidelityMap,
                  nOfBirths,
                  nOfIterations,
                  lastIterationNOfBirths,
                  progressRate,
                  nOfRunning,
                  stopped,
                  startingDateTime,
                  problem,
                  random,
                  executor,
                  listener
              )
          );
        }
      } catch (RuntimeException e) {
        Logger.getLogger(getClass().getName())
            .log(
                Level.WARNING,
                "Unexpected exception, ignore failing task: %s".formatted(e),
                e
            );
      } finally {
        nOfRunning.decrementAndGet();
        synchronized (nOfRunning) {
          nOfRunning.notify();
        }
      }
    };
  }

}
