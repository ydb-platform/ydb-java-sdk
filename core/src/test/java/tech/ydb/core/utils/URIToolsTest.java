package tech.ydb.core.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class URIToolsTest {
    @Test
    public void emptyQueryTest() throws URISyntaxException {
        assertTrue(URITools.splitQuery(new URI("grpc://localhost:2136/local")).isEmpty());
        assertTrue(URITools.splitQuery(new URI("grpc://localhost:2136/local?")).isEmpty());
    }

    @Test
    public void multipleValuesTest() throws URISyntaxException {
        Map<String, List<String>> params = URITools.splitQuery(
                new URI("grpc://localhost:2136/?database=/local&node_id=1&node_id=2")
        );

        assertEquals(Collections.singletonList("/local"), params.get("database"));
        assertEquals(Arrays.asList("1", "2"), params.get("node_id"));
    }

    @Test
    public void decodedValuesTest() throws URISyntaxException {
        Map<String, List<String>> params = URITools.splitQuery(
                new URI("grpc://localhost:2136/?database=%2Flocal%2Fdb")
        );

        assertEquals(Collections.singletonList("/local/db"), params.get("database"));
    }

    @Test
    public void parameterWithoutValueTest() throws URISyntaxException {
        Map<String, List<String>> params = URITools.splitQuery(
                new URI("grpc://localhost:2136/?database=&flag&node_id=1")
        );

        // a parameter without a value is reported as a null value, not as a failure
        assertEquals(Collections.singletonList(null), params.get("database"));
        assertEquals(Collections.singletonList(null), params.get("flag"));
        assertEquals(Collections.singletonList("1"), params.get("node_id"));
    }
}
