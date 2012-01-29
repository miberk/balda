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
 * The  implementation of the Gibbs Sampler that follows
 * <a href="http://www.cs.umass.edu/~mimno/papers/fast-topic-model.pdf">this algorithm</a>.
 * This class also provides in-sample perplexity estimates to control convergence.
 */
public class SparseGibbsSampler implements Sampler {
    private static Logger logger = Logger.getLogger(SparseGibbsSampler.class);
    private final int numberOfIterations;
    private final int burnIn;
    private final int sampleLag;
    private double perplexityThreshold;
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
    /**
     * Current value of in-sample perplexity
     */
    private double perplexity;

    public static final double PERPLEXITY_CHANGE_THRESHOLD = 5e-4;
    public static final int DEFAULT_NUMBER_ITERATIONS = 10000;
    public static final int DEFAULT_BURN_IN = 1000;
    public static final int DEFAULT_SAMPLE_LAG = 100;

    /**
     * @param numberOfTopics     - number of topics to extract
     * @param numberOfIterations - number of iterations the sampler will perform until stopped
     * @param burnIn             - number of iterations within burn-in period
     * @param sampleLag          - number of iterations between collecting statistics about
     */
    public SparseGibbsSampler(int numberOfTopics,
                              int numberOfIterations, int burnIn, int sampleLag) {
        this.numberOfIterations = numberOfIterations;
        this.burnIn = burnIn;
        this.sampleLag = sampleLag;
        this.numberOfTopics = numberOfTopics;
        this.alpha = Math.min(1d, 50.0d / numberOfTopics);
        this.beta = 0.01;
        this.perplexityThreshold = PERPLEXITY_CHANGE_THRESHOLD;
    }

    public SparseGibbsSampler(int numberOfTopics) {
        this.numberOfIterations = DEFAULT_NUMBER_ITERATIONS;
        this.burnIn = DEFAULT_BURN_IN;
        this.sampleLag = DEFAULT_SAMPLE_LAG;
        this.numberOfTopics = numberOfTopics;
        this.alpha = Math.min(1d, 50.0d / numberOfTopics);
        this.beta = 0.01;
        this.perplexityThreshold = PERPLEXITY_CHANGE_THRESHOLD;
    }

    /**
     * @param numberOfTopics      - number of topics to extract
     * @param numberOfIterations  - number of iterations the sampler will perform until stopped
     * @param burnIn              - number of iterations within burn-in period
     * @param sampleLag           - number of iterations between collecting statistics about
     * @param perplexityThreshold - threshold value of the perplexity change
     *                            used as a stopping criteria. Setting this value to 0 disables perplexity estimates.
     */
    public SparseGibbsSampler(int numberOfTopics,
                              int numberOfIterations, int burnIn, int sampleLag, double perplexityThreshold) {
        this(numberOfTopics, numberOfIterations, burnIn, sampleLag);
        this.perplexityThreshold = perplexityThreshold;
    }

    /**
     * This will disable perplexity estimate as a stopping criteria
     */
    public SparseGibbsSampler disablePerplexityEstimate() {
        this.perplexityThreshold = 0;
        return this;
    }

    /**
     * Runs the sampling
     *
     * @param tokensInDocuments - an array where the first dimension represents the
     *                          document index, and each row represents tokens of the document. These tokens are in random order.
     * @param vocabularySize    - size of vocabulary used
     */
    public void execute(int[][] tokensInDocuments, int vocabularySize) {
        execute(tokensInDocuments, vocabularySize, new Random());
    }

    /**
     * Runs the sampling, using the <code>Random</code> instance specified by client
     *
     * @param tokensInDocuments - an array where the first dimension represents the
     *                          document index, and each document (row) is defined by a set of token IDs.
     *                          These tokens are in random order.
     * @param vocabularySize    - size of vocabulary used
     * @param random            - an instance of the <code>Random</code> class used for simulation
     */
    public void execute(int[][] tokensInDocuments, int vocabularySize, Random random) {
        logger.debug("Initializing the sampler, using " + tokensInDocuments.length + " documents ");
        double[][] thetaSum = new double[tokensInDocuments.length][numberOfTopics];
        double[][] phiSum = new double[numberOfTopics][vocabularySize];
        //Total number of tokens assigned to topic
        int[] nwSum = new int[numberOfTopics];
        //Initially, randomly assign tokens to topics
        int numDocs = tokensInDocuments.length;
        int[] ndSum = new int[numDocs];
        SparseMatrix nw = new SparseMatrix(vocabularySize, numberOfTopics);
        int[][] nd = new int[numDocs][numberOfTopics];
        //Topic assignment per token
        int[][] z = new int[numDocs][];
        // Size of statistics
        int numStats = 1;

        for (int d = 0; d < numDocs; ++d) {
            int nbrTokensInDoc = tokensInDocuments[d].length;
            if (nbrTokensInDoc <= 1) {
                throw new IllegalArgumentException("Too few tokens (" + nbrTokensInDoc + ") in document #" + d);
            }
            ndSum[d] = nbrTokensInDoc;
            z[d] = new int[nbrTokensInDoc];
            for (int w = 0; w < nbrTokensInDoc; w++) {
                int topic = (int) (random.nextDouble() * numberOfTopics);
                z[d][w] = topic;
                // number of instances of token w  assigned to the topic
                nw.add(tokensInDocuments[d][w], topic);
                // number of tokens in document w assigned to the topic
                nd[d][topic]++;
                nwSum[topic]++;
            }
        }
        double nBeta = vocabularySize * beta;

        logger.debug("Initialization complete. Entering main loop.");
        for (int step = 0; step < numberOfIterations; ++step) {

            double[] cache = new double[numberOfTopics];
            for (int t = 0; t < numberOfTopics; ++t) {
                cache[t] = alpha / (nwSum[t] + nBeta);
            }

            double s = 0f;
            for (int t = 0; t < numberOfTopics; t++) {
                s += cache[t];
            }
            s *= beta;

            for (int d = 0; d < z.length; d++) {

                double r = 0f;
                for (int t = 0; t < numberOfTopics; t++) {
                    if (nd[d][t] > 0) {
                        double x = nd[d][t] / (nwSum[t] + nBeta);
                        r += x;
                        cache[t] += x;
                    }
                }
                r *= beta;

                for (int w = 0; w < z[d].length; w++) {
                    int topic = z[d][w];  //current topic assignment
                    int wid = tokensInDocuments[d][w];
                    //update cache
                    double x = nwSum[topic] + nBeta;
                    //update s

                    double Y = beta / (x * x - x);
                    s += Y * alpha;
                    //update r
                    r -= Y * (x - nd[d][topic]);


                    nw.decrement(wid, topic);
                    nd[d][topic]--;
                    nwSum[topic]--;
                    cache[topic] = (alpha + nd[d][topic]) / (nwSum[topic] + nBeta);

                    double q = 0f;
                    for (int row : nw.array[wid]) {
                        int _nw = row >> nw.shift;
                        int t = row & nw.mask;
                        q += cache[t] * _nw;
                    }

                    double u = random.nextDouble() * (q + r + s);
                    if (u < s) {
                        double sum = 0;
                        for (topic = 0; topic < numberOfTopics; topic++) {
                            sum += beta * alpha / (nwSum[topic] + nBeta);
                            if (sum > u) {
                                break;
                            }
                        }
                    } else if (u < s + r) {
                        double sum = 0;
                        for (topic = 0; topic < numberOfTopics; topic++) {
                            if (nd[d][topic] > 0) {
                                sum += beta * nd[d][topic] / (nwSum[topic] + nBeta);
                                if (sum + s > u) {
                                    break;
                                }
                            }
                        }
                    } else {
                        int[] row = nw.array[wid];
                        int l = row.length;
                        assert l > 0;
                        double sum = 0d;
                        for (int i = l - 1; i >= 0; --i) {
                            int _nw = row[i] >> nw.shift;
                            topic = row[i] & nw.mask;
                            assert topic < numberOfTopics;
                            sum += cache[topic] * _nw;
                            if (sum + s + r > u) {
                                break;
                            }
                        }

                    }

                    x = nwSum[topic] + nBeta;
                    double X = beta / (x * x + x);
                    s -= X * alpha;
                    r += X * (x - nd[d][topic]);
                    nw.increment(wid, topic);
                    nd[d][topic]++;
                    nwSum[topic]++;
                    cache[topic] = (alpha + nd[d][topic]) / (nwSum[topic] + nBeta);
                    z[d][w] = topic;   //new topic assignment
                }
                for (int t = 0; t < numberOfTopics; t++) {
                    if (nd[d][t] > 0) {
                        cache[t] -= nd[d][t] / (nwSum[t] + nBeta);
                    }
                }
            }

            if (step > burnIn && (step % sampleLag == 0 || step == numberOfIterations - 1)) {

                collectStats(tokensInDocuments, thetaSum, ndSum, nwSum, nd, vocabularySize, phiSum, nw,
                        numStats, numDocs);
                if (perplexityThreshold > 0 &&
                        calculatePerplexity(numDocs, tokensInDocuments, nd, nw, ndSum, nwSum, vocabularySize)) {
                    logger.info("terminating since perplexity has converged");
                    break;
                }
                logger.debug("Completed step " + step + " out of " + numberOfIterations
                        + " perplexity=" + perplexity);
                numStats++;
            }
        }
        logger.info("Simulation complete ");

    }

    private boolean calculatePerplexity(int numDocs, int[][] tokensInDocuments,
                                        int[][] nd, SparseMatrix nw, int[] ndSum, int[] nwSum, int numberOfTokens) {
        double exp = 0d;
        int count = 0;
        for (int d = 0; d < numDocs; d++) {

            double terms = 0d;
            for (int w : tokensInDocuments[d]) {
                double c = 0;
                for (int t = 0; t < numberOfTopics; t++) {
                    double term = (nd[d][t] + alpha) / (ndSum[d] + numberOfTopics * alpha);
                    term *= (nw.get(w, t) + beta) / (nwSum[t] + numberOfTokens * beta);
                    c += term;
                }
                terms += Math.log(c);
            }
            exp += terms;
            count += tokensInDocuments[d].length;
        }

        double pp = Math.exp(-exp / count);
        logger.info("Current perplexity: " + pp);

        if (Math.abs(pp - perplexity) < perplexityThreshold * pp) {
            return true;
        }
        perplexity = pp;
        return false;
    }


    private void collectStats(int[][] tokensInDocuments, double[][] thetaSum,
                              int[] ndSum, int[] nwSum, int[][] nd, int numberOfTokens,
                              double[][] phiSum, SparseMatrix nw, int numStats, int numDocs) {
        for (int d = 0; d < tokensInDocuments.length; d++) {
            for (int t = 0; t < numberOfTopics; t++) {
                thetaSum[d][t] += (nd[d][t] + alpha) / (ndSum[d] + numberOfTopics * alpha);
            }
        }
        for (int t = 0; t < numberOfTopics; t++) {
            for (int w = 0; w < numberOfTokens; w++) {
                phiSum[t][w] += (nw.get(w, t) + beta) / (nwSum[t] + numberOfTokens * beta);
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
