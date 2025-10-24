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
package io.github.ericmedvet.jgea.core.listener;

import io.github.ericmedvet.jgea.core.util.Naming;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AccTester {

  public static void main(String[] args) {
    AccumulatorFactory<Integer, String, String> f = AccumulatorFactory.from(
        "base.fac",
        s -> Accumulator.from(
            "base.acc",
            () -> s,
            (i, as) -> as + "," + i,
            as -> System.out.println("done")
        ),
        () -> System.out.println("shutting down")
    );
    f = f.conditional(
        Naming.named("always", (Predicate<String>) s -> true),
        Naming.named("even", (Predicate<Integer>) i -> i % 2 == 0)
    );
    f = f.thenOnShutdown(Naming.named("bye", (Consumer<String>) s -> System.out.println("bye")));
    System.out.println(f);
    Accumulator<Integer, String> accumulator = f.build("a");
    accumulator.listen(1);
    accumulator.listen(2);
    accumulator.listen(3);
    System.out.printf("%s got: %s%n", accumulator, accumulator.get());
    accumulator.done();
    accumulator = f.build("b");
    accumulator.listen(1);
    accumulator.listen(2);
    accumulator.listen(3);
    System.out.printf("%s got: %s%n", accumulator, accumulator.get());
    accumulator.done();
    f.shutdown();
  }

}
