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
package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Operator;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.NumericTreeUtils;
import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.control.geometry.Rectangle;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Objects;

public class FormulaDrawer implements Drawer<Tree<Element>> {

  private final Configuration c;

  public FormulaDrawer(Configuration configuration) {
    this.c = configuration;
  }

  private static Rectangle at(Point center, Point size) {
    return new Rectangle(center, size.x(), size.y());
  }

  private static Rectangle atMinX(double minX, double centerY, Rectangle r) {
    return new Rectangle(new Point(minX + r.width() / 2, centerY), r.width(), r.height());
  }

  private static boolean hasParentheses(Element.Operator o, Tree<Element> child) {
    if (child.nChildren() == 0) {
      return false;
    }
    if (o.equals(Operator.ADDITION)) {
      return false;
    }
    // TODO complete
    return true;
  }

  private static boolean isConstant(Tree<Element> t) {
    return t.content() instanceof Element.Constant;
  }

  public static void main(String[] args) {
    Tree<Element> t = NumericTreeUtils.parse("*(+(*(1;sin(x));+(1;²(2)));*(x;÷(5;2)))");
    new TreeDrawer(TreeDrawer.Configuration.DEFAULT.scaled(10)).show(t);
    new FormulaDrawer(Configuration.DEFAULT.scaled(10)).show(t);
  }

  @Override
  public void draw(Graphics2D g, Tree<Element> tree) {
    java.awt.Rectangle clipBounds = g.getClipBounds();
    g.setFont(new Font("Monospaced", Font.PLAIN, (int) c.charH));
    draw(
        g,
        Rectangle.of(
            new io.github.ericmedvet.jsdynsym.control.geometry.Point(
                clipBounds.getMinX(),
                clipBounds.getMinY()
            ).sum(
                new io.github.ericmedvet.jsdynsym.control.geometry.Point(c.margin, c.margin)
            ),
            new io.github.ericmedvet.jsdynsym.control.geometry.Point(
                clipBounds.getMaxX(),
                clipBounds.getMaxY()
            ).diff(new Point(c.margin, c.margin))
        ),
        tree
    );
  }

  private Rectangle draw(Graphics2D g, Rectangle r, Tree<Element> t) {
    Rectangle innerR = switch (t.content()) {
      case Element.Variable v -> {
        Rectangle nodeR = at(r.center(), stringSize(v.name()));
        if (Objects.nonNull(g)) {
          g.setColor(c.varBGColor);
          g.fill(TreeDrawer.drawable(nodeR));
          TreeDrawer.drawString(g, nodeR.center(), c.varFGColor, c.charH, c.debug, v.name());
        }
        yield nodeR;
      }
      case Element.Constant n -> {
        Rectangle nodeR = at(r.center(), stringSize(n.toString()));
        if (Objects.nonNull(g)) {
          TreeDrawer.drawString(g, nodeR.center(), c.constColor, c.charH, c.debug, n.toString());
        }
        yield nodeR;
      }
      case Element.Operator o -> {
        Rectangle opR = at(r.center(), stringSize(switch (o) {
          case MULTIPLICATION -> "·";
          default -> o.toString();
        }));
        List<Rectangle> childRs = t.childStream().map(child -> {
          Rectangle childR = draw(null, r, child);
          if (hasParentheses(o, child)) {
            childR = new Rectangle(
                childR.center(),
                childR.width() + c.parenthesisWHRate * childR.height() * 2d,
                childR.height() * (1 + c.parenthesisWRate)
            );
          }
          return childR;
        }).toList();
        double childrenW = childRs.stream().mapToDouble(Rectangle::width).sum();
        double w = switch (o) {
          case MULTIPLICATION -> {
            int nOfDots = 0;
            for (int i = 1; i < t.nChildren(); i = i + 1) {
              if (isConstant(t.child(i - 1)) && isConstant(t.child(i))) {
                nOfDots = nOfDots + 1;
              }
            }
            yield (opR.width() + 2 * c.operatorXGap) * nOfDots + c.operatorXGap * (t
                .nChildren() - 1 - nOfDots) + childrenW;
          }
          case ADDITION, SUBTRACTION, GT, LT ->
            (opR.width() + 2 * c.operatorXGap) * (t.nChildren() - 1) + childrenW;
          default -> opR.width() + c.operatorXGap * t.nChildren() + childrenW;
        };
        double h = Math.max(
            opR.height(),
            childRs.stream().mapToDouble(Rectangle::height).max().orElse(0)
        );
        if (Objects.nonNull(g)) {
          double x = r.min().x() + (r.width() - w) / 2d;
          switch (o) {
            case MULTIPLICATION -> {
              for (int i = 0; i < t.nChildren(); i = i + 1) {
                if (i > 0) {
                  if (isConstant(t.child(i - 1)) && isConstant(t.child(i))) {
                    drawOp(g, x + opR.width() / 2d + c.operatorXGap, r.center().y(), "·");
                    x = x + opR.width() + c.operatorXGap;
                  }
                  x = x + c.operatorXGap;
                }
                Rectangle childR = atMinX(x, r.center().y(), childRs.get(i));
                drawChild(g, childR, o, t.child(i));
                x = x + childR.width();
              }
            }
            case ADDITION, SUBTRACTION, GT, LT -> {
              for (int i = 0; i < t.nChildren(); i = i + 1) {
                if (i > 0) {
                  drawOp(g, x + opR.width() / 2d + c.operatorXGap, r.center().y(), o.toString());
                  x = x + opR.width() + 2 * c.operatorXGap;
                }
                Rectangle childR = atMinX(x, r.center().y(), childRs.get(i));
                drawChild(g, childR, o, t.child(i));
                x = x + childR.width();
              }
            }
            default -> {
              drawOp(g, x + opR.width() / 2d, r.center().y(), o.toString());
              x = x + opR.width() + c.operatorXGap;
              for (int i = 0; i < t.nChildren(); i = i + 1) {
                Rectangle childR = atMinX(x, r.center().y(), childRs.get(i));
                drawChild(g, childR, o, t.child(i));
                x = x + childR.width() + c.operatorXGap;
              }
            }
          }
        }
        yield new Rectangle(r.center(), w, h);
      }
      default ->
        throw new IllegalArgumentException("Unexpected node type: %s".formatted(t.content()));
    };
    if (Objects.nonNull(g) && c.debug) {
      TreeDrawer.drawDebugBox(g, innerR);
    }
    return innerR;
  }

  private void drawChild(Graphics2D g, Rectangle childR, Operator o, Tree<Element> child) {
    draw(g, childR, child);
    if (hasParentheses(o, child)) {
      drawParenthesis(
          g,
          Rectangle.of(
              childR.min(),
              childR.min().sum(new Point(childR.height() * c.parenthesisWHRate, childR.height()))
          ),
          true
      );
      drawParenthesis(
          g,
          Rectangle.of(
              childR.max().diff(new Point(childR.height() * c.parenthesisWHRate, childR.height())),
              childR.max()
          ),
          false
      );
    }
  }

  private void drawOp(Graphics2D g, double x, double y, String s) {
    TreeDrawer.drawString(g, new Point(x, y), c.operatorColor, c.charH, c.debug, s);
  }

  private void drawParenthesis(Graphics2D g, Rectangle r, boolean open) {
    g.setStroke(new BasicStroke((float) c.fractionThickness));
    g.setColor(c.parenthesisColor);
    Path2D path = new Path2D.Double();
    if (open) {
      path.moveTo(r.max().x(), r.min().y());
      path.lineTo(r.min().x(), r.min().y());
      path.lineTo(r.min().x(), r.max().y());
      path.lineTo(r.max().x(), r.max().y());
    } else {
      path.moveTo(r.min().x(), r.min().y());
      path.lineTo(r.max().x(), r.min().y());
      path.lineTo(r.max().x(), r.max().y());
      path.lineTo(r.min().x(), r.max().y());
    }
    g.draw(path);
  }

  @Override
  public ImageInfo imageInfo(Tree<Element> tree) {
    Rectangle r = draw(null, new Rectangle(Point.ORIGIN, 1, 1), tree);
    return new ImageInfo(
        (int) Math.ceil(r.width() + c.margin * 2d),
        (int) Math.ceil(r.height() + c.margin * 2d)
    );
  }

  private Point stringSize(String s) {
    return new Point(
        s.lines().mapToDouble(String::length).max().orElse(0) * c.charW,
        s.lines().count() * c.charH
    );
  }

  public record Configuration(
      double margin, double fractionThickness, double parenthesisThickness,
      Color operatorColor, Color constColor, Color varFGColor,
      Color varBGColor, Color parenthesisColor, double charW, double charH,
      double fractionYGap, double operatorXGap, double powerRaiseRate,
      double parenthesisWHRate, double parenthesisWRate, boolean debug
  ) {

    public static Configuration DEFAULT = new Configuration(
        5,
        1,
        0.5,
        Color.DARK_GRAY,
        Color.BLUE,
        Color.BLACK,
        Color.LIGHT_GRAY,
        Color.DARK_GRAY,
        10,
        16,
        2,
        3,
        0.5,
        0.1,
        0.15,
        true
    );

    public Configuration scaled(double r) {
      return new Configuration(
          margin * r,
          fractionThickness * r,
          parenthesisThickness * r,
          operatorColor,
          constColor,
          varFGColor,
          varBGColor,
          parenthesisColor,
          charW * r,
          charH * r,
          fractionYGap * r,
          operatorXGap * r,
          powerRaiseRate,
          parenthesisWHRate,
          parenthesisWRate,
          debug
      );
    }
  }

}