package co.gongzh.procbridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author Gong Zhang
 */
public class Client {

    private final @NotNull String host;
    private final int port;
    private final long timeout;
    private final @Nullable Executor executor;

    public static final long FOREVER = 0;

    public Client(@NotNull String host, int port, long timeout, @Nullable Executor executor) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.executor = executor;
    }

    public Client(@NotNull String host, int port) {
        this(host, port, FOREVER, null);
    }

    @NotNull
    public final String getHost() {
        return host;
    }

    public final int getPort() {
        return port;
    }

    public long getTimeout() {
        return timeout;
    }

    @Nullable
    public Executor getExecutor() {
        return executor;
    }

    @Nullable
    public final Object request(@Nullable String method, @Nullable Object payload) throws ClientException, TimeoutException, ServerException {
        final StatusCode[] respStatusCode = { null };
        final Object[] respPayload = { null };
        final Throwable[] innerException = { null };

        try (final Socket socket = new Socket(host, port)) {
            Runnable task = () -> {
                try (OutputStream os = socket.getOutputStream();
                     InputStream is = socket.getInputStream()) {

                    Protocol.writeRequest(os, method, payload);
                    Map.Entry<StatusCode, Object> entry = Protocol.readResponse(is);
                    respStatusCode[0] = entry.getKey();
                    respPayload[0] = entry.getValue();

                } catch (Exception ex) {
                    innerException[0] = ex;
                }
            };

            if (timeout <= 0) {
                task.run();
            } else {
                TimeoutExecutor guard = new TimeoutExecutor(timeout, executor);
                guard.execute(task);
            }
        } catch (IOException ex) {
            throw new ClientException(ex);
        }

        if (innerException[0] != null) {
            throw new RuntimeException(innerException[0]);
        }

        if (respStatusCode[0] != StatusCode.GOOD_RESPONSE) {
            throw new ServerException((String) respPayload[0]);
        }

        return respPayload[0];
    }

}
