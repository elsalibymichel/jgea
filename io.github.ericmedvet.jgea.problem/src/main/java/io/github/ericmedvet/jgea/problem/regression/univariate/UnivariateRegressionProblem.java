/*-
 * ========================LICENSE_START=================================
 * jgea-problem
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
package io.github.ericmedvet.jgea.problem.regression.univariate;

import io.github.ericmedvet.jgea.core.problem.SimpleEBMOProblem;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jgea.core.util.Naming;
import io.github.ericmedvet.jnb.datastructure.TriFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface UnivariateRegressionProblem extends SimpleEBMOProblem<NamedUnivariateRealFunction, Map<String, Double>, Double, UnivariateRegressionProblem.Outcome, Double> {
  enum Metric implements Function<List<Outcome>, Double> {
    MAE(
        ys -> ys.stream()
            .mapToDouble(y -> Math.abs(y.predicted - y.actual))
            .average()
            .orElse(Double.NaN)
    ), MSE(
        ys -> ys.stream()
            .mapToDouble(y -> (y.predicted - y.actual) * (y.predicted - y.actual))
            .average()
            .orElse(Double.NaN)
    ), RMSE(
        ys -> Math.sqrt(
            ys.stream()
                .mapToDouble(y -> (y.predicted - y.actual) * (y.predicted - y.actual))
                .average()
                .orElse(Double.NaN)
        )
    ), NMSE(
        ys -> ys.stream()
            .mapToDouble(y -> (y.predicted - y.actual) * (y.predicted - y.actual))
            .average()
            .orElse(Double.NaN) / ys.stream().mapToDouble(y -> y.actual).average().orElse(1d)
    ), R2(
        ys -> {
          double sumOfSquaredResiduals = ys.stream()
              .mapToDouble(y -> (y.predicted - y.actual) * (y.predicted - y.actual))
              .sum();
          double actualMean = ys.stream().mapToDouble(y -> y.actual).average().orElseThrow();
          double sumOfSquaredMeanResiduals = ys.stream()
              .mapToDouble(y -> (actualMean - y.actual) * (actualMean - y.actual))
              .sum();
          return 1d - sumOfSquaredResiduals / sumOfSquaredMeanResiduals;
        }
    ), ABS_COR(
        ys -> {
          double actualMean = ys.stream().mapToDouble(y -> y.actual).average().orElseThrow();
          double predictedMean = ys.stream().mapToDouble(y -> y.predicted).average().orElseThrow();
          double sumOfCrossResiduals = ys.stream()
              .mapToDouble(y -> (y.actual - actualMean) * (y.predicted - predictedMean))
              .sum();
          double sumOfActualSquaredMeanResiduals = ys.stream()
              .mapToDouble(y -> (actualMean - y.actual) * (actualMean - y.actual))
              .sum();
          double sumOfPredictedSquaredMeanResiduals = ys.stream()
              .mapToDouble(y -> (predictedMean - y.predicted) * (predictedMean - y.predicted))
              .sum();
          return sumOfCrossResiduals / Math.sqrt(sumOfPredictedSquaredMeanResiduals * sumOfActualSquaredMeanResiduals);
        }
    );

    private final Function<List<Outcome>, Double> function;

    Metric(Function<List<Outcome>, Double> function) {
      this.function = function;
    }

    @Override
    public Double apply(List<Outcome> ys) {
      return function.apply(ys);
    }

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  record Outcome(double actual, double predicted) {}

  List<Metric> metrics();

  String yVarName();

  static UnivariateRegressionProblem from(
      List<Metric> metrics,
      String yVarName,
      IndexedProvider<Example<Map<String, Double>, Double>> caseProvider,
      IndexedProvider<Example<Map<String, Double>, Double>> validationCaseProvider
  ) {
    record HardUnivariateRegressionProblem(
        List<Metric> metrics,
        String yVarName,
        IndexedProvider<Example<Map<String, Double>, Double>> caseProvider,
        IndexedProvider<Example<Map<String, Double>, Double>> validationCaseProvider
    ) implements UnivariateRegressionProblem {}
    return new HardUnivariateRegressionProblem(metrics, yVarName, caseProvider, validationCaseProvider);
  }

  @Override
  default SequencedMap<String, Objective<List<Outcome>, Double>> aggregateObjectives() {
    return metrics().stream()
        .collect(
            Misc.toSequencedMap(
                Enum::toString,
                m -> new Objective<>(m, Double::compareTo)
            )
        );
  }

  @Override
  default TriFunction<Map<String, Double>, Double, Double, Outcome> errorFunction() {
    return Naming.named(
        "aY|pY",
        (TriFunction<Map<String, Double>, Double, Double, Outcome>) (input, actualY, preditectY) -> new Outcome(
            actualY,
            preditectY
        )
    );
  }

  @Override
  default BiFunction<NamedUnivariateRealFunction, Map<String, Double>, Double> predictFunction() {
    return NamedUnivariateRealFunction::computeAsDouble;
  }

  @Override
  default Optional<NamedUnivariateRealFunction> example() {
    Example<Map<String, Double>, Double> example = caseProvider().first();
    return Optional.of(
        NamedUnivariateRealFunction.from(
            UnivariateRealFunction.from(inputs -> 0d, example.input().size()),
            example.input().keySet().stream().sorted().toList(),
            yVarName()
        )
    );
  }
}
