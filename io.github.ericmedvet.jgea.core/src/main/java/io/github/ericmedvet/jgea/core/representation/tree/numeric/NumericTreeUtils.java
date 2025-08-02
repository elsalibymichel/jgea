/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
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

import io.github.ericmedvet.jgea.core.IndependentFactory;
import io.github.ericmedvet.jgea.core.representation.tree.RampedHalfAndHalf;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Constant;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Operator;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Variable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
      if (operator.equals(Element.Operator.ADDITION) || operator.equals(
          Element.Operator.MULTIPLICATION
      )) {
        if (commutationEquals(t1.child(0), t2.child(0)) && commutationEquals(
            t1.child(1),
            t2.child(1)
        )) {
          return true;
        }
        if (commutationEquals(t1.child(0), t2.child(1)) && commutationEquals(
            t1.child(1),
            t2.child(0)
        )) {
          return true;
        }
        return false;
      }
      if (operator.arity() == 1) {
        return commutationEquals(t1.child(0), t2.child(0));
      }
      if (operator.arity() == 2) {
        return commutationEquals(t1.child(0), t2.child(0)) && commutationEquals(
            t1.child(1),
            t2.child(1)
        );
      }
      return IntStream.range(0, operator.arity())
          .allMatch(i -> commutationEquals(t1.child(i), t2.child(i)));
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
        return simplify(
            Tree.of(
                new Element.Constant(
                    operator.applyAsDouble(
                        tree.childStream().mapToDouble(c -> ((Element.Constant) c.content()).value()).toArray()
                    )
                )
            )
        );
      }
      // x + 0 -> x; 0 + x -> x
      if (operator.equals(Element.Operator.ADDITION)) {
        if (hasValue(tree.child(0), 0)) {
          return simplify(tree.child(1));
        }
        if (hasValue(tree.child(1), 0)) {
          return simplify(tree.child(0));
        }
      }
      // x - 0 -> x
      if (operator.equals(Element.Operator.SUBTRACTION)) {
        if (hasValue(tree.child(1), 0)) {
          return simplify(tree.child(0));
        }
      }
      // x - x -> 0
      if (operator.equals(Element.Operator.SUBTRACTION)) {
        if (commutationEquals(tree.child(0), tree.child(1))) {
          return simplify(Tree.of(new Element.Constant(0)));
        }
      }
      // x * 0 -> x; 0 * x -> x
      if (operator.equals(Element.Operator.MULTIPLICATION)) {
        if (hasValue(tree.child(0), 1)) {
          return simplify(tree.child(1));
        }
        if (hasValue(tree.child(1), 1)) {
          return simplify(tree.child(0));
        }
      }
      // x / 1 -> x
      if (operator.equals(Element.Operator.DIVISION) || operator.equals(
          Element.Operator.PROT_DIVISION
      )) {
        if (hasValue(tree.child(1), 1)) {
          return simplify(tree.child(0));
        }
      }
      // x / x -> 1
      if (operator.equals(Element.Operator.DIVISION) || operator.equals(
          Element.Operator.PROT_DIVISION
      )) {
        if (commutationEquals(tree.child(0), tree.child(1))) {
          return simplify(Tree.of(new Element.Constant(1)));
        }
      }
      // inv inv (x) -> x
      if (operator.equals(Element.Operator.INVERSE) && tree.child(0)
          .content()
          .equals(Element.Operator.INVERSE)) {
        return simplify(tree.child(0).child(0));
      }
      // - - (x) -> x
      if (operator.equals(Element.Operator.OPPOSITE) && tree.child(0)
          .content()
          .equals(Element.Operator.OPPOSITE)) {
        return simplify(tree.child(0).child(0));
      }
      // exp log (x) -> x
      if (operator.equals(Element.Operator.EXP) && (tree.child(0)
          .content()
          .equals(Element.Operator.LOG) || tree.child(0)
              .content()
              .equals(Element.Operator.PROT_LOG))) {
        return simplify(tree.child(0).child(0));
      }
      // log exp (x) -> x
      if ((operator.equals(Element.Operator.LOG) || operator.equals(Element.Operator.PROT_LOG)) && tree.child(0)
          .content()
          .equals(Element.Operator.EXP)) {
        return simplify(tree.child(0).child(0));
      }
      // sq sqrt (x) -> x
      if ((operator.equals(Element.Operator.SQ)) && tree.child(0)
          .content()
          .equals(Operator.SQRT)) {
        return simplify(tree.child(0).child(0));
      }
      // sqrt sq (x) -> x
      if ((operator.equals(Element.Operator.SQRT)) && tree.child(0)
          .content()
          .equals(Operator.SQ)) {
        return simplify(tree.child(0).child(0));
      }
      // x + x -> 2 x
      if (operator.equals(Element.Operator.ADDITION)) {
        if (commutationEquals(tree.child(0), tree.child(1))) {
          return simplify(
              Tree.of(
                  Element.Operator.MULTIPLICATION,
                  List.of(Tree.of(new Element.Constant(2d)), tree.child(0))
              )
          );
        }
      }
      // (x + y) - x -> y; (y + x) - x -> y
      if (operator.equals(Element.Operator.SUBTRACTION)) {
        if (tree.child(0).content().equals(Element.Operator.ADDITION)) {
          if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
            return simplify(tree.child(0).child(1));
          }
          if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
            return simplify(tree.child(0).child(0));
          }
        }
      }
      // x - (- y) -> x + y
      if (operator.equals(Element.Operator.SUBTRACTION)) {
        if (tree.child(1).content().equals(Operator.OPPOSITE)) {
          return simplify(
              Tree.of(Operator.ADDITION, List.of(tree.child(0), tree.child(1).child(0)))
          );
        }
      }
      // x + (y - x) -> y; (y - x) + x -> y
      if (operator.equals(Operator.ADDITION)) {
        if (tree.child(1).content().equals(Operator.SUBTRACTION)) {
          if (commutationEquals(tree.child(1).child(1), tree.child(0))) {
            return simplify(tree.child(1).child(0));
          }
        }
        if (tree.child(0).content().equals(Operator.SUBTRACTION)) {
          if (commutationEquals(tree.child(0).child(1), tree.child(0))) {
            return simplify(tree.child(0).child(0));
          }
        }
      }
      // (x * y) / x -> y; (y * x) / x -> y
      if (operator.equals(Element.Operator.DIVISION) || operator.equals(
          Element.Operator.PROT_DIVISION
      )) {
        if (tree.child(0).content().equals(Element.Operator.MULTIPLICATION)) {
          if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
            return simplify(tree.child(0).child(1));
          }
          if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
            return simplify(tree.child(0).child(0));
          }
        }
      }
      // (k * x) + x -> (1 + k) * x; (x * k) + x -> (1 + k) * x (and commuted)
      if (operator.equals(Element.Operator.ADDITION)) {
        if (tree.child(0).content().equals(Element.Operator.MULTIPLICATION)) {
          if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
            return simplify(
                Tree.of(
                    Element.Operator.MULTIPLICATION,
                    List.of(
                        Tree.of(
                            Element.Operator.ADDITION,
                            List.of(Tree.of(new Element.Constant(1)), tree.child(0).child(0))
                        ),
                        tree.child(1)
                    )
                )
            );
          }
          if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
            return simplify(
                Tree.of(
                    Element.Operator.MULTIPLICATION,
                    List.of(
                        Tree.of(
                            Element.Operator.ADDITION,
                            List.of(Tree.of(new Element.Constant(1)), tree.child(0).child(1))
                        ),
                        tree.child(1)
                    )
                )
            );
          }
        }
        if (tree.child(1).content().equals(Element.Operator.MULTIPLICATION)) {
          if (commutationEquals(tree.child(1).child(1), tree.child(0))) {
            return simplify(
                Tree.of(
                    Element.Operator.MULTIPLICATION,
                    List.of(
                        Tree.of(
                            Element.Operator.ADDITION,
                            List.of(Tree.of(new Element.Constant(1)), tree.child(1).child(0))
                        ),
                        tree.child(0)
                    )
                )
            );
          }
          if (commutationEquals(tree.child(1).child(0), tree.child(0))) {
            return simplify(
                Tree.of(
                    Element.Operator.MULTIPLICATION,
                    List.of(
                        Tree.of(
                            Element.Operator.ADDITION,
                            List.of(Tree.of(new Element.Constant(1)), tree.child(1).child(1))
                        ),
                        tree.child(0)
                    )
                )
            );
          }
        }
      }
    }
    while (true) {
      Tree<Element> simplified = Tree.of(
          tree.content(),
          tree.childStream().map(NumericTreeUtils::simplify).toList()
      );
      if (simplified.equals(tree)) {
        return simplified;
      }
      tree = simplify(simplified);
    }
  }

  public static Tree<Element> parse(String string) {
    return new StringParser(string).parse();
  }

  private record StringParser(String s) {

    private final static String CONSTANT_REGEX = "-?[0-9]+(\\.[0-9]+)?";
    private final static String VAR_REGEX = "[A-Za-z][A-Za-z0-9_]*";
    public static final String VOID_REGEX = "\\s*";

    public Tree<Element> parse() {
      try {
        return parseNode(0).element;
      } catch (ParseException e) {
        throw new RuntimeException("Cannot parse '%s': %s".formatted(s, e), e);
      }
    }

    private record ParsedNode(Tree<Element> element, int start, int end) {

      @SafeVarargs
      private ParsedNode(Matcher matcher, Element element, Tree<Element>... children) {
        this(Tree.of(element, List.of(children)), matcher.start(), matcher.end());
      }
    }

    private static class ParseException extends Exception {

      public ParseException(int i, Class<? extends Element> elementType) {
        super("Node %s not found at %d".formatted(elementType, i));
      }
    }

    private String content(Matcher matcher) {
      return s.substring(matcher.start(), matcher.end())
          .replaceAll("\\A" + VOID_REGEX, "")
          .replaceAll(VOID_REGEX + "\\z", "");
    }

    private Matcher findAt(int i, String regex) throws ParseException {
      Matcher matcher = Pattern.compile(VOID_REGEX + regex + VOID_REGEX).matcher(s);
      if (!matcher.find(i) || matcher.start() != i) {
        throw new ParseException(i, Constant.class);
      }
      return matcher;
    }

    private ParsedNode parseNode(int i) throws ParseException {
      try {
        return parseOperator(i);
      } catch (ParseException e) {
        // ignore
      }
      try {
        return parseVariable(i);
      } catch (ParseException e) {
        // ignore
      }
      try {
        return parseConstant(i);
      } catch (ParseException e) {
        // ignore
      }
      throw new ParseException(i, Element.class);
    }

    private ParsedNode parseConstant(int i) throws ParseException {
      Matcher m = findAt(i, CONSTANT_REGEX);
      return new ParsedNode(m, new Constant(Double.parseDouble(content(m))));
    }

    private ParsedNode parseVariable(int i) throws ParseException {
      Matcher m = findAt(i, VAR_REGEX);
      return new ParsedNode(m, new Variable(content(m)));
    }

    private ParsedNode parseOperator(int i) throws ParseException {
      Matcher m;
      for (Operator operator : Operator.values()) {
        try {
          m = findAt(i, Pattern.quote(operator.toString()));
          m = findAt(m.end(), Pattern.quote(Tree.CHILDREN_START_DELIMITER));
          List<Tree<Element>> children = new ArrayList<>(operator.arity());
          int end = m.end();
          for (int j = 0; j < operator.arity(); j++) {
            ParsedNode child = parseNode(m.end());
            children.add(child.element);
            end = child.end;
            if (j < operator.arity() - 1) {
              m = findAt(child.end, Pattern.quote(Tree.CHILDREN_SEPARATOR));
            }
          }
          m = findAt(end, Pattern.quote(Tree.CHILDREN_END_DELIMITER));
          return new ParsedNode(Tree.of(operator, children), i, m.end());
        } catch (ParseException e) {
          //ignore
        }
      }
      throw new ParseException(i, Operator.class);
    }

  }

  public static void main(String[] args) {

    RandomGenerator rg = new Random(0);

    List<Map<String, Double>> dataset = IntStream.range(0, 10)
        .mapToObj(
            i -> Map.ofEntries(
                Map.entry("x", (double) rg.nextInt(10)),
                Map.entry("y", (double) rg.nextInt(10)),
                Map.entry("z", (double) rg.nextInt(10))
            )
        )
        .toList();

    Consumer<String> checker = s -> {
      TreeBasedUnivariateRealFunction origUrf = new TreeBasedUnivariateRealFunction(
          parse(s),
          List.of("x", "y"),
          "z",
          false
      );
      TreeBasedUnivariateRealFunction simplUrf = new TreeBasedUnivariateRealFunction(
          parse(s),
          List.of("x", "y"),
          "z",
          true
      );
      System.out.printf(
          "%s -> %s -> %s -> %d/%d%n",
          s,
          parse(s),
          simplify(parse(s)),
          dataset.stream().filter(o -> simplUrf.compute(o).equals(origUrf.compute(o))).count(),
          dataset.size()
      );
      dataset.stream()
          .filter(o -> !simplUrf.compute(o).equals(origUrf.compute(o)))
          .forEach(
              o -> System.out.printf(
                  "\t%s -> simp=%f orig=%f%n",
                  o,
                  simplUrf.computeAsDouble(o),
                  origUrf.computeAsDouble(o)
              )
          );
    };
    checker.accept("-(²(÷(5.0;2.0));_(+(x;x)))");
    checker.accept("+(x;1)");
    checker.accept("+(x;x)");
    checker.accept("+(x;-(1;x))");
    checker.accept("*(x;log(exp(1)))");

    RampedHalfAndHalf<? extends Serializable> factory = new RampedHalfAndHalf<>(
        2,
        8,
        e -> switch (e) {
          case Operator operator -> operator.arity();
          default -> 0;
        },
        IndependentFactory.picker(Operator.values()),
        IndependentFactory.oneOf(
            IndependentFactory.picker(new Constant(1d), new Constant(2d), new Constant(5d)),
            IndependentFactory.picker(new Variable("x"), new Variable("y"))
        )
    );

    System.out.println("\nFactory!");
    factory.build(100, rg).forEach(t -> checker.accept(t.toString()));
  }

}
