package com.ingenium.ds;

import java.util.Objects;

public final class TimingWheel<T> {
    private static final int DEFAULT_WHEEL_BITS = 12;
    private static final int DEFAULT_WHEEL_SIZE = 1 << DEFAULT_WHEEL_BITS;

    private final int wheelBits;
    private final int wheelSize;
    private final int wheelMask;

    private final Node<T>[] buckets;

    private long currentTick;

    @SuppressWarnings("unchecked")
    public TimingWheel() {
        this(DEFAULT_WHEEL_BITS);
    }

    @SuppressWarnings("unchecked")
    public TimingWheel(int wheelBits) {
        if (wheelBits < 6 || wheelBits > 20) {
            throw new IllegalArgumentException("wheelBits out of range: " + wheelBits);
        }
        this.wheelBits = wheelBits;
        this.wheelSize = 1 << wheelBits;
        this.wheelMask = wheelSize - 1;
        this.buckets = (Node<T>[]) new Node[wheelSize];
    }

    public long now() {
        return currentTick;
    }

    public void advanceTo(long tick) {
        if (tick < currentTick) {
            throw new IllegalArgumentException("Cannot move backwards: " + tick + " < " + currentTick);
        }
        currentTick = tick;
    }

    public Handle schedule(long dueTick, T payload) {
        Objects.requireNonNull(payload, "payload");

        var delta = dueTick - currentTick;
        if (delta < 0) delta = 0;

        var slot = slotFor(dueTick);
        var node = new Node<>(dueTick, payload);

        node.next = buckets[slot];
        buckets[slot] = node;

        return new Handle(node);
    }

    public int drainDue(long tick, int limit, DrainConsumer<T> consumer) {
        if (limit <= 0) return 0;

        advanceTo(tick);
        var slot = slotFor(tick);

        var head = buckets[slot];
        if (head == null) return 0;

        Node<T> newHead = null;
        var drained = 0;

        while (head != null) {
            var node = head;
            head = head.next;

            if (node.cancelled) continue;

            if (node.dueTick <= tick && drained < limit) {
                consumer.accept(node.payload);
                drained++;
                continue;
            }

            node.next = newHead;
            newHead = node;
        }

        buckets[slot] = newHead;
        return drained;
    }

    private int slotFor(long tick) {
        return (int) tick & wheelMask;
    }

    public interface DrainConsumer<T> {
        void accept(T payload);
    }

    public static final class Handle {
        private final Node<?> node;

        private Handle(Node<?> node) {
            this.node = node;
        }

        public void cancel() {
            node.cancelled = true;
        }
    }

    private static final class Node<T> {
        private final long dueTick;
        private final T payload;

        private Node<T> next;
        private boolean cancelled;

        private Node(long dueTick, T payload) {
            this.dueTick = dueTick;
            this.payload = payload;
        }
    }
}
