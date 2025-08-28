/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
package io.github.ericmedvet.jgea.experimenter;

import io.github.ericmedvet.jgea.core.order.*;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Grid;
import io.github.ericmedvet.jviz.core.plot.Value;
import io.github.ericmedvet.jviz.core.plot.XYDataSeries;
import io.github.ericmedvet.jviz.core.plot.XYDataSeriesPlot;
import io.github.ericmedvet.jviz.core.plot.XYPlot;
import io.github.ericmedvet.jviz.core.plot.image.LinesPlotDrawer;
import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class POCTester {

  private static final PartialComparator<BiChar> PARTIAL_COMPARATOR = new ParetoDominance<>(
      Collections.nCopies(2, Comparator.comparingInt((Character c) -> c))
  ).on(bc -> List.of(bc.c1, bc.c2));
  private static final char[] ALPHABET = "abcdefghilmnopqrstuvz".toCharArray();
  private static final Function<RandomGenerator, BiChar> SUPPLIER = rg -> new BiChar(
      ALPHABET[rg.nextInt(ALPHABET.length)],
      ALPHABET[rg.nextInt(ALPHABET.length)]
  );

  record BiChar(char c1, char c2) {
    @Override
    public String toString() {
      return c1 + "" + c2;
    }
  }

  public static void check() {
    int n = 100;
    RandomGenerator rg = new Random(2);
    PartiallyOrderedCollection<BiChar> slowPoc = new DAGPartiallyOrderedCollection<>(PARTIAL_COMPARATOR);
    PartiallyOrderedCollection<BiChar> fastPoc = new FastDAGPOC<>(PARTIAL_COMPARATOR);
    IntStream.range(0, n).forEach(i -> {
      BiChar element = SUPPLIER.apply(rg);
      slowPoc.add(element);
      fastPoc.add(element);
      //System.out.println("adding " + element);
      //System.out.printf("slow: %s%n", slowPoc);
      //System.out.printf("fast: %s%n", fastPoc);
    });
    IntStream.range(0, n).forEach(i -> {
      BiChar element = slowPoc.fronts().get(rg.nextInt(slowPoc.fronts().size())).iterator().next();
      slowPoc.remove(element);
      fastPoc.remove(element);
      List<Collection<BiChar>> slowFronts = slowPoc.fronts();
      List<Collection<BiChar>> fastFronts = fastPoc.fronts();
      if (slowFronts.size() != fastFronts.size()) {
        System.out.printf("Different num of fronts:%n\t%d%n\t%d%n", slowFronts.size(), fastFronts.size());
        System.exit(0);
      }
      IntStream.range(0, slowFronts.size()).forEach(f -> {
        if (!slowFronts.get(f).containsAll(fastFronts.get(f)) || !fastFronts.get(f).containsAll(slowFronts.get(f))) {
          System.out.printf("Different %d-th fronts:%n\t%s%n\t%s%n", f, slowFronts.get(f), fastFronts.get(f));
          System.exit(0);
        }
      });
    });
    System.out.println(slowPoc);
    System.out.println(fastPoc);
  }

  public static void main(String[] args) {
    profile();
  }

  public static void profile() {
    List<Integer> sizes = List.of(5, 10, 20, 30, 100, 200, 300, 400, 500, 1000);
    Map<String, Function<PartialComparator<BiChar>, PartiallyOrderedCollection<BiChar>>> pocBuilders = new TreeMap<>(
        Map.ofEntries(
            Map.entry("slow-dag", DAGPartiallyOrderedCollection::new),
            Map.entry("fast-dag", FastDAGPOC::new)
        )
    );
    int nOfOps = 10;
    RandomGenerator rg = new Random();
    Map<String, Map<Double, Double>> data = pocBuilders.keySet()
        .stream()
        .collect(
            Collectors.toMap(
                name -> name,
                name -> sizes.stream()
                    .collect(
                        Collectors.toMap(
                            size -> (double) size,
                            size -> profile(
                                pocBuilders.get(name),
                                SUPPLIER,
                                PARTIAL_COMPARATOR,
                                size,
                                nOfOps,
                                rg
                            )
                        )
                    )
            )
        );
    new LinesPlotDrawer().show(
        new XYDataSeriesPlot(
            "Time",
            "",
            "",
            "initial size",
            "time [s]",
            DoubleRange.UNBOUNDED,
            DoubleRange.UNBOUNDED,
            Grid.create(
                1,
                1,
                (gX, gY) -> new XYPlot.TitledData<>(
                    "",
                    "",
                    data.keySet()
                        .stream()
                        .map(
                            name -> XYDataSeries.of(
                                name,
                                new TreeMap<>(data.get(name)).keySet()
                                    .stream()
                                    .map(
                                        x -> new XYDataSeries.Point(
                                            Value.of(x),
                                            Value.of(data.get(name).get(x))
                                        )
                                    )
                                    .toList()
                            )
                        )
                        .toList()
                )
            )
        )
    );
  }

  private static <T> double profile(
      Function<PartialComparator<T>, PartiallyOrderedCollection<T>> pocBuilder,
      Function<RandomGenerator, T> supplier,
      PartialComparator<T> partialComparator,
      int size,
      int nOfOps,
      RandomGenerator rg
  ) {
    PartiallyOrderedCollection<T> poc = pocBuilder.apply(partialComparator);
    IntStream.range(0, size).forEach(i -> poc.add(supplier.apply(rg)));
    long startTime = System.currentTimeMillis();
    IntStream.range(0, nOfOps).forEach(i -> {
      //System.out.println(poc);
      // remove
      List<Collection<T>> fronts = poc.fronts();
      Collection<T> front = fronts.get(rg.nextInt(fronts.size()));
      T element = front.iterator().next();
      //System.out.printf("\tremove %s%n", element);
      poc.remove(element);
      //System.out.println(poc);
      // add
      T newElement = supplier.apply(rg);
      //System.out.printf("\tadd %s%n", newElement);
      poc.add(newElement);
    });
    return (double) (System.currentTimeMillis() - startTime) / 1000d;
  }

}
