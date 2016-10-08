package adb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Class QueryCLI: Command line interface of the Query. Main class of package adb.
 * Package: adb
 * Advanced Database Systems Project 1
 * Group 3
 * Ziyi Luo (zl2471) & Guihao Liang (gl2520)
 */
public class QueryCLI {

	private final static Scanner SCANNER = new Scanner(System.in);
	private static PrintWriter transcript;

	public static void main(String[] args) throws Exception {

		// Handle invalid parameter numbers.
		if (args.length != 3) {
			System.err.println("Usage: QueryCLI <bing account key> <precision> <'query'>");
			System.exit(1);
		}

		String accountKey = args[0]; // get accountKey for bing
		float targetPrecision = Float.parseFloat(args[1]), currentPrecision = 0;
		String searchKeyword = args[2]; // keywords within ' '.
		final int TOP_QUERIES = 10; 
		transcript = new PrintWriter(new FileOutputStream(new File("transcript.txt"), true)); // log file
		int round = 0; //record the current round of query.
		
		// Since our relevance feedback is accumulated.
		RelevanceFeedback feedback = new RelevanceFeedback();

		while (currentPrecision < targetPrecision) {
			// log info
			transcript.println("=====================================");
			transcript.println("ROUND " + (++round));
			transcript.println("QUERY " + searchKeyword);
			// print to screen about the query.
			System.out.println("Parameters:");
			System.out.println("Client key  = " + accountKey);
			System.out.println("Query       = " + searchKeyword);
			System.out.println("Precision   = " + targetPrecision);
			InputStream inputStream = getBingSearchContent(accountKey, searchKeyword);
			List<SingleResult> currentSearchResult = parseXml(inputStream);
			int totalRelevant = 0, currentResultOrder = 0;
			List<SingleResult> relevantResult = new ArrayList<>();
			
			// We do not handle scenarios when results are less than TOP_QUERIES.
			if (currentSearchResult.size() != TOP_QUERIES) {
				System.err.println(
						"[ERROR] Total available search results are less than " + TOP_QUERIES + ", cannot proceed.");
				clearAndExit(1);
			}

			System.out.println("Bing Search Results:\n======================");
			for (SingleResult singleResult : currentSearchResult) {
				singleResult.setIsRelevant(displayResultAndAskRelevance(++currentResultOrder, singleResult));
				relevantResult.add(singleResult);
				if (singleResult.getIsRelevant()) {
					totalRelevant++; // Count for total relevance so as to calculate the precision.
				}
				transcript.println("\nResult " + currentResultOrder);
				transcript.println("Relevant: " + (singleResult.getIsRelevant() ? "YES" : "NO"));
				transcript.println(singleResult + "\n");
			}
			currentPrecision = (float) totalRelevant / TOP_QUERIES;
			transcript.println("PRECISION " + currentPrecision);
			transcript.flush();
			System.out.println("======================\nFEEDBACK SUMMARY");
			System.out.println("Query " + searchKeyword);
			System.out.println("Precision " + currentPrecision);
			if (currentPrecision < targetPrecision && totalRelevant != 0) {
				System.out.println("Still below the desired precision of " + targetPrecision);
                String newKeywords;
                // Decide whether we need one or two more term(s) in the new query.
                // See README - bad/good feedback
                if (currentPrecision <= 0.5)
				    newKeywords = feedback.queryExpansion(1, relevantResult, searchKeyword.split(" "));
				else
				    newKeywords = feedback.queryExpansion(2, relevantResult, searchKeyword.split(" "));
				System.out.println("Augmented query: " + newKeywords);
				System.out.println("======================");
						searchKeyword = newKeywords;
			} else if (totalRelevant == 0) {
				System.out.println("Below desired precision, but can no longer augment the query.");
				clearAndExit(1);
			}
		}

		feedback.allClear();
		System.out.println("Desired precision reached, done");
		clearAndExit(0);
	}

	/**
	 * Get the xml stream according to the searchKeyword.
	 * 
	 * @param accountKey Bing account key needed for the search.
	 * @param searchKeyword Current search keyword we use to get the result.
	 * @return Xml stream that contains the returned result.
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private static InputStream getBingSearchContent(String accountKey, String searchKeyword)
			throws UnsupportedEncodingException, IOException {
		String bingUrl = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%27"
				+ URLEncoder.encode(searchKeyword, "UTF-8") + "%27&$top=10&$format=Atom";
		System.out.println("URL: " + bingUrl);
		byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
		String accountKeyEnc = new String(accountKeyBytes);

		URL url = new URL(bingUrl);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

		return (InputStream) urlConnection.getContent();
	}

	/**
	 * Xml parser that parses the xml stream into a list of SingleResult objects.
	 * 
	 * @param xmlStream Xml stream of the result from Bing.
	 * @return List of SingleResult object that contains all information needed for analysis.
	 * @throws Exception
	 */
	private static List<SingleResult> parseXml(InputStream xmlStream) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document xmlDoc = builder.parse(xmlStream);
		Element rootElement = xmlDoc.getDocumentElement();
		NodeList allEntries = rootElement.getElementsByTagName("entry");
		List<SingleResult> allResults = new ArrayList<>();
		for (int i = 0; i < allEntries.getLength(); i++) {
			Element content = getFirstElementByTagName((Element) allEntries.item(i), "content");
			Element properties = getFirstElementByTagName(content, "m:properties");
			allResults.add(new SingleResult(getFirstElementByTagName(properties, "d:Url").getTextContent(),
					getFirstElementByTagName(properties, "d:Title").getTextContent(),
					getFirstElementByTagName(properties, "d:Description").getTextContent()));
		}
		System.out.println("Total no of results : " + allResults.size());
		return allResults;
	}

	private static Element getFirstElementByTagName(Element parentElement, String name) {
		return (Element) parentElement.getElementsByTagName(name).item(0);
	}

	/**
	 * Display message of one result and ask for relevance on the CLI.
	 * 
	 * @param currentResult Current result number, for display.
	 * @param singleResult Current result.
	 * @return The document is relevant to the desired query or not.
	 * @throws IOException
	 */
	private static boolean displayResultAndAskRelevance(int currentResult, SingleResult singleResult)
			throws IOException {
		System.out.println("Result " + currentResult);
		System.out.println(singleResult);
		System.out.print("Relevant (Y/N)? ");
		return SCANNER.next().equalsIgnoreCase("Y");
	}

	private static void clearAndExit(int exitNum) {
		transcript.flush();
		transcript.close();
		System.exit(exitNum);
	}

}