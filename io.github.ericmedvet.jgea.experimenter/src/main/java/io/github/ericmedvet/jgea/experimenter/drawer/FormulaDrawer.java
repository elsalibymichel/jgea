package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.control.geometry.Rectangle;
import io.github.ericmedvet.jviz.core.drawer.Drawer;

import java.awt.*;
import java.util.Objects;

public class FormulaDrawer implements Drawer<Tree<Element>> {

  public record Configuration(
      double margin,
      double fractionThickness,
      double parenthesisThickness,
      Color operatorColor,
      Color constColor,
      Color varFGColor,
      Color varBGColor,
      double charW,
      double charH,
      double fractionYGap,
      double operatorXGap,
      double powerRaiseRate,
      double parenthesisWHRate,
      boolean debug
  ) {
    public static Configuration DEFAULT = new Configuration(
        5,
        2,
        1,
        Color.DARK_GRAY,
        Color.BLUE,
        Color.BLACK,
        Color.LIGHT_GRAY,
        10,
        16,
        2,
        3,
        0.5,
        0.1,
        false
    );
  }

  private final Configuration configuration;

  public FormulaDrawer(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void draw(Graphics2D g, Tree<Element> tree) {
    java.awt.Rectangle clipBounds = g.getClipBounds();
    g.setFont(new Font("Monospaced", Font.PLAIN, (int) configuration.charW));
    draw(
        g,
        Rectangle.of(
            new io.github.ericmedvet.jsdynsym.control.geometry.Point(clipBounds.getMinX(), clipBounds.getMinY()).sum(
                new io.github.ericmedvet.jsdynsym.control.geometry.Point(configuration.margin, configuration.margin)
            ),
            new io.github.ericmedvet.jsdynsym.control.geometry.Point(clipBounds.getMaxX(), clipBounds.getMaxY()).diff(
                new Point(configuration.margin, configuration.margin)
            )
        ),
        tree
    );
  }

  @Override
  public ImageInfo imageInfo(Tree<Element> elementTree) {
    // TODO
    return Drawer.super.imageInfo(elementTree);
  }

  private Rectangle draw(Graphics2D g, Rectangle r, Tree<Element> t) {
    return switch (t.content()) {
      case Element.Variable v -> {
        Point size = stringSize(v.name());
        Rectangle nodeR = new Rectangle(r.center(), size.x(), size.y());
        if (Objects.nonNull(g)) {
          g.setColor(configuration.varBGColor);
          g.fill(TreeDrawer.drawable(nodeR));
          TreeDrawer.drawString(g, nodeR.center(), configuration.varFGColor, configuration.charH, configuration.debug, v.name());
        }
        yield nodeR;
      }
      case Element.Constant c -> {
        String s = c.toString();
        Point size = stringSize(s);
        Rectangle nodeR = new Rectangle(r.center(), size.x(), size.y());
        if (Objects.nonNull(g)) {
          TreeDrawer.drawString(g, nodeR.center(), configuration.constColor, configuration.charH, configuration.debug, s);
        }
        yield nodeR;
      }
      default -> {
        yield null;
      }
    };
  }

  private Point stringSize(String s) {
    return new Point(
        s.lines().mapToDouble(String::length).max().orElse(0) * configuration.charW,
        s.lines().count() * configuration.charH
    );
  }

  private static boolean hasParentheses(Element.Operator op, Tree<Element> child) {
    // TODO implement
    return true;
  }

}