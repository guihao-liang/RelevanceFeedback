package adb;

import java.util.HashMap;

/**
 * Class Term: Contains the term frequencies of all terms in one single document.
 * Package: adb
 * Advanced Database Systems Project 1
 */
public class Term {
    private boolean isRelevant;
    private int length; // the number of words in documents
    private HashMap<String, Integer> tf; // word : frequency of word in single document
//    private double[] weights; // vector space weights
    /**
     * @return the length of the document before stop word elimination.
     */
    public Term() {
    	tf = new HashMap<>();
    }
    
    public int getLength() {
        return length;
    }

    /**
     * @param length set the document length before stop word elimination.
     */
    public void setLength(int length) {
        this.length = length;
    }

    public boolean getIsRelevant() {
        return isRelevant;
    }

    public void setIsRelevant(boolean relevant) {
        isRelevant = relevant;
    }

    /**
     * @return return the tf map of single document.
     */
    public HashMap<String, Integer> getTf() {
        return tf;
    }

    /**
     * @param tf word : frequency for this document.
     */
    public void setTf(HashMap<String, Integer> tf) {
        this.tf = tf;
    }
    
}
