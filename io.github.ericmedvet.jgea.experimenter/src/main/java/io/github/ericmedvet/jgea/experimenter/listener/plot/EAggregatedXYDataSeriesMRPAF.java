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

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class EAggregatedXYDataSeriesMRPAF<E, R, K> extends AggregatedXYDataSeriesMRPAF<E, R, K> {
  private final Function<? super E, ? extends Number> xFunction;

  public EAggregatedXYDataSeriesMRPAF(
      Function<? super R, ? extends K> xSubplotFunction,
      Function<? super R, ? extends K> ySubplotFunction,
      Function<? super R, ? extends K> lineFunction,
      Function<? super E, ? extends Number> yFunction,
      Function<List<Number>, Number> valueAggregator,
      Function<List<Number>, Number> minAggregator,
      Function<List<Number>, Number> maxAggregator,
      UnaryOperator<List<Number>> rFilter,
      DoubleRange xRange,
      DoubleRange yRange,
      Function<? super E, ? extends Number> xFunction
  ) {
    super(
        xSubplotFunction,
        ySubplotFunction,
        lineFunction,
        yFunction,
        valueAggregator,
        minAggregator,
        maxAggregator,
        rFilter,
        xRange,
        yRange
    );
    this.xFunction = xFunction;
  }

  @Override
  protected Number xValue(E e, R r) {
    return xFunction.apply(e);
  }

  @Override
  protected String xName() {
    return NamedFunction.name(xFunction);
  }
}
