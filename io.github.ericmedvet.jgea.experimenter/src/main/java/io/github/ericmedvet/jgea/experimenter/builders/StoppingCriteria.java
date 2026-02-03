/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2026 Eric Medvet
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
package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.problem.Problem;
import io.github.ericmedvet.jgea.core.problem.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.solver.*;
import io.github.ericmedvet.jgea.core.util.Progress;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;

@Discoverable(prefixTemplate = "ea.stoppingCriterion|sc")
public class StoppingCriteria {
  private StoppingCriteria() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <I extends Individual<G, S, Q>, G, S, Q, P extends QualityBasedProblem<S, Q>> ProgressBasedStopCondition<POCPopulationState<I, G, S, Q, P>> cumulativeFidelity(
      @Param(value = "v", dD = 1000) int v
  ) {
    return s -> {
      if (s instanceof MultiFidelityPOCPopulationState<?, ?, ?, ?, ?> mfS) {
        return new Progress(0, v, mfS.cumulativeFidelity());
      } else {
        return new Progress(0, v, s.nOfQualityEvaluations());
      }
    };
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <P extends Problem<S>, S> ProgressBasedStopCondition<State<P, S>> elapsed(
      @Param(value = "v", dD = 10) double v
  ) {
    return s -> new Progress(0, Math.round(v * 1000), s.elapsedMillis());
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <P extends Problem<S>, S> ProgressBasedStopCondition<State<P, S>> nOfIterations(
      @Param(value = "n", dI = 100) int n
  ) {
    return s -> new Progress(0, n, s.nOfIterations());
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <I extends Individual<G, S, Q>, G, S, Q, P extends QualityBasedProblem<S, Q>> ProgressBasedStopCondition<POCPopulationState<I, G, S, Q, P>> nOfQualityEvaluations(
      @Param(value = "n", dI = 1000) int n
  ) {
    return s -> new Progress(0, n, s.nOfQualityEvaluations());
  }

}
