package de.diddiz.LogBlock;

import de.diddiz.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class QueryParsingTest {

    @Test
    public void testParseQuotes() {
        // input = /lb clearlog world "my world of swag" player "player"
        List<String> input = Arrays.asList("lb", "clearlog", "world", "\"my", "world", "of", "swag\"", "player", "\"player\"");
        List<String> expectedOut = Arrays.asList("lb", "clearlog", "world", "\"my world of swag\"", "player", "\"player\"");
        Assert.assertEquals(Utils.parseQuotes(input), expectedOut);
    }

}
