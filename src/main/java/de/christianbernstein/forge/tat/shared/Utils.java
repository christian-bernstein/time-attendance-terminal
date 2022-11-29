package de.christianbernstein.forge.tat.shared;

import org.jetbrains.annotations.NotNull;

/**
 * @author Christian Bernstein
 */
public final class Utils {

    public static double reduce(double @NotNull [] arr, double amount) {
        int i = 0;
        double left = amount, overflow = 0;
        while (true) {
            left = arr[i] - left;
            if (left > 0) {
                arr[i] = left;
                break;
            } else {
                arr[i] = 0;
                left = Math.abs(left);
            }
            i++;
            if (i == arr.length) {
                overflow = left;
                break;
            }
        }
        return overflow;
    }
}
