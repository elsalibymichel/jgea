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
package io.github.ericmedvet.jgea.experimenter;

import io.github.ericmedvet.jgea.core.IndependentFactory;
import io.github.ericmedvet.jgea.core.representation.tree.GrowTreeBuilder;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.core.representation.tree.bool.BooleanTreeUtils;
import io.github.ericmedvet.jgea.core.representation.tree.bool.Element;
import io.github.ericmedvet.jgea.experimenter.drawer.TreeDrawer;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class Main {
  public static void main(String[] args) {
    GrowTreeBuilder<Element> builder = new GrowTreeBuilder<>(
        e -> switch (e) {
          case Element.Operator o -> o.arity();
          default -> 0;
        },
        IndependentFactory.picker(
            List.of(
                Element.Operator.AND,
                Element.Operator.OR,
                Element.Operator.NOT
            )
        ),
        IndependentFactory.picker(
            List.of(
                new Element.Variable(0),
                new Element.Variable(1)
            )
        )
    );
    RandomGenerator rg = new Random(1);
    List<Tree<Element>> trees = IntStream.range(0, 3).mapToObj(i -> builder.build(rg, 8)).toList();
    System.out.println(trees);
    Drawer<Tree<Element>> treeDrawer = (Drawer) (new TreeDrawer(TreeDrawer.Configuration.DEFAULT.scaled(2)));
    Drawer<List<Tree<Element>>> drawer = Drawer.stacked(
        List.of(
            treeDrawer.multi(Drawer.Arrangement.HORIZONTAL),
            treeDrawer.on(BooleanTreeUtils::simplify).multi(Drawer.Arrangement.HORIZONTAL)
        ),
        Drawer.Arrangement.VERTICAL
    );
    drawer.show(trees);
  }
}
