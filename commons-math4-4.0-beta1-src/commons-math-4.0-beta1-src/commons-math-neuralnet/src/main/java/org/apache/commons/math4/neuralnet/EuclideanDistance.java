/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.math4.neuralnet;

import org.apache.commons.math4.neuralnet.internal.NeuralNetException;

/**
 * Euclidean distance measures of n-dimensional vectors.
 *
 * @since 4.0
 */
public class EuclideanDistance implements DistanceMeasure {
    /** {@inheritDoc} */
    @Override
    public double applyAsDouble(double[] a,
                                double[] b) {
        final int len = a.length;
        if (len != b.length) {
            throw new NeuralNetException(NeuralNetException.SIZE_MISMATCH,
                                         len, b.length);
        }

        double sum = 0;
        for (int i = 0; i < len; i++) {
            final double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
