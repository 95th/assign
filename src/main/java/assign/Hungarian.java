package assign;

import java.util.Arrays;

/**
 * Java implementation of https://github.com/bmc/munkres/blob/master/munkres.py
 * 
 * The Munkres module provides an implementation of the Munkres algorithm (also
 * called the Hungarian algorithm or the Kuhn-Munkres algorithm), useful for
 * solving the Assignment Problem.
 * 
 * For complete usage documentation, see: https://software.clapper.org/munkres/
 */
public class Hungarian {
    private final boolean maximize;
    private final boolean debug;

    private double[][] originalCost;
    private double[][] cost;
    private byte[][] mask;

    private int rowCount;
    private int size;
    private int step;

    private int[] rowCover;
    private int[] colCover;

    private int row;
    private int col;

    private int pathRow0;
    private int pathCol0;
    private int pathCount;
    private int[][] path;

    /**
     * Create new solver with minimization solution and debug off.
     */
    public Hungarian() {
        this(false, false);
    }

    /**
     * Create new solver with given solution and debug off.
     */
    public Hungarian(boolean maximize) {
        this(maximize, false);
    }

    /**
     * Create new solver with given solution and debug.
     */
    public Hungarian(boolean maximize, boolean debug) {
        this.maximize = maximize;
        this.debug = debug;
    }

    /**
     * Set required size of the matrix.
     */
    public void setMatrixSize(int rows, int cols) {
        this.rowCount = rows;
        this.size = Math.max(rows, cols);
        ensureCapacity();
    }

    /**
     * Set value in given row and column.
     */
    public void setCell(int row, int col, double value) {
        this.originalCost[row][col] = value;
        this.cost[row][col] = maximize ? -value : value;
    }

    /**
     * Return an array of mapping of rows to columns where index is row and its
     * value is the mapped column.
     */
    public int[] getResult() {
        int[] out = new int[rowCount];
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < size; c++) {
                if (mask[r][c] == 1) {
                    out[r] = c;
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Return the optimal average (maximum or minimum based on the provided
     * parameter maximize)
     */
    public double optimalAverage() {
        if (rowCount == 0) {
            return 0;
        }

        double sum = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < size; c++) {
                if (mask[r][c] == 1) {
                    sum += originalCost[r][c];
                    break;
                }
            }
        }

        return sum / rowCount;
    }

    /**
     * Solve the assignment.
     */
    public void reduce() {
        boolean done = false;
        step = 1;
        while (!done) {
            printCostMatrix();
            printMaskMatrix();

            switch (step) {
                case 1:
                    stepOne();
                    break;
                case 2:
                    stepTwo();
                    break;
                case 3:
                    stepThree();
                    break;
                case 4:
                    stepFour();
                    break;
                case 5:
                    stepFive();
                    break;
                case 6:
                    stepSix();
                    break;
                default:
                    done = true;
                    break;
            }
        }
    }

    private void stepOne() {
        for (int r = 0; r < size; r++) {
            double minValue = Double.POSITIVE_INFINITY;
            for (int c = 0; c < size; c++) {
                if (minValue > cost[r][c]) {
                    minValue = cost[r][c];
                }
            }

            for (int c = 0; c < size; c++) {
                cost[r][c] -= minValue;
            }
        }

        step = 2;
    }

    private void stepTwo() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (cost[r][c] == 0 && rowCover[r] == 0 && colCover[c] == 0) {
                    mask[r][c] = 1;
                    rowCover[r] = 1;
                    colCover[c] = 1;
                }
            }
        }

        clearCovers();
        step = 3;
    }

    private void stepThree() {
        int colCount = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (mask[r][c] == 1 && colCover[c] == 0) {
                    colCover[c] = 1;
                    colCount++;
                }
            }
        }

        if (colCount >= size) {
            step = 7;
        } else {
            step = 4;
        }
    }

    private void stepFour() {
        boolean done = false;

        while (!done) {
            findAZero();
            if (row < 0) {
                done = true;
                step = 6;
            } else {
                mask[row][col] = 2;
                if (findStarInRow(row)) {
                    rowCover[row] = 1;
                    colCover[col] = 0;
                } else {
                    done = true;
                    step = 5;
                    pathRow0 = row;
                    pathCol0 = col;
                }
            }
        }
    }

    private void stepFive() {
        boolean done = false;
        row = -1;
        col = -1;

        pathCount = 0;
        path[pathCount][0] = pathRow0;
        path[pathCount][1] = pathCol0;

        while (!done) {
            findStarInCol(path[pathCount][1]);
            if (row >= 0) {
                pathCount++;
                path[pathCount][0] = row;
                path[pathCount][1] = path[pathCount - 1][1];
            } else {
                done = true;
            }
            if (!done) {
                findPrimeInRow(path[pathCount - 1][0]);
                pathCount++;
                path[pathCount][0] = path[pathCount - 1][0];
                path[pathCount][1] = col;
            }
        }
        augmentPaths();
        clearCovers();
        erasePrimes();
        step = 3;
    }

    private void stepSix() {
        double minVal = findSmallest();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (rowCover[r] == 1) {
                    cost[r][c] += minVal;
                }

                if (colCover[c] == 0) {
                    cost[r][c] -= minVal;
                }
            }
        }
        step = 4;
    }

    private void findAZero() {
        row = -1;
        col = -1;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (cost[r][c] == 0 && rowCover[r] == 0 && colCover[c] == 0) {
                    row = r;
                    col = c;
                    return;
                }
            }
        }
    }

    private boolean findStarInRow(int row) {
        for (int c = 0; c < size; c++) {
            if (mask[row][c] == 1) {
                col = c;
                return true;
            }
        }
        return false;
    }

    private void findStarInCol(int col) {
        row = -1;
        for (int r = 0; r < size; r++) {
            if (mask[r][col] == 1) {
                row = r;
                break;
            }
        }
    }

    private void findPrimeInRow(int row) {
        col = -1;
        for (int c = 0; c < size; c++) {
            if (mask[row][c] == 2) {
                col = c;
                break;
            }
        }
    }

    private void augmentPaths() {
        for (int i = 0; i <= pathCount; i++) {
            int r = path[i][0];
            int c = path[i][1];

            if (mask[r][c] == 1) {
                mask[r][c] = 0;
            } else {
                mask[r][c] = 1;
            }
        }
    }

    private void clearCovers() {
        Arrays.fill(rowCover, 0, size, 0);
        Arrays.fill(colCover, 0, size, 0);
    }

    private void erasePrimes() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (mask[r][c] == 2) {
                    mask[r][c] = 0;
                }
            }
        }
    }

    private double findSmallest() {
        double minVal = Double.POSITIVE_INFINITY;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (rowCover[r] == 0 && colCover[c] == 0 && minVal > cost[r][c]) {
                    minVal = cost[r][c];
                }
            }
        }
        return minVal;
    }

    private void printCostMatrix() {
        if (!debug) {
            return;
        }

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                System.out.printf(" % 1.3f |", cost[r][c]);
            }
            System.out.println();
        }
    }

    private void printMaskMatrix() {
        if (!debug) {
            return;
        }

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                System.out.printf(" %2d |", mask[r][c]);
            }
            System.out.println();
        }
    }

    private void ensureCapacity() {
        if (cost == null || cost.length < size) {
            int oldLength = cost == null ? 0 : cost.length;
            int newLength = Math.max(size, oldLength * 2);

            originalCost = new double[newLength][newLength];
            cost = new double[newLength][newLength];
            mask = new byte[newLength][newLength];
            rowCover = new int[newLength];
            colCover = new int[newLength];
            path = new int[newLength * newLength][2];
        } else {
            clear(originalCost);
            clear(cost);
            clear(mask);
            clear(path);
            clear(rowCover);
            clear(colCover);
        }
    }

    private void clear(int[][] arr) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                arr[r][c] = 0;
            }
        }
    }

    private void clear(double[][] arr) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                arr[r][c] = 0;
            }
        }
    }

    private void clear(byte[][] arr) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                arr[r][c] = 0;
            }
        }
    }

    private void clear(int[] arr) {
        for (int i = 0; i < size; i++) {
            arr[i] = 0;
        }
    }
}
