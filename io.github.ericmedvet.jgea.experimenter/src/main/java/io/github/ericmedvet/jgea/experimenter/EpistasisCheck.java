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
package io.github.ericmedvet.jgea.experimenter;

import io.github.ericmedvet.jgea.core.InvertibleMapper;
import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.representation.sequence.integer.IntString;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.mapelites.MapElites;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.Grid;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EpistasisCheck {
  public static void main(String[] args) {
    RandomGenerator rg = new Random();
    int l1 = 18;
    int l2 = 232;
    NamedBuilder<Object> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") FormattedNamedFunction<Pair<List<Double>, IntString>, Double> intraEpistasisF = ((FormattedNamedFunction<Pair<List<Double>, IntString>, Double>) nb
        .build(
            """
                ea.f.intraEpistasis(of = f.pairSecond())"""
        ));
    @SuppressWarnings("unchecked") FormattedNamedFunction<Pair<List<Double>, IntString>, Double> intraEpistasis1F = ((FormattedNamedFunction<Pair<List<Double>, IntString>, Double>) nb
        .build(
            """
                ea.f.intraEpistasis(of = f.pairSecond(); endOffset = %d)""".formatted(l2)
        ));
    @SuppressWarnings("unchecked") FormattedNamedFunction<Pair<List<Double>, IntString>, Double> intraEpistasis2F = ((FormattedNamedFunction<Pair<List<Double>, IntString>, Double>) nb
        .build(
            """
                ea.f.intraEpistasis(of = f.pairSecond(); startOffset = %d)""".formatted(l1)
        ));
    @SuppressWarnings("unchecked") Function<Pair<List<Double>, IntString>, Representation<Pair<List<Double>, IntString>>> representationF = ((Function<Pair<List<Double>, IntString>, Representation<Pair<List<Double>, IntString>>>) nb
        .build(
            """
                ea.representation.pair(
                  first = ea.representation.doubleString();
                  second = ea.representation.intString(
                    mutations = [ea.r.go.oneMutation(mutations = [ea.r.go.isSymbolCopyMutation(); ea.r.go.isFlipMutation()])]
                  )
                )"""
        ));
    @SuppressWarnings("unchecked") InvertibleMapper<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>> iMapper = ((InvertibleMapper<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>>) nb
        .build(
            """
                ea.m.splitter(of = ea.m.isIndexed(relativeLength = 1))"""
        ));
    @SuppressWarnings("unchecked") MapElites.Descriptor<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object> descriptor1 = ((MapElites.Descriptor<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object>) nb
        .build(
            """
                ea.s.me.d.descriptor(f = ea.f.intraEpistasis(of = f.pairSecond(of = ea.f.genotype()); endOffset = %d); nOfBins = 10)"""
                .formatted(l2)
        ));
    @SuppressWarnings("unchecked") MapElites.Descriptor<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object> descriptor2 = ((MapElites.Descriptor<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object>) nb
        .build(
            """
                ea.s.me.d.descriptor(f = ea.f.intraEpistasis(of = f.pairSecond(of = ea.f.genotype()); startOffset = %d); nOfBins = 10)"""
                .formatted(l1)
        ));
    Pair<List<Double>, List<Double>> exampleS = new Pair<>(
        Collections.nCopies(l1, 0d),
        Collections.nCopies(l2, 0d)
    );
    Pair<List<Double>, IntString> exampleG = iMapper.exampleFor(exampleS);
    Representation<Pair<List<Double>, IntString>> representation = representationF.apply(exampleG);
    Function<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>> mapper = iMapper.mapperFor(
        exampleS
    );
    Mutation<Pair<List<Double>, IntString>> mutation = representation.mutations().getFirst();
    //    mutation = Mutation.pair(
    //        new GaussianMutation(0.35d),
    //        (is, rnd) -> {
    //          List<Integer> genes = new ArrayList<>(is.genes());
    //          genes.set(rnd.nextInt(genes.size()), genes.get(rnd.nextInt(genes.size())));
    //          return new IntString(genes, is.lowerBound(), is.upperBound());
    //        }
    //    );
    // random genotypes
    representation.factory().build(0, rg).forEach(g -> {
      System.out.println();
      for (int i = 0; i < 1000; i++) {
        Pair<List<Double>, IntString> mutatedG = mutation.mutate(g, rg);
        if (intraEpistasisF.apply(mutatedG) > intraEpistasisF.apply(g)) {
          g = mutatedG;
          Individual<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object> individual = Individual
              .of(
                  0,
                  g,
                  mapper.apply(g),
                  null,
                  0,
                  0,
                  List.of()
              );
          System.out.printf(
              " \t%s\t%4d\tintra: %s intra1: %s intra2: %s\t[%2d,%2d]%n",
              usage(g),
              i,
              intraEpistasisF.applyFormatted(g),
              intraEpistasis1F.applyFormatted(g),
              intraEpistasis2F.applyFormatted(g),
              descriptor1.coordinate(individual).bin(),
              descriptor2.coordinate(individual).bin()
          );
        }
      }
    });
    // mini map-elites
    Grid<Individual<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object>> archive = Grid.create(
        descriptor1.nOfBins(),
        descriptor2.nOfBins()
    );
    ToDoubleFunction<Grid<?>> coverage = g -> (double) g.values()
        .stream()
        .filter(Objects::nonNull)
        .count() / (double) g.w() / (double) g.h();
    int nOfAttempts = 0;
    Pair<List<Double>, IntString> seedG = representation.factory().independent().build(rg);
    Individual<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object> seedIndividual = Individual.of(
        nOfAttempts,
        seedG,
        mapper.apply(seedG),
        null,
        nOfAttempts,
        nOfAttempts,
        List.of()
    );
    archive.set(
        descriptor1.coordinate(seedIndividual).bin(),
        descriptor2.coordinate(seedIndividual).bin(),
        seedIndividual
    );
    while (coverage.applyAsDouble(archive) < 0.5d) {
      nOfAttempts = nOfAttempts + 1;
      Individual<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object> parent = Misc.pickRandomly(
          archive.values()
              .stream()
              .filter(Objects::nonNull)
              .toList(),
          rg
      );
      Pair<List<Double>, IntString> childG = mutation
          .mutate(parent.genotype(), rg);
      Individual<Pair<List<Double>, IntString>, Pair<List<Double>, List<Double>>, Object> child = Individual.of(
          nOfAttempts,
          childG,
          mapper.apply(childG),
          null,
          nOfAttempts,
          nOfAttempts,
          List.of(parent.id())
      );
      archive.set(
          descriptor1.coordinate(child).bin(),
          descriptor2.coordinate(child).bin(),
          child
      );
      System.out.printf("%5d %.4f%n", nOfAttempts, coverage.applyAsDouble(archive));
    }
  }

  private static String usage(Pair<? extends List<?>, IntString> g) {
    return IntStream.range(g.second().lowerBound(), g.second().upperBound())
        .mapToObj(
            i -> g.second()
                .genes()
                .contains(i) ? "o" : "·"
        )
        .collect(Collectors.joining());
  }

}
