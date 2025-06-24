package com.wynprice.secretrooms.server.registry;

import com.wynprice.secretrooms.SecretRooms7;
import com.wynprice.secretrooms.server.registry.RegistryHolder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FabricRegistryHolder<T> implements RegistryHolder<T> {
    private final Registry<T> registry;
    private final Map<String, Supplier<T>> objects = new LinkedHashMap<>();

    public FabricRegistryHolder(Registry<T> registry) {
        this.registry = registry;
    }

    @Override
    public <I extends T> Supplier<I> register(String name, Supplier<I> supplier) {
        // Create a cached supplier that registers the object when first accessed
        Supplier<I> cachedSupplier = new CachedSupplier<I>(name, supplier, (Registry<I>) registry);
        objects.put(name, () -> cachedSupplier.get());
        return cachedSupplier;
    }

    @Override
    public Collection<Supplier<T>> listAll() {
        return objects.values();
    }

    public void register() {
        // Force registration of all objects by accessing them
        for (Supplier<T> supplier : objects.values()) {
            supplier.get();
        }
    }
    
    private static class CachedSupplier<I> implements Supplier<I> {
        private final String name;
        private final Supplier<I> factory;
        private final Registry<I> registry;
        private I cached;
        
        public CachedSupplier(String name, Supplier<I> factory, Registry<I> registry) {
            this.name = name;
            this.factory = factory;
            this.registry = registry;
        }
        
        @Override
        public I get() {
            if (cached == null) {
                cached = factory.get();
                Registry.register(registry, new ResourceLocation(SecretRooms7.MODID, name), cached);
            }
            return cached;
        }
    }
} 