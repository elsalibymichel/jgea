package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.problem.synthetic.numerical.*;
import io.github.ericmedvet.jnb.core.*;

import java.util.stream.DoubleStream;

@Discoverable(prefixTemplate = "ea.biproblem|bp.synthetic|s")
public class SyntheticBiProblems {

  private SyntheticBiProblems() {
  }

  @SuppressWarnings("unused")
  public static PointAimingBiProblem pointAimingBiProblem(
      @Param(value = "name", iS = "pointAimingBiProblem-{p}-{target}") String name, @Param(value = "p", dI = 100) int p, @Param(value = "target", dD = 1d) double target) {
    return new PointAimingBiProblem(p, DoubleStream.generate(() -> target).limit(p).toArray());
  }
}
