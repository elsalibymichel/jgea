package io.github.ericmedvet.jgea.core.representation.tree.numeric;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;

import java.util.List;
import java.util.stream.IntStream;

public class NumericTreeUtils {
  private NumericTreeUtils() {
  }

  private static boolean commutationEquals(Tree<Element> t1, Tree<Element> t2) {
    if (!t1.content().equals(t2.content())) {
      return false;
    }
    if (t1.nChildren() == 0 && t2.nChildren() == 0) {
      return true;
    }
    if (t1.content() instanceof Element.Operator operator) {
      if (operator.equals(Element.Operator.ADDITION) || operator.equals(Element.Operator.MULTIPLICATION)) {
        if (commutationEquals(t1.child(0), t2.child(0)) && commutationEquals(t1.child(1), t2.child(1))) {
          return true;
        }
        if (commutationEquals(t1.child(0), t2.child(1)) && commutationEquals(t1.child(1), t2.child(0))) {
          return true;
        }
        return false;
      }
      if (operator.arity() == 1) {
        return commutationEquals(t1.child(0), t2.child(0));
      }
      if (operator.arity() == 2) {
        return commutationEquals(t1.child(0), t2.child(0)) && commutationEquals(t1.child(1), t2.child(1));
      }
      return IntStream.range(0, operator.arity()).allMatch(i -> commutationEquals(t1.child(i), t2.child(i)));
    }
    return false;
  }

  private static boolean hasValue(Tree<Element> tree, double v) {
    if (tree.content() instanceof Element.Constant(double value)) {
      return value == v;
    }
    return false;
  }

  public static Tree<Element> simplify(Tree<Element> tree) {
    if (tree.nChildren() == 0) {
      return tree;
    }
    if (tree.content() instanceof Element.Operator operator) {
      // all constants
      if (tree.childStream().allMatch(c -> c.content() instanceof Element.Constant)) {
        return Tree.of(new Element.Constant(operator.applyAsDouble(
            tree.childStream().mapToDouble(c -> ((Element.Constant) c.content()).value()).toArray()
        )));
      }
      // x + 0 -> x; 0 + x -> x
      if (operator.equals(Element.Operator.ADDITION)) {
        if (hasValue(tree.child(0), 0)) {
          return tree.child(1);
        }
        if (hasValue(tree.child(1), 0)) {
          return tree.child(0);
        }
      }
      // x - 0 -> x
      if (operator.equals(Element.Operator.SUBTRACTION)) {
        if (hasValue(tree.child(1), 0)) {
          return tree.child(0);
        }
      }
      // x - x -> 0
      if (operator.equals(Element.Operator.SUBTRACTION)) {
        if (commutationEquals(tree.child(0), tree.child(1))) {
          return Tree.of(new Element.Constant(0));
        }
      }
      // x * 0 -> x; 0 * x -> x
      if (operator.equals(Element.Operator.MULTIPLICATION)) {
        if (hasValue(tree.child(0), 1)) {
          return tree.child(1);
        }
        if (hasValue(tree.child(1), 1)) {
          return tree.child(0);
        }
      }
      // x / 1 -> x
      if (operator.equals(Element.Operator.DIVISION) || operator.equals(Element.Operator.PROT_DIVISION)) {
        if (hasValue(tree.child(1), 1)) {
          return tree.child(0);
        }
      }
      // x / x -> 1
      if (operator.equals(Element.Operator.DIVISION) || operator.equals(Element.Operator.PROT_DIVISION)) {
        if (commutationEquals(tree.child(0), tree.child(1))) {
          return Tree.of(new Element.Constant(1));
        }
      }
      // inv inv (x) -> x
      if (operator.equals(Element.Operator.INVERSE) && tree.child(0).content().equals(Element.Operator.INVERSE)) {
        return tree.child(0).child(0);
      }
      // - - (x) -> x
      if (operator.equals(Element.Operator.OPPOSITE) && tree.child(0).content().equals(Element.Operator.OPPOSITE)) {
        return tree.child(0).child(0);
      }
      // exp log (x) -> x
      if (operator.equals(Element.Operator.EXP) && (tree.child(0)
          .content()
          .equals(Element.Operator.LOG) || tree.child(0).content().equals(Element.Operator.PROT_LOG))) {
        return tree.child(0).child(0);
      }
      // log exp (x) -> x
      if ((operator.equals(Element.Operator.LOG) || operator.equals(Element.Operator.PROT_LOG)) && tree.child(0)
          .content()
          .equals(Element.Operator.EXP)) {
        return tree.child(0).child(0);
      }
      // x + x -> 2 x
      if (operator.equals(Element.Operator.ADDITION)) {
        if (commutationEquals(tree.child(0), tree.child(1))) {
          return Tree.of(Element.Operator.MULTIPLICATION, List.of(Tree.of(new Element.Constant(2d)), tree.child(0)));
        }
      }
      // (x + y) - x -> y; (y + x) - x -> y
      if (operator.equals(Element.Operator.SUBTRACTION)) {
        if (tree.child(0).content().equals(Element.Operator.ADDITION)) {
          if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
            return tree.child(0).child(1);
          }
          if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
            return tree.child(0).child(0);
          }
        }
      }
      // (x * y) / x -> y; (y * x) / x -> y
      if (operator.equals(Element.Operator.DIVISION) || operator.equals(Element.Operator.PROT_DIVISION)) {
        if (tree.child(0).content().equals(Element.Operator.MULTIPLICATION)) {
          if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
            return tree.child(0).child(1);
          }
          if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
            return tree.child(0).child(0);
          }
        }
      }
      // (k * x) + x -> (1 + k) * x; (x * k) + x -> (1 + k) * x (and commuted)
      if (operator.equals(Element.Operator.ADDITION)) {
        if (tree.child(0).content().equals(Element.Operator.MULTIPLICATION)) {
          if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
            return Tree.of(
                Element.Operator.MULTIPLICATION, List.of(
                    Tree.of(
                        Element.Operator.ADDITION,
                        List.of(Tree.of(new Element.Constant(1)), tree.child(0).child(0))
                    ),
                    tree.child(1)
                )
            );
          }
          if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
            return Tree.of(
                Element.Operator.MULTIPLICATION, List.of(
                    Tree.of(
                        Element.Operator.ADDITION,
                        List.of(Tree.of(new Element.Constant(1)), tree.child(0).child(1))
                    ),
                    tree.child(1)
                )
            );
          }
        }
        if (tree.child(1).content().equals(Element.Operator.MULTIPLICATION)) {
          if (commutationEquals(tree.child(1).child(1), tree.child(0))) {
            return Tree.of(Element.Operator.MULTIPLICATION, List.of(
                Tree.of(Element.Operator.ADDITION, List.of(Tree.of(new Element.Constant(1)), tree.child(1).child(0))),
                tree.child(0)
            ));
          }
          if (commutationEquals(tree.child(1).child(0), tree.child(0))) {
            return Tree.of(Element.Operator.MULTIPLICATION, List.of(
                Tree.of(Element.Operator.ADDITION, List.of(Tree.of(new Element.Constant(1)), tree.child(1).child(1))),
                tree.child(0)
            ));
          }
        }
      }
    }
    while (true) {
      Tree<Element> simplified = Tree.of(tree.content(), tree.childStream().map(NumericTreeUtils::simplify).toList());
      if (simplified.equals(tree)) {
        return simplified;
      }
      tree = simplify(simplified);
    }
  }

}
