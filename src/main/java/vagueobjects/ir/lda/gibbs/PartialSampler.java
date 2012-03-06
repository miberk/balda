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
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * This sampler assigns topics to new document(s), given topics
 * already assigned in training corpus.
 */
public class PartialSampler {
    private static Logger logger = Logger.getLogger(PartialSampler.class);
    private final int numberOfIterations;

    private final Random random;
    /**
     * Document-topic association
     */
    private final double alpha;

    private final int numberOfTopics;

    public PartialSampler(int numberOfTopics, int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
        this.numberOfTopics = numberOfTopics;
        this.alpha = Math.min(1f, 50.0f / numberOfTopics);
        this.random = new Random();
    }

    /**
     * Sample a single document using test corpus
     *
     * @param tokensInDoc - distribution of tokens in a document
     * @param phi         - training values for word-topic associations
     * @return document-topic distribution a for new document
     */
    public double[][] sample(int[] tokensInDoc, double[][] phi) {
        int[][] tokensInDocuments = new int[1][];
        tokensInDocuments[0] = tokensInDoc;
        return sample(tokensInDocuments,  phi);
    }

    /**
     * Samples several documents using test corpus
     *
     * @param tokensInDocuments - distribution of tokens in new documents
     * @param phi         - training values for word-topic associations
     * @return document-topic distribution for new documents
     */
    public double[][] sample(int[][] tokensInDocuments, double[][] phi) {
        // Document-topic associations

        logger.debug("Initializing the sampler");
        double[][] thetaSum = new double[tokensInDocuments.length][numberOfTopics];
        //Initially, randomly assign tokens to topics
        int numDocs = tokensInDocuments.length;
        int[] ndSum = new int[numDocs];

        int[][] nd = new int[numDocs][numberOfTopics];
        //Topic assignments per token
        int[][] z = new int[numDocs][];


        for (int d = 0; d < numDocs; ++d) {
            int nbrTokensInDoc = tokensInDocuments[d].length;
            ndSum[d] = nbrTokensInDoc;
            z[d] = new int[nbrTokensInDoc];
            for (int w = 0; w < nbrTokensInDoc; w++) {
                int topic = (int) (random.nextDouble() * numberOfTopics);
                z[d][w] = topic;
                // number of tokens in document w assigned to topic
                nd[d][topic]++;
            }
        }
        logger.debug("Initialization complete. Entering main loop.");
        //double nBeta = numberOfTokens*beta;
        for (int step = 0; step < numberOfIterations; ++step) {
            for (int d = 0; d < z.length; d++) {
                for (int w = 0; w < z[d].length; w++) {
                    int topic = z[d][w];  //current topic assignment
                    int wid = tokensInDocuments[d][w];
                    nd[d][topic]--;
                    double[] p = new double[numberOfTopics];
                    for (int t = 0; t < numberOfTopics; t++) {
                        p[t] = phi[t][wid] * (nd[d][t] + alpha);
                    }
                    // accumulate multinomial parameters
                    for (int k = 1; k < p.length; k++) {
                        p[k] += p[k - 1];
                    }
                    // scaled sample because of un-normalised p[]
                    double u = random.nextDouble() * p[numberOfTopics - 1];
                    for (topic = 0; topic < p.length; topic++) {
                        if (u < p[topic])
                            break;
                    }
                    nd[d][topic]++;
                    z[d][w] = topic;
                }
            }

            for (int d = 0; d < tokensInDocuments.length; d++) {
                for (int t = 0; t < numberOfTopics; t++) {
                    thetaSum[d][t] += (nd[d][t] + alpha) / (ndSum[d] + numberOfTopics * alpha);
                }
            }
        }

        logger.info("Simulation complete");
        double[][] theta  = new double[tokensInDocuments.length][numberOfTopics];
        for (int d = 0; d < tokensInDocuments.length; d++) {
            for (int k = 0; k < numberOfTopics; k++) {
                theta[d][k] = thetaSum[d][k] / numberOfIterations ;
            }
        }

        return theta;

    }

}
