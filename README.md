## Advanced Database System Project one
<!-- We recommend you to use an md viewer to read this README.md
    e.g., https://jbt.github.io/markdown-editor/ -->

### a)
__Course Info__: COMS 6111, Advanced database 

### b)
__Submitted File List__  
```
Makefile
README.md
adb
  \_ QueryCLI.java
  \_ SingleResult.java
  \_ RelevanceFeedback.java
  \_ Term.java
lib
  \_ commons-codec-1.10.jar
run.sh
stop.txt
transcript_sample.txt
```

### c)
To run the program, under the root directory of the project, do:
```bash
bash run.sh <bing account key> <precision> <'query'> # You are RECOMMENDED to use bash instead of sh to execute run.sh.
# sh on some cs lab computers is too old to support "if [ ]" inside sh script.
```
This shell script will check whether all files are properly compilered. If not, it will first do "make" first and then execute the program. This script will also check the correctness of the input arguments number. 

### d)
#### Overview
Our project can be divided into two parts. The first part is the front end, which parse the XML into __List<SingleResult>__ and __query string__. Query is the string used to feed bing search engine, like "taj mahal". And SingleResult class contains url, title, summary, and whether search result is relevant or not, which is formed from single search result. Part of its structure is shown below.
```java
public class SingleResult {
    private String url;
    private String title;
    private String summary;
    private boolean isRelevant;

```

The second part it the backend, which receives parsed message from front end and then return the expanded query to front end for further iteration.

#### RelevanceFeedback.java
```java
{
    static final double ALPHA = 1.0;
    static final double BETA  = .75;
    static final double GAMMA = .15;

    private Set<String> stop; // Stop word elimination.
    private String[] query; // query words
    private List<Term> terms; // documents after procession
    private Set<String> history;
    private HashMap<String, Integer> termToIdx; // String : index
    private HashMap<Integer, String> idxToTerm; // index : String
    private HashMap<String, Integer> docFreq; // String : # docs

    // vector space
    private ArrayList<double[]> dr; // document related pivoted weights
    private ArrayList<double[]> dnr; // document non-relevant pivoted weights

    // used compute current round precision
    private double dr_n; // number of relevant document in current round
    private double dnr_n; // number of non-relevant document in current round

    // used to compute penalty
    private double lastPrecision; // precision in the last round
    private HashMap<String, Double> markovPenalty; // map query term with our penalty factor
}
```
The comment is sufficient to explain. Here, I want to clarify that we assign very term (word) with an index in weights vector. Also, we use map to retrieve words from indexes.

#### Term.java
```java
public class Term {
    private boolean isRelevant;
    private int length; // the number of words in documents
    private HashMap<String, Integer> tf; // word : frequency of word in single document
```
Every Term instance can be viewed as a document. tf, as its name suggests, is used to log native term frequency in a single document.

### e)
#### Stop words elimination
We eliminate stop words of the results returned from bing. __getStopSet()__ is used to generate stop words set from text provided by instruction.
```java
    BufferedReader text = new BufferedReader(new FileReader("stop.txt"));
    stop = new HashSet<>();
    String word;
    while ((word = text.readLine()) != null) {
        stop.add(word);
    }
```
#### Ricchio with logarithm tf and idf
* logarithm tf
```
(1 + log2(tf))   tf > 0
0                otherwise              
```
in this way, if words "a" appears 2 times in same document will have less weight than "b" appears 2 times in different documents.  
Though, logarithm with base 10 is recommended, our document base is small, we observe most words appear 1 or 2 times in first round, and we should use small base logarithm (like 2) to distinguish their difference.

* idf
```
idf = log10(|D| / df)
```

* apply (logarithm tf) * (idf) to generate weights vector
```java
private double[] docVector(Term doc) {
        double N = this.terms.size();
        HashMap<String, Integer> tf = doc.getTf();
        double[] v = new double[getVectorSize()];
        for (String term : tf.keySet()) {
            v[termToIdx.get(term)] = (1 + Math.log(tf.get(term))) * Math.log10(1 + N / docFreq.get(term));
        }
        return v;
    }
```

* Relevance feedback with Rocchio's algorithm, setting ALPHA to 1, BETA to 0.75, and GAMMA to 0.15
```java
private double[] rocchio(double[] q0) {
        // set up dr, dnr
        groupVector(terms);
        double[] dr_centroid  = centroid(dr);
        double[] dnr_centroid = centroid(dnr);

        vectorMulConst(q0, ALPHA);
        vectorMulConst(dr_centroid, BETA);
        vectorMulConst(dnr_centroid, GAMMA);

        for (int i = 0; i < q0.length; i++) {
            q0[i] += (dr_centroid[i] ) - dnr_centroid[i];
            // ignore negative weights
            if (q0[i] < 0) q0[i] = 0;
        }

        return q0;
    }
```

#### Bad/good feedback
* __bad feedback__ is precision@10 < 0.5, thus we return only 1 word to expand the query. Since we lack of truth about the query, this way can minimize the possibility of misleading query results in the next round. The worst case is that the following round won't provide additional truth to correct the previous query.

* __good feedback__ is precision@10 >= 0.5, and we will provide two additional query keywords to expand previous search because we believe the truth is adequate.

#### Query expansion
* __expand keywords__  
We generate relevance feedback query vector __qm__ both from current query results and previous results (see the last part). Then we choose 1 or 2 words with biggest weights as our expansion keywords, based on our criteria of whether it's a bad or good feedback.

* __penalized tf-idf weight__  
The penalized tf-idf weight should be __penalty \* (tf-idf weight)__, and we use it to rank and reorder our query.  
The __penalty__ is based on the current query. If the query is bad, which means we can't trust the expanded words because of the fact that there is lack of truth (relevant results), and we will penalize on its tf-idf weights for further ranking.  
The thought is inspired by __markov chain__. Every new expanded word's penalty is only related to current and last round precision.  
The __penalty factor__ (penalty for short) is constructed by:
```
(current precision) / (last precision) * (current precision)
```
If (current precision) / (last precision) is high, that is, the precision is __increased drastically__ by last expanded word, the new expanded word should be paid attention to and given high penalized weights since our augmented query is on the right track. Then we can put it in front of our query according to its rank. __Once penalty for a word is generated, it won't change afterwards.__

* __caveat__  
The precision may not same to precision@10, since our query is accumulative (see last part), duplicate results won't be taken into account. That is, if we have 7 newly generated results at this round, which are different from previous, we take precision@7 here by simply ignoring duplicates.  
Penalty for the initial query is 1.0 and initial value of last precision is 1.0. If the second round precision is 0.2, then the penalty will be 0.04 \* weight of the word expanded for next round, which means we don't trust it and it's not as important as word expanded from a good query.  
As we can see in this case, the __penalized weight__ should be very low since we multiply a very small factor, 0.04, to its tf-idf weight, even though it is the word with biggest tf-idf weight in current __qm__, expcept query words of previous rounds.

* __reorder query by penalized weight instead of original tf-idf weight__  
Finally before the program returns the new query back to the Command line interface for the next round search, we reorder all terms in the query according to the __penalized weight__ described above. Here we created a subclass to implement it:
```java
private class QueryExpansion implements Comparable<QueryExpansion> //RelevanceFeedback.java@43
```
Employ a hash map to trace penalty for every query words.
```java
private HashMap<String, Double> markovPenalty;
```

### f)
Bing Key: __your bing key__

### g)

#### accumulate truth from feedback
We not only utilize query results to build out document base from current round, but also accumulate from previous results to expand our document base.

In this way, the negative feedback can provide much more detail and reduce noise in relevant document base. Also, with the increasing documents in relevant document base, we can extract more truth.

* avoid duplicate documents by discriminating their URL.
```java
if (!history.add(sr.getUrl())) continue;
```

* separate documents into relevant and non-relevant
```
private ArrayList<double[]> dr; // document related pivoted weights
private ArrayList<double[]> dnr; // document non-relevant pivoted weights
```
