package assign;

import java.util.Arrays;

public class Hungarian {
    private static final byte UNMARKED = 0;
    private static final byte MARK_ROW = -1;
    private static final byte MARK_COL = 1;
    private static final byte MARK_BOTH = 2;

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

    public void setMatrixSize(int rows, int cols) {
        this.rowCount = rows;
        this.size = Math.max(rows, cols);
        ensureCapacity();
    }

    public void setCell(int row, int col, double value) {
        originalValues[row][col] = value;
        values[row][col] = value;
    }

    public void reduce() {
        subtractRowMax();
        subtractColMax();

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

    private void subtractRowMax() {
        for (int r = 0; r < size; r++) {
            double max = Double.NEGATIVE_INFINITY;
            for (int c = 0; c < size; c++) {
                double val = values[r][c];
                if (max < val) {
                    max = val;
                }
            }
            for (int c = 0; c < size; c++) {
                values[r][c] -= max;
            }
        }
    }

    private void subtractColMax() {
        for (int c = 0; c < size; c++) {
            double max = Double.NEGATIVE_INFINITY;
            for (int r = 0; r < size; r++) {
                double val = values[r][c];
                if (max < val) {
                    max = val;
                }
            }
            for (int r = 0; r < size; r++) {
                values[r][c] -= max;
            }
        }
    }

    private void coverZeros() {
        tryAssignment();

        Arrays.fill(markedRows, 0, size, false);
        Arrays.fill(markedCols, 0, size, false);

        for (int r = 0; r < size; r++) {
            if (rows[r] == -1) {
                markRow(r);
            }
        }

        lineCount = invertRowMarks();
    }

    private void tryAssignment() {
        Arrays.fill(rows, 0, size, -1);
        Arrays.fill(occupiedCols, 0, size, false);
        clear(lines);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (values[r][c] == 0 && !occupiedCols[c]) {
                    rows[r] = c;
                    occupiedCols[c] = true;
                    break;
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
        double maxUncoveredVal = Double.NEGATIVE_INFINITY;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (lines[row][col] != UNMARKED) {
                    continue;
                }

                double val = values[row][col];
                if (maxUncoveredVal < val) {
                    maxUncoveredVal = val;
                }
            }
        }

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                byte mark = lines[row][col];
                if (mark == UNMARKED) {
                    values[row][col] -= maxUncoveredVal;
                } else if (mark == MARK_BOTH) {
                    values[row][col] += maxUncoveredVal;
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
}
