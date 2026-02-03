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

package io.github.ericmedvet.jgea.core.order;

import java.util.Comparator;
import java.util.List;

public class LexicoGraphical<C extends Comparable<C>> implements Comparator<List<C>> {

  private final int[] order;

  public LexicoGraphical(Class<C> cClass, int... order) {
    this.order = order;
  }

  @Override
  public int compare(List<C> k1, List<C> k2) {
    for (int i : order) {
      C o1 = k1.get(i);
      C o2 = k2.get(i);
      int outcome = o1.compareTo(o2);
      if (outcome != 0) {
        return outcome;
      }
    }
    return 0;
  }
}
