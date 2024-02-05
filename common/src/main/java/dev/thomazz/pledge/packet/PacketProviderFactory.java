package dev.thomazz.pledge.packet;

import com.google.common.collect.ImmutableSet;
import dev.thomazz.pledge.api.Pledge;
import dev.thomazz.pledge.packet.providers.PingPongPacketProvider;
import dev.thomazz.pledge.packet.providers.TransactionPacketProvider;

import java.util.Optional;
import java.util.Set;

public final class PacketProviderFactory {

    private static final Set<ThrowingSupplier<PacketProvider>> suppliers = ImmutableSet.of(
        TransactionPacketProvider::new,
        PingPongPacketProvider::new
    );

    public static PacketProvider build(Pledge<?, ?> pledge) {
        return PacketProviderFactory.suppliers.stream()
            .map((supplier) -> buildProvider(supplier, pledge))
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Could not create packet provider!"));
    }

    private static Optional<PacketProvider> buildProvider(ThrowingSupplier<PacketProvider> supplier, Pledge<?, ?> pledge) {
        try {
            return Optional.of(supplier.get(pledge));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private interface ThrowingSupplier<T> {
        T get(Pledge<?, ?> pledge) throws Exception;
    }
}
