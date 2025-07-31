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

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

@Discoverable(prefixTemplate = "ea.schedule")
public class Schedules {

  private Schedules() {
  }

  //  these aliases should work, but don't...
  //
  //  @Alias(
  //      name = "linear", passThroughParams = {@PassThroughParam(name = "min", type = Type.DOUBLE, value = "0.0"), @PassThroughParam(name = "max", type = Type.DOUBLE, value = "1.0")
  //      }, value = "evenPiecewise(values = [$min; $max])"
  //  )
  //  @Alias(
  //      name = "flat", passThroughParams = {@PassThroughParam(name = "value", type = Type.DOUBLE, value = "1.0")
  //      }, value = "linear(min = $value; max = $value)"
  //  )
  @SuppressWarnings("unused")
  @Cacheable
  public static DoubleUnaryOperator evenPiecewise(
      @Param(value = "name", iS = "evenPiecewise[{values}]") String name,
      @Param(value = "values", dDs = {0d, 1d}) List<Double> values
  ) {
    if (values.size() < 2) {
      throw new IllegalArgumentException(
          "Wrong number of values: %d found, at least 2 expected".formatted(values.size())
      );
    }
    if (values.size() == 2) {
      DoubleRange range = new DoubleRange(values.getFirst(), values.getLast());
      return range::denormalize;
    }
    double size = values.size();
    return p -> {
      for (int i = 1; i < values.size(); i++) {
        double initP = (double) (i - 1) / size;
        double endP = (double) (i) / size;
        if (p >= initP && p <= endP) {
          return new DoubleRange(values.get(i - 1), values.get(i)).denormalize(
              new DoubleRange(initP, endP).normalize(p)
          );
        }
      }
      throw new IllegalArgumentException(
          "Cannot determine schedule with p=%.3f and values=[%s]".formatted(
              p,
              values.stream().map("%.3f"::formatted).collect(Collectors.joining(", "))
          )
      );
    };
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static DoubleUnaryOperator flat(
      @Param(value = "name", iS = "flat[{value}]") String name,
      @Param(value = "value", dD = 1d) double value
  ) {
    return p -> value;
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static DoubleUnaryOperator linear(
      @Param(value = "name", iS = "linear[{min}:{max}]") String name,
      @Param(value = "min", dD = 0d) double min,
      @Param(value = "max", dD = 1d) double max
  ) {
    DoubleRange range = new DoubleRange(min, max);
    return range::denormalize;
  }

}
