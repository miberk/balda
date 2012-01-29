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
 * Basic Gibbs  sampler, used as baseline.
 */
public class BasicGibbsSampler implements Sampler {
    private static Logger logger = Logger.getLogger(BasicGibbsSampler.class);
    private final int numberOfIterations;
    private final int burnIn;
    private final int sampleLag;
    /**
     * Document-topic association
     */
    private final double alpha;
    /**
     * Topic-term association
     */
    private final double beta;
    private final int numberOfTopics;
    //Simulation results
    /**
     * Topic-token associations
     */
    private double[][] phi;
    /**
     * Document-topic associations
     */
    private double[][] theta;
    private double perplexity;

    public BasicGibbsSampler(int numberOfTopics,
                             int numberOfIterations, int burnIn, int sampleLag) {
        this.numberOfIterations = numberOfIterations;
        this.burnIn = burnIn;
        this.sampleLag = sampleLag;
        this.numberOfTopics = numberOfTopics;
        this.alpha = Math.min(1f, 50.0f / numberOfTopics);
        this.beta = 0.01f;

    }

    public void execute(int[][] tokensInDocuments, int numberOfTokens) {
        execute(tokensInDocuments, numberOfTokens, new Random());
    }

    public void execute(int[][] tokensInDocuments, int numberOfTokens, Random random) {
        logger.debug("Initializing the sampler");
        double[][] thetaSum = new double[tokensInDocuments.length][numberOfTopics];
        double[][] phiSum = new double[numberOfTopics][numberOfTokens];
        //Total number of tokens assigned to topic
        int[] nwSum = new int[numberOfTopics];
        //Initially, randomly assign tokens to topics
        int numDocs = tokensInDocuments.length;
        int[] ndSum = new int[numDocs];
        int[][] nw = new int[numberOfTokens][numberOfTopics];
        int[][] nd = new int[numDocs][numberOfTopics];
        //Topic assignments per token
        int[][] z = new int[numDocs][];
        // Size of statistics
        int numStats = 0;

        for (int d = 0; d < numDocs; ++d) {
            int nbrTokensInDoc = tokensInDocuments[d].length;
            ndSum[d] = nbrTokensInDoc;
            z[d] = new int[nbrTokensInDoc];
            for (int w = 0; w < nbrTokensInDoc; w++) {
                int topic = (int) (random.nextDouble() * numberOfTopics);
                z[d][w] = topic;
                // number of instances of token w  assigned to topic
                nw[tokensInDocuments[d][w]][topic]++;
                // number of tokens in document w assigned to topic
                nd[d][topic]++;
                nwSum[topic]++;
            }
        }
        logger.debug("Initialization complete. Entering main loop.");
        double nBeta = numberOfTokens * beta;
        for (int step = 0; step < numberOfIterations; ++step) {
            for (int d = 0; d < z.length; d++) {
                for (int w = 0; w < z[d].length; w++) {
                    int topic = z[d][w];  //current topic assignment
                    int wid = tokensInDocuments[d][w];
                    nw[wid][topic]--;
                    nd[d][topic]--;
                    nwSum[topic]--;
                    double[] p = new double[numberOfTopics];
                    for (int t = 0; t < numberOfTopics; t++) {
                        p[t] = ((nw[wid][t] + beta) / (nwSum[t] + nBeta))
                                * (nd[d][t] + alpha);
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
                    nw[wid][topic]++;
                    nd[d][topic]++;
                    nwSum[topic]++;
                    z[d][w] = topic;
                }
            }

            if (step > burnIn && step % sampleLag == 0) {
                logger.debug("Completed step " + step + " out of " + numberOfIterations
                        + " perplexity=" + perplexity);

                for (int d = 0; d < tokensInDocuments.length; d++) {
                    for (int t = 0; t < numberOfTopics; t++) {
                        thetaSum[d][t] += (nd[d][t] + alpha) / (ndSum[d] + numberOfTopics * alpha);
                    }
                }
                for (int t = 0; t < numberOfTopics; t++) {
                    for (int w = 0; w < numberOfTokens; w++) {
                        phiSum[t][w] += (nw[w][t] + beta) / (nwSum[t] + numberOfTokens * beta);
                    }
                }
                theta = new double[tokensInDocuments.length][numberOfTopics];
                for (int d = 0; d < tokensInDocuments.length; d++) {
                    for (int k = 0; k < numberOfTopics; k++) {
                        theta[d][k] = thetaSum[d][k] / numStats;
                    }
                }
                phi = new double[numberOfTopics][numberOfTokens];
                for (int k = 0; k < numberOfTopics; k++) {
                    for (int w = 0; w < numberOfTokens; w++) {
                        phi[k][w] = phiSum[k][w] / numStats;
                    }
                }
                double sum = 0d;
                int Nd = 0;
                for (int d = 0; d < numDocs; d++) {
                    Nd += ndSum[d];
                    int nbrTokensInDoc = tokensInDocuments[d].length;
                    for (int w = 0; w < nbrTokensInDoc; w++) {
                        double c = 0d;
                        for (int k = 0; k < numberOfTopics; k++) {
                            c += phi[k][tokensInDocuments[d][w]] * theta[d][k];
                        }
                        sum += Math.log(c);
                    }
                    //number of terms per document
                }
                perplexity = Math.exp(-sum / Nd);
                numStats++;
            }
        }
        logger.info("Simulation complete");
        theta = new double[tokensInDocuments.length][numberOfTopics];
        for (int d = 0; d < tokensInDocuments.length; d++) {
            for (int k = 0; k < numberOfTopics; k++) {
                theta[d][k] = thetaSum[d][k] / numStats;
            }
        }
        phi = new double[numberOfTopics][numberOfTokens];
        for (int k = 0; k < numberOfTopics; k++) {
            for (int w = 0; w < numberOfTokens; w++) {
                phi[k][w] = phiSum[k][w] / numStats;
            }
        }
        double sum = 0d;
        int Nd = 0;
        for (int d = 0; d < numDocs; d++) {
            Nd += ndSum[d];
            int nbrTokensInDoc = tokensInDocuments[d].length;
            for (int w = 0; w < nbrTokensInDoc; w++) {
                double c = 0d;
                for (int k = 0; k < numberOfTopics; k++) {
                    c += phi[k][tokensInDocuments[d][w]] * theta[d][k];
                }
                sum += Math.log(c);
            }
            //number of terms per document
        }
        perplexity = Math.exp(-sum / Nd);

    }

    @Override
    public double[][] getPhi() {
        return phi;
    }

    @Override
    public double[][] getTheta() {
        return theta;
    }
}
