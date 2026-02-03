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

import java.util.*;

public class ParetoDominance<C> implements PartialComparator<List<C>> {

  private final List<Comparator<C>> comparators;

  public ParetoDominance(List<Comparator<C>> comparators) {
    this.comparators = comparators;
  }

  public static <C> PartialComparatorOutcome compare(
      List<C> cs1,
      List<C> cs2,
      List<Comparator<C>> comparators
  ) {
    if (cs1.size() != cs2.size() || cs1.size() != comparators.size()) {
      throw new IllegalArgumentException("Cannot compare: lists sizes mismatch.");
    }
    int afterCount = 0;
    int beforeCount = 0;
    for (int i = 0; i < cs1.size(); i++) {
      C o1 = cs1.get(i);
      C o2 = cs2.get(i);
      int outcome = comparators.get(i).compare(o1, o2);
      if (outcome < 0) {
        beforeCount = beforeCount + 1;
      } else if (outcome > 0) {
        afterCount = afterCount + 1;
      }
    }
    if ((beforeCount > 0) && (afterCount == 0)) {
      return PartialComparatorOutcome.BEFORE;
    }
    if ((beforeCount == 0) && (afterCount > 0)) {
      return PartialComparatorOutcome.AFTER;
    }
    if ((beforeCount == 0) && (afterCount == 0)) {
      return PartialComparatorOutcome.SAME;
    }
    return PartialComparatorOutcome.NOT_COMPARABLE;
  }

  public static <C extends Comparable<C>> ParetoDominance<C> from(@SuppressWarnings("unused") Class<C> cClass, int n) {
    return new ParetoDominance<>(Collections.nCopies(n, Comparable::compareTo));
  }

  @Override
  public PartialComparatorOutcome compare(List<C> cs1, List<C> cs2) {
    return compare(cs1, cs2, comparators);
  }

  @Override
  public boolean isTotal() {
    return comparators.size() == 1;
  }
}
