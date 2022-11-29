/*
 * Copyright (c) 2022.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package de.christianbernstein.forge.tat;

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
