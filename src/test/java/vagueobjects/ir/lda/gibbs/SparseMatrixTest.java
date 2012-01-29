package vagueobjects.ir.lda.gibbs;

import junit.framework.TestCase;

import java.util.Arrays;

public class SparseMatrixTest extends TestCase{


    public void testSparse1(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0,1);
        int x = matrix.get(0,1);
        assertEquals(1, x);
    }

    public void testSparse2(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0,1);
        matrix.add(0,1);
        assertEquals(2, matrix.get(0,1));
    }

    public void testSparse3(){
        SparseMatrix matrix  = new SparseMatrix(1,10);
        matrix.add(0, 1);
        matrix.add(0, 1);
        matrix.add(0, 2);
        matrix.add(0, 0);
        matrix.add(0, 9);
        matrix.add(0, 3);
        int x = matrix.get(0,1);
        assertEquals(2, x);
        verifySorted(matrix);
    }

    public void testSparse3a(){
        SparseMatrix matrix  = new SparseMatrix(1,10);
        matrix.add(0, 1);
        matrix.add(0, 1);
        matrix.add(0, 2);
        matrix.increment(0, 0);
        matrix.increment(0, 9);
        matrix.increment(0, 3);
        int x = matrix.get(0,1);
        assertEquals(2, x);
        verifySorted(matrix);
    }
    public void testSparse4(){
        SparseMatrix matrix  = new SparseMatrix(2,4);
        matrix.add(0,1);
        matrix.add(0,2);
        matrix.add(0,0);
        assertEquals(1, matrix.get(0,1));
        verifySorted(matrix);
    }

    public void testRemoveAt0(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0,0);
        matrix.add(0, 1);
        matrix.add(0,1);
        matrix.add(0,2);
        assertTrue(Arrays.equals(new int[]{4,6,9}, matrix.array[0]));
        matrix.removeAt(0,2);
        assertTrue(Arrays.equals(new int[]{4,6}, matrix.array[0]));
        verifySorted(matrix);
    }


    public void testRemoveAt1(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0,0);
        matrix.add(0,1);
        matrix.add(0,1);
        matrix.add(0,2);
        assertTrue(Arrays.equals(new int[]{4, 6, 9}, matrix.array[0]));
        matrix.removeAt(0, 1);
        assertTrue(Arrays.equals(new int[]{4, 9}, matrix.array[0]));
        verifySorted(matrix);
    }

    public void testRemoveAt2(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0,0);
        matrix.add(0,1);
        matrix.add(0,1);
        matrix.add(0,2);
        verifySorted(matrix);
        assertEquals(3, matrix.size(0));
        matrix.removeAt(0,0);
        assertEquals(2, matrix.size(0));
        assertTrue(Arrays.equals(new int[]{6,9}, matrix.array[0]));
        verifySorted(matrix);
    }

    public void testIncrement0(){
        SparseMatrix vector  = new SparseMatrix(1,3);
        vector.add(0,1);
        int x = vector.get(0,1);
        assertEquals(1, x);
        vector.increment(0,1);
        x = vector.get(0,1);
        assertEquals(2, x);
        assertTrue(verifySorted(vector));
        verifySorted(vector);
    }
    public void testIncrement1(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0, 1);
        matrix.add(0, 2);
        matrix.add(0, 0);
        int x = matrix.get(0,1);
        assertEquals(1, x);
        matrix.increment(0, 1);
        x = matrix.get(0,1);
        assertEquals(2, x);
        assertTrue(verifySorted(matrix));
    }
    public void testIncrement2(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0, 1);
        matrix.add(0, 2);
        matrix.add(0, 0);
        int x = matrix.get(0,2);
        assertEquals(1, x);
        matrix.increment(0, 2);
        x = matrix.get(0,2);
        assertEquals(2, x);
        assertEquals(1, matrix.get(0, 1));
        assertTrue(verifySorted(matrix));
    }
    public void testDecrement0(){
        SparseMatrix vector  = new SparseMatrix(1,3);
        vector.add(0,1);
        vector.decrement(0,1);
        assertEquals(0, vector.size(0));
    }
    public void testDecrement1(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0, 1);
        matrix.add(0, 1);
        matrix.add(0, 2);
        matrix.add(0, 0);
        int x = matrix.get(0,1);
        assertEquals(2, x);
        matrix.decrement(0,1);
        x = matrix.get(0,1);
        assertEquals(1, x);
        assertTrue("not sorted properly...", verifySorted(matrix));
    }
    public void testDecrement2(){
        SparseMatrix matrix  = new SparseMatrix(1,3);
        matrix.add(0, 1);
        matrix.add(0, 1);
        int x = matrix.get(0,1);
        assertEquals(2, x);
        matrix.decrement(0,1);
        assertEquals(1, matrix.get(0, 1));
        matrix.decrement(0,1);
        assertEquals(0, matrix.size(0));
        verifySorted(matrix);
    }
    
    boolean verifySorted(SparseMatrix matrix){
        for(int[] row: matrix.array){
            int prev=0;
            for(int c=0; c<row.length; ++c){
                if(row[c]<prev){
                    return false;
                }
                prev = row[c];
            }
        }
        return true;
    }
}
