package com.getaji.bmshashwatcher;

import java.util.function.Consumer;

public class Either<L, R> {
    private final L leftValue;
    private final R rightValue;

    private final boolean isLeft;

    private Either(L leftValue, R rightValue, boolean isLeft) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.isLeft = isLeft;
    }

    public static <L, R> Either<L, R> left(L leftValue) {
        return new Either<>(leftValue, null, true);
    }

    public static <L, R> Either<L, R> right(R rightValue) {
        return new Either<>(null, rightValue, false);
    }

    public boolean isLeft() {
        return isLeft;
    }

    public boolean isRight() {
        return !isLeft;
    }

    public L left() {
        return leftValue;
    }

    public R right() {
        return rightValue;
    }

    public R getOrElse(R elseValue) {
        return isLeft ? elseValue : rightValue;
    }

    public void consumeRight(Consumer<R> consumer) {
        if (isRight()) {
            consumer.accept(rightValue);
        }
    }
}
