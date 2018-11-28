package co.gongzh.procbridge;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ServerClientTest {

    private static final int PORT = 8000;

    private static Server server;
    private Client client;

    @BeforeClass
    public static void setUpClass() throws Exception {
        server = new Server(PORT, new Delegate() {
            @Handler
            Object echo(Object payload) {
                return payload;
            }

            @Handler
            int sum(JSONArray numbers) {
                return numbers.toList().stream().mapToInt(el -> (int) el).sum();
            }

            @Handler
            void err() {
                throw new RuntimeException("generated error");
            }

            @Override
            protected @Nullable Object handleUnknownRequest(@Nullable String method, @Nullable Object payload) {
                return null;
            }
        });
        server.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        server.stop();
        server = null;
    }

    @Before
    public void setUp() throws Exception {
        client = new Client("127.0.0.1", PORT);
    }

    @After
    public void tearDown() throws Exception {
        client = null;
    }

    @Test
    public void testNone() {
        Object reply = client.request(null, null);
        assertNull(reply);
        reply = client.request("echo", null);
        assertNull(reply);
        reply = client.request(null, "hello");
        assertNull(reply);
    }

    @Test
    public void testEcho() {
        Object reply = client.request("echo", 123);
        assertEquals(123, reply);
        reply = client.request("echo", 3.14);
        assertEquals(3.14, reply);
        reply = client.request("echo", "hello");
        assertEquals("hello", reply);
        reply = client.request("echo", Arrays.asList(1, 2, 3));
        assertNotNull(reply);
        assertEquals(Arrays.asList(1, 2, 3), ((JSONArray) reply).toList());
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");
        reply = client.request("echo", obj);
        assertNotNull(reply);
        assertEquals("value", ((JSONObject) reply).optString("key"));
    }

    @Test
    public void testSum() {
        Object reply = client.request("sum", Arrays.asList(1, 2, 3, 4));
        assertEquals(10, reply);
    }

    @Test
    public void testError() {
        ServerException exception = null;
        try {
            client.request("err", null);
        } catch (ServerException ex) {
            exception = ex;
        }
        assertNotNull(exception);
        assertEquals("generated error", exception.getMessage());
    }

    @Test
    public void testBigPayload() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("article.txt").getFile());
            String text = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);
            Object reply = client.request("echo", text);
            assertEquals(text, reply);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
