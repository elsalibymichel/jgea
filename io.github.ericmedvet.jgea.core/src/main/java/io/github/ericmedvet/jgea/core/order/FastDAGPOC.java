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
import java.util.stream.Collectors;

public class FastDAGPOC<T> extends AbstractPartiallyOrderedCollection<T> {

  private final Map<Integer, Node<T>> nodeMap;
  private final Map<T, Integer> contentMap;
  private final Map<Integer, Integer> nodeFrontMap;
  private final SortedMap<Integer, Set<Integer>> frontMap;
  private List<Collection<T>> precomputedFronts;
  private int maxId;

  public FastDAGPOC(PartialComparator<? super T> partialComparator) {
    super(partialComparator);
    nodeMap = new LinkedHashMap<>();
    contentMap = new LinkedHashMap<>();
    nodeFrontMap = new LinkedHashMap<>();
    frontMap = new TreeMap<>();
    maxId = -1;
    precomputedFronts = null;
  }

  private record Node<T>(
      int id, SequencedCollection<T> contents, SequencedSet<Integer> beforeNodes, SequencedSet<Integer> afterNodes
  ) {
  }

  @Override
  public void add(T t) {
    Optional<Node<T>> existingNodeOptional = nodeMap.values()
        .stream()
        .filter(
            n -> n.contents.stream()
                .anyMatch(oT -> comparator().compare(oT, t).equals(PartialComparator.PartialComparatorOutcome.SAME))
        )
        .findAny();
    if (existingNodeOptional.isPresent()) {
      Node<T> existingNode = existingNodeOptional.get();
      existingNode.contents.add(t);
      contentMap.put(t, existingNode.id);
      return;
    }
    int newId = freeId();
    maxId = Math.max(maxId, newId);
    Node<T> newNode = new Node<>(
        newId,
        new ArrayList<>(),
        matchingIds(t, PartialComparator.PartialComparatorOutcome.BEFORE),
        matchingIds(t, PartialComparator.PartialComparatorOutcome.AFTER)
    );
    newNode.beforeNodes.forEach(id -> nodeMap.get(id).afterNodes.add(newNode.id));
    newNode.afterNodes.forEach(id -> nodeMap.get(id).beforeNodes.add(newNode.id));
    newNode.contents.add(t);
    nodeMap.put(newNode.id, newNode);
    contentMap.put(t, newId);
    updateNodeFront(newNode);
    newNode.afterNodes.forEach(id -> updateNodeFront(nodeMap.get(id)));
  }

  @Override
  public List<Collection<T>> fronts() {
    if (precomputedFronts == null) {
      precomputedFronts = frontMap.values()
          .stream()
          .map(ids -> (Collection<T>) ids.stream().flatMap(id -> nodeMap.get(id).contents.stream()).toList())
          .filter(nodes -> !nodes.isEmpty())
          .toList();
    }
    return precomputedFronts;
  }

  @Override
  public boolean remove(T t) {
    Integer previousId = contentMap.remove(t);
    if (previousId == null) {
      return false;
    }
    Node<T> node = nodeMap.get(previousId);
    node.contents.remove(t);
    if (node.contents.isEmpty()) {
      nodeMap.remove(node.id);
      int front = nodeFrontMap.remove(node.id);
      frontMap.get(front).remove(node.id);
      node.beforeNodes.forEach(id -> nodeMap.get(id).afterNodes.remove(node.id));
      node.afterNodes.forEach(id -> nodeMap.get(id).beforeNodes.remove(node.id));
      node.afterNodes.forEach(id -> updateNodeFront(nodeMap.get(id)));
    }
    precomputedFronts = null;
    return true;
  }

  private int freeId() {
    for (int i = 0; i <= maxId; i = i + 1) {
      if (!nodeMap.containsKey(i)) {
        return i;
      }
    }
    return maxId + 1;
  }

  private SequencedSet<Integer> matchingIds(T t, PartialComparator.PartialComparatorOutcome outcome) {
    return nodeMap.entrySet()
        .stream()
        .filter(e -> comparator().compare(e.getValue().contents().getFirst(), t).equals(outcome))
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private void updateNodeFront(Node<T> node) {
    int newFront = node.beforeNodes.stream().mapToInt(nodeFrontMap::get).max().orElse(-1) + 1;
    Integer oldFront = nodeFrontMap.put(node.id, newFront);
    if (oldFront == null || oldFront != newFront) {
      frontMap.computeIfAbsent(newFront, f -> new LinkedHashSet<>()).add(node.id);
      if (oldFront != null) {
        frontMap.get(oldFront).remove(node.id);
      }
      node.afterNodes.forEach(id -> updateNodeFront(nodeMap.get(id)));
      precomputedFronts = null;
    }
  }

}
