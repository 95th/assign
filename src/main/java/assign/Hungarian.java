package assign;

import java.util.Arrays;

public class Hungarian {
    private static final byte UNMARKED = 0;
    private static final byte MARK_ROW = -1;
    private static final byte MARK_COL = 1;
    private static final byte MARK_BOTH = 2;

    private final boolean maximize;

    private double[][] originalValues;
    private double[][] values;

    private byte[][] lines;
    private int lineCount;

    private boolean[] markedRows;
    private boolean[] markedCols;

    private boolean[] occupiedCols;
    private int[] rows;

    private int rowCount;
    private int size;

    public Hungarian(boolean maximize) {
        this.maximize = maximize;
    }

    public void setMatrixSize(int rows, int cols) {
        this.rowCount = rows;
        this.size = Math.max(rows, cols);
        ensureCapacity();
    }

    public void setCell(int row, int col, double value) {
        originalValues[row][col] = value;
        values[row][col] = maximize ? -value : value;
    }

    public void reduce() {
        subtractRowMin();
        subtractColMin();

        coverZeros();
        while (lineCount != size) {
            createAdditionalZeros();
            coverZeros();
        }

        optimization();
    }

    public int[] getResult() {
        return Arrays.copyOf(rows, rowCount);
    }

    public double maximumAverage() {
        if (rowCount == 0) {
            return 0;
        }

        double total = 0;
        for (int row = 0; row < rowCount; row++) {
            total += originalValues[row][rows[row]];
        }

        return total / rowCount;
    }

    private void subtractRowMin() {
        for (int r = 0; r < size; r++) {
            double min = Double.POSITIVE_INFINITY;
            for (int c = 0; c < size; c++) {
                double val = values[r][c];
                if (min > val) {
                    min = val;
                }
            }
            for (int c = 0; c < size; c++) {
                values[r][c] -= min;
            }
        }
    }

    private void subtractColMin() {
        for (int c = 0; c < size; c++) {
            double min = Double.POSITIVE_INFINITY;
            for (int r = 0; r < size; r++) {
                double val = values[r][c];
                if (min > val) {
                    min = val;
                }
            }
            for (int r = 0; r < size; r++) {
                values[r][c] -= min;
            }
        }
    }

    private void coverZeros() {
        tryAssignment();

        Arrays.fill(markedRows, 0, size, false);
        Arrays.fill(markedCols, 0, size, false);
        clear(lines);

        // System.out.println("Initially values:");
        // printValues();

        // System.out.println("Initially lines:");
        // printLines();

        for (int r = 0; r < size; r++) {
            if (rows[r] == -1) {
                markRow(r);
            }
        }

        lineCount = invertRowMarks();

        // System.out.println("Invert lines:");
        // printLines();
    }

    private void tryAssignment() {
        Arrays.fill(rows, 0, size, -1);
        Arrays.fill(occupiedCols, 0, size, false);

        int limit = 0;
        int count = 0;
        while (count < size && limit < size) {
            limit++;
            for (int row = 0; row < size; row++) {
                int lastCol = -1;
                int zeros = 0;
                for (int col = 0; col < size; col++) {
                    if (values[row][col] == 0 && !occupiedCols[col]) {
                        zeros++;
                        if (zeros > limit) {
                            break;
                        }
                        lastCol = col;
                    }
                }

                if (lastCol != -1 && zeros <= limit) {
                    rows[row] = lastCol;
                    occupiedCols[lastCol] = true;
                    count++;
                }
            }

            for (int col = 0; col < size; col++) {
                if (occupiedCols[col]) {
                    continue;
                }

                int lastRow = -1;
                int zeros = 0;
                for (int row = 0; row < size; row++) {
                    if (values[row][col] == 0 && rows[row] != -1) {
                        zeros++;
                        if (zeros > limit) {
                            break;
                        }
                        lastRow = row;
                    }
                }

                if (lastRow != -1 && zeros <= limit) {
                    rows[lastRow] = col;
                    occupiedCols[col] = true;
                    count++;
                }
            }
        }
    }

    private void markRow(int row) {
        if (markedRows[row]) {
            return;
        }

        markedRows[row] = true;

        for (int col = 0; col < size; col++) {
            byte mark = lines[row][col];
            if (mark == MARK_COL || mark == MARK_BOTH) {
                lines[row][col] = MARK_BOTH;
            } else {
                lines[row][col] = MARK_ROW;
            }
        }

        // System.out.println("Marking row: " + row);
        // printLines();

        for (int col = 0; col < size; col++) {
            if (values[row][col] == 0) {
                markColumn(col);
            }
        }
    }

    private void markColumn(int col) {
        if (markedCols[col]) {
            return;
        }

        markedCols[col] = true;

        for (int row = 0; row < size; row++) {
            byte mark = lines[row][col];
            if (mark == MARK_ROW || mark == MARK_BOTH) {
                lines[row][col] = MARK_BOTH;
            } else {
                lines[row][col] = MARK_COL;
            }
        }

        // System.out.println("Marking column: " + col);
        // printLines();

        for (int row = 0; row < size; row++) {
            if (rows[row] == col) {
                markRow(row);
            }
        }
    }

    private int invertRowMarks() {
        int count = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                byte mark = lines[row][col];
                switch (mark) {
                    case UNMARKED:
                        lines[row][col] = MARK_ROW;
                        break;
                    case MARK_ROW:
                        lines[row][col] = UNMARKED;
                        break;
                    case MARK_COL:
                        lines[row][col] = MARK_BOTH;
                        break;
                    case MARK_BOTH:
                        lines[row][col] = MARK_COL;
                        break;
                    default:
                        break;
                }
                if (row == col) {
                    count += Math.abs(lines[row][col]);
                }
            }
        }
        return count;
    }

    private void createAdditionalZeros() { // NOSONAR
        double minUncoveredVal = Double.POSITIVE_INFINITY;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (lines[row][col] != UNMARKED) {
                    continue;
                }

                double val = values[row][col];
                if (minUncoveredVal > val) {
                    minUncoveredVal = val;
                }
            }
        }

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                byte mark = lines[row][col];
                if (mark == UNMARKED) {
                    values[row][col] -= minUncoveredVal;
                } else if (mark == MARK_BOTH) {
                    values[row][col] += minUncoveredVal;
                }
            }
        }
    }

    private void optimization() {
        Arrays.fill(rows, 0, size, -1);
        Arrays.fill(occupiedCols, 0, size, false);
        optimization(0);
    }

    private boolean optimization(int row) {
        if (row >= size) {
            return true;
        }

        for (int col = 0; col < size; col++) {
            if (values[row][col] == 0 && !occupiedCols[col]) {
                rows[row] = col;
                occupiedCols[col] = true;
                if (optimization(row + 1)) {
                    return true;
                }
                occupiedCols[col] = false;
            }
        }

        return false;
    }

    private void ensureCapacity() {
        if (originalValues == null || originalValues.length < size) {
            int oldLength = originalValues == null ? 0 : originalValues.length;
            int newLength = Math.max(size, oldLength * 2);

            originalValues = new double[newLength][newLength];
            values = new double[newLength][newLength];
            lines = new byte[newLength][newLength];
            markedRows = new boolean[newLength];
            markedCols = new boolean[newLength];
            occupiedCols = new boolean[newLength];
            rows = new int[newLength];
        } else {
            clear(originalValues);
            clear(values);
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

    // private void printLines() {
    // for (int row = 0; row < size; row++) {
    // for (int col = 0; col < size; col++) {
    // System.out.printf("% 1d | ", lines[row][col]);
    // }
    // System.out.println();
    // }
    // System.out.println();
    // }

    // private void printValues() {
    // for (int row = 0; row < size; row++) {
    // for (int col = 0; col < size; col++) {
    // System.out.printf("% 1.3f | ", values[row][col]);
    // }
    // System.out.println();
    // }
    // System.out.println();
    // }
}
