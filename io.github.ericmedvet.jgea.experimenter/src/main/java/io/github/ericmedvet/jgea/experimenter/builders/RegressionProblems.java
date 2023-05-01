package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.problem.regression.Dataset;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionFitness;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionProblem;
import io.github.ericmedvet.jgea.problem.regression.univariate.synthetic.*;
import io.github.ericmedvet.jnb.core.Param;

import java.io.IOException;
import java.util.List;

/**
 * @author "Eric Medvet" on 2023/04/30 for jgea
 */
public class RegressionProblems {
  private RegressionProblems() {
  }

  @SuppressWarnings("unused")
  public static UnivariateRegressionProblem<UnivariateRegressionFitness> bundled(
      @Param("name") String name,
      @Param(value = "metric", dS = "mse") UnivariateRegressionFitness.Metric metric
  ) {
    Dataset dataset;
    try {
      dataset = switch (name) {
        case "concrete" -> Dataset.loadFromCSVResource("/datasets/regression/concrete.csv", "strength");
        case "xor" -> Dataset.loadFromCSVResource("/datasets/regression/xor.csv", "y");
        default -> throw new IllegalArgumentException("Unknown bundled dataset: %s".formatted(name));
      };
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot load bundled dataset: %s".formatted(name));
    }
    return switch (name) {
      case "concrete" -> new UnivariateRegressionProblem<>(
          new UnivariateRegressionFitness(dataset.folds(List.of(0, 1, 2, 3), 5), metric),
          new UnivariateRegressionFitness(dataset.folds(List.of(4), 5), metric)
      );
      case "xor" -> new UnivariateRegressionProblem<>(
          new UnivariateRegressionFitness(dataset, metric),
          new UnivariateRegressionFitness(dataset, metric)
      );
      default -> throw new IllegalArgumentException("Unknown bundled dataset: %s".formatted(name));
    };
  }

  @SuppressWarnings("unused")
  public static SyntheticUnivariateRegressionProblem synthetic(
      @Param("name") String name,
      @Param(value = "metric", dS = "mse") UnivariateRegressionFitness.Metric metric,
      @Param(value = "seed", dI = 1) int seed
  ) {
    return switch (name) {
      case "keijzer6" -> new Keijzer6(metric);
      case "nguyen7" -> new Nguyen7(metric, seed);
      case "pagie1" -> new Pagie1(metric);
      case "polynomial4" -> new Polynomial4(metric);
      case "vladislavleva4" -> new Vladislavleva4(metric, seed);
      default -> throw new IllegalArgumentException("Unknown synthetic function: %s".formatted(name));
    };
  }
}