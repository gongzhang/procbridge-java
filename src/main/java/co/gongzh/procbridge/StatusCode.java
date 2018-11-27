package co.gongzh.procbridge;

import org.jetbrains.annotations.Nullable;

enum StatusCode {

    REQUEST(0), GOOD_RESPONSE(1), BAD_RESPONSE(2);

    int rawValue;

    StatusCode(int rawValue) {
        this.rawValue = rawValue;
    }

    @Nullable
    static StatusCode fromRawValue(int rawValue) {
        for (StatusCode sc : StatusCode.values()) {
            if (sc.rawValue == rawValue) {
                return sc;
            }
        }
        return null;
    }

}
