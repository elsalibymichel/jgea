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

import io.github.ericmedvet.jnb.core.*;

@Discoverable(prefixTemplate = "ea.plot.multi|m")
@Alias(
    name = "scatterExp", value = // spotless:off
    """
        viz.plot.multi.scatter(
          xSubplot = ea.f.runString(name = problem; s = "{run.problem.name}");
          ySubplot = ea.f.runString(name = none; s = "_");
          group = ea.f.runString(name = solver; s = "{run.solver.name}");
          predicateValue = ea.f.rate(of = ea.f.progress());
          condition = predicate.gtEq(t = 1)
        )
        """) // spotless:on
@Alias(
    name = "xyExp", passThroughParams = {@PassThroughParam(name = "xQuantization", value = "1", type = ParamMap.Type.INT)}, value = // spotless:off
    """
        viz.plot.multi.xy(
          xSubplot = ea.f.runString(name = problem; s = "{run.problem.name}");
          ySubplot = ea.f.runString(name = none; s = "_");
          line = ea.f.runString(name = solver; s = "{run.solver.name}");
          x = f.quantized(of = ea.f.nOfEvals(); q = $xQuantization)
        )
        """) // spotless:on
@Alias(
    name = "quality", passThroughParams = {@PassThroughParam(name = "q", value = "f.identity()", type = ParamMap.Type.NAMED_PARAM_MAP)}, value = // spotless:off
    """
        ea.plot.multi.xyExp(y = f.composition(of = ea.f.quality(of = ea.f.best()); then = $q))
        """) // spotless:on
@Alias(
    name = "uniqueness", value = // spotless:off
    """
        ea.plot.multi.xyExp(y = f.uniqueness(of = f.each(mapF = ea.f.genotype(); of = ea.f.all())))
        """) // spotless:on
@Alias(
    name = "yBoxplotExp", value = // spotless:off
    """
        viz.plot.multi.yBoxplot(
          xSubplot = ea.f.runString(name = problem; s = "{run.problem.name}");
          ySubplot = ea.f.runString(name = none; s = "_");
          box = ea.f.runString(name = solver; s = "{run.solver.name}");
          predicateValue = ea.f.rate(of = ea.f.progress());
          condition = predicate.gtEq(t = 1)
        )
        """) // spotless:on
@Alias(
    name = "qualityBoxplot", passThroughParams = {@PassThroughParam(name = "q", value = "f.identity()", type = ParamMap.Type.NAMED_PARAM_MAP)
    }, value = // spotless:off
    """
        ea.plot.multi.yBoxplotExp(y = f.composition(of = ea.f.quality(of = ea.f.best()); then = $q))
        """) // spotless:on
@Alias(
    name = "uniquenessBoxplot", value = // spotless:off
    """
        ea.plot.multi.yBoxplotExp(y = f.uniqueness(of = f.each(mapF = ea.f.genotype(); of = ea.f.all())))
        """) // spotless:on
public class MultiPlots {
  private MultiPlots() {
  }
}
