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
package io.github.ericmedvet.jgea.core.representation.tree.bool;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import java.util.List;

public class BooleanTreeUtils {
  private BooleanTreeUtils() {
  }

  private static boolean isFalse(Element e) {
    if (e instanceof Element.Constant(boolean value)) {
      return !value;
    }
    return false;
  }

  private static boolean isTrue(Element e) {
    if (e instanceof Element.Constant(boolean value)) {
      return value;
    }
    return false;
  }

  public static Tree<Element> simplify(Tree<Element> tree) {
    //not(not(x)) -> x
    if (tree.content().equals(Element.Operator.NOT)) {
      if (tree.child(0).content().equals(Element.Operator.NOT)) {
        return simplify(tree.child(0).child(0));
      }
    }
    //and(x) -> x; or(x) -> x
    if (tree.content().equals(Element.Operator.AND) || tree.content().equals(Element.Operator.OR)) {
      if (tree.nChildren() == 1) {
        return simplify(tree.child(0));
      }
    }
    //and(x;x;...) -> and(x;...); and(T;...) -> and(...)
    if (tree.content().equals(Element.Operator.AND)) { // TODO merge children and
      List<Tree<Element>> reducedChildren = tree.childStream()
          .distinct()
          .filter(c -> !isTrue(c.content()))
          .toList();
      if (reducedChildren.size() < tree.nChildren()) {
        return simplify(Tree.of(Element.Operator.AND, reducedChildren));
      }
    }
    //or(x;x;...) -> or(x;...); or(F;...) -> or(...)
    if (tree.content().equals(Element.Operator.OR)) { // TODO merge children or
      List<Tree<Element>> reducedChildren = tree.childStream()
          .distinct()
          .filter(c -> !isFalse(c.content()))
          .toList();
      if (reducedChildren.size() < tree.nChildren()) {
        return simplify(Tree.of(Element.Operator.OR, reducedChildren));
      }
    }
    //and(F;...) -> F
    if (tree.content().equals(Element.Operator.AND)) {
      if (tree.childStream().anyMatch(c -> isFalse(c.content()))) {
        return Tree.of(new Element.Constant(false));
      }
    }
    //or(T;...) -> T
    if (tree.content().equals(Element.Operator.OR)) {
      if (tree.childStream().anyMatch(c -> isTrue(c.content()))) {
        return Tree.of(new Element.Constant(true));
      }
    }
    while (true) {
      Tree<Element> simplified = Tree.of(
          tree.content(),
          tree.childStream().map(BooleanTreeUtils::simplify).toList()
      );
      if (simplified.equals(tree)) {
        return simplified;
      }
      tree = simplify(simplified);
    }
  }
}
