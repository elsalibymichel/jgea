/*-
 * ========================LICENSE_START=================================
 * jgea-core
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
package io.github.ericmedvet.jgea.core.representation.sequence.integer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class UniformUniqueIntStringFactory extends UniformIntStringFactory {
  public UniformUniqueIntStringFactory(int lowerBound, int upperBound, int size) {
    super(lowerBound, upperBound, size);
    if (size > (upperBound - lowerBound)) {
      throw new IllegalArgumentException(
          "Cannot ensure uniqueness: %d values in [%d-%d[, %d required as length".formatted(
              upperBound - lowerBound,
              lowerBound,
              upperBound,
              size
          )
      );
    }
  }

  @Override
  public IntString build(RandomGenerator random) {
    List<Integer> values = new ArrayList<>(IntStream.range(lowerBound, upperBound).boxed().toList());
    Collections.shuffle(values, random);
    return new IntString(values.subList(0, Math.min(upperBound - lowerBound, size)), lowerBound, upperBound);
  }
}
