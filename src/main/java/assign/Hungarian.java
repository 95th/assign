package assign;

import java.util.Arrays;

public class Hungarian {
    private static final byte UNMARKED = 0;
    private static final byte MARK_ROW = -1;
    private static final byte MARK_COL = 1;
    private static final byte MARK_BOTH = 2;

    private double[][] original_values;
    private double[][] values;

    private byte[][] lines;
    private int line_count;

    private boolean[] marked_rows;
    private boolean[] marked_cols;

    private boolean[] occupied_cols;
    private int[] rows;

    private int row_count;
    private int size;

    public void set_size(int rows, int cols) {
        this.row_count = rows;
        this.size = Math.max(rows, cols);
        ensure_capacity();
    }

    public void set_value(int row, int col, double value) {
        original_values[row][col] = value;
        values[row][col] = value;
    }

    public void reduce() {
        subtract_row_max();
        subtract_col_max();

        cover_zeros();
        while (line_count != size) {
            create_additional_zeros();
            cover_zeros();
        }

        optimization();
    }

    public int[] get_result() {
        return Arrays.copyOf(rows, row_count);
    }

    private void subtract_row_max() {
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

    private void subtract_col_max() {
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

    private void cover_zeros() {
        try_assignment();

        Arrays.fill(marked_rows, 0, size, false);
        Arrays.fill(marked_cols, 0, size, false);

        for (int r = 0; r < size; r++) {
            if (rows[r] == -1) {
                mark_row(r);
            }
        }

        line_count = invert_row_marks();
    }

    private void try_assignment() {
        Arrays.fill(rows, 0, size, -1);
        Arrays.fill(occupied_cols, 0, size, false);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (values[r][c] == 0 && !occupied_cols[c]) {
                    rows[r] = c;
                    occupied_cols[c] = true;
                    break;
                }
            }
        }
    }

    private void mark_row(int row) {
        if (marked_rows[row]) {
            return;
        }

        marked_rows[row] = true;

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
                mark_column(col);
            }
        }
    }

    private void mark_column(int col) {
        if (marked_cols[col]) {
            return;
        }

        marked_cols[col] = true;

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
                mark_row(row);
            }
        }
    }

    private int invert_row_marks() {
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
                }
                if (row == col) {
                    count += Math.abs(lines[row][col]);
                }
            }
        }
        return count;
    }

    private void create_additional_zeros() {
        double max_uncovered_val = Double.NEGATIVE_INFINITY;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (lines[row][col] != UNMARKED) {
                    continue;
                }

                double val = values[row][col];
                if (max_uncovered_val < val) {
                    max_uncovered_val = val;
                }
            }
        }

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                byte mark = lines[row][col];
                if (mark == UNMARKED) {
                    values[row][col] -= max_uncovered_val;
                } else if (mark == MARK_BOTH) {
                    values[row][col] += max_uncovered_val;
                }
            }
        }
    }

    private void optimization() {
        Arrays.fill(rows, 0, size, -1);
        Arrays.fill(occupied_cols, 0, size, false);
        optimization(0);
    }

    private boolean optimization(int row) {
        if (row >= size) {
            return true;
        }

        for (int col = 0; col < size; col++) {
            if (values[row][col] == 0 && !occupied_cols[col]) {
                rows[row] = col;
                occupied_cols[col] = true;
                if (optimization(row + 1)) {
                    return true;
                }
                occupied_cols[col] = false;
            }
        }

        return false;
    }

    private void ensure_capacity() {
        if (original_values == null || original_values.length < size) {
            int old_length = original_values == null ? 0 : original_values.length;
            int new_length = Math.max(size, old_length * 2);

            original_values = new double[new_length][new_length];
            values = new double[new_length][new_length];
            lines = new byte[new_length][new_length];
            marked_rows = new boolean[new_length];
            marked_cols = new boolean[new_length];
            occupied_cols = new boolean[new_length];
            rows = new int[new_length];
        } else {
            clear(original_values);
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
}
