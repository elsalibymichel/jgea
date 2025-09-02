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
package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jviz.core.plot.image.Configuration;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.BoxPlot;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.BoxPlot.ExtremeType;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.BoxPlot.MidType;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.Colors;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.General;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.LandscapePlot;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.Layout;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.LinesPlot;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.PlotMatrix;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.PlotMatrix.Independence;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.PlotMatrix.Show;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.PointsPlot;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.Text;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.Text.Use;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.UnivariateGridPlot;
import io.github.ericmedvet.jviz.core.plot.image.Configuration.VectorialFieldPlot;
import io.github.ericmedvet.jviz.core.plot.image.XYPlotDrawer.Marker;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Discoverable(prefixTemplate = "ea.plot.configuration|conf|c")
public class PlotConfigurations {

  private PlotConfigurations() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Configuration image(
      @Param(value = "axesShow", dS = "border") Show axesShow,
      @Param(value = "titleShow", dS = "border") Show titleShow,
      @Param(value = "axesIndependences", dSs = {"rows", "cols"}) List<Independence> axesIndependences,
      @Param(value = "fontSizeRate", dD = 1d) double fontSizeRate,
      @Param(value = "fontName", dS = "SansSerif") String fontName,
      @Param(value = "strokeSizeRate", dD = 1d) double strokeSizeRate,
      @Param(value = "markerSizeRate", dD = 1d) double markerSizeRate,
      @Param(value = "alpha", dD = 0.3) double alpha,
      @Param("linesMarkers") boolean linesMarkers,
      @Param(value = "boxplotExtremeType", dS = "iqr_1_5") ExtremeType boxplotExtremeType,
      @Param(value = "boxplotMidType", dS = "median") MidType boxplotMidType,
      @Param("boxplotMarkers") boolean boxplotMarkers,
      @Param(value = "marker", dS = "circle") Marker marker,
      @Param("debug") boolean debug
  ) {
    return new Configuration(
        General.DEFAULT,
        Layout.DEFAULT,
        Colors.DEFAULT,
        new Text(
            Text.DEFAULT.fontSizeRate() * fontSizeRate,
            Map.ofEntries(
                Map.entry(Use.TITLE, Text.DEFAULT.sizeRates().get(Use.TITLE) * fontSizeRate),
                Map.entry(
                    Use.TICK_LABEL,
                    Text.DEFAULT.sizeRates().get(Use.TICK_LABEL) * fontSizeRate
                ),
                Map.entry(Use.NOTE, Text.DEFAULT.sizeRates().get(Use.NOTE) * fontSizeRate)
            ),
            fontName
        ),
        new PlotMatrix(axesShow, titleShow, new HashSet<>(axesIndependences)),
        new LinesPlot(
            LinesPlot.DEFAULT.strokeSizeRate() * strokeSizeRate,
            alpha,
            LinesPlot.DEFAULT.markerSizeRate() * markerSizeRate,
            marker,
            linesMarkers,
            LinesPlot.DEFAULT.legendImageWRate(),
            LinesPlot.DEFAULT.legendImageHRate(),
            LinesPlot.DEFAULT.colors(),
            LinesPlot.DEFAULT.xExtensionRate(),
            LinesPlot.DEFAULT.yExtensionRate()
        ),
        new PointsPlot(
            PointsPlot.DEFAULT.strokeSizeRate() * strokeSizeRate,
            PointsPlot.DEFAULT.markerSizeRate() * markerSizeRate,
            alpha,
            PointsPlot.DEFAULT.legendImageSizeRate(),
            marker,
            PointsPlot.DEFAULT.colors(),
            PointsPlot.DEFAULT.xExtensionRate(),
            PointsPlot.DEFAULT.yExtensionRate()
        ),
        UnivariateGridPlot.DEFAULT,
        LandscapePlot.DEFAULT,
        new BoxPlot(
            BoxPlot.DEFAULT.strokeSizeRate() * strokeSizeRate,
            BoxPlot.DEFAULT.markerSizeRate() * markerSizeRate,
            marker,
            BoxPlot.DEFAULT.boxWRate(),
            BoxPlot.DEFAULT.whiskersWRate(),
            BoxPlot.DEFAULT.legendImageWRate(),
            BoxPlot.DEFAULT.legendImageHRate(),
            boxplotExtremeType,
            boxplotMidType,
            alpha,
            BoxPlot.DEFAULT.colors(),
            BoxPlot.DEFAULT.yExtensionRate(),
            boxplotMarkers,
            BoxPlot.DEFAULT.jitter()
        ),
        VectorialFieldPlot.DEFAULT,
        debug
    );
  }
}
