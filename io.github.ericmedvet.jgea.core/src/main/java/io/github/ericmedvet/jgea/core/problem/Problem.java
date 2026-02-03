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

package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.order.PartialComparator;
import java.util.Optional;
import java.util.function.Function;

public interface Problem<S> extends PartialComparator<S> {

  default Optional<S> example() {
    return Optional.empty();
  }

  static <S> Problem<S> of(PartialComparator<S> partialComparator, S example, String name) {
    return new Problem<>() {
      @Override
      public PartialComparatorOutcome compare(S s1, S s2) {
        return partialComparator.compare(s1, s2);
      }

      @Override
      public Optional<S> example() {
        return Optional.ofNullable(example);
      }

      @Override
      public String toString() {
        return name;
      }
    };

  }

  default <T> Problem<T> on(Function<T, S> function, T example) {
    return of(
        comparing(function),
        example,
        "%s[on=%s]".formatted(this, function)
    );
  }
}
