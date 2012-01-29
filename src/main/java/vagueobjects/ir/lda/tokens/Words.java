package vagueobjects.ir.lda.tokens;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Words {
    private List<String> additions = new ArrayList<String>();
    private final TokenExtractor extractor;
    private final List<String> vocabulary = new ArrayList<String>();

    public static final int MINIMAL_DOCUMENT_SIZE = 1;

    public Words(int cutOff) {
        this.extractor = new TokenExtractor(cutOff);
    }

    public Words addTokens(String... tokens) {
        Collections.addAll(additions, tokens);
        return this;
    }

    public int[][] processDocuments(Processor processor) {
        processor.process(new SourceHandler() {
            @Override
            public void handle(String text) {
                extractor.addTokensToFreqMap(text);
            }
        });
        processor.process(new SourceHandler() {
            @Override
            public void handle(String text) {
                extractor.addToTfIdf(text);
            }
        });
        extractor.addToVocabulary(vocabulary);

        for (String add : additions) {
            if (!vocabulary.contains(add)) {
                vocabulary.add(add);
            }
        }

        Collections.sort(vocabulary);
        final List<int[]> documents = new ArrayList<int[]>();

        processor.process(new SourceHandler() {
            @Override
            public void handle(String text) {
                List<Integer> tokenIds = new ArrayList<Integer>();

                List<String> tokens = TokenExtractor.extractTokens(text);
                for (String token : tokens) {
                    int i = Collections.binarySearch(vocabulary, token);
                    if (i >= 0) {
                        tokenIds.add(i);
                    }
                }
                if (tokenIds.size() > MINIMAL_DOCUMENT_SIZE) {
                    int[] ids = new int[tokenIds.size()];
                    for (int j = 0; j < tokenIds.size(); ++j) {
                        ids[j] = tokenIds.get(j);
                    }
                    documents.add(ids);
                }
            }
        });

        int[][] arr = new int[documents.size()][];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = new int[documents.get(i).length];
            for (int j = 0; j < arr[i].length; ++j) {
                arr[i][j] = documents.get(i)[j];
            }
        }
        return arr;
    }

    public String getToken(int i) {
        return vocabulary.get(i);
    }

    public int size() {
        return vocabulary.size();
    }
}
