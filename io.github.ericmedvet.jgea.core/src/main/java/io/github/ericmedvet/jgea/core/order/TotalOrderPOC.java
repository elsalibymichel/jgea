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

public class TotalOrderPOC<T> extends AbstractPartiallyOrderedCollection<T> {
  private final Comparator<SequencedCollection<T>> frontComparator;
  private final Comparator<? super T> comparator;
  private final List<SequencedCollection<T>> fronts;

  public TotalOrderPOC(Comparator<? super T> comparator) {
    super(PartialComparator.from(comparator));
    this.comparator = comparator;
    this.frontComparator = Comparator.comparing(SequencedCollection::getFirst, comparator);
    fronts = new ArrayList<>();
  }

  @Override
  public void add(T t) {
    Optional<SequencedCollection<T>> oFront = fronts.stream()
        .filter(f -> comparator.compare(f.getFirst(), t) == 0)
        .findAny();
    if (oFront.isPresent()) {
      oFront.get().add(t);
    } else {
      SequencedCollection<T> front = new ArrayList<>();
      front.add(t);
      fronts.add(front);
      fronts.sort(frontComparator);
    }
  }

  @Override
  public List<? extends Collection<T>> fronts() {
    return fronts;
  }

  @Override
  public boolean remove(T t) {
    Optional<SequencedCollection<T>> oFront = fronts.stream().filter(f -> f.contains(t)).findAny();
    if (oFront.isPresent()) {
      oFront.get().remove(t);
      if (oFront.get().isEmpty()) {
        fronts.remove(oFront.get());
      }
      return true;
    }
    return false;
  }

}
