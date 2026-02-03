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

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.representation.sequence.FixedLengthListFactory;
import io.github.ericmedvet.jgea.core.representation.sequence.bit.BitString;
import io.github.ericmedvet.jgea.core.representation.sequence.bit.BitStringFactory;
import io.github.ericmedvet.jgea.core.representation.sequence.integer.IntString;
import io.github.ericmedvet.jgea.core.representation.sequence.integer.SequentialIntStringFactory;
import io.github.ericmedvet.jgea.core.representation.sequence.integer.UniformIntStringFactory;
import io.github.ericmedvet.jgea.core.representation.sequence.integer.UniformUniqueIntStringFactory;
import io.github.ericmedvet.jgea.core.representation.sequence.numeric.UniformDoubleFactory;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import java.util.List;
import java.util.function.Function;

@Discoverable(prefixTemplate = "ea.representation|r.factory|f")
public class Factories {

  private Factories() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Function<BitString, Factory<BitString>> bsUniform() {
    return eBs -> new BitStringFactory(eBs.size());
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Function<List<Double>, Factory<List<Double>>> dsUniform(
      @Param(value = "initialMinV", dD = -1d) double initialMinV,
      @Param(value = "initialMaxV", dD = 1d) double initialMaxV
  ) {
    return eDs -> new FixedLengthListFactory<>(eDs.size(), new UniformDoubleFactory(initialMinV, initialMaxV));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Function<IntString, Factory<IntString>> isSequential() {
    return eIs -> new SequentialIntStringFactory(eIs.lowerBound(), eIs.upperBound(), eIs.size());
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Function<IntString, Factory<IntString>> isUniform() {
    return eIs -> new UniformIntStringFactory(eIs.lowerBound(), eIs.upperBound(), eIs.size());
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Function<IntString, Factory<IntString>> isUniformUnique() {
    return eIs -> new UniformUniqueIntStringFactory(eIs.lowerBound(), eIs.upperBound(), eIs.size());
  }

}
