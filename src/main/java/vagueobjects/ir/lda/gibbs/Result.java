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
import vagueobjects.ir.lda.tokens.Words;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class Result {
    public static DecimalFormat format = new DecimalFormat("#.###");
    public static final int MAX_NUM_TOKENS = 10;
    private final Tuple[][] topicMap;

    public Result(Sampler sampler, Words vocabulary) {
        double[][] phi = sampler.getPhi();

        int vocabularySize = vocabulary.size();
        int K = phi.length;
        topicMap = new Tuple[K][];

        for (int topic = 0; topic < K; ++topic) {
            Set<Tuple> tuples = new TreeSet<Tuple>();

            for (int token = 0; token < vocabularySize; token++) {
                String word = vocabulary.getToken(token);
                tuples.add(new Tuple<String>(word, phi[topic][token]));
            }

            int bound = Math.min(MAX_NUM_TOKENS, tuples.size());
            topicMap[topic] = new Tuple[bound];
            Iterator<Tuple> itr = tuples.iterator();
            for (int i = 0; i < bound; ++i) {
                topicMap[topic][i] = itr.next();
            }
        }
    }


    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int topic = 0; topic < topicMap.length; ++topic) {
            Tuple[] tuples = topicMap[topic];
            stringBuilder.append(topic).append(":\t");
            for (Tuple t : tuples) {
                stringBuilder.append("[").append(t.value).append("->")
                        .append(format.format(t.score)).append("] ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }


    static class Tuple<V extends Comparable<V>> implements Comparable<Tuple> {
        final V value;
        final double score;

        public Tuple(V value, double score) {
            this.value = value;
            this.score = score;
        }

        @Override
        public int compareTo(Tuple o) {
            if (o.score == score) {
                return o.value.compareTo(value);
            }
            return score < o.score ? 1 : -1;
        }

        @Override
        public String toString() {
            return "Tuple{value=" + value + ", score=" + score + '}';
        }
    }

}


