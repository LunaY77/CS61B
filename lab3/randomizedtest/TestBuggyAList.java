package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    // YOUR TESTS HERE
    @Test
    public void testTwoAList() {

        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> ls = new BuggyAList<>();

        int N = 50000;
        for (int i = 0; i < N; i++) {
            int operationNumber = StdRandom.uniform(0, 3);
            int randVal = StdRandom.uniform(0, 100);
            if (L.size() != 0 && ls.size() != 0) {
                if (operationNumber == 1) {
                    // addLast
                    L.addLast(randVal);
                    ls.addLast(randVal);
                    System.out.println("AddLast: " + randVal);
                } else if (operationNumber == 2) {
                    // removeLast
                    assertEquals(L.removeLast(), ls.removeLast());
                } else {
                    // size
                    int size1 = L.size();
                    int size2 = ls.size();
                    assertEquals(size2, size1);
                }
            } else{
                if (operationNumber == 1) {
                    // addLast
                    L.addLast(randVal);
                    ls.addLast(randVal);
                    System.out.println("AddLast: " + randVal);
                } else if (operationNumber == 2) {
                    // size
                    int size1 = L.size();
                    int size2 = ls.size();
                    assertEquals(size2, size1);
                }
            }
        }
    }
}
