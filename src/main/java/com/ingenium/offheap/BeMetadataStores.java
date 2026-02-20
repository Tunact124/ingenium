package com.ingenium.offheap;

/**
 * Factory for BE metadata stores.
 */
public final class BeMetadataStores {
    private BeMetadataStores() {}

    public static IBeMetadataStore createDefault(int capacityRecords) {
        OffHeapBeMetadataStore off = new OffHeapBeMetadataStore(capacityRecords);
        if (off.isEnabled()) return off;
        return new OnHeapBeMetadataStore();
    }
}
