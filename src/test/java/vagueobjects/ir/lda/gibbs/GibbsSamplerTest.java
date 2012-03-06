package vagueobjects.ir.lda.gibbs;

import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;

import java.util.*;

public class GibbsSamplerTest extends TestCase{

    @Override
    public void setUp(){
        BasicConfigurator.configure();
    }

    public void testBasic(){
        Random random = new Random(10000000000001L);
        SparseGibbsSampler sampler = new SparseGibbsSampler(2, 10000, 100, 10);
        int [][] wordInDocs = new Docs().getWordsInDocs();
        sampler.execute(wordInDocs, 5, random);

        assertTrue(true);
    }

    static enum Token  { River, Stream, Bank, Money, Loan }
    
    static class Docs {
        List<Doc> docs = new ArrayList<Doc>  ();
        
        int[][] getWordsInDocs(){
            int[][] wordInDocs = new int[16][];
            for(int d=0; d< 16;++d) {
                wordInDocs[d] = docs.get(d).getDocCounts();
            }
            return wordInDocs;
        }
        
        Docs(){
            docs.add(new Doc().add(Token.Bank, 4).add(Token.Money, 6).add(Token.Loan, 6));    
            docs.add(new Doc().add(Token.Bank, 5).add(Token.Money, 7).add(Token.Loan, 4));    
            docs.add(new Doc().add(Token.Bank, 7).add(Token.Money, 5).add(Token.Loan, 4));    
            docs.add(new Doc().add(Token.Bank, 7).add(Token.Money, 6).add(Token.Loan, 3));    
            docs.add(new Doc().add(Token.Bank, 7).add(Token.Money, 2).add(Token.Loan, 7));    
            docs.add(new Doc().add(Token.Bank, 9).add(Token.Money, 3).add(Token.Loan, 4));    
            docs.add(new Doc().add(Token.River,1)
                              .add(Token.Bank, 4).add(Token.Money, 6).add(Token.Loan, 5));
            docs.add(new Doc().add(Token.River,1).add(Token.Stream,2)
                              .add(Token.Bank, 6).add(Token.Money, 4).add(Token.Loan, 3));
            docs.add(new Doc().add(Token.River,1).add(Token.Stream,3)
                              .add(Token.Bank, 6).add(Token.Money, 4).add(Token.Loan, 2));
            docs.add(new Doc().add(Token.River,2).add(Token.Stream,3)
                              .add(Token.Bank, 6).add(Token.Money, 1).add(Token.Loan, 4));
            docs.add(new Doc().add(Token.River,2).add(Token.Stream,3)
                              .add(Token.Bank, 7).add(Token.Money, 3).add(Token.Loan, 1));
            docs.add(new Doc().add(Token.River,3).add(Token.Stream,6)
                              .add(Token.Bank, 6).add(Token.Money, 1));
            docs.add(new Doc().add(Token.River,6).add(Token.Stream,3)
                              .add(Token.Bank, 6)                    .add(Token.Loan, 1));
            docs.add(new Doc().add(Token.River,2).add(Token.Stream,8).add(Token.Bank, 6));
            docs.add(new Doc().add(Token.River,4).add(Token.Stream,7).add(Token.Bank, 5));
            docs.add(new Doc().add(Token.River,5).add(Token.Stream,7).add(Token.Bank, 4));

        }
    }
    
    static class Doc {
        Map<Token,Integer> counts = new HashMap<Token, Integer>();
        
        Doc add(Token token, int count){
            counts.put(token,count); 
            return this;
        }

        public int[] getDocCounts() {
            
            int r = countFor(Token.River);
            int s = countFor(Token.Stream);
            int b = countFor(Token.Bank);
            int m = countFor(Token.Money);
            int l = countFor(Token.Loan);
            
            int[] docCounts = new int[r + s + b + m + l];
            int c=0;
            for(; c< r;++c){
                docCounts[c] = 0;
            }
            for(; c< r+s;++c){
                docCounts[c] = 1;
            }
            for(;c<r+s+b;++c){
                docCounts[c] = 2;
            }
            for(;c<r+s+b+m;++c){
                docCounts[c] = 3;
            }
            for(;c<r+s+b+m+l;++c){
                docCounts[c] = 4;
            }

            return docCounts;
        }
        int countFor(Token token){
            return counts.containsKey(token)?counts.get(token):0;
        }
    }
}
