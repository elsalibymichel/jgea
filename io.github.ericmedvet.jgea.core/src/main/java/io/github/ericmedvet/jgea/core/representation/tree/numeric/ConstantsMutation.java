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
package io.github.ericmedvet.jgea.core.representation.tree.numeric;

import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.core.representation.tree.TreeUtils;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Constant;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.List;
import java.util.random.RandomGenerator;

public class ConstantsMutation implements Mutation<Tree<Element>> {

  private final double sigma;

  public ConstantsMutation(double sigma) {
    this.sigma = sigma;
  }

  @Override
  public Tree<Element> mutate(Tree<Element> parent, RandomGenerator random) {
    List<Tree<Element>> constantSubtrees = parent.topSubtrees()
        .stream()
        .filter(st -> st.size() == 1)
        .filter(st -> st.content() instanceof Constant)
        .toList();
    if (constantSubtrees.isEmpty()) {
      return parent;
    }
    Tree<Element> toReplaceConstant = Misc.pickRandomly(constantSubtrees, random);
    Tree<Element> newConstant = Tree.of(
        new Constant(
            ((Constant) toReplaceConstant.content()).value() * (1d + random.nextGaussian() * sigma)
        )
    );
    return TreeUtils.replaceFirst(parent, toReplaceConstant, newConstant);
  }
}
