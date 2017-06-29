package com.cpjd.roblu;

import com.cpjd.roblu.utils.Text;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void contains() throws Exception {
        assertEquals(Text.contains("Team CRUSH", "CRUSH"), true);
    }
}