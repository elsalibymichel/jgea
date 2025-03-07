package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.order.PartialComparator;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface TotalOrderQualityBasedBiProblem<S, O, Q> extends QualityBasedBiProblem<S, O, Q> {

    Comparator<Q> totalOrderComparator();

    @Override
    default PartialComparator<Q> qualityComparator() {
        return (q1, q2) -> {
            int outcome = totalOrderComparator().compare(q1, q2);
            if (outcome == 0) {
                return PartialComparator.PartialComparatorOutcome.SAME;
            }
            if (outcome < 0) {
                return PartialComparator.PartialComparatorOutcome.BEFORE;
            }
            return PartialComparator.PartialComparatorOutcome.AFTER;
        };
    }

    static <S, O, Q> TotalOrderQualityBasedBiProblem<S, O, Q> from(
            QualityBasedBiProblem<S, O, Q> qbBiProblem,
            Comparator<Q> comparator
    ){
        return from(qbBiProblem.outcomeFunction(), qbBiProblem.firstQualityFunction(), qbBiProblem.secondQualityFunction(), comparator, qbBiProblem.example());
    }

    static <S, O, Q> TotalOrderQualityBasedBiProblem<S, O, Q> from(
            BiFunction<S, S, O> outcomeFunction,
            Function<O, Q> firstQualityFunction,
            Function<O, Q> secondQualityFunction,
            Comparator<Q> totalOrderComparator,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<S> example
    ) {
        record HardTOQBiProblem<S, O, Q>(
                BiFunction<S, S, O> outcomeFunction,
                Function<O, Q> firstQualityFunction,
                Function<O, Q> secondQualityFunction,
                Comparator<Q> totalOrderComparator,
                Optional<S> example
        ) implements TotalOrderQualityBasedBiProblem<S, O, Q> {
        }
        return new HardTOQBiProblem<>(outcomeFunction, firstQualityFunction, secondQualityFunction, totalOrderComparator, example);
    }
}
