package assign;

import java.util.Arrays;

public class App {
    public static void main(String[] args) {
        double[][] matrix = { { 0, 0, 0 }, { 0, 0, 0 }, { 0.55, 0.52, 0.51 } };

        Hungarian h = new Hungarian();
        h.set_size(3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                h.set_value(i, j, matrix[i][j]);
            }
        }

        h.reduce();
        System.out.println(Arrays.toString(h.get_result()));
    }
}
