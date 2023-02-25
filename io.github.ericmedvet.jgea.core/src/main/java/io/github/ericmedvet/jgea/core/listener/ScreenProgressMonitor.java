/*
 * Copyright 2022 eric
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

package io.github.ericmedvet.jgea.core.listener;

import io.github.ericmedvet.jgea.core.util.Progress;
import io.github.ericmedvet.jgea.core.util.TextPlotter;

import java.io.PrintStream;

public class ScreenProgressMonitor implements ProgressMonitor {

  private final PrintStream ps;

  public ScreenProgressMonitor(PrintStream ps) {
    this.ps = ps;
  }

  @Override
  public void notify(Progress progress, String message) {
    ps.printf("Progress: %s %s%n", TextPlotter.horizontalBar(progress.rate(), 0, 1, 8), message);
  }

}