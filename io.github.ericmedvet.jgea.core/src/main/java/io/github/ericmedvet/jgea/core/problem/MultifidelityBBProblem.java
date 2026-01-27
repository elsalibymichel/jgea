/*-
 * ========================LICENSE_START=================================
 * jgea-core
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
package io.github.ericmedvet.jgea.core.problem;

public interface MultifidelityBBProblem<S, B, BQ> extends BehaviorBasedProblem<S, B, BQ>, MultifidelityQualityBasedProblem<S, BehaviorBasedProblem.Outcome<B, BQ>> {

  MultifidelityFunction<? super S, ? extends B> behaviorFunction();

  @Override
  default MultifidelityFunction<S, Outcome<B, BQ>> qualityFunction() {
    return (s, fidelity) -> {
      B b = behaviorFunction().apply(s, fidelity);
      return new Outcome<>(b, behaviorQualityFunction().apply(b));
    };
  }
}
