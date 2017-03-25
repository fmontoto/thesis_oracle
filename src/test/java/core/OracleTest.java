package core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by fmontoto on 19-01-17.
 */
public class OracleTest {
    Oracle t1, t2;
    String a1, a2;

    @Before
    public void setUp() {
        a1 = "1F1tAaz5x1HUXrCNLbtMDqcw6o5GNn4xqX";
        a2 = "mrV3e1QTX2ZqkNcTreaqYbTKLD7ASFXkVA";
        t1 = new Oracle(a1);
        t2 = new Oracle(a2);
    }

    @Test
    public void serialize() throws Exception {
        assertEquals(a1, Oracle.loadFromSerialized(t1.serialize()).getAddress());
        assertEquals(a2, Oracle.loadFromSerialized(t2.serialize()).getAddress());
    }
}