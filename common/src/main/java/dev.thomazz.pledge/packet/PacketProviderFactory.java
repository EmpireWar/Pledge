package dev.thomazz.pledge.packet;

import com.google.common.collect.ImmutableSet;
import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.packet.providers.PingPongPacketProvider;
import dev.thomazz.pledge.packet.providers.TransactionPacketProvider;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@UtilityClass
public class PacketProviderFactory {
    private final Set<ThrowingSupplier<PingPacketProvider>> suppliers = ImmutableSet.of(
        TransactionPacketProvider::new,
        PingPongPacketProvider::new
    );

    public PingPacketProvider buildPingProvider(Pledge<?> pledge) {
        return PacketProviderFactory.suppliers.stream()
                .map((supplier) -> buildProvider(supplier, pledge))
                .flatMap(optional -> optional.map(Stream::of).orElseGet(Stream::empty))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not create packet provider!"));
    }

    private Optional<PingPacketProvider> buildProvider(ThrowingSupplier<PingPacketProvider> supplier, Pledge<?> pledge) {
        try {
            return Optional.of(supplier.get(pledge));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private interface ThrowingSupplier<T> {
        T get(Pledge<?> pledge) throws Exception;
    }
}
