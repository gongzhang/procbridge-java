package co.gongzh.procbridge;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gong Zhang
 */
public final class Server {

    private final int port;
    private final @NotNull IDelegate delegate;

    private ExecutorService executor;
    private ServerSocket serverSocket;
    private boolean started;

    public Server(int port, @NotNull IDelegate delegate) {
        this.port = port;
        this.delegate = delegate;

        this.started = false;
        this.executor = null;
        this.serverSocket = null;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    public int getPort() {
        return port;
    }

    public synchronized void start() throws IOException {
        if (started) {
            throw new IllegalStateException("server already started");
        }

        final ServerSocket serverSocket = new ServerSocket(this.port);
        this.serverSocket = serverSocket;

        final ExecutorService executor = Executors.newCachedThreadPool();
        this.executor = executor;
        executor.execute(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    Connection conn = new Connection(socket, delegate);
                    synchronized (Server.this) {
                        if (!started) {
                            return; // finish listener
                        }
                        executor.execute(conn);
                    }
                } catch (IOException ignored) {
                    return; // finish listener
                }
            }
        });

        started = true;
    }

    public synchronized void stop() {
        if (!started) {
            throw new IllegalStateException("server does not started");
        }

        executor.shutdown();
        executor = null;

        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        serverSocket = null;

        this.started = false;
    }

    static final class Connection implements Runnable {

        private final Socket socket;
        private final IDelegate delegate;

        Connection(Socket socket, IDelegate delegate) {
            this.socket = socket;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try (OutputStream os = socket.getOutputStream();
                 InputStream is = socket.getInputStream()) {

                Map.Entry<String, Object> req = Protocol.readRequest(is);
                String method = req.getKey();
                Object payload = req.getValue();

                Object result = null;
                Exception exception = null;
                try {
                    result = delegate.handleRequest(method, payload);
                } catch (Exception ex) {
                    exception = ex;
                }

                if (exception != null) {
                    Protocol.writeBadResponse(os, exception.getMessage());
                } else {
                    Protocol.writeGoodResponse(os, result);
                }
            } catch (Exception ignored) {
            }
        }

    }

}
