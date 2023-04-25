/*
 * Copyright 2023 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jgea.experimenter.net;

import io.github.ericmedvet.jgea.core.util.Progress;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author "Eric Medvet" on 2023/03/27 for jgea
 */
public record Update(
    long localTime,
    String runMap,
    int runIndex,
    Progress runProgress,
    boolean isRunning,
    Map<DataItemKey, List<Object>> dataItems,
    Map<PlotItemKey, List<PlotPoint>> plotItems
) implements Serializable {
  public record DataItemKey(String name, String format) implements Serializable {}

  public record PlotItemKey(String xName, String yName, double minX, double maxX) implements Serializable {}

  public record PlotPoint(double x, double y) implements Serializable {}
}