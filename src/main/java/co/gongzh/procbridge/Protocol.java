package co.gongzh.procbridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;

import static co.gongzh.procbridge.ProtocolException.*;


final class Protocol {

    private static final byte[] FLAG = { 'p', 'b' };

    private static @NotNull Map.Entry<StatusCode, JSONObject> read(@NotNull InputStream stream) throws IOException, ProtocolException {
        int b;

        // 1. FLAG
        b = stream.read();
        if (b == -1 || b != FLAG[0]) throw new ProtocolException(UNRECOGNIZED_PROTOCOL);
        b = stream.read();
        if (b == -1 || b != FLAG[1]) throw new ProtocolException(UNRECOGNIZED_PROTOCOL);

        // 2. VERSION
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        if (b != Versions.CURRENT[0]) throw new ProtocolException(INCOMPATIBLE_VERSION);
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        if (b != Versions.CURRENT[1]) throw new ProtocolException(INCOMPATIBLE_VERSION);

        // 3. STATUS CODE
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        StatusCode statusCode = StatusCode.fromRawValue(b);
        if (statusCode == null) {
            throw new ProtocolException(INVALID_STATUS_CODE);
        }

        // 4. RESERVED BYTES (2 bytes)
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);

        // 5. LENGTH (little endian)
        int bodyLen;
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        bodyLen = b;
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        bodyLen |= (b << 8);
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        bodyLen |= (b << 16);
        b = stream.read();
        if (b == -1) throw new ProtocolException(INCOMPLETE_DATA);
        bodyLen |= (b << 24);

        // 6. JSON OBJECT
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int readCount;
        int restCount = bodyLen;
        byte[] buf = new byte[Math.min(bodyLen, 1024 * 1024)];
        while ((readCount = stream.read(buf, 0, Math.min(buf.length, restCount))) != -1) {
            buffer.write(buf, 0, readCount);
            restCount -= readCount;
            if (restCount == 0) {
                break;
            }
        }

        if (buffer.size() != bodyLen) {
            throw new ProtocolException(INCOMPLETE_DATA);
        }

        buffer.flush();
        buf = buffer.toByteArray();

        try {
            String jsonText = new String(buf, StandardCharsets.UTF_8);
            JSONObject body = new JSONObject(jsonText);
            return new AbstractMap.SimpleEntry<>(statusCode, body);
        } catch (Exception ex) {
            throw new ProtocolException(INVALID_BODY);
        }
    }

    private static void write(@NotNull OutputStream stream, @NotNull StatusCode statusCode, @NotNull JSONObject body) throws IOException {
        // 1. FLAG 'p', 'b'
        stream.write(FLAG);

        // 2. VERSION
        stream.write(Versions.CURRENT);

        // 3. STATUS CODE
        stream.write(statusCode.rawValue);

        // 4. RESERVED BYTES (2 bytes)
        stream.write(0);
        stream.write(0);

        // make json object
        byte[] buf = body.toString().getBytes(StandardCharsets.UTF_8);

        // 5. LENGTH (4-byte, little endian)
        int len = buf.length;
        int b0 = len & 0xff;
        int b1 = (len & 0xff00) >> 8;
        int b2 = (len & 0xff0000) >> 16;
        int b3 = (len & 0xff000000) >> 24;
        stream.write(b0);
        stream.write(b1);
        stream.write(b2);
        stream.write(b3);

        // 6. JSON OBJECT
        stream.write(buf);

        stream.flush();
    }

    static @NotNull Map.Entry<String, Object> readRequest(@NotNull InputStream stream) throws IOException, ProtocolException {
        Map.Entry<StatusCode, JSONObject> entry = read(stream);
        StatusCode statusCode = entry.getKey();
        JSONObject body = entry.getValue();
        if (statusCode != StatusCode.REQUEST) {
            throw new ProtocolException(INVALID_STATUS_CODE);
        }
        String method = body.optString(Keys.METHOD);
        Object payload = body.opt(Keys.PAYLOAD);
        return new AbstractMap.SimpleEntry<>(method, payload);
    }

    static @NotNull Map.Entry<StatusCode, Object> readResponse(@NotNull InputStream stream) throws IOException, ProtocolException {
        Map.Entry<StatusCode, JSONObject> entry = read(stream);
        StatusCode statusCode = entry.getKey();
        JSONObject body = entry.getValue();
        if (statusCode == StatusCode.GOOD_RESPONSE) {
            return new AbstractMap.SimpleEntry<>(StatusCode.GOOD_RESPONSE, body.opt(Keys.PAYLOAD));
        } else if (statusCode == StatusCode.BAD_RESPONSE) {
            return new AbstractMap.SimpleEntry<>(StatusCode.BAD_RESPONSE, body.optString(Keys.MESSAGE));
        } else {
            throw new ProtocolException(INVALID_STATUS_CODE);
        }
    }

    static void writeRequest(@NotNull OutputStream stream, @Nullable String method, @Nullable Object payload) throws IOException {
        JSONObject body = new JSONObject();
        if (method != null) {
            body.put(Keys.METHOD, method);
        }
        if (payload != null) {
            body.put(Keys.PAYLOAD, payload);
        }
        write(stream, StatusCode.REQUEST, body);
    }

    static void writeGoodResponse(@NotNull OutputStream stream, @Nullable Object payload) throws IOException {
        JSONObject body = new JSONObject();
        if (payload != null) {
            body.put(Keys.PAYLOAD, payload);
        }
        write(stream, StatusCode.GOOD_RESPONSE, body);
    }

    static void writeBadResponse(@NotNull OutputStream stream, @Nullable String message) throws IOException {
        JSONObject body = new JSONObject();
        if (message != null) {
            body.put(Keys.MESSAGE, message);
        }
        write(stream, StatusCode.BAD_RESPONSE, body);
    }

    private Protocol() {}

}
