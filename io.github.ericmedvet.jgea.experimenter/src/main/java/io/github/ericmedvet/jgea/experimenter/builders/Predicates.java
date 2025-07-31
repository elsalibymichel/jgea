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

import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.core.util.Naming;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import java.util.function.Function;
import java.util.function.Predicate;

@Discoverable(prefixTemplate = "ea.predicate")
public class Predicates {
  private Predicates() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> isSrConstant(
      @Param(value = "f", dNPM = "f.identity()") Function<X, ? extends Element> function
  ) {
    return Naming.named(
        "isSrConstant",
        (Predicate<X>) (x -> function.apply(x) instanceof Element.Constant)
    );
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X> Predicate<X> isSrVariable(
      @Param(value = "f", dNPM = "f.identity()") Function<X, ? extends Element> function
  ) {
    return Naming.named(
        "isSrVariable",
        (Predicate<X>) (x -> function.apply(x) instanceof Element.Variable)
    );
  }
}
