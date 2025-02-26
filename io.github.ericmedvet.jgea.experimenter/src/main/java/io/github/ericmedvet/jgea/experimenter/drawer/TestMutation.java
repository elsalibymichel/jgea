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
package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.*;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import java.util.*;
import java.util.random.RandomGenerator;

public class TestMutation {

  public static void main(
      String[] args
  ) throws NetworkStructureException, ProgramExecutionException, NoSuchMethodException, TypeException {
    //biLongestString();
//    rIntSum();
//    iArraySum();
//    iBiMax();
//    iTriMax();
//    vScProduct();
    sLengther();
//    triLongestString();
//    vProduct();
  }

  private static void rIntSum() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"rIntSum\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Base.REAL),
            Gate.input(Base.REAL),
            Gates.rPMathOperator(Element.Operator.ADDITION),
            Gates.rToI(),
            Gate.output(Base.INT)
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 2, 1),
            Wire.of(2, 0, 3, 0),
            Wire.of(3, 0, 4, 0)

        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);

    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("rIntSum - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("rIntSum - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("rIntSum - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

    }
  }


  private static void biLongestString() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"biLongestString\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Base.STRING),
            Gate.input(Base.STRING),
            Gates.sSplitter(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.length(),
            Gates.iBefore(),
            Gates.select(),
            Gate.output(Generic.of("t"))
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 3, 0),
            Wire.of(2, 0, 4, 0),
            Wire.of(3, 0, 5, 0),
            Wire.of(4, 0, 6, 0),
            Wire.of(5, 0, 6, 1),
            Wire.of(0, 0, 7, 1),
            Wire.of(1, 0, 7, 0),
            Wire.of(6, 0, 7, 2),
            Wire.of(7, 0, 8, 0)
        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("biLongestString - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("biLongestString - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("biLongestString - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

    }
  }


  private static void iArraySum() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"iArraySum\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.INT)),
            Gates.splitter(),
            Gates.iSPSum(),
            Gate.output(Base.INT)
        ),
        Set.of(
            Wire.of(0, 0, 1, 0),
            Wire.of(1, 0, 2, 0),
            Wire.of(2, 0, 3, 0),
            Wire.of(2, 0, 2, 1)
        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("iArraySum - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("iArraySum - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("iArraySum - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

    }
  }

  private static void iBiMax() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"iBiMax\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Base.INT),
            Gate.input(Base.INT),
            Gate.output(Base.INT),
            Gates.iBefore(),
            Gates.select()
        ),
        Set.of(
            Wire.of(0, 0, 3, 0),
            Wire.of(1, 0, 3, 1),
            Wire.of(0, 0, 4, 1),
            Wire.of(1, 0, 4, 0),
            Wire.of(3, 0, 4, 2),
            Wire.of(4, 0, 2, 0)
        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("iBiMax - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("iBiMax - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("iBiMax - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

    }
  }

  private static void iTriMax() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"iTriMax\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Base.INT),
            Gate.input(Base.INT),
            Gate.input(Base.INT),
            Gates.iBefore(),
            Gates.select(),
            Gates.iBefore(),
            Gates.select(),
            Gate.output(Base.INT)
        ),
        Set.of(
            Wire.of(0, 0, 3, 0),
            Wire.of(1, 0, 3, 1),
            Wire.of(0, 0, 4, 1),
            Wire.of(1, 0, 4, 0),
            Wire.of(3, 0, 4, 2),

            Wire.of(4, 0, 5, 0),
            Wire.of(2, 0, 5, 1),
            Wire.of(4, 0, 6, 1),
            Wire.of(2, 0, 6, 0),
            Wire.of(5, 0, 6, 2),
            Wire.of(6, 0, 7, 0)
        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("iTriMax - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("iTriMax - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("iTriMax - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

    }
  }

  private static void vScProduct() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"vScProduct\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Base.REAL),
            Gates.length(),
            Gates.repeater(),
            Gates.splitter(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            Gates.sPSequencer(),
            Gate.output(Composed.sequence(Base.REAL))
        ),
        Set.of(
            Wire.of(1, 0, 3, 0),
            Wire.of(0, 0, 2, 0),
            Wire.of(0, 0, 4, 0),
            Wire.of(2, 0, 3, 1),
            Wire.of(3, 0, 5, 0),
            Wire.of(4, 0, 5, 1),
            Wire.of(5, 0, 6, 0),
            Wire.of(6, 0, 6, 1),
            Wire.of(6, 0, 7, 0)
        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 1000, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("vScProduct - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("vScProduct - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("vScProduct - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

    }
  }

  private static void sLengther() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"sLengther\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.STRING)),
            Gates.splitter(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.pairer(),
            Gates.sPSequencer(),
            Gate.output(Composed.sequence(Composed.tuple(List.of(Base.STRING, Base.INT))))
        ),
        Set.of(
            Wire.of(0, 0, 1, 0),
            Wire.of(1, 0, 2, 0),
            Wire.of(1, 0, 4, 0),
            Wire.of(2, 0, 3, 0),
            Wire.of(3, 0, 4, 1),
            Wire.of(4, 0, 5, 0),
            Wire.of(5, 0, 5, 1),
            Wire.of(5, 0, 6, 0)

        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 1000, 1000, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("sLengther - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("sLengther - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("sLengther - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

//      psb.caseProvider()
//        .stream()
//        .forEach(
//                e -> System.out.printf(
//                        "in=%s\tactualOut=%s\tpredOut=%s\terror=%s\tsteps=%d%n",
//                        e.input(),
//                        e.output().outputs(),
//                        runner.run(mutated, e.input()).outputs(),
//                        psb.errorFunction().apply(e.input(), e.output(), runner.run(mutated, e.input())),
//                        runner.run(mutated, e.input()).profile().states().size()
//                ));
    }
  }

  private static void triLongestString() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"triLongestString\")");
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Base.STRING),
            Gate.input(Base.STRING),
            Gate.input(Base.STRING),
            Gates.sSplitter(),
            Gates.sSplitter(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.length(),
            Gates.length(),
            Gates.iBefore(),
            Gates.select(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.iBefore(),
            Gates.select(),
            Gate.output(Generic.of("t"))
        ),
        Set.of(
            Wire.of(0, 0, 3, 0),
            Wire.of(0, 0, 10, 1),
            Wire.of(1, 0, 4, 0),
            Wire.of(1, 0, 10, 0),
            Wire.of(3, 0, 6, 0),
            Wire.of(4, 0, 7, 0),
            Wire.of(6, 0, 9, 0),
            Wire.of(7, 0, 9, 1),
            Wire.of(9, 0, 10, 2),
            Wire.of(10, 0, 11, 0),
            Wire.of(10, 0, 14, 1),
            Wire.of(2, 0, 5, 0),
            Wire.of(5, 0, 8, 0),
            Wire.of(8, 0, 13, 1),
            Wire.of(2, 0, 14, 0),
            Wire.of(11, 0, 12, 0),
            Wire.of(12, 0, 13, 0),
            Wire.of(13, 0, 14, 2),
            Wire.of(14, 0, 15, 0)
        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("triLongestString - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)));
    }

    System.out.println();
    System.out.println("triLongestString - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("triLongestString - Wire Swapper Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));

    }
  }

  private static void vProduct() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"vProduct\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    // good solution
    Network goodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.splitter(),
            Gates.splitter(),
            Gates.queuer(),
            Gates.rSMult(),
            Gates.rSPSum(),
            Gate.output(Base.REAL)
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 3, 0),
            Wire.of(2, 0, 4, 0),
            Wire.of(3, 0, 4, 1),
            Wire.of(4, 0, 5, 0),
            Wire.of(5, 0, 6, 0),
            Wire.of(6, 0, 6, 1),
            Wire.of(6, 0, 7, 0)


        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    //drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

//    psb.caseProvider()
//        .stream()
//        .forEach(
//            e -> System.out.printf(
//                "in=%s\tactualOut=%s\tpredOut=%s\terror=%s\tsteps=%d%n",
//                e.input(),
//                e.output().outputs(),
//                runner.run(goodNetwork, e.input()).outputs(),
//                psb.errorFunction().apply(e.input(), e.output(), runner.run(goodNetwork, e.input())),
//                runner.run(goodNetwork, e.input()).profile().states().size()
//            )
//        );

    //System.out.println(runner.asInstrumentedProgram(goodNetwork).runInstrumented(psb.caseProvider().get(0).input()).profile().states().size()); // thıs ıs the number of steps for one case


    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    Map<String, Double> qualityMetrics = psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork));
    System.out.println(qualityMetrics.get("fail_rate"));
    System.out.println(qualityMetrics.get("avg_raw_dissimilarity"));
    System.out.println(qualityMetrics.get("profile_avg_steps"));


    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);

    System.out.println();
    System.out.println("vProduct - Gate Inserter Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = giMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("vProduct - Gate Remover Mutation");
    for (int i = 0; i < 10; i++) {
      Network mutated = grMutation.mutate(goodNetwork, rnd);
      drawer.show(mutated);
      System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
    }

    System.out.println();
    System.out.println("vProduct - Wire Swapper Mutation");

    for (int i = 0; i < 10; i++) {
      Network mutated = wsMutation.mutate(goodNetwork, rnd);
      //drawer.show(mutated);
      System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(mutated)).get("fail_rate"));
      System.out.println();
    }
    //runner.asInstrumentedProgram(mutated).runInstrumented(null).profile().states().size(); // thıs ıs the number of steps for one case
    //      psb.caseProvider()
    //              .stream()
    //              .forEach(
    //                      e -> System.out.printf(
    //                              "in=%s\tactualOut=%s\tpredOut=%s%n",
    //                              e.input(),
    //                              e.output().outputs(),
    //                              runner.run(mutated, e.input()).outputs()
    //                      )
    //              );
  }
}


