package co.gongzh.procbridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

@FunctionalInterface
public interface IDelegate {
    /**
     * An interface that defines how server handles requests.
     *
     * @param method the requested method
     * @param payload the requested payload, must be a JSON value
     * @return the result, must be a JSON value
     */
    @Nullable
    Object handleRequest(@Nullable String method, @Nullable Object payload);
}
