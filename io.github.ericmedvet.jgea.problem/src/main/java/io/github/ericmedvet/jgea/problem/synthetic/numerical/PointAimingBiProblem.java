package io.github.ericmedvet.jgea.problem.synthetic.numerical;

import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.SymmetricQualityBasedBiProblem;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PointAimingBiProblem implements SymmetricQualityBasedBiProblem<List<Double>, List<Integer>, Double>, ProblemWithExampleSolution<List<Double>> {

  private final int p;
  private final double[] target;

  public PointAimingBiProblem(int p, double[] target) {
    this.p = p;
    this.target = target;
  }

  @Override
  public List<Double> example() {
    return Collections.nCopies(p, 0d);
  }

  @Override
  public UnaryOperator<Double> symmetryFunction() {
    return q -> 1 - q;
  }

  @Override
  public BiFunction<List<Double>, List<Double>, List<Integer>> outcomeFunction() {

    return (s0, s1) -> IntStream.range(0, p).mapToObj(i -> Math.abs(s0.get(i) - target[i]) < Math.abs(s1.get(i) - target[i]) ? 0 : 1).collect(Collectors.toList());
  }

  @Override
  public Function<List<Integer>, Double> firstQualityFunction() {
    return o -> (double) o.stream().mapToInt(Integer::intValue).sum() / (double) p;
  }

  @Override
  public PartialComparator<Double> qualityComparator() {
    return PartialComparator.from(Double.class);
  }
}
