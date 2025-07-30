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
import io.github.ericmedvet.jgea.core.solver.mapelites.MultiFidelityMEPopulationState.LocalState;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public class AsynchronousMultiFidelityMapElites<G, S, Q> extends AbstractPopulationBasedIterativeSolver<MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>, MultifidelityQualityBasedProblem<S, Q>, MEIndividual<G, S, Q>, G, S, Q> {

  private final Mutation<G> mutation;
  private final List<Descriptor<G, S, Q>> descriptors;
  private final DoubleUnaryOperator schedule;

  public AsynchronousMultiFidelityMapElites(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      ProgressBasedStopCondition<? super MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>> stopCondition,
      List<PartialComparator<? super MEIndividual<G, S, Q>>> additionalIndividualComparators,
      Mutation<G> mutation,
      List<Descriptor<G, S, Q>> descriptors,
      DoubleUnaryOperator schedule
  ) {
    super(solutionMapper, genotypeFactory, stopCondition, true, additionalIndividualComparators);
    this.mutation = mutation;
    this.descriptors = descriptors;
    this.schedule = schedule;
  }

  @Override
  public MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> init(
      MultifidelityQualityBasedProblem<S, Q> problem,
      RandomGenerator random,
      ExecutorService executor
  ) throws SolverException {
    MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state = MultiFidelityMEPopulationState
        .empty(
            problem,
            stopCondition(),
            descriptors
        );
    ChildGenotype<G> childGenotype = new ChildGenotype<>(
        0,
        genotypeFactory.build(1, random).getFirst(),
        List.of()
    );
    MEIndividual<G, S, Q> individual = buildIndividual(childGenotype, state);
    return state.updatedWithIteration(
        1,
        1,
        state.archive()
            .updated(List.of(individual), MEIndividual::bins, partialComparator(problem)),
        state.fidelityArchive() // TODO this should maybe be copied...
    );
  }

  private MEIndividual<G, S, Q> buildIndividual(
      ChildGenotype<G> childGenotype,
      MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state
  ) {
    S solution = solutionMapper.apply(childGenotype.genotype());
    List<Coordinate> coordinates = descriptors.stream()
        .map(
            d -> d.coordinate(
                Individual.of(
                    childGenotype.id(),
                    childGenotype.genotype(),
                    solution,
                    null,
                    state.nOfIterations(),
                    state.nOfIterations(),
                    childGenotype.parentIds()
                )
            )
        )
        .toList();
    List<Integer> bins = coordinates.stream().map(Coordinate::bin).toList();
    LocalState localState = state.fidelityArchive().get(bins);
    double fidelity;
    if (localState == null) {
      fidelity = schedule.applyAsDouble(0);
      localState = new LocalState(1, 0, schedule.applyAsDouble(fidelity));
    } else {
      double progressRate = ((ProgressBasedStopCondition<? super MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>>) stopCondition())
          .progress(
              state
          )
          .rate();
      double localRateOfNOfEvaluations = (double) localState.nOfQualityEvaluations() / state.fidelityArchive()
          .asMap()
          .values()
          .stream()
          .mapToDouble(ls -> (double) ls.nOfQualityEvaluations())
          .max()
          .orElse(1d);
      double localFidelity = progressRate * localRateOfNOfEvaluations;
      fidelity = Math.max(localFidelity, localState.fidelity());
    }
    Q quality = state.problem().qualityFunction().apply(solution, fidelity);
    state.fidelityArchive()
        .put(
            bins,
            new LocalState(
                localState.nOfQualityEvaluations() + 1,
                localState.nOfVariations(),
                fidelity
            ),
            PartialComparator.from(Comparator.comparing(LocalState::nOfQualityEvaluations).reversed())
        );
    return MEIndividual.of(
        childGenotype.id(),
        childGenotype.genotype(),
        solution,
        quality,
        state.nOfIterations(),
        state.nOfIterations(),
        childGenotype.parentIds(),
        coordinates
    );
  }

  @Override
  public MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> update(
      RandomGenerator random,
      ExecutorService executor,
      MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state
  ) throws SolverException {
    throw new UnsupportedOperationException("This solver is not actually iterative");
  }

  @Override
  public Collection<S> solve(
      MultifidelityQualityBasedProblem<S, Q> problem,
      RandomGenerator random,
      ExecutorService executor,
      Listener<? super MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>>> listener
  ) throws SolverException {
    MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state = init(
        problem,
        random,
        executor
    );
    listener.listen(state);
    // TODO make fluidly parallel
    AtomicLong ongoingTasks = new AtomicLong();
    // start one variationRunnable per cell
    // wait for no more runnables to go
    listener.done();
    return extractSolutions(problem, random, executor, state);
  }

  private Runnable variationRunnable(
      RandomGenerator random,
      ExecutorService executor,
      MultiFidelityMEPopulationState<G, S, Q, MultifidelityQualityBasedProblem<S, Q>> state,
      AtomicLong counter
  ) {
    return () -> {
      if (terminate(random, executor, state)) {
        return;
      }
      counter.incrementAndGet();
      // find cell with lower number of variations
      // create new individual
      // update "global" state
      // start new variationRunnable on this cell
    };
  }
}
