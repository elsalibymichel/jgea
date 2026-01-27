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
/*
 * Copyright 2026 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;

public class TreeDrawer implements Drawer<Tree<?>> {

  private final Configuration configuration;

  public TreeDrawer(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void draw(Graphics2D g, Tree<?> tree) {
    // TODO
  }

  @Override
  public ImageInfo imageInfo(Tree<?> tree) {
    Size treeSize = treeSize(
        tree,
        s -> new Size(
            s.lines().mapToDouble(String::length).max().orElse(0) * configuration.charW,
            s.lines().count() * configuration.charH
        )
    );
    return new ImageInfo(
        (int) Math.ceil(treeSize.w + 2 * configuration.margin),
        (int) Math.ceil(treeSize.h + 2 * configuration.margin)
    );
  }

  private Size nodeSize(Tree<?> t, Function<String, Size> stringSizer) {
    Size sSize = stringSizer.apply(t.content().toString());
    return new Size(
        sSize.w + 2 * configuration.boxMargin,
        sSize.h + 2 * configuration.boxMargin
    );
  }

  private Size stringSize(Graphics2D g, String s) {
    Rectangle2D r = g.getFontMetrics().getStringBounds(s, g);
    return new Size(
        r.getWidth(),
        r.getHeight()
    );
  }

  private Size treeSize(Tree<?> t, Function<String, Size> stringSizer) {
    if (t.nChildren() == 0) {
      return nodeSize(t, stringSizer);
    }
    Size nodeSize = nodeSize(t, stringSizer);
    double w = 0;
    double h = 0;
    for (int i = 0; i < t.nChildren(); i = i + 1) {
      Size childSize = treeSize(t.child(i), stringSizer);
      w = w + childSize.w + ((i > 0) ? configuration.nodeMinXGap : 0);
      h = Math.max(h, childSize.h);
    }
    return new Size(
        Math.max(nodeSize.w, w),
        nodeSize.h + configuration.nodeMinYGap + h
    );
  }

  public record Configuration(
      double margin,
      double boxBorderThickness,
      double edgeThickness,
      Color boxBorderColor,
      Color boxBGColor,
      Color edgeColor,
      double charW,
      double charH,
      double boxMargin,
      double nodeMinXGap,
      double nodeMinYGap
  ) {

    public static Configuration DEFAULT = new Configuration(
        5,
        1,
        1,
        Color.BLACK,
        Color.LIGHT_GRAY,
        Color.DARK_GRAY,
        10,
        16,
        2,
        5,
        20
    );
  }

  private record Size(double w, double h) {

  }
}