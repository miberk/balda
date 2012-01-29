package vagueobjects.ir.lda.gibbs;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Arrays;

/**
 * This class is useful when dealing with sparse arrays; it takes lesser amount of memory (virtually linear
 * in the number of rows).
 */
class SparseMatrix {
    /**
     * Rows are maintained in sorted order
     */
    int[][] array;
    final int mask;
    final int shift;

    public SparseMatrix(int numRows, int maxKey) {
        this.shift = minExponent(maxKey);
        this.mask = (1 << shift) - 1;
        this.array = new int[numRows][];
        for (int i = 0; i < numRows; ++i) {
            array[i] = new int[0];
        }
    }

    public int size(int row) {
        return array[row].length;
    }

    /**
     * Increments value at a given position.
     * This is a slow operation that involves sorting of the
     * array.
     *
     * @param t - key for the value to increment
     */
    public void add(int r, int t) {
        int length = array[r].length;
        if (length == 0) {
            startArray(r, t);
        } else {
            boolean found = false;
            for (int i = 0; i < length; ++i) {
                if ((array[r][i] & mask) == t) {
                    int v = 1 + (array[r][i] >> shift);
                    array[r][i] = (v << shift) + t;
                    found = true;
                    break;
                }
            }
            if (!found) {
                int[] _arr = new int[length + 1];
                System.arraycopy(array[r], 0, _arr, 0, length);
                _arr[length] = (1 << shift) + t;
                array[r] = _arr;
            }
            Arrays.sort(array[r]);
        }
    }

    /**
     * Increments element at a given key
     *
     * @param t - the key
     */
    public void increment(int r, int t) {
        if (array[r].length == 0) {
            startArray(r, t);
        } else {
            boolean found = false;
            for (int i = 0; i < array[r].length; ++i) {
                if ((array[r][i] & mask) == t) {
                    //increment the element at this position
                    int newValue = 1 + (array[r][i] >> shift);
                    array[r][i] = (newValue << shift) + t;
                    //if origValue+1 exceeds the next element, 
                    //swap subsequent elements until all elements are sorted
                    for (int j = i + 1; j < array[r].length; ++j) {
//                        int nextValue = array[r][j]>>shift;
                        if (array[r][j] < array[r][j - 1]) {
                            //swap 
                            int temp = array[r][j];
                            array[r][j] = array[r][j - 1];
                            array[r][j - 1] = temp;
                        } else {
                            break;
                        }
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                int[] _arr = new int[array[r].length + 1];
                System.arraycopy(array[r], 0, _arr, 0, array[r].length);
                _arr[array[r].length] = (1 << shift) + t;
                array[r] = _arr;
                Arrays.sort(array[r]);
            }
        }
    }

    public void decrement(int r, int t) {
        if (array[r].length == 0) {
            throw new IllegalStateException("Internal array is empty");
        } else {
            boolean found = false;
            for (int i = 0; i < array[r].length; ++i) {
                if ((array[r][i] & mask) == t) {
                    found = true;
                    //increment the element at this position
                    int newValue = (array[r][i] >> shift) - 1;
                    if (newValue == 0) {
                        removeAt(r, i);
                        break;
                    }
                    array[r][i] = (newValue << shift) + t;
                    //if origValue+1 exceeds the next element, 
                    //swap subsequent elements until all elements are sorted
                    for (int j = i - 1; j >= 0; --j) {
                        if (array[r][j] > array[r][j + 1]) {
                            //swap 
                            int temp = array[r][j];
                            array[r][j] = array[r][j + 1];
                            array[r][j + 1] = temp;
                        } else {
                            break;
                        }
                    }
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("Could not find the key: " + t);
            }
        }
    }

    void removeAt(int r, int i) {
        //keep elements before and after
        int length = array[r].length;
        if (length == 1) {
            array[r] = new int[0];
        } else {
            int[] _arr = new int[array[r].length - 1];
            if (i > 0) {
                System.arraycopy(array[r], 0, _arr, 0, i);
            }
            System.arraycopy(array[r], i + 1, _arr, i, _arr.length - i);
            array[r] = _arr;
        }
    }

    private void startArray(int r, int t) {
        array[r] = new int[1];
        array[r][0] = (1 << shift) + t;
    }

    /**
     * Traverses the internal array
     */
    public int get(int w, int t) {
        for (int i = array[w].length - 1; i >= 0; i--) {
            if ((array[w][i] & mask) == t) {
                return array[w][i] >> shift;
            }
        }
        return 0;
    }

    /**
     * @return smallest m such that 2**m>=t
     */
    static int minExponent(int t) {
        for (int i = 0; ; ++i) {
            int p = 2 << i;
            if (t <= p) return i + 1;

        }
    }


    @Override
    public String toString() {
        return Arrays.toString(array);
    }
}
