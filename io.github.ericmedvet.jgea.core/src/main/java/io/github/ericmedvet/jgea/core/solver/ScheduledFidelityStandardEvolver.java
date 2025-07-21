package io.github.ericmedvet.jgea.core.solver;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.operator.GeneticOperator;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.order.PartiallyOrderedCollection;
import io.github.ericmedvet.jgea.core.problem.MultifidelityQualityBasedProblem;
import io.github.ericmedvet.jgea.core.selector.Selector;
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
        currentFidelity(state) * individuals.size(),
        PartiallyOrderedCollection.from(individuals, partialComparator(state.problem()))
    );
  }
}
