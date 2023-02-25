/*
 * Copyright 2022 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.IndependentFactory;
import io.github.ericmedvet.jgea.core.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.operator.GeneticOperator;
import io.github.ericmedvet.jgea.core.representation.sequence.FixedLengthListFactory;
import io.github.ericmedvet.jgea.core.representation.sequence.UniformCrossover;
import io.github.ericmedvet.jgea.core.representation.sequence.numeric.GaussianMutation;
import io.github.ericmedvet.jgea.core.representation.sequence.numeric.UniformDoubleFactory;
import io.github.ericmedvet.jgea.core.selector.Last;
import io.github.ericmedvet.jgea.core.selector.Tournament;
import io.github.ericmedvet.jgea.core.solver.*;
import io.github.ericmedvet.jgea.core.solver.state.POSetPopulationState;
import io.github.ericmedvet.jgea.experimenter.InvertibleMapper;
import io.github.ericmedvet.jnb.core.Param;

import java.util.List;
import java.util.Map;

/**
 * @author "Eric Medvet" on 2022/11/21 for 2d-robot-evolution
 */
public class Solvers {

  private Solvers() {
  }

  @SuppressWarnings("unused")
  public static <S, Q> StandardEvolver<POSetPopulationState<List<Double>, S, Q>, QualityBasedProblem<S, Q>,
      List<Double>, S, Q> numGA(
      @Param(value = "mapper") InvertibleMapper<List<Double>, S> mapper,
      @Param(value = "initialMinV", dD = -1d) double initialMinV,
      @Param(value = "initialMaxV", dD = 1d) double initialMaxV,
      @Param(value = "crossoverP", dD = 0.8d) double crossoverP,
      @Param(value = "sigmaMut", dD = 0.35d) double sigmaMut,
      @Param(value = "tournamentRate", dD = 0.05d) double tournamentRate,
      @Param(value = "minNTournament", dI = 3) int minNTournament,
      @Param(value = "nPop", dI = 100) int nPop,
      @Param(value = "nEval") int nEval,
      @Param(value = "diversity") boolean diversity,
      @Param(value = "remap") boolean remap
  ) {
    IndependentFactory<List<Double>> doublesFactory = new FixedLengthListFactory<>(
        mapper.exampleInput().size(),
        new UniformDoubleFactory(initialMinV, initialMaxV)
    );
    Map<GeneticOperator<List<Double>>, Double> geneticOperators = Map.of(
        new GaussianMutation(sigmaMut),
        1d - crossoverP,
        new UniformCrossover<>(doublesFactory).andThen(new GaussianMutation(sigmaMut)),
        crossoverP
    );
    if (!diversity) {
      return new StandardEvolver<>(
          mapper,
          doublesFactory,
          nPop,
          StopConditions.nOfFitnessEvaluations(nEval),
          geneticOperators,
          new Tournament(Math.max(minNTournament, (int) Math.ceil((double) nPop * tournamentRate))),
          new Last(),
          nPop,
          true,
          remap,
          (p, r) -> new POSetPopulationState<>()
      );
    } else {
      return new StandardWithEnforcedDiversityEvolver<>(
          mapper,
          doublesFactory,
          nPop,
          StopConditions.nOfFitnessEvaluations(nEval),
          geneticOperators,
          new Tournament(Math.max(minNTournament, (int) Math.ceil((double) nPop * tournamentRate))),
          new Last(),
          nPop,
          true,
          remap,
          (p, r) -> new POSetPopulationState<>(),
          100
      );
    }
  }

  @SuppressWarnings("unused")
  public static <S, Q> OpenAIEvolutionaryStrategy<S, Q> openAIES(
      @Param(value = "mapper") InvertibleMapper<List<Double>, S> mapper,
      @Param(value = "initialMinV", dD = -1d) double initialMinV,
      @Param(value = "initialMaxV", dD = 1d) double initialMaxV,
      @Param(value = "sigma", dD = 0.35d) double sigma,
      @Param(value = "batchSize", dI = 15) int batchSize,
      @Param(value = "nEval") int nEval
  ) {
    return new OpenAIEvolutionaryStrategy<>(
        mapper,
        new FixedLengthListFactory<>(mapper.exampleInput().size(), new UniformDoubleFactory(initialMinV, initialMaxV)),
        batchSize,
        StopConditions.nOfFitnessEvaluations(nEval),
        sigma
    );
  }

  @SuppressWarnings("unused")
  public static <S, Q> SimpleEvolutionaryStrategy<S, Q> simpleES(
      @Param(value = "mapper") InvertibleMapper<List<Double>, S> mapper,
      @Param(value = "initialMinV", dD = -1d) double initialMinV,
      @Param(value = "initialMaxV", dD = 1d) double initialMaxV,
      @Param(value = "sigma", dD = 0.35d) double sigma,
      @Param(value = "parentsRate", dD = 0.33d) double parentsRate,
      @Param(value = "nOfElites", dI = 1) int nOfElites,
      @Param(value = "nPop", dI = 30) int nPop,
      @Param(value = "nEval") int nEval,
      @Param(value = "remap") boolean remap
  ) {
    return new SimpleEvolutionaryStrategy<>(
        mapper,
        new FixedLengthListFactory<>(mapper.exampleInput().size(), new UniformDoubleFactory(initialMinV, initialMaxV)),
        nPop,
        StopConditions.nOfFitnessEvaluations(nEval),
        nOfElites,
        (int) Math.round(nPop * parentsRate),
        sigma,
        remap
    );
  }
}