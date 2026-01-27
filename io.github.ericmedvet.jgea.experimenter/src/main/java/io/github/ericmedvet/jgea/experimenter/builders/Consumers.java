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
package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.experimenter.Run;
import io.github.ericmedvet.jgea.experimenter.listener.telegram.TelegramClient;
import io.github.ericmedvet.jnb.core.*;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jnb.datastructure.Naming;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Discoverable(prefixTemplate = "ea.consumer|c")
public class Consumers {

  private Consumers() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, O> BiConsumer<X, Run<?, ?, ?, ?>> telegram(
      @Param(value = "of", dNPM = "f.identity()") Function<X, O> f,
      @Param(
          value = "title", dS = // spotless:off
          """
              Experiment:
              \t{name}
              Run {index}:
              \tSolver: {solver.name}
              \tProblem: {problem.name}
              \tSeed: {randomGenerator.seed}
              """ // spotless:on
      ) String titleTemplate,
      @Param("chatId") String chatId,
      @Param("botIdFilePath") String botIdFilePath
  ) {
    TelegramClient client = new TelegramClient(new File(botIdFilePath), Long.parseLong(chatId));
    return Naming.named(
        "telegram[%s→to:%s]".formatted(NamedFunction.name(f), chatId),
        (BiConsumer<X, Run<?, ?, ?, ?>>) (x, run) -> client.send(
            Interpolator.interpolate(titleTemplate, run),
            f.apply(x)
        )
    );
  }
}
