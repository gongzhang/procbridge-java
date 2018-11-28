package co.gongzh.procbridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gong Zhang
 */
public abstract class Delegate implements IDelegate {

    @NotNull
    private final Map<String, Method> handlers;

    protected Delegate() {
        this.handlers = new HashMap<>();
        for (Method m : this.getClass().getMethods()) {
            if (m.getAnnotation(Handler.class) != null) {
                String key = m.getName();
                if (handlers.containsKey(key)) {
                    throw new UnsupportedOperationException("duplicate handler name: " + key);
                }
                m.setAccessible(true);
                handlers.put(key, m);
            }
        }
    }

    protected void willHandleRequest(@Nullable String method, @Nullable Object payload) {
    }

    protected @Nullable Object handleUnknownRequest(@Nullable String method, @Nullable Object payload) {
        throw new ServerException("unknown method: " + method);
    }

    @Override
    public final @Nullable Object handleRequest(@Nullable String method, @Nullable Object payload) {
        willHandleRequest(method, payload);

        Method m = handlers.get(method);
        if (m == null) {
            return handleUnknownRequest(method, payload);
        }

        Object result = null;
        try {
            int pcnt = m.getParameterCount();
            if (pcnt == 0) {
                result = m.invoke(this);
            } else if (pcnt == 1) {
                result = m.invoke(this, payload);
            } else {
                // unpack
                if (!(payload instanceof JSONArray)) {
                    throw new ServerException("payload must be an array");
                }
                JSONArray arr = (JSONArray) payload;
                if (arr.length() != pcnt) {
                    throw new ServerException(String.format("method needs %d elements", pcnt));
                }
                result = m.invoke(this, arr.toList().toArray());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ServerException(e);
        }

        return result;
    }

}
