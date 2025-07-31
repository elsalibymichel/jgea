/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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

package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.problem.ExampleBasedProblem;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionProblem;
import io.github.ericmedvet.jgea.problem.regression.univariate.synthetic.*;
import io.github.ericmedvet.jnb.core.*;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

@Discoverable(prefixTemplate = "ea.problem|p.univariateRegression|ur")
public class UnivariateRegressionProblems {
  private UnivariateRegressionProblems() {
  }

  @SuppressWarnings("unused")
  @Alias(
      name = "bundled", passThroughParams = {@PassThroughParam(name = "name", type = ParamMap.Type.STRING), @PassThroughParam(name = "xScaling", value = "none", type = ParamMap.Type.STRING), @PassThroughParam(name = "yScaling", value = "none", type = ParamMap.Type.STRING)
      }, value = // spotless:off
      """
          fromData(provider = ea.provider.num.fromBundled(name = $name; xScaling = $xScaling; yScaling = $yScaling))
          """) // spotless:on
  @Cacheable
  public static UnivariateRegressionProblem fromData(
      @Param("provider") IndexedProvider<ExampleBasedProblem.Example<Map<String, Double>, Map<String, Double>>> provider,
      @Param(value = "metrics", dSs = {"mse"}) List<UnivariateRegressionProblem.Metric> metrics,
      @Param(value = "nFolds", dI = 10) int nFolds,
      @Param(value = "testFold", dI = 0) int testFold,
      @Param(value = "shuffle", dB = true) boolean shuffle,
      @Param(value = "randomGenerator", dNPM = "m.defaultRG()") RandomGenerator randomGenerator
  ) {
    String yVarName = provider.first()
        .output()
        .keySet()
        .stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No output y var in dataset"));
    if (shuffle) {
      provider = provider.shuffled(randomGenerator);
    }
    IndexedProvider<ExampleBasedProblem.Example<Map<String, Double>, Double>> eProvider = provider.then(
        e -> new ExampleBasedProblem.Example<>(
            e.input(),
            e.output().get(yVarName)
        )
    );
    if (nFolds > 0) {
      return UnivariateRegressionProblem.from(
          metrics,
          yVarName,
          eProvider.negatedFold(testFold, nFolds),
          eProvider.fold(testFold, nFolds)
      );
    }
    return UnivariateRegressionProblem.from(
        metrics,
        yVarName,
        eProvider,
        eProvider
    );
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static SyntheticURProblem synthetic(
      @Param("name") String name,
      @Param(value = "metrics", dSs = {"mse"}) List<UnivariateRegressionProblem.Metric> metrics,
      @Param(value = "randomGenerator", dNPM = "m.defaultRG()") RandomGenerator randomGenerator
  ) {
    return switch (name) {
      case "keijzer6" -> new Keijzer6(metrics, randomGenerator);
      case "nguyen7" -> new Nguyen7(metrics, randomGenerator);
      case "pagie1" -> new Pagie1(metrics, randomGenerator);
      case "polynomial4" -> new Polynomial4(metrics, randomGenerator);
      case "vladislavleva4" -> new Vladislavleva4(metrics, randomGenerator);
      case "korns12" -> new Korns12(metrics, randomGenerator);
      case "xor" -> new Xor(metrics, randomGenerator);
      default -> throw new IllegalArgumentException("Unknown synthetic function: %s".formatted(name));
    };
  }
}
