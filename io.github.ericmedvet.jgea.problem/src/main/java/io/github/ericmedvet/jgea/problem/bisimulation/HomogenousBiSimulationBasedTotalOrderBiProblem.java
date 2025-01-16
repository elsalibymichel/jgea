package io.github.ericmedvet.jgea.problem.bisimulation;

import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.QualityBasedBiProblem;
import io.github.ericmedvet.jsdynsym.control.Simulation;

public interface HomogenousBiSimulationBasedTotalOrderBiProblem<S, B, O extends Simulation.Outcome<B>, Q extends Comparable<Q>> extends QualityBasedBiProblem<S, B, O>, ProblemWithExampleSolution<S> {

}
