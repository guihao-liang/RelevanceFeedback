package adb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class RelevanceFeedback: Implemented relevance feedback using Rocchio's algorithm with penalized weight
 * Package: adb
 * Advanced Database Systems Project 1
 */
public class RelevanceFeedback {
	// Rocchio constant
	static final double ALPHA = 1.0;
	static final double BETA = .75;
	static final double GAMMA = .15;

	private Set<String> stop; // Stop word elimination.
	private String[] query; // query words
	private List<Term> terms; // documents after procession
	private Set<String> history; // avoid duplicate of a Term (document)
	private HashMap<String, Integer> termToIdx; // String : index
	private HashMap<Integer, String> idxToTerm; // index : String
	private HashMap<String, Integer> docFreq; // String : # docs

	// vector space in group of dr and dnr
	private ArrayList<double[]> dr; // relevant document tf-idf weights
	private ArrayList<double[]> dnr; // non-relevant document tf-idf weights

    // used compute current round precision
	private double dr_n; // number of relevant document in current round
	private double dnr_n; // number of non-relevant document in current round

    // used to compute penalty
	private double lastPrecision; // precision in the last round
	private HashMap<String, Double> markovPenalty; // map query term with our penalty factor
	
	/**
	 * Class QueryExpansion: Comparable class used for sorting weights of queries.
	 */
	private class QueryExpansion implements Comparable<QueryExpansion> {
		private String query;
		private double weight;

		QueryExpansion(String query, double weight) {
			this.query = query;
			this.weight = weight;
		}

		// in reverse order
		@Override
		public int compareTo(QueryExpansion q2) {
			double qe1 = this.weight, qe2 = q2.weight;
			if (qe1 < qe2)
				return 1;
			else if (qe1 > qe2)
				return -1;
			else
				return 0;
		}
	}

	/**
	 * clear everything, user should not call this.
	 */
	public void allClear() {
		query = null;
		history.clear();
		terms.clear();
		idxToTerm.clear();
		termToIdx.clear();
		docFreq.clear();
		dr.clear();
		dnr.clear();
		markovPenalty.clear();
		dr_n = 0;
		dnr_n = 0;
	}

	public RelevanceFeedback() {
		stop = getStopSet();
		terms = new ArrayList<>();
		history = new HashSet<>();
		termToIdx = new HashMap<>();
		idxToTerm = new HashMap<>();
		docFreq = new HashMap<>();
		dr = new ArrayList<>();
		dnr = new ArrayList<>();
		markovPenalty = new HashMap<>();
		lastPrecision = 1;
	}

	public String[] getQuery() {
		return query;
	}

	public List<Term> getTerms() {
		return terms;
	}

	/**
	 * Get the stop words from file.
	 * 
	 * @return stop stop words constructed from adb/stop.txt
	 */
	private Set<String> getStopSet() {
		Set<String> stop = null;

		try {
			BufferedReader text = new BufferedReader(new FileReader("stop.txt"));
			stop = new HashSet<>();
			String word;
			while ((word = text.readLine()) != null) {
				stop.add(word);
			}
			text.close();
		} catch (IOException ex) {
			System.err.println(ex + " stop elimination can't proceed");
			System.exit(2);
		}

		return stop;
	}

	/**
	 * process List of SingleResult to List of Term set up terms, query, termToIdx, idxToTerm.
	 * 
	 * @param documents lists of SingleResult, documents haven't been processed.
	 * @param query query that contains all keywords, separated by white space.
	 */
	private void preProcession(List<SingleResult> documents, String[] query) {
		/*
		 * construct vector, docFreq. docFreq.getKeys() should be same as
		 * vector.getKeys() for all documents. tf set for every term.
		 */
		this.query = query;
		for (SingleResult sr : documents) {
			// accumulate from history
			if (!history.add(sr.getUrl()))
				continue;
			Pattern regex = Pattern.compile("[\\s\\p{Punct}|]+", Pattern.UNICODE_CHARACTER_CLASS);
			String[] content = regex.split((sr.getTitle() + " " + sr.getSummary()).toLowerCase());
			
			Term term = new Term();
			term.setLength(content.length);
			// term.tf word : frequency of word in single document
			HashMap<String, Integer> tf = new HashMap<>();
			term.setIsRelevant(sr.getIsRelevant());

			// statics about relevant and non-relevant documents
			if (sr.getIsRelevant())
				this.dr_n++;
			else
				this.dnr_n++;

			for (String word : content) {
				if (!stop.contains(word)) {
					// update inverted list
					Integer freq = tf.get(word);
					if (freq == null) {
						tf.put(word, 1);
					} else {
						tf.put(word, freq + 1);
					}
				}
			}

			// unordered key sets here, the order is not determined by the order
			// of input
			for (String word : tf.keySet()) {
				Integer freq = docFreq.get(word);
				if (freq != null) {
					docFreq.put(word, freq + 1);
				} else {
					docFreq.put(word, 1);
					int idx = termToIdx.size();
					termToIdx.put(word, idx);
					idxToTerm.put(idx, word);
				}
			}
			term.setTf(tf);

			terms.add(term);
		}
		// put the query item to the vector first.
		for (String word : query) {
			if (!termToIdx.containsKey(word)) {
				int idx = termToIdx.size();
				termToIdx.put(word, idx);
				idxToTerm.put(idx, word);
				docFreq.put(word, 1);
			}
		}
	}

	/**
	 * Compute the size of vector that modeled in vector space model.
	 * 
	 * @return size of terms.
	 */
	private int getVectorSize() {
		return termToIdx.size();
	}

	private Term queryToTerm() {
		Term q0 = new Term();
		q0.setLength(query.length);

		HashMap<String, Integer> tf = q0.getTf();
		for (String term : query) {
			if (!stop.contains(term)) {
				if (tf.containsKey(term)) {
					tf.put(term, tf.get(term) + 1);
				} else {
					tf.put(term, 1);
				}
			}
		}

		return q0;
	}

	private double[] docVector(Term doc) {
		double N = this.terms.size();
		HashMap<String, Integer> tf = doc.getTf();
		double[] v = new double[getVectorSize()];
		for (String term : tf.keySet()) {
			// v[termToIdx.get(term)] = (1 + Math.log10(tf.get(term))) *
			// Math.log10(N / docFreq.get(term));
			v[termToIdx.get(term)] = (1 + Math.log(tf.get(term))) * Math.log10(1 + N / docFreq.get(term));
		}
		return v;
	}

	/**
	 * group terms to dr or dnr set up dr and dnr
	 * 
	 * @param terms
	 */
	private void groupVector(List<Term> terms) {
		if (terms == null) {
			System.err.println("List<Term> hasn't been initialized.");
			System.exit(2);
		}

		this.dr.clear();
		this.dnr.clear();
		for (Term term : terms) {
			if (term.getIsRelevant()) {
				this.dr.add(docVector(term));
			} else {
				this.dnr.add(docVector(term));
			}
		}
	}
	
	/**
	 * Multiply a const to all elements in a vector.
	 * 
	 * @param vector Array of double numbers.
	 * @param FACTOR Const factor.
	 */
	private void vectorMulConst(double[] vector, final double FACTOR) {
		for (int i = 0; i < vector.length; i++) {
			vector[i] *= FACTOR;
		}
	}

	/**
	 * find the centroid from a set of vectors.
	 * 
	 * @param d vector lists of weights
	 * @return the centroid of these vectors
	 */
	private double[] centroid(ArrayList<double[]> d) {
		double num = d.size();
		double[] ret = new double[getVectorSize()];

		for (double[] v : d) {
			for (int i = 0; i < v.length; i++) {
				ret[i] += v[i];
			}
		}

		for (int i = 0; i < ret.length; i++) {
			ret[i] /= num;
		}

		return ret;
	}

	/**
	 * implement Rocchio's algorithm
	 * 
	 * @return qm, which will be used for query expansion.
	 */
	private double[] rocchio(double[] q0) {
		// set up dr, dnr
		groupVector(terms);
		double[] dr_centroid = centroid(dr);
		double[] dnr_centroid = centroid(dnr);

		vectorMulConst(q0, ALPHA);
		vectorMulConst(dr_centroid, BETA);
		vectorMulConst(dnr_centroid, GAMMA);

		for (int i = 0; i < q0.length; i++) {
			q0[i] += (dr_centroid[i]) - dnr_centroid[i];
			// ignore negative weights
			if (q0[i] < 0)
				q0[i] = 0;
		}

		// debugQm(q0);
		return q0;
	}

	/**
	 * print out query vector after relevance feedback.
	 * 
	 * @param qm
	 *            query weight vector
	 */
	private void debugQm(double[] qm) {
		for (int i = 0; i < qm.length; i++) {
			String k = idxToTerm.get(i);
			double v = qm[i];
			if (v != .0)
				System.err.printf("%-18s: %11f; Freq: %5d\n", k, qm[i], docFreq.get(k));
		}

		System.err.printf("Accumulate: %5d\n", terms.size());
	}

	/**
	 * return at most 2 words with highest weights except the keywords.
	 * 
	 * @param qm Optimized query vector.
	 * @return New query that expands at most 2 words.
	 */
	private String expand(double[] qm, int number) {
		if (qm.length < 2)
			return idxToTerm.get(0);
		List<QueryExpansion> queries = new ArrayList<>();
		for (String q : query) {
			// except the query word.
			Double penalty = markovPenalty.get(q);
			if (penalty == null) {
				markovPenalty.put(q, 1.0);
				penalty = 1.0;
			}
			queries.add(new QueryExpansion(q, qm[termToIdx.get(q)] * penalty));
			qm[termToIdx.get(q)] = 0;
		}
		// biggest weight and second biggest weight
		int fst = 0, snd = 1;
		if (qm[0] < qm[1]) {
			fst = 1;
			snd = 0;
		}
		for (int i = 2; i < qm.length; i++) {
			if (qm[i] > qm[fst]) {
				snd = fst;
				fst = i;
			} else if (qm[i] > qm[snd]) {
				snd = i;
			}
		}

		double curPrecision = this.dr_n / (this.dr_n + this.dnr_n);
		// System.err.println("dr: " + this.dr_n + " dnr: " + this.dnr_n);
		double penalty = (curPrecision / this.lastPrecision) * curPrecision;
		System.err.println("(-: Penalty factor for new word is " + penalty + " :-)");
		String term = idxToTerm.get(fst);
		markovPenalty.put(term, penalty);
		queries.add(new QueryExpansion(term, qm[fst] * penalty));
		if (number == 2) {
			term = idxToTerm.get(snd);
			markovPenalty.put(term, penalty);
			queries.add(new QueryExpansion(term, qm[snd] * penalty));
		}

		Collections.sort(queries);
		// debugSortOrder(queries);

		String next = "";
		for (QueryExpansion qe : queries) {
			next += qe.query + " ";
		}
		// set cur for next round.
		this.lastPrecision = curPrecision;
		// reset the sets of dr and dnr to zero for next round.
		this.dr_n = 0;
		this.dnr_n = 0;
		return next;
	}

	private void debugSortOrder(List<QueryExpansion> qes) {
		System.err.println("\n---------------------------------------------");
		System.err.println(" non ascending order of weight for new queries.\n");
		for (QueryExpansion qe : qes) {
			System.err.printf("%-18s: %11f\n", qe.query, qe.weight);
		}
		System.err.println("\n---------------------------------------------");
	}
	
	/**
	 * Public method that expand the query according to the given documents and current query.
	 * 
	 * @param number Number of new terms wanted to add to the query.
	 * @param documents List of documents in the last search.
	 * @param query Array of terms in the last query.
	 * @return New query that contains all sorted terms.
	 */
	public String queryExpansion(int number, List<SingleResult> documents, String[] query) {
		preProcession(documents, query);
		return expand(rocchio(docVector(queryToTerm())), number);
	}

}
