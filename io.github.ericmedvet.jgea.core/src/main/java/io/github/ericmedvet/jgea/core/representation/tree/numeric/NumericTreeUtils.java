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

import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Constant;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Operator;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element.Variable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NumericTreeUtils {

  private NumericTreeUtils() {
  }

  public enum SimplificationType { ALGEBRAIC, STRUCTURAL }

  private static List<Tree<Element>> operatorDescendants(Operator operator, Tree<Element> t) {
    if (t.content() instanceof Operator o) {
      if (o.equals(operator)) {
        return t.childStream().flatMap(c -> operatorDescendants(operator, c).stream()).toList();
      }
    }
    return List.of(t);
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
        if (t1.nChildren() != t2.nChildren()) {
          return false;
        }
        if (t1.nChildren() == 2) {
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
        List<Tree<Element>> t1Children = t1.childStream()
            .sorted(Comparator.comparing(Tree::toString))
            .toList();
        List<Tree<Element>> t2Children = t2.childStream()
            .sorted(Comparator.comparing(Tree::toString))
            .toList();
        for (int i = 0; i < t1Children.size(); i = i + 1) {
          if (!commutationEquals(t1Children.get(i), t2Children.get(i))) {
            return false;
          }
        }
        return true;
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

  private static boolean isNegative(Tree<Element> tree) {
    if (tree.content() instanceof Constant(double value)) {
      return value < 0;
    }
    return false;
  }

  private static boolean isPositive(Tree<Element> tree) {
    if (tree.content() instanceof Constant(double value)) {
      return value > 0;
    }
    if (tree.content() instanceof Operator o) {
      return switch (o) {
        case EXP, SQRT, SQ -> true;
        default -> false;
      };
    }
    return false;
  }

  public static Tree<Element> simplify(Tree<Element> tree) {
    return simplify(tree, EnumSet.of(SimplificationType.ALGEBRAIC, SimplificationType.STRUCTURAL));
  }

  public static Tree<Element> simplify(
      Tree<Element> tree,
      Set<SimplificationType> types
  ) {
    if (types.isEmpty()) {
      return tree;
    }
    if (tree.nChildren() == 0) {
      return tree;
    }
    // from here, algebraic simplification
    if (types.contains(SimplificationType.ALGEBRAIC)) {
      if (tree.content() instanceof Element.Operator operator) {
        // all constants
        if (tree.childStream().allMatch(c -> c.content() instanceof Element.Constant)) {
          return simplify(
              Tree.of(
                  new Element.Constant(
                      operator.applyAsDouble(
                          tree.childStream()
                              .mapToDouble(c -> ((Element.Constant) c.content()).value())
                              .toArray()
                      )
                  )
              ),
              types
          );
        }
        // some constants for + and *
        if (operator.equals(Operator.ADDITION) || operator.equals(Operator.MULTIPLICATION)) {
          SequencedMap<Boolean, List<Tree<Element>>> simplifiedChildren = tree.childStream()
              .map((Tree<Element> t) -> simplify(t, types))
              .collect(
                  Collectors.groupingBy(
                      c -> c.content() instanceof Element.Constant,
                      LinkedHashMap::new,
                      Collectors.toList()
                  )
              );
          if (simplifiedChildren.containsKey(true) && simplifiedChildren.get(true).size() > 1) {
            double reducedV = simplifiedChildren.get(true)
                .stream()
                .mapToDouble(c -> ((Constant) c.content()).value())
                .reduce((v1, v2) -> switch (operator) {
                  case ADDITION -> v1 + v2;
                  case MULTIPLICATION -> v1 * v2;
                  default -> throw new IllegalStateException(
                      "Unexpected operator %s instead of %s or %s".formatted(
                          operator,
                          Operator.ADDITION,
                          Operator.MULTIPLICATION
                      )
                  );
                })
                .orElseThrow();
            if (!simplifiedChildren.containsKey(false)) {
              return Tree.of(new Constant(reducedV), List.of());
            }
            return Tree.of(
                operator,
                Stream.concat(
                    simplifiedChildren.get(false).stream(),
                    Stream.of(Tree.<Element>of(new Constant(reducedV), List.of()))
                ).toList()
            );
          }
        }
        // x + 0 -> x; 0 + x -> x
        if (operator.equals(Element.Operator.ADDITION) && tree.nChildren() == 2) {
          if (hasValue(tree.child(0), 0)) {
            return simplify(tree.child(1), types);
          }
          if (hasValue(tree.child(1), 0)) {
            return simplify(tree.child(0), types);
          }
        }
        // x - 0 -> x
        if (operator.equals(Element.Operator.SUBTRACTION)) {
          if (hasValue(tree.child(1), 0)) {
            return simplify(tree.child(0), types);
          }
        }
        // x - x -> 0
        if (operator.equals(Element.Operator.SUBTRACTION)) {
          if (commutationEquals(tree.child(0), tree.child(1))) {
            return simplify(Tree.of(new Element.Constant(0)), types);
          }
        }
        // x * 1 -> x; 1 * x -> x
        if (operator.equals(Element.Operator.MULTIPLICATION) && tree.nChildren() == 2) {
          if (hasValue(tree.child(0), 1)) {
            return simplify(tree.child(1), types);
          }
          if (hasValue(tree.child(1), 1)) {
            return simplify(tree.child(0), types);
          }
        }
        // x / 1 -> x
        if (operator.equals(Element.Operator.DIVISION) || operator.equals(
            Element.Operator.PROT_DIVISION
        )) {
          if (hasValue(tree.child(1), 1)) {
            return simplify(tree.child(0), types);
          }
        }
        // x / x -> 1
        if (operator.equals(Element.Operator.DIVISION) || operator.equals(
            Element.Operator.PROT_DIVISION
        )) {
          if (commutationEquals(tree.child(0), tree.child(1))) {
            return simplify(Tree.of(new Element.Constant(1)), types);
          }
        }
        // inv inv (x) -> x
        if (operator.equals(Element.Operator.INVERSE) && tree.child(0)
            .content()
            .equals(Element.Operator.INVERSE)) {
          return simplify(tree.child(0).child(0), types);
        }
        // - - (x) -> x
        if (operator.equals(Element.Operator.OPPOSITE) && tree.child(0)
            .content()
            .equals(Element.Operator.OPPOSITE)) {
          return simplify(tree.child(0).child(0), types);
        }
        // exp log (x) -> x
        if (operator.equals(Element.Operator.EXP) && (tree.child(0)
            .content()
            .equals(Element.Operator.LOG) || tree.child(0)
                .content()
                .equals(Element.Operator.PROT_LOG))) {
          return simplify(tree.child(0).child(0), types);
        }
        // log exp (x) -> x
        if ((operator.equals(Element.Operator.LOG) || operator.equals(Element.Operator.PROT_LOG)) && tree.child(0)
            .content()
            .equals(Element.Operator.EXP)) {
          return simplify(tree.child(0).child(0), types);
        }
        // sq sqrt (x) -> x
        if ((operator.equals(Element.Operator.SQ)) && tree.child(0)
            .content()
            .equals(Operator.SQRT)) {
          return simplify(tree.child(0).child(0), types);
        }
        // ternary (pos; x; y) -> x, ternary (pos; x; neg) -> y
        if (operator.equals(Operator.TERNARY)) {
          if (isPositive(tree.child(0))) {
            return simplify(tree.child(1), types);
          }
          if (isNegative(tree.child(0))) {
            return simplify(tree.child(2), types);
          }
        }
        // sqrt sq (x) -> x
        if ((operator.equals(Element.Operator.SQRT)) && tree.child(0)
            .content()
            .equals(Operator.SQ)) {
          return simplify(tree.child(0).child(0), types);
        }
        // x + x -> 2 x
        if (operator.equals(Element.Operator.ADDITION) && tree.nChildren() == 2) {
          if (commutationEquals(tree.child(0), tree.child(1))) {
            return simplify(
                Tree.of(
                    Element.Operator.MULTIPLICATION,
                    List.of(Tree.of(new Element.Constant(2d)), tree.child(0))
                ),
                types
            );
          }
        }
        // (x + y) - x -> y; (y + x) - x -> y
        if (operator.equals(Element.Operator.SUBTRACTION)) {
          if (tree.child(0).content().equals(Element.Operator.ADDITION) && tree.child(0).nChildren() == 2) {
            if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
              return simplify(tree.child(0).child(1), types);
            }
            if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
              return simplify(tree.child(0).child(0), types);
            }
          }
        }
        // x - (- y) -> x + y
        if (operator.equals(Element.Operator.SUBTRACTION)) {
          if (tree.child(1).content().equals(Operator.OPPOSITE)) {
            return simplify(
                Tree.of(Operator.ADDITION, List.of(tree.child(0), tree.child(1).child(0))),
                types
            );
          }
        }
        // x + (y - x) -> y; (y - x) + x -> y
        if (operator.equals(Operator.ADDITION) && tree.nChildren() == 2) {
          if (tree.child(1).content().equals(Operator.SUBTRACTION)) {
            if (commutationEquals(tree.child(1).child(1), tree.child(0))) {
              return simplify(tree.child(1).child(0), types);
            }
          }
          if (tree.child(0).content().equals(Operator.SUBTRACTION)) {
            if (commutationEquals(tree.child(0).child(1), tree.child(0))) {
              return simplify(tree.child(0).child(0), types);
            }
          }
        }
        // (x * y) / x -> y; (y * x) / x -> y
        if (operator.equals(Element.Operator.DIVISION) || operator.equals(
            Element.Operator.PROT_DIVISION
        )) {
          if (tree.child(0).content().equals(Element.Operator.MULTIPLICATION) && tree.child(0).nChildren() == 2) {
            if (commutationEquals(tree.child(0).child(0), tree.child(1))) {
              return simplify(tree.child(0).child(1), types);
            }
            if (commutationEquals(tree.child(0).child(1), tree.child(1))) {
              return simplify(tree.child(0).child(0), types);
            }
          }
        }
        // (k * x) + x -> (1 + k) * x; (x * k) + x -> (1 + k) * x (and commuted)
        if (operator.equals(Element.Operator.ADDITION) && tree.nChildren() == 2) {
          if (tree.child(0).content().equals(Element.Operator.MULTIPLICATION) && tree.child(0).nChildren() == 2) {
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
                  ),
                  types
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
                  ),
                  types
              );
            }
          }
          if (tree.child(1).content().equals(Element.Operator.MULTIPLICATION) && tree.child(1).nChildren() == 2) {
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
                  ),
                  types
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
                  ),
                  types
              );
            }
          }
        }
      }
    }
    // here structural
    if (types.contains(SimplificationType.STRUCTURAL)) {
      if (tree.content() instanceof Operator o) {
        if (o.equals(Operator.ADDITION) || o.equals(Operator.MULTIPLICATION)) {
          List<Tree<Element>> reducedChildren = operatorDescendants(o, tree);
          if (reducedChildren.size() != tree.nChildren()) {
            return simplify(Tree.of(o, reducedChildren), types);
          }
        }
      }
    }
    while (true) {
      Tree<Element> simplified = Tree.of(
          tree.content(),
          tree.childStream().map((Tree<Element> t) -> simplify(t, types)).toList()
      );
      if (simplified.equals(tree)) {
        return simplified;
      }
      tree = simplify(simplified, types);
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

      public ParseException(int i, String message) {
        super("At %d: %s".formatted(i, message));
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
          if (!operator.isUnlimitedArity()) {
            for (int j = 0; j < operator.arity(); j++) {
              ParsedNode child = parseNode(m.end());
              children.add(child.element);
              end = child.end;
              if (j < operator.arity() - 1) {
                m = findAt(child.end, Pattern.quote(Tree.CHILDREN_SEPARATOR));
              }
            }
          } else {
            int j = m.end();
            while (true) {
              if (!children.isEmpty()) {
                try {
                  j = findAt(j, Pattern.quote(Tree.CHILDREN_SEPARATOR)).end();
                } catch (ParseException e) {
                  break;
                }
              }
              ParsedNode child = parseNode(j);
              children.add(child.element);
              j = child.end;
            }
            if (children.size() < operator.arity()) {
              throw new ParseException(
                  j,
                  "Not enough args for %s: %d < %d".formatted(
                      operator,
                      children.size(),
                      operator.arity()
                  )
              );
            }
            end = j;
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

}