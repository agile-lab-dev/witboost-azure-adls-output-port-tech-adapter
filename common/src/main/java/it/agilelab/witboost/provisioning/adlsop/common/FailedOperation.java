package it.agilelab.witboost.provisioning.adlsop.common;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import io.vavr.control.Either;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public record FailedOperation(List<Problem> problems) {
    public FailedOperation {
        Objects.requireNonNull(problems);
    }

    /**
     * Combines a list of {@code Either<FailedOperation, T>} such that it accumulates all results into a single Either.
     * For FailedOperation, it accumulates problems into a single FailedOperation. For {@code T} it uses {@code accumulator} as a combine function.
     * @param seed Initial value for the reduce combination
     * @param failedOperations List of {@code Either<FailedOperation, T>}
     * @param accumulator An associative, non-interfering, stateless function to combine {@code T} objects
     * @return Returns {@code Either.left} with the combination of {@code FailedOperation} if at least one {@code Either.left} is present.
     * Otherwise, it returns {@code Either.right} with the combine result
     */
    public static <T> Either<FailedOperation, T> combineEither(
            Either<FailedOperation, T> seed,
            Collection<Either<FailedOperation, T>> failedOperations,
            BinaryOperator<T> accumulator) {
        return failedOperations.stream().reduce(seed, (a, b) -> {
            if (a.isRight() && b.isRight()) return right(accumulator.apply(a.get(), b.get()));
            else if (a.isLeft() && b.isLeft())
                return left(new FailedOperation(
                        Stream.concat(a.getLeft().problems().stream(), b.getLeft().problems().stream())
                                .toList()));
            else if (a.isLeft()) return a;
            else return b;
        });
    }
}
