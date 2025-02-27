/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.*;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import java.util.List;
import java.util.Set;

public class TestManual {

  public static void main(
      String[] args
  ) throws NetworkStructureException, ProgramExecutionException, NoSuchMethodException, TypeException {
    //biLongestString();
    //rIntSum();
    //iArraySum();
    //iBiMax();
    //iTriMax();
    //vScProduct();
    //sLengther();
    //triLongestString();
    vProduct();
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> {
              InstrumentedProgram.InstrumentedOutcome outcome = runner.run(goodNetwork, e.input());
              System.out.printf(
                  "in=%s\tactualOut=%s\tpredOut=%s exc=%s%n",
                  e.input(),
                  e.output().outputs(),
                  outcome.outputs(),
                  outcome.exception()
              );
            }
        );
    System.out.print(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)).get("fail_rate"));
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
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
  }

  private static void vProduct() throws NetworkStructureException, TypeException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build("ea.p.ps.synthetic(name = \"vProduct\")");
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
    drawer.show(goodNetwork);
    Runner runner = new Runner(100, 100, 100, 100, false);

    System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    // check
    psb.caseProvider()
        .stream()
        .forEach(
            e -> System.out.printf(
                "in=%s\tactualOut=%s\tpredOut=%s%n",
                e.input(),
                e.output().outputs(),
                runner.run(goodNetwork, e.input()).outputs()
            )
        );
  }


}
