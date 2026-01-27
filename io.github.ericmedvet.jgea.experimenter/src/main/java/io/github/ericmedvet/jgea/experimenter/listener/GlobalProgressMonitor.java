/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
package io.github.ericmedvet.jgea.experimenter.listener;

import io.github.ericmedvet.jgea.core.util.Progress;

public class GlobalProgressMonitor implements ProgressMonitor {

  private Progress lastProgress;
  private String lastMessage;

  // see https://www.baeldung.com/java-implement-thread-safe-singleton#bill-pugh-singleton-lazy-and-elegant
  private static class GlobalProgressMonitorHelper {
    private static final GlobalProgressMonitor INSTANCE = new GlobalProgressMonitor();
  }

  private GlobalProgressMonitor() {
    lastProgress = Progress.NA;
    lastMessage = "";
  }

  @Override
  public void notify(Progress progress, String message) {
    lastProgress = progress;
    lastMessage = message;
  }

  public Progress progress() {
    return lastProgress;
  }

  public String lastMessage() {
    return lastMessage;
  }

  public static GlobalProgressMonitor get() {
    return GlobalProgressMonitorHelper.INSTANCE;
  }

}
