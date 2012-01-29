package vagueobjects.ir.lda.demo;

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
import java.io.*;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.w3c.tidy.DOMTextImpl;
import org.w3c.tidy.Tidy;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import vagueobjects.ir.lda.gibbs.Result;
import vagueobjects.ir.lda.gibbs.Sampler;
import vagueobjects.ir.lda.gibbs.SparseGibbsSampler;
import vagueobjects.ir.lda.tokens.Processor;
import vagueobjects.ir.lda.tokens.SourceHandler;
import vagueobjects.ir.lda.tokens.Words;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.nio.CharBuffer;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.*;

public class Reuters implements Processor {
    static DecimalFormat df = new DecimalFormat("##.##");
    static Logger logger = Logger.getLogger(Reuters.class);
    final String path;

    Reuters(String path) {
        this.path = path;
    }

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();

        logger.info("using path " + args[0]);
        Reuters r = new Reuters(args[0]);
        int vocabSize = 1000;
        int numberOfTopics = 100;
        Words vocabulary = new Words(vocabSize);
        int[][] tokensInDocs = vocabulary.processDocuments(r);
        int numTokens = vocabulary.size();
        logger.info("extraction complete");
        long start = System.currentTimeMillis();
        Sampler sampler = new SparseGibbsSampler(numberOfTopics);
        sampler.execute(tokensInDocs, numTokens);
        logger.info("simulation complete in " + df.format(1e-3 * (System.currentTimeMillis() - start) / 60.0) + " min");
        Result result = new Result(sampler, vocabulary);
        logger.info("\n" + result);

    }

    public void process(SourceHandler handler) {
        FileInputStream fis = null;
        Tidy jt = new Tidy();
        jt.setXmlTags(true);

        GZIPInputStream gz = null;
        try {
            fis = new FileInputStream(path);
            gz = new GZIPInputStream(fis);
            TarArchiveInputStream is = new TarArchiveInputStream(gz);
            ArchiveEntry e;
            while ((e = is.getNextTarEntry()) != null) {
                String name = e.getName();
                if (name.endsWith("sgm")) {
                    logger.info("Processing " + name);
                    int size = (int) e.getSize();
                    byte[] content = new byte[size];
                    is.read(content);

                    ByteArrayInputStream bs = new ByteArrayInputStream(content);
                    Document document = jt.parseDOM(bs, null);
                    NodeList nl = document.getElementsByTagName("BODY");
                    for (int n = 0; n < nl.getLength(); ++n) {
                        Node node = nl.item(n);
                        DOMTextImpl c = (DOMTextImpl) node.getFirstChild();
                        String text = c.getNodeValue();
                        handler.handle(text);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(gz);
        }
    }

}

