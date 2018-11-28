package co.gongzh.procbridge;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.*;

public class ServerClientTest {

    private static Server server;
    private Client client;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            server = new TestServer();
            server.start();
        } catch (ServerException ex) {
            System.out.println(String.format("use existing server on port %d", TestServer.PORT));
            server = null;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Before
    public void setUp() throws Exception {
        client = new Client("127.0.0.1", TestServer.PORT);
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
            File file = new File(Objects.requireNonNull(classLoader.getResource("article.txt")).getFile());
            String text = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);
            Object reply = client.request("echo", text);
            assertEquals(text, reply);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
