package ru.wardreg.ccpower.platform;

/** Static holder for the loader implementation, installed at startup. */
public final class CcpPlatformHolder {
    private static CcpPlatform instance;

    public static void init(CcpPlatform platform) {
        instance = platform;
    }

    public static CcpPlatform get() {
        if (instance == null) throw new IllegalStateException("CC: Power platform not initialized");
        return instance;
    }

    private CcpPlatformHolder() {}
}
