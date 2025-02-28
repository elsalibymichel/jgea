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
import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Base;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Composed;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Generic;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import java.util.*;
import java.util.random.RandomGenerator;

public class BiggerNetworks {

    public static void main(
            String[] args
    ) throws NetworkStructureException, ProgramExecutionException, NoSuchMethodException, TypeException {

        Network goodNetwork = new Network(
                List.of(
                        Gate.input(Base.REAL),
                        Gate.input(Base.REAL),
                        Gates.sPSequencer(),
                        Gates.rPMathOperator(Element.Operator.ADDITION),
                        Gates.rToI(),
                        Gate.output(Base.INT)
                ),
                Set.of(
                        Wire.of(0, 0, 2, 0),
                        Wire.of(2, 0, 2, 1),
                        Wire.of(2, 0, 3, 0),
                        Wire.of(1, 0, 3, 1),
                        Wire.of(3, 0, 4, 0),
                        Wire.of(4, 0, 5, 0)
                        )
        );

        NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
        ProgramSynthesisProblem psb = (ProgramSynthesisProblem) nb.build(
                "ea.p.ps.synthetic(name = \"rIntSum\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
        );

        TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
        Runner runner = new Runner(100, 1000, 1000, 100, false);

        drawer.show(goodNetwork);

        System.out.println("Good network:");

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

        System.out.println(psb.qualityFunction().apply(runner.asInstrumentedProgram(goodNetwork)));
    }}