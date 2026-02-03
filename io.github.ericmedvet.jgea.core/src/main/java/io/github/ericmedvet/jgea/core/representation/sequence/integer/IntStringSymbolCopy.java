/*-
 * ========================LICENSE_START=================================
 * jgea-core
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
package io.github.ericmedvet.jgea.core.representation.sequence.integer;

import io.github.ericmedvet.jgea.core.operator.Mutation;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public class IntStringSymbolCopy implements Mutation<IntString> {
  @Override
  public IntString mutate(IntString intString, RandomGenerator random) {
    List<Integer> genes = new ArrayList<>(intString.genes());
    genes.set(random.nextInt(genes.size()), genes.get(random.nextInt(genes.size())));
    return new IntString(genes, intString.lowerBound(), intString.upperBound());
  }
}
