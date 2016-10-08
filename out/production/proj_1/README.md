## Advanced Database System Project one

### a)
__Group ID__: Project 1 Group 3  
__Group members__: Ziyi Luo (zl2471) & Guihao Liang (gl2520)  

```bash
run.sh <bing account key> <precision> <query>
```

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
stop.txt
transcript.txt
```

### c) A clear description of how to run your program (note that your project must compile/run under Linux in your CS account)

### d) A clear description of the internal design of your project
#### RelevanceFeedback.java
```
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
}
```
### e) A detailed description of your query-modification method (this is the core component of the project; see below)

#### Ricchio with logarithm tf and idf
* logarithm tf
```
(1 + log2(tf)) tf > 0
0             otherwise              
```
in this way, if words __a__ appears 2 times in same document will have less weight than __b__ appears 2 times in different documents.

Though, logarithm with base 10 is recommended, our document base is small, we observe most words appear 1 or 2 times in first round, and we should use small base logarithm (like 2) to distinguish their difference.

* idf
```
log10(|D| / df)
```

* apply (logarithm tf) * (idf) to generate weights vector
```
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
```
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

        debugQm(q0);
        return q0;
    }
```

#### bad/good feedback
* __bad feedback__ is precision@10 < 0.4, thus we return only 1 word to expand the query. Since we lack of truth about the query, this way can minimize the possibility of misleading query results in the next round. The worst case is that the following round won't provide additional truth to correct the previous query.

* __good feedback__ is precision@10 >= 0.4, and we will provide two additional query keywords to expand previous search because we believe the truth is adequate.

##### query expansion
* expand keywords
We generate relevance feedback query vector __qm__ both from current query results and previous results (see the last part). Then we choose 1 or 2 words with biggest weights as our expansion keywords, based on our criteria of whether it's a bad or good feedback.

* reorder query


### f)
__Bing Key:__ BgupotoHH0LDJeCTmCd7xNndPQUtIwb1VxJOV3vPYv0  

### g) Any additional information that you consider significant

#### accumulate truth from feedback
We not only utilize query results to build out document base from current round, but also accumulate from previous results to expand our document base.

In this way, the negative feedback can provide much more detail and reduce noise in relevant document base. Also, with the increasing documents in relevant document base, we can extract more truth.

* avoid duplicate documents by discriminating their URL.
```
if (!history.add(sr.getUrl())) continue;
```

* separate documents into relevant and non-relevant
```
private ArrayList<double[]> dr; // document related pivoted weights
private ArrayList<double[]> dnr; // document non-relevant pivoted weights
```
