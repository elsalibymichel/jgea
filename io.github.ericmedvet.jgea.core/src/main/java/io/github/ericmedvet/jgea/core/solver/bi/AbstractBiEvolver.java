package io.github.ericmedvet.jgea.core.solver.bi;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.problem.QualityBasedBiProblem;
import io.github.ericmedvet.jgea.core.solver.AbstractPopulationBasedIterativeSolver;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.POCPopulationState;

import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractBiEvolver<
        T extends POCPopulationState<I, G, S, Q, P>,
        P extends QualityBasedBiProblem<S, O, Q>,
        I extends Individual<G, S, Q>,
        G,
        S,
        Q, O> extends AbstractPopulationBasedIterativeSolver<T,P,I,G,S,Q> {

    public AbstractBiEvolver(Function<? super G, ? extends S> solutionMapper, Factory<? extends G> genotypeFactory, Predicate<? super T> stopCondition, boolean remap) {
        super(solutionMapper, genotypeFactory, stopCondition, remap);
    }

}
