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
package io.github.ericmedvet.jgea.experimenter.listener.plot;

import io.github.ericmedvet.jnb.datastructure.*;
import io.github.ericmedvet.jviz.core.plot.Value;
import io.github.ericmedvet.jviz.core.plot.XYDataSeries;
import io.github.ericmedvet.jviz.core.plot.XYDataSeries.Point;
import io.github.ericmedvet.jviz.core.plot.XYDataSeriesPlot;
import io.github.ericmedvet.jviz.core.plot.XYPlot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ScatterMRPAF<E, R, K, X> extends AbstractMultipleRPAF<E, XYDataSeriesPlot, R, List<XYDataSeries>, K, Map<K, Map<R, List<XYDataSeries.Point>>>> {

  private final Function<? super R, ? extends K> groupFunction;
  private final Function<? super E, ? extends Number> xFunction;
  private final Function<? super E, ? extends Number> yFunction;
  private final Function<? super E, X> predicateValueFunction;
  private final Predicate<? super X> predicate;
  private final UnaryOperator<List<XYDataSeries.Point>> rFilter;
  private final DoubleRange xRange;
  private final DoubleRange yRange;

  public ScatterMRPAF(
      Function<? super R, ? extends K> xSubplotFunction,
      Function<? super R, ? extends K> ySubplotFunction,
      Function<? super R, ? extends K> groupFunction,
      Function<? super E, ? extends Number> xFunction,
      Function<? super E, ? extends Number> yFunction,
      Function<? super E, X> predicateValueFunction,
      Predicate<? super X> predicate,
      UnaryOperator<List<XYDataSeries.Point>> rFilter,
      DoubleRange xRange,
      DoubleRange yRange
  ) {
    super(xSubplotFunction, ySubplotFunction);
    this.groupFunction = groupFunction;
    this.xFunction = xFunction;
    this.yFunction = yFunction;
    this.predicateValueFunction = predicateValueFunction;
    this.predicate = predicate;
    this.rFilter = rFilter;
    this.xRange = xRange;
    this.yRange = yRange;
  }

  @Override
  protected int size(Map<K, Map<R, List<Point>>> kMap) {
    return kMap.values()
        .stream()
        .mapToInt(
            rMap -> rMap.values()
                .stream()
                .mapToInt(List::size)
                .sum()
        )
        .sum();
  }

  @Override
  protected List<XYDataSeries> buildData(K xK, K yK, Map<K, Map<R, List<XYDataSeries.Point>>> map) {
    return map.entrySet()
        .stream()
        .map(
            entry -> XYDataSeries.of(
                FormattedFunction.format(groupFunction).formatted(entry.getKey()),
                entry.getValue()
                    .values()
                    .stream()
                    .map(rFilter)
                    .flatMap(Collection::stream)
                    .toList()
            )
        )
        .toList();
  }

  @Override
  protected XYDataSeriesPlot buildPlot(Table<K, K, List<XYDataSeries>> data) {
    Grid<XYPlot.TitledData<List<XYDataSeries>>> grid = Grid.create(
        data.nColumns(),
        data.nRows(),
        (x, y) -> new XYPlot.TitledData<>(
            FormattedFunction.format(xSubplotFunction)
                .formatted(data.colIndexes().get(x)),
            FormattedFunction.format(ySubplotFunction)
                .formatted(data.rowIndexes().get(y)),
            data.get(x, y)
        )
    );
    String subtitle = "";
    if (grid.w() > 1 && grid.h() == 1) {
      subtitle = "→ %s".formatted(NamedFunction.name(xSubplotFunction));
    } else if (grid.w() == 1 && grid.h() > 1) {
      subtitle = "↓ %s".formatted(NamedFunction.name(ySubplotFunction));
    } else if (grid.w() > 1 && grid.h() > 1) {
      subtitle = "→ %s, ↓ %s".formatted(
          NamedFunction.name(xSubplotFunction),
          NamedFunction.name(ySubplotFunction)
      );
    }
    return new XYDataSeriesPlot(
        "%s vs. %s%s"
            .formatted(
                NamedFunction.name(yFunction),
                NamedFunction.name(xFunction),
                subtitle.isEmpty() ? subtitle : (" (%s)".formatted(subtitle))
            ),
        NamedFunction.name(xSubplotFunction),
        NamedFunction.name(ySubplotFunction),
        NamedFunction.name(xFunction),
        NamedFunction.name(yFunction),
        xRange,
        yRange,
        grid
    );
  }

  @Override
  protected Map<K, Map<R, List<XYDataSeries.Point>>> init(K xK, K yK) {
    return new HashMap<>();
  }

  @Override
  protected Map<K, Map<R, List<XYDataSeries.Point>>> update(
      K xK,
      K yK,
      Map<K, Map<R, List<XYDataSeries.Point>>> map,
      E e,
      R r
  ) {
    X predicateValue = predicateValueFunction.apply(e);
    if (predicate.test(predicateValue)) {
      K groupK = groupFunction.apply(r);
      map.computeIfAbsent(groupK, k -> new HashMap<>())
          .computeIfAbsent(r, thisR -> new ArrayList<>())
          .add(
              new XYDataSeries.Point(
                  Value.of(xFunction.apply(e).doubleValue()),
                  Value.of(yFunction.apply(e).doubleValue())
              )
          );
    }
    return map;
  }

  @Override
  public String toString() {
    return "scatterMRPAF(xFunction=" + xFunction + ";yFunction=" + yFunction + ')';
  }

}
