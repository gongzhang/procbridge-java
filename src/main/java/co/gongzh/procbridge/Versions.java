package co.gongzh.procbridge;

public final class Versions {

    private static final byte[] V1_0 = { 1, 0 };
    private static final byte[] V1_1 = { 1, 1 };
    static final byte[] CURRENT = V1_1;

    public static byte[] getCurrent() {
        return CURRENT.clone();
    }

    private Versions() {}

}
