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

import io.github.ericmedvet.jgea.core.solver.POCPopulationState;
import io.github.ericmedvet.jgea.experimenter.Run;
import io.github.ericmedvet.jnb.core.*;
import io.github.ericmedvet.jnb.datastructure.AccumulatorFactory;
import java.util.Map;
import java.util.function.Function;

@Discoverable(prefixTemplate = "ea.accumulator|acc|a")
@Alias(name = "bests", value = "accumulator.all(eFunction = ea.f.best())")
@Alias(name = "lastBest", value = "accumulator.last(function = ea.f.best())")
public class Accumulators {

  private Accumulators() {
  }

  @SuppressWarnings("unused")
  public static <G> AccumulatorFactory<POCPopulationState<?, G, ?, ?, ?>, NamedParamMap, Run<?, G, ?, ?>> lastPopulationMap(
      @Param(value = "serializerF", dNPM = "f.toBase64()") Function<Object, String> serializer
  ) {
    return AccumulatorFactory.last(
        (s, run) -> new MapNamedParamMap(
            "ea.runOutcome",
            Map.ofEntries(
                Map.entry("index", run.index()),
                Map.entry("run", run.map()),
                Map.entry(
                    "serializedGenotypes",
                    s.pocPopulation()
                        .all()
                        .stream()
                        .map(i -> serializer.apply(i.genotype()))
                        .toList()
                )
            )
        )
    );
  }
}
