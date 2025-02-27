/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2024 Eric Medvet
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
package io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.RunProfile;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Runner {

  private final int maxNOfSteps;
  private final int maxNOfTokens;
  private final int maxTokensSize;
  private final int maxSingleTokenSize;
  private final boolean skipExecutionOfBlockedNetworks;

  public Runner(
      int maxNOfSteps,
      int maxNOfTokens,
      int maxTokensSize,
      int maxSingleTokenSize,
      boolean skipExecutionOfBlockedNetworks
  ) {
    this.maxNOfSteps = maxNOfSteps;
    this.maxNOfTokens = maxNOfTokens;
    this.maxTokensSize = maxTokensSize;
    this.maxSingleTokenSize = maxSingleTokenSize;
    this.skipExecutionOfBlockedNetworks = skipExecutionOfBlockedNetworks;
  }

  private static <T> Queue<T> emptyQueue() {
    return new ArrayDeque<>();
  }

  private static <T> List<T> takeAll(Queue<T> queue) {
    List<T> list = new ArrayList<>(queue);
    queue.clear();
    return list;
  }

  private static <T> List<T> takeExactly(Queue<T> queue, int n) {
    return IntStream.range(0, n).mapToObj(i -> queue.remove()).toList();
  }

  private static <T> Optional<T> takeOne(Queue<T> queue) {
    if (queue.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(queue.remove());
  }

  public InstrumentedProgram asInstrumentedProgram(Network network) {
    Runner thisRunner = this;
    return new InstrumentedProgram() {
      @Override
      public InstrumentedOutcome runInstrumented(List<Object> inputs) {
        return thisRunner.run(network, inputs);
      }

      @Override
      public List<Type> inputTypes() {
        return network.inputGates().values().stream().toList();
      }

      @Override
      public List<Type> outputTypes() {
        return network.outputGates().values().stream().toList();
      }

      @Override
      public String toString() {
        return "ttpn[g=%d,w=%d]".formatted(network.gates().size(), network.wires().size());
      }
    };
  }

  public InstrumentedProgram.InstrumentedOutcome run(Network network, List<Object> inputs) {
    // check validity
    List<Type> inputTypes = network.inputGates().values().stream().toList();
    SortedMap<Integer, Type> outputTypes = network.outputGates();
    if (inputs.size() != inputTypes.size()) {
      return InstrumentedProgram.InstrumentedOutcome.from(
          new ProgramExecutionException(
              "Wrong number of inputs: %d expected, %d found".formatted(
                  inputTypes.size(),
                  inputs.size()
              )
          )
      );
    }
    for (int i = 0; i < inputTypes.size(); i++) {
      if (!inputTypes.get(i).matches(inputs.get(i))) {
        return InstrumentedProgram.InstrumentedOutcome.from(
            new ProgramExecutionException(
                "Invalid input type for input %d of type %s: %s".formatted(
                    i,
                    inputTypes.get(i),
                    inputs.get(i).getClass()
                )
            )
        );
      }
    }
    // check if blocked
    if (skipExecutionOfBlockedNetworks) {
      long nOfBlockedOutputs = network.outputGates()
          .keySet()
          .stream()
          .filter(gi -> network.isDeadGate(gi) || !network.isWiredToInput(gi))
          .count();
      if (nOfBlockedOutputs > 0) {
        return InstrumentedProgram.InstrumentedOutcome.from(
            new ProgramExecutionException(
                "Blocked/unwired output gates: %d on %d".formatted(nOfBlockedOutputs, network.outputGates().size())
            )
        );
      }
    }
    // prepare memory
    Map<Wire, Queue<Object>> current = new HashMap<>();
    network.wires().forEach(w -> current.put(w, new ArrayDeque<>()));
    Map<Wire, List<Object>> next = new HashMap<>();
    Map<Wire, Type> actualTypes = new HashMap<>();
    Map<Integer, Object> inputsMap = new HashMap<>();
    int ii = 0;
    for (Integer inputGI : network.inputGates().keySet()) {
      inputsMap.put(inputGI, inputs.get(ii));
      ii = ii + 1;
    }
    for (Wire w : network.wires()) {
      Type type = network.concreteOutputType(w.src());
      if (type == null) {
        return InstrumentedProgram.InstrumentedOutcome.from(
            new ProgramExecutionException("No concrete type at output port %s".formatted(w.src()))
        );
      }
      actualTypes.put(w, type);
    }
    // prepare state, counter, and output map
    int k = 0;
    List<RunProfile.State> states = new ArrayList<>();
    SortedMap<Integer, Object> outputs = new TreeMap<>();
    // iterate
    while (k < maxNOfSteps) {
      for (Map.Entry<Integer, Gate> gateEntry : network.gates().entrySet()) {
        int gi = gateEntry.getKey();
        Gate g = gateEntry.getValue();
        if (g instanceof Gate.InputGate) {
          if (k == 0) {
            network.wiresFrom(gi, 0).forEach(w -> next.put(w, List.of(inputsMap.get(gi))));
          }
        } else if (g instanceof Gate.OutputGate) {
          network.wireTo(gi, 0).flatMap(w -> takeOne(current.get(w))).ifPresent(token -> outputs.put(gi, token));
        } else {
          List<Queue<Object>> inputQueues = IntStream.range(0, g.inputPorts().size())
              .mapToObj(
                  pi -> network.wireTo(
                      gi,
                      pi
                  ).map(current::get).orElse(emptyQueue())
              )
              .toList();
          // check conditions and possibly apply function
          if (IntStream.range(0, g.inputPorts().size())
              .allMatch(
                  pi -> inputQueues.get(pi).size() >= g.inputPorts()
                      .get(
                          pi
                      )
                      .n()
              )) {
            Gate.Data localIn = Gate.Data.of(
                IntStream.range(0, g.inputPorts().size())
                    .mapToObj(pi -> switch (g.inputPorts().get(pi).condition()) {
                      case EXACTLY -> takeExactly(inputQueues.get(pi), g.inputPorts().get(pi).n());
                      case AT_LEAST -> takeAll(inputQueues.get(pi));
                    })
                    .toList()
            );
            try {
              Gate.Data localOut = g.operator().apply(localIn);
              // check number of outputs
              if (localOut.lines().size() != g.outputTypes().size()) {
                return InstrumentedProgram.InstrumentedOutcome.from(
                    new ProgramExecutionException(
                        "Unexpected wrong number of outputs: %d expected, %d found".formatted(
                            g.outputTypes()
                                .size(),
                            localOut.lines().size()
                        )
                    ),
                    new RunProfile(states)
                );
              }
              // check output size
              for (int pi = 0; pi < g.outputTypes().size(); pi++) {
                Type type = network.concreteOutputType(new Wire.EndPoint(gi, pi));
                if (type != null && !type.isGeneric()) {
                  for (Object token : localOut.all(pi)) {
                    if (type.sizeOf(token) > maxSingleTokenSize) {
                      return InstrumentedProgram.InstrumentedOutcome.from(
                          new ProgramExecutionException(
                              "Exceeded size of token of type %s on gate %s: %d > %d".formatted(
                                  type,
                                  g,
                                  type.sizeOf(token),
                                  maxSingleTokenSize
                              )
                          ),
                          new RunProfile(states)
                      );
                    }
                  }
                }
              }
              // put outputs
              IntStream.range(0, localOut.lines().size())
                  .forEach(
                      pi -> network.wiresFrom(gi, pi)
                          .forEach(w -> next.put(w, localOut.lines().get(pi)))
                  );
            } catch (RuntimeException e) {
              return InstrumentedProgram.InstrumentedOutcome.from(
                  new ProgramExecutionException("Cannot run %s on %s due to %s".formatted(g, localIn, e.toString()), e),
                  new RunProfile(states)
              );
            }
          }
        }
      }
      // check no new tokens
      if (next.values().stream().mapToInt(List::size).sum() == 0) {
        break;
      }
      // add tokens to current
      next.forEach((w, ts) -> current.get(w).addAll(ts));
      next.clear();
      // build state
      RunProfile.State state = RunProfile.State.from(
          current.entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      e -> actualTypes.get(e.getKey()),
                      Map.Entry::getValue,
                      (c1, c2) -> Stream.concat(c1.stream(), c2.stream()).toList()
                  )
              )
      );
      if (state.count() > maxNOfTokens) {
        return InstrumentedProgram.InstrumentedOutcome.from(
            new ProgramExecutionException(
                "Exceeded number of tokens: %d > %d".formatted(state.count(), maxNOfTokens)
            ),
            new RunProfile(states)
        );
      }
      if (state.size() > maxTokensSize) {
        return InstrumentedProgram.InstrumentedOutcome.from(
            new ProgramExecutionException(
                "Exceeded size of tokens: %d > %d".formatted(state.size(), maxTokensSize)
            ),
            new RunProfile(states)
        );
      }
      states.add(state);
      // increment k
      k = k + 1;
    }
    // check output
    if (!outputTypes.keySet().equals(outputs.keySet())) {
      return InstrumentedProgram.InstrumentedOutcome.from(
          new ProgramExecutionException(
              "Missing outputs on gates: %s".formatted(
                  outputTypes.keySet()
                      .stream()
                      .filter(gi -> !outputs.containsKey(gi))
                      .map(gi -> Integer.toString(gi))
                      .collect(
                          Collectors.joining(",")
                      )
              )
          ),
          new RunProfile(states)
      );
    }
    for (int gi : outputTypes.keySet()) {
      if (!outputTypes.get(gi).matches(outputs.get(gi))) {
        return InstrumentedProgram.InstrumentedOutcome.from(
            new ProgramExecutionException(
                "Invalid output type for input %d of type %s: %s".formatted(
                    gi,
                    outputTypes.get(gi),
                    outputs.get(gi).getClass()
                )
            ),
            new RunProfile(states)
        );
      }
    }
    return InstrumentedProgram.InstrumentedOutcome.from(
        outputs.values().stream().toList(),
        new RunProfile(states)
    );
  }
}
