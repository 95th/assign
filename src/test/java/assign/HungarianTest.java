package assign;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class HungarianTest {

    void run_test(int rows, int cols, double[][] matrix, int[] expected) {
        Hungarian h = new Hungarian(true);
        h.setMatrixSize(rows, cols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                h.setCell(i, j, matrix[i][j]);
            }
        }

        h.reduce();
        assertArrayEquals(expected, h.getResult());
    }

    @Test
    void test_01() {
        double[][] matrix = { { 1, 2, 3 }, { 5, 1, 7 }, { 1, 1, 0 } };
        int[] expected = { 1, 2, 0 };
        run_test(3, 3, matrix, expected);
    }

    @Test
    void test_02() {
        double[][] matrix = { { 0, 0, 0 }, { 1, 0, 0 }, { 1, 0, 1 } };
        int[] expected = { 1, 0, 2 };
        run_test(3, 3, matrix, expected);
    }

    @Test
    void test_03() {
        double[][] matrix = { { 0, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 } };
        int[] expected = { 2, 1, 0 };
        run_test(3, 3, matrix, expected);
    }

    @Test
    void test_04() {
        double[][] matrix = { { 0, 0, 0, 0, 0, 0 }, { 0.42, 0, 0, 0, 0, 0 }, { 0.41, 0, 0, 0, 0, 0 } };
        int[] expected = { 5, 0, 1 };
        run_test(3, 6, matrix, expected);
    }

    @Test
    void test_05() {
        double[][] matrix = { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 1 } };
        int[] expected = { 0, 1, 5 };
        run_test(3, 6, matrix, expected);
    }

    @Test
    void test_06() {
        double[][] matrix = { { 1, 2, 1 }, { 5, 4, 3 }, { 1, 1, 2 } };
        int[] expected = { 1, 0, 2 };
        run_test(3, 3, matrix, expected);
    }

    @Test
    void test_07() {
        double[][] matrix = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 1, 2 }, { 0, 0, 3, 5 } };
        int[] expected = { 0, 1, 2, 3 };
        run_test(4, 4, matrix, expected);
    }
}
