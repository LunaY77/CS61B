package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AListBugTest {
    @Test
    public void testThreeAddThreeRemove() {

        AListNoResizing<Integer> AList = new AListNoResizing<>();
        BuggyAList<Integer> buggyAList = new BuggyAList<>();

        AList.addLast(1);
        AList.addLast(2);
        AList.addLast(3);
        buggyAList.addLast(1);
        buggyAList.addLast(2);
        buggyAList.addLast(3);

        assertEquals(AList.size(), buggyAList.size());

        assertEquals(AList.removeLast(), buggyAList.removeLast());
        assertEquals(AList.removeLast(), buggyAList.removeLast());
        assertEquals(AList.removeLast(), buggyAList.removeLast());
    }

    @Test
    public void testRandomized() {
        AListNoResizing<Integer> L = new AListNoResizing<>();

        int N  = 500;
        for (int i = 0; i < N; i ++) {
            int operationNumber = StdRandom.uniform(0, 2);
            if (operationNumber == 0) {
                // addLast
                int randval = StdRandom.uniform(0, 100);
                L.addLast(randval);
                System.out.println(randval);
            } else if (operationNumber == 1) {
                //size
                int size = L.size();
                System.out.println("size: " + size);
            }
        }
    }

    @Test
    public void testMoreRandomized() {
        AListNoResizing<Integer> L = new AListNoResizing<>();

        int N = 500;
        for (int i = 0; i < N; i++) {
            int operationNumber = StdRandom.uniform(0, 3);
            int randVal = StdRandom.uniform(0, 100);
            if (L.size() != 0) {
                if (operationNumber == 1) {
                    // addLast
                    L.addLast(randVal);
                    System.out.println("AddLast: " + randVal);
                } else if (operationNumber == 2) {
                    // removeLast
                    System.out.println("RemoveLast: " + L.removeLast());
                } else {
                    // size
                    int size = L.size();
                    System.out.println(size);
                }
            } else{
                if (operationNumber == 1) {
                    // addLast
                    L.addLast(randVal);
                    System.out.println("AddLast: " + randVal);
                } else if (operationNumber == 2) {
                    // size
                    int size = L.size();
                    System.out.println(size);
                }
            }
        }
    }
}
