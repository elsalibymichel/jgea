/*
 * Copyright 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
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

package io.github.ericmedvet.jgea.problem.regression.univariate;

import io.github.ericmedvet.jgea.core.fitness.CaseBasedFitness;
import io.github.ericmedvet.jgea.problem.regression.Dataset;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * @author eric
 */
public class UnivariateRegressionFitness extends CaseBasedFitness<UnivariateRealFunction, double[], Double, Double> {

  private final Dataset dataset;
  private final Metric metric;
  public UnivariateRegressionFitness(Dataset dataset, Metric metric) {
    super(
        dataset.examples().stream().map(Dataset.Example::xs).toList(),
        UnivariateRealFunction::applyAsDouble,
        aggregateFunction(dataset.examples().stream().map(e -> e.ys()[0]).toList(), metric)
    );
    this.dataset = dataset;
    this.metric = metric;
  }

  public enum Metric implements BiFunction<List<Double>, List<Double>, Double> {

    MAE((errs, ys) -> errs.stream()
        .mapToDouble(Math::abs)
        .average()
        .orElse(Double.NaN)),
    MSE((errs, ys) -> errs.stream()
        .mapToDouble(err -> err * err)
        .average()
        .orElse(Double.NaN)),
    RMSE((errs, ys) -> Math.sqrt(errs.stream()
        .mapToDouble(err -> err * err)
        .average()
        .orElse(Double.NaN))),
    NMSE((errs, ys) -> errs.stream()
        .mapToDouble(err -> err * err)
        .average()
        .orElse(Double.NaN) / ys.stream().mapToDouble(y -> y * y).average().orElse(1d));

    private final BiFunction<List<Double>, List<Double>, Double> function;

    Metric(BiFunction<List<Double>, List<Double>, Double> function) {
      this.function = function;
    }


    @Override
    public Double apply(List<Double> errs, List<Double> ys) {
      return function.apply(errs, ys);
    }

  }

  private static Function<List<Double>, Double> aggregateFunction(List<Double> ys, Metric metric) {
    return outcomes -> metric.apply(regressionError(ys,outcomes), ys);
  }

  private static List<Double> regressionError(List<Double> groundTruth, List<Double> predictions) {
    return IntStream.range(0, groundTruth.size()).mapToObj(i ->
        groundTruth.get(i) - predictions.get(i)
    ).toList();
  }

  public Dataset getDataset() {
    return dataset;
  }

  public Metric getMetric() {
    return metric;
  }
}