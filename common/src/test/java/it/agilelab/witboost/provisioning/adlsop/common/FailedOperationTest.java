package it.agilelab.witboost.provisioning.adlsop.common;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;

import io.vavr.control.Either;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FailedOperationTest {
    @Test
    void testCombineAllRight() {
        List<Either<FailedOperation, Integer>> input = List.of(right(1), right(2), right(3));

        var output = FailedOperation.combineEither(right(0), input, Integer::sum);
        assertEquals(right(6), output);
    }

    @Test
    void testCombineOneLeft() {
        var error = new FailedOperation(Collections.singletonList(new Problem("Error!")));
        List<Either<FailedOperation, Integer>> input = List.of(right(1), left(error), right(3));

        var output = FailedOperation.combineEither(right(0), input, Integer::sum);
        assertEquals(left(error), output);
    }

    @Test
    void testCombineMoreThanOneLeft() {
        var error = new FailedOperation(Collections.singletonList(new Problem("Error!")));
        var errorTwo = new FailedOperation(Collections.singletonList(new Problem("Error two!")));
        List<Either<FailedOperation, Integer>> input = List.of(right(1), left(error), left(errorTwo));

        var output = FailedOperation.combineEither(right(0), input, Integer::sum);
        assertEquals(
                left(new FailedOperation(Stream.concat(error.problems().stream(), errorTwo.problems().stream())
                        .toList())),
                output);
    }
}
