import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;


public class RankedRetrieval {
	
	private StanfordLemmatizer stanfordLemma;

	private Set<String> StopWords;
	private String cranfieldFolder;
	private String PathtostopWords;
	private String Pathtoqueries;
	private Map<Integer, List<String>> queryMap1;
	private Map<Integer, String> queryMap;
	//Map<String, HashMap<String, Double>> weight1;
	//doc,word,weight for W1
	Map<Integer, HashMap<String, Double>> weight1;
	//doc,word,weight for W2
	Map<Integer, HashMap<String, Double>> weight2;
	// doc,doclen
	//private Map<String, Integer> docLen;
	private Map<Integer, Integer> docLen;
	private static Map<Integer, HashMap<String,Integer>> maxFreq_query;
	private static TreeMap<Integer, HashMap<String,Double>> queryvector;
	private static TreeMap<Integer, HashMap<String,Double>> queryvector1;
	private static TreeMap<Integer, HashMap<String,Double>> docvector;
	private static TreeMap<Integer, HashMap<String,Double>> docvector1;
	// doc, word,freq
	//private Map<String, HashMap<String, Integer>> docLen2;
	private Map<Integer, HashMap<String, Integer>> docLen2;
	//queryno , docno ,cosine similarity
	private Map<Integer, HashMap<Integer, Double>> cosine_similarity;
	private Map<Integer, HashMap<Integer, Double>> cosine_similarity1;
	static String query_vector_W1_file = "query_vector_W1";
	static String query_vector_W2_file = "query_vector_W2";
	static String doc_vector_W1_file = "doc_vector_W1";
	static String doc_vector_W2_file = "doc_vector_W2";
	String version1_uncompress = "Index_Version1.uncompress.txt";
	private int avgDocLen;
	public RankedRetrieval(String cranfieldFolder, String PathtostopWords,
			String Pathtoqueries) {
		this.cranfieldFolder = cranfieldFolder;
		this.PathtostopWords = PathtostopWords;
		stanfordLemma = new StanfordLemmatizer();
		StopWords = new HashSet<String>();
		this.Pathtoqueries = Pathtoqueries;
		queryMap1 = new HashMap<Integer, List<String>>();
		queryMap = new HashMap<Integer, String>();
		docLen = new HashMap<Integer, Integer>();
		docLen2 = new HashMap<>();
		maxFreq_query = new HashMap<Integer, HashMap<String,Integer>>();
		queryvector = new TreeMap<Integer,HashMap<String, Double>>();
		queryvector1 = new TreeMap<Integer,HashMap<String, Double>>();
		docvector = new TreeMap<Integer,HashMap<String, Double>>();
		docvector1 = new TreeMap<Integer,HashMap<String, Double>>();
		cosine_similarity = new TreeMap<Integer,HashMap<Integer, Double>>();
		cosine_similarity1 = new TreeMap<Integer,HashMap<Integer, Double>>();
		readMaxTfDocLen();
		
		weight1 = new TreeMap<Integer, HashMap<String, Double>>();
		weight2 = new TreeMap<Integer, HashMap<String, Double>>();
	}

	public int avgDocLen() {
		int sum = 0;
		for (int i : docLen.values()) {
			sum += i;
		}
		return sum / docLen.size();
	}

	public void readQueries() {
		try (FileReader inputFile = new FileReader(Pathtoqueries);
				BufferedReader bufferReader = new BufferedReader(inputFile);) {
			String line = "";
			int count = 1;
			while ((line = bufferReader.readLine()) != null) {
				if (line.isEmpty() || line.equals(""))
					count++;
					Lemmas(line, count);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void Lemmas(String line, int quesNo) {
		if (line.contains(":") || line.isEmpty())
			return;
		else {
			if (!queryMap.containsKey(quesNo)) {
				queryMap.put(quesNo, line);
			} else {
				String text = queryMap.get(quesNo) + " " + line;
				queryMap.put(quesNo, text);
			}
			 //System.out.println("bfr list "+line);
			List<String> list = stanfordLemma.lemmatize(line);
			  // System.out.println("list "+list);
			list.removeAll(StopWords);
			storeLemmas(list, quesNo);
		}

	}

	private void storeLemmas(List<String> words, int quesNo) {
		if (!queryMap1.containsKey(quesNo)) {
			//ArrayList<String> wordSet = new ArrayList<String>();
			queryMap1.put(quesNo, words);
		} else {
			List<String> wordSet = queryMap1.get(quesNo);
			wordSet.addAll(words);
			
		}
	}
	
	private String readMaxTfDocLen() {
		String maxTf_file = "doc_tf_version1.txt";
		try (FileReader inputFile = new FileReader(maxTf_file);
				BufferedReader bufferReader = new BufferedReader(inputFile);) {
			String line = "";
			while ((line = bufferReader.readLine()) != null) {
				String[] datas = line.split("\t");
				Integer docNo = Integer.parseInt(datas[0]);
				String[] datas2 = datas[1].split("-");
				String word = datas2[0];
				int freq = Integer.parseInt(datas2[1]);
				int docLen = Integer.parseInt(datas[2]);

				this.docLen.put(docNo, docLen);
				HashMap<String, Integer> map = new HashMap<String, Integer>();
				map.put(word, freq);
				this.docLen2.put(docNo, map);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void readStopWords() {
	//	System.out.println("PathtostopWords "+ PathtostopWords);
		try (FileReader inputFile = new FileReader(PathtostopWords.toString());
				BufferedReader bufferReader = new BufferedReader(inputFile);) {
			String line;

			while ((line = bufferReader.readLine()) != null) {
				//System.out.println("line" + line);
				StopWords.add(line);
			}
			bufferReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void calculateWeight() throws IOException {

		List<Integer> keySet = new ArrayList<Integer>(queryMap1.keySet());
		Collections.sort(keySet);
		for (Integer queryNo : keySet) {
			int max_tf_query=calmaxtf_query(queryNo);
		//     System.out.println("max_tf_freq " +max_tf_query);
			List<String> set = queryMap1.get(queryNo);
			for (String word : set) {
				calweight(word, queryNo,max_tf_query);
			}
		}
	}
		
		private void calweight(String word, int queryNo,int max_tf_query) throws IOException {
	//		System.out.println("word "+word);
			String line = readIndex(word);
			int count = 0;
			int tf_query = 0;
			if (line != null) {
				
				String[] documentFreq = line.split("\t\t\t");
				
				String intValue = documentFreq[0].replaceAll("[^0-9]", "");
				
				List<Integer> keySet = new ArrayList<Integer>(queryMap1.keySet());
				Collections.sort(keySet);
				for (Integer queryNo1 : keySet) {
					List<String> set = queryMap1.get(queryNo1);
					for (String word1 : set) {
						if (word1.equals(word)){
							count++;
			//				 System.out.println("word1"+word1);
						}
					}
					tf_query=count;
				    
		//		    System.out.println("tf_query"+tf_query);
				    // ...
				}
				
				int docFreq = Integer.parseInt(intValue);
		//		System.out.println("docfreq " +docFreq );
				String[] termFreqs = documentFreq[1].split("\t");
				for (String termFreq : termFreqs) {
					String[] docNo_freq = termFreq.split("-");
					int docNo = Integer.parseInt(docNo_freq[0]);
					int term_freq = Integer.parseInt(docNo_freq[1]);
					int maxtf = 0;
					for (String str : docLen2.get(docNo).keySet()) {
						maxtf = docLen2.get(docNo).get(str);
					}
					int docLen = this.docLen.get(docNo);
					calculateW1_query(word, queryNo, docFreq, tf_query, max_tf_query);
					calculateW1_doc(docNo,word, queryNo, docFreq, term_freq, maxtf);
					calculateW2_query(word, queryNo, docFreq, tf_query, docLen);
					calculateW2_doc(docNo,word, queryNo, docFreq, term_freq, docLen);
				}
			}
		}
		
		private void calculateW2_doc(int docNo, String word, int queryNo,
				int df, int tf, int docLen) {
			// TODO Auto-generated method stub
			double avgdoclen = 0, sum = 0;
			
			double collectionsize = 1400;
			sum = avgDocLen();
			avgdoclen = sum / collectionsize;
			double weight = (0.4 + 0.6
					* (tf / (tf + 0.5 + 1.5 * (docLen/avgdoclen)))
					* Math.log(collectionsize / df) / Math.log(collectionsize));
			
			if (!weight2.containsKey(docNo)) {
				HashMap<String, Double> temp = new HashMap<String, Double>();
				temp.put(word, weight);
				weight2.put(docNo, temp);
			} else {

				HashMap<String, Double> temp1 = weight2.get(docNo);
				if (temp1.containsKey(word)) {
					/*double val = temp1.get(docNo) + weight;
					temp1.put(docNo, val);*/
				} else {
					temp1.put(word, weight);
				}
			}

			
			

		}

		private void calculateW1_doc(Integer docNo,String word, int queryNo, int docFreq,
				int term_freq, int maxtf) {
			// TODO Auto-generated method stub
			double avgdoclen = 0, sum = 0;
			double collectionsize = 1400;
			sum = avgDocLen();
			avgdoclen = sum / collectionsize;
			double weight = (0.4 + 0.6 * Math.log(term_freq + 0.5) / Math.log(maxtf + 1.0))
					* (Math.log(collectionsize / docFreq) / Math.log(collectionsize));
			
			if (!weight1.containsKey(docNo)) {
				HashMap<String, Double> temp = new HashMap<String, Double>();
				temp.put(word, weight);
				weight1.put(docNo, temp);
			} else {

				HashMap<String, Double> temp1 = weight1.get(docNo);
				if (temp1.containsKey(word)) {
					/*double val = temp1.get(docNo) + weight;
					temp1.put(docNo, val);*/
				} else {
					temp1.put(word, weight);
				}
			}

			
			
		}

		private void calculateW2_query(String word, int queryNo, int df, int tf, int docLen) {
			double avgdoclen = 0, sum = 0;
			int collectionsize = 1400;
			sum = avgDocLen();
			avgdoclen = sum / collectionsize;

			double weight = (0.4 + 0.6
					* (tf / (tf + 0.5 + 1.5 * (docLen / avgdoclen)))
					* Math.log(collectionsize / df) / Math.log(collectionsize));

			if (!queryvector1.containsKey(queryNo)) {
				HashMap<String, Double> temp = new HashMap<String, Double>();
				temp.put(word, weight);
				queryvector1.put(queryNo, temp);
			} else {

				HashMap<String, Double> temp1 = queryvector1.get(queryNo);
				if (temp1.containsKey(word)) {
					
				} else {
					temp1.put(word, weight);
				}
			}

		}



		private int calmaxtf_query(
				int queryNo) throws IOException {
			
		
			List<Integer> keySet = new ArrayList<Integer>(queryMap1.keySet());
			Collections.sort(keySet);
			
				List<String> set = queryMap1.get(queryNo);
				for (String word1 : set) {
		//			System.out.println("word1 "+ word1);
					if(!maxFreq_query.containsKey(queryNo))
					{
						HashMap<String,Integer> temp1 = new HashMap<String,Integer>();
						temp1.put(word1,1);
						maxFreq_query.put(queryNo,temp1);
					}
					else
					{
						HashMap<String,Integer> temp2 = maxFreq_query.get(queryNo);
						if(temp2.containsKey(word1))
						{
							int count3 = temp2.get(word1) + 1;
							temp2.put(word1,count3); 
						}
						else
							temp2.put(word1,1);
					}
				}
					
			
			
			int max_value = 0;
			String word2 = "";
			for(Entry<Integer,HashMap<String,Integer>> entry : maxFreq_query.entrySet()){
				  
				  
				  int value;
				  
				  HashMap<String,Integer> WordFreq = entry.getValue();
				  for(Entry<String,Integer> entry2 : WordFreq.entrySet())
				     {
				             value =  (int)(entry2.getValue());
				           
				          if( max_value < value) 
				          {
				             max_value = value;
				             word2 = entry2.getKey()+": "+"   ";
				          } 
				     }
			}
//			System.out.println("word2 "+ word2);
			
			return max_value;
		}
		public String readIndex(String word) {
			
			try (FileReader inputFile = new FileReader(version1_uncompress);
					BufferedReader bufferReader = new BufferedReader(inputFile);) {
				word = word + "-";
				String line = "";
				while ((line = bufferReader.readLine()) != null) {
					if (line.startsWith(word))
						return line;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;

		}
		
		private void calculateW1_query(String word, int queryNo, int df, int tf,
				int maxtf) {
			double collectionsize = 1400;
			double weight = (0.4 + 0.6 * Math.log(tf + 0.5) / Math.log(maxtf + 1.0))
					* (Math.log(collectionsize / df) / Math.log(collectionsize));

			
			if (!queryvector.containsKey(queryNo)) {
				HashMap<String, Double> temp = new HashMap<String, Double>();
				temp.put(word, weight);
				queryvector.put(queryNo, temp);
			} else {

				HashMap<String, Double> temp1 = queryvector.get(queryNo);
				if (temp1.containsKey(word)) {
					
				} else {
					temp1.put(word, weight);
				}
			}
			
			/*for (Entry<Integer, HashMap<String, Double>> entry : queryvector.entrySet()) {
				int count = 0;
				int queryNo1 = entry.getKey();
				HashMap<String, Double> wordWeight = entry.getValue();
				//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
				//List<Entry<String, Double>> subList = list.subList(0, 5);
				System.out.println("*************************************");
				System.out.println("Q:" + queryNo1 + " " );
				System.out.println("*************************************");
				for (Entry<String, Double> entry1 : wordWeight.entrySet()) {
					String word1 = entry1.getKey();
					Double weight1 = entry1.getValue();
					System.out.println("Q:" + queryNo1 + " " );
					System.out.println("word : " + word1);
					System.out.println("weight: " + weight);
					System.out.println("External Document Identifier: cranfield"
							+ docNo);
					System.out.println("Headline: " + title);
					System.out.println("*************************************");
				}
			}*/
			/*if (!weight1.containsKey(queryNo)) {
				HashMap<String, Double> temp = new HashMap<String, Double>();
				temp.put(docNo, weight);
				weight1.put(queryNo, temp);
			} else {

				HashMap<String, Double> temp1 = weight1.get(queryNo);
				if (temp1.containsKey(docNo)) {
					double val = temp1.get(docNo) + weight;
					temp1.put(docNo, val);
				} else {
					temp1.put(docNo, weight);
				}
			}
*/
		}
		private void printqueryVector_W1() throws IOException {
			// TODO Auto-generated method stub
			for (Entry<Integer, HashMap<String, Double>> entry : queryvector.entrySet()) {
				int count = 0;
				int queryNo1 = entry.getKey();
				HashMap<String, Double> wordWeight = entry.getValue();
				//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
				//List<Entry<String, Double>> subList = list.subList(0, 5);
				/*System.out.println("*************************************");
				System.out.println("Q:" + queryNo1 + " " );
				System.out.println("*************************************");*/
				for (Entry<String, Double> entry1 : wordWeight.entrySet()) {
					String word1 = entry1.getKey();
					Double weight1 = entry1.getValue();
					/*System.out.println("Q:" + queryNo1 + " " );
					System.out.println("word : " + word1 + "weight: " + weight1);*/
					/*System.out.println("External Document Identifier: cranfield"
					String 		+ docNo);
					System.out.println("Headline: " + title);*/
					String data = ("\n"+"Q:" + queryNo1 + " " );
					writedata(data,query_vector_W1_file);
					String data1 = "word : " + word1 + " weight: " + weight1;
					writedata(data1,query_vector_W1_file);
				//	writedata("\n",query_vector_file);
				}
			}	
		}
		
		private void printqueryVector_W2() throws IOException {
			// TODO Auto-generated method stub
			for (Entry<Integer, HashMap<String, Double>> entry : queryvector1.entrySet()) {
				int count = 0;
				int queryNo1 = entry.getKey();
				HashMap<String, Double> wordWeight = entry.getValue();
				//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
				//List<Entry<String, Double>> subList = list.subList(0, 5);
				/*System.out.println("*************************************");
				System.out.println("Q:" + queryNo1 + " " );
				System.out.println("*************************************");*/
				for (Entry<String, Double> entry1 : wordWeight.entrySet()) {
					String word1 = entry1.getKey();
					Double weight1 = entry1.getValue();
					/*System.out.println("Q:" + queryNo1 + " " );
					System.out.println("word : " + word1 + "weight: " + weight1);*/
					/*System.out.println("External Document Identifier: cranfield"
					String 		+ docNo);
					System.out.println("Headline: " + title);*/
					String data = ("\n"+"Q:" + queryNo1 + " " );
					writedata(data,query_vector_W2_file);
					String data1 = "word : " + word1 + " weight: " + weight1;
					writedata(data1,query_vector_W2_file);
				//	writedata("\n",query_vector_file);
				}
			}	
		}
		private void printDocumentVector_W1() throws IOException {
			// TODO Auto-generated method stub
			for (Entry<Integer, HashMap<String, Double>> entry : docvector.entrySet()) {
				int count = 0;
				Integer DocNo = entry.getKey();
				HashMap<String, Double> wordWeight = entry.getValue();
				//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
				//List<Entry<String, Double>> subList = list.subList(0, 5);
				/*System.out.println("*************************************");
				System.out.println("Q:" + queryNo1 + " " );
				System.out.println("*************************************");*/
				for (Entry<String, Double> entry1 : wordWeight.entrySet()) {
					String word1 = entry1.getKey();
					Double weight1 = entry1.getValue();
					/*System.out.println("Q:" + queryNo1 + " " );
					System.out.println("word : " + word1 + "weight: " + weight1);*/
					/*System.out.println("External Document Identifier: cranfield"
					String 		+ docNo);
					System.out.println("Headline: " + title);*/
					String data = ("\n"+"DocNo " + DocNo + " " );
					writedata(data,doc_vector_W1_file);
					String data1 = "word : " + word1 + " weight: " + weight1;
					writedata(data1,doc_vector_W1_file);
				//	writedata("\n",query_vector_file);
				}
			}
		}
		
		private void printDocumentVector_W2() throws IOException {
			// TODO Auto-generated method stub
			for (Entry<Integer, HashMap<String, Double>> entry : docvector1.entrySet()) {
				int count = 0;
				Integer DocNo = entry.getKey();
				HashMap<String, Double> wordWeight = entry.getValue();
				//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
				//List<Entry<String, Double>> subList = list.subList(0, 5);
				/*System.out.println("*************************************");
				System.out.println("Q:" + queryNo1 + " " );
				System.out.println("*************************************");*/
				for (Entry<String, Double> entry1 : wordWeight.entrySet()) {
					String word1 = entry1.getKey();
					Double weight1 = entry1.getValue();
					/*System.out.println("Q:" + queryNo1 + " " );
					System.out.println("word : " + word1 + "weight: " + weight1);*/
					/*System.out.println("External Document Identifier: cranfield"
					String 		+ docNo);
					System.out.println("Headline: " + title);*/
					String data = ("\n"+"DocNo " + DocNo + " " );
					writedata(data,doc_vector_W2_file);
					String data1 = "word : " + word1 + " weight: " + weight1;
					writedata(data1,doc_vector_W2_file);
				//	writedata("\n",query_vector_file);
				}
			}
		}

		private void writedata(String data,
				String filename1) throws IOException {
				  // TODO Auto-generated method stub
				  File myFile = new File(filename1);
				  FileWriter fw = new FileWriter(myFile,true);
		          PrintWriter pw = new PrintWriter(fw);
		          pw.print(data);
		          //pw.print("\n");
		          
		          pw.close();
		            
		}
		  public void deleteFile(String fileName)
		  {
			  //System.out.println("deleting file");
		     File file = new File(fileName);
		     if(file.exists())
		        file.delete();

		  }
		  
		  static <K, V extends Comparable<? super V>> List<Entry<K, V>> SortByValues(
					Map<K, V> map) {

				List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(
						map.entrySet());

				Collections.sort(sortedEntries, new Comparator<Entry<K, V>>() {
					@Override
					public int compare(Entry<K, V> e1, Entry<K, V> e2) {
						return e2.getValue().compareTo(e1.getValue());
					}
				});

				return sortedEntries;
			}
			private void findRank_W1() throws FileNotFoundException, IOException {
				// TODO Auto-generated method stub
				//for (int i = 1 ; i<=20 ; i++){
					for (Entry<Integer, HashMap<Integer, Double>> entry : cosine_similarity.entrySet()) {
						int count = 0;
						int queryNo = entry.getKey();
						HashMap<Integer, Double> cosine_similarity1 = entry.getValue();
						List<Entry<Integer, Double>> list = SortByValues(cosine_similarity1);
						List<Entry<Integer, Double>> subList = list.subList(0, 5);
						System.out.println("*************************************");
						System.out.println("Q:" + queryNo + " " + queryMap.get(queryNo)
								+ "?");
						System.out.println("*************************************");
						String data = "Document vector for top 5 documents for " +"Q:"+ queryNo;
						writedata("\n",doc_vector_W1_file);
						writedata("\n"+ data ,doc_vector_W1_file);
						writedata("\n",doc_vector_W1_file);
						for (Entry<Integer, Double> entry1 : subList) {
							Integer docNo = entry1.getKey();
							//System.out.println("Checkfordocno "+ docNo);
							
							readindexfordocvector_W1(docNo);
							Double cosine_similarity = entry1.getValue();
							count++;
							String title = "";
							title = DocProcessor.getTitle(cranfieldFolder, docNo);
							System.out.println("Rank: " + count);
							System.out.println("Score: " + cosine_similarity);
							System.out.println("External Document Identifier: cranfield"
									+ docNo);
							System.out.println("Headline: " + title);
							System.out.println("*************************************");
							printDocumentVector_W1();
							docvector.clear();
						}

					}


					}
				
			
			private void findRank_W2() throws FileNotFoundException, IOException {
				// TODO Auto-generated method stub
				//for (int i = 1 ; i<=20 ; i++){
					for (Entry<Integer, HashMap<Integer, Double>> entry : cosine_similarity1.entrySet()) {
						int count = 0;
						int queryNo = entry.getKey();
						HashMap<Integer, Double> cosine_similarity2 = entry.getValue();
						List<Entry<Integer, Double>> list1 = SortByValues(cosine_similarity2);
						List<Entry<Integer, Double>> subList1 = list1.subList(0, 5);
						System.out.println("*************************************");
						System.out.println("Q:" + queryNo + " " + queryMap.get(queryNo)
								+ "?");
						System.out.println("*************************************");
						String data = "Document vector for top 5 documents for " +"Q:"+ queryNo;
						writedata("\n",doc_vector_W2_file);
						writedata("\n"+ data ,doc_vector_W2_file);
						writedata("\n",doc_vector_W2_file);
						for (Entry<Integer, Double> entry2 : subList1) {
							Integer docNo = entry2.getKey();
							//System.out.println("Checkfordocno "+ docNo);
							
							readindexfordocvector_W2(docNo);
							Double cosine_similarity = entry2.getValue();
							count++;
							String title = "";
							title = DocProcessor.getTitle(cranfieldFolder, docNo);
							System.out.println("Rank: " + count);
							System.out.println("Score: " + cosine_similarity);
							System.out.println("External Document Identifier: cranfield"
									+ docNo);
							System.out.println("Headline: " + title);
							System.out.println("*************************************");
							printDocumentVector_W2();
							docvector1.clear();
						}

					}


					}
				



			private void readindexfordocvector_W1(Integer docNo) throws FileNotFoundException, IOException {
				// TODO Auto-generated method stub
				try (FileReader inputFile = new FileReader(version1_uncompress);
						BufferedReader bufferReader = new BufferedReader(inputFile);) {
					//word = word + "-";
					String line = "";
					while ((line = bufferReader.readLine()) != null) {
					
							
							String[] documentFreq = line.split("\t\t\t");
				//			System.out.println("documentFreq[0] "+ documentFreq[0]);
							String intValue[] = documentFreq[0].split("-");
							String word_key = intValue[0];
							//String intValue = documentFreq[0].replaceAll("[^0-9]", "");
				//			System.out.println("word_key "+ word_key);
							int docFreq = Integer.parseInt(intValue[1]);
				//			System.out.println("docfreq " +docFreq );
							String[] termFreqs = documentFreq[1].split("\t");
							for (String termFreq : termFreqs) {
								String[] docNo_freq = termFreq.split("-");
								 int docNo1 = Integer.parseInt(docNo_freq[0]); 
								 
									 int term_freq = Integer.parseInt(docNo_freq[1]);
									 int maxtf = 0;
									 for (String str : docLen2.get(docNo).keySet()) {
										 maxtf = docLen2.get(docNo).get(str);
									 }
									 int docLen = this.docLen.get(docNo);
									 if (docNo1 == docNo){
										//System.out.println("testing for docno "+  docNo);
/*										if (!docvector.containsKey(word_key)) {
												HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
												temp.put(docNo,term_freq );
												docvector.put(word_key,temp);
										} else {
										
											HashMap<String, Double> temp1 = queryvector.get(queryNo);
											if (temp1.containsKey(word)) {
													
											} else {
												temp1.put(word, weight);
											}
										}*/
										caldoc_vector_W1(word_key, docNo, docFreq, term_freq, maxtf);
										// caldoc_vector_W2(word_key, docNo, docFreq, term_freq, docLen);
									 }

								 }

					}
				}
			}
			
			

			private void readindexfordocvector_W2(Integer docNo) throws FileNotFoundException, IOException {
				// TODO Auto-generated method stub
				try (FileReader inputFile = new FileReader(version1_uncompress);
						BufferedReader bufferReader = new BufferedReader(inputFile);) {
					//word = word + "-";
					String line = "";
					while ((line = bufferReader.readLine()) != null) {
					
							
							String[] documentFreq = line.split("\t\t\t");
				//			System.out.println("documentFreq[0] "+ documentFreq[0]);
							String intValue[] = documentFreq[0].split("-");
							String word_key = intValue[0];
							//String intValue = documentFreq[0].replaceAll("[^0-9]", "");
				//			System.out.println("word_key "+ word_key);
							int docFreq = Integer.parseInt(intValue[1]);
				//			System.out.println("docfreq " +docFreq );
							String[] termFreqs = documentFreq[1].split("\t");
							for (String termFreq : termFreqs) {
								String[] docNo_freq = termFreq.split("-");
								 int docNo1 = Integer.parseInt(docNo_freq[0]); 
								 
									 int term_freq = Integer.parseInt(docNo_freq[1]);
									 int maxtf = 0;
									 for (String str : docLen2.get(docNo).keySet()) {
										 maxtf = docLen2.get(docNo).get(str);
									 }
									 int docLen = this.docLen.get(docNo);
									 if (docNo1 == docNo){
										//System.out.println("testing for docno "+  docNo);
/*										if (!docvector.containsKey(word_key)) {
												HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
												temp.put(docNo,term_freq );
												docvector.put(word_key,temp);
										} else {
										
											HashMap<String, Double> temp1 = queryvector.get(queryNo);
											if (temp1.containsKey(word)) {
													
											} else {
												temp1.put(word, weight);
											}
										}*/
										// caldoc_vector_W1(word_key, docNo, docFreq, term_freq, maxtf);
										 caldoc_vector_W2(word_key, docNo, docFreq, term_freq, docLen);
									 }

								 }

					}
				}
			}

			private void caldoc_vector_W1(String word_key, Integer docNo,
					int docFreq, int term_freq, int maxtf) {
				// TODO Auto-generated method stub
				double collectionsize = 1400;
				double weight = (0.4 + 0.6 * Math.log(term_freq + 0.5) / Math.log(maxtf + 1.0))
						* (Math.log(collectionsize / docFreq) / Math.log(collectionsize));
				
				if (!docvector.containsKey(docNo)) {
					HashMap<String, Double> temp = new HashMap<String, Double>();
					temp.put(word_key, weight);
					docvector.put(docNo, temp);
				} else {

					HashMap<String, Double> temp1 = docvector.get(docNo);
					if (temp1.containsKey(word_key)) {
						
					} else {
						temp1.put(word_key, weight);
					}
				}
			}


			private void caldoc_vector_W2(String word_key, Integer docNo,
					int df, int tf, int docLen) {
				double avgdoclen = 0, sum = 0;
				int collectionsize = 1400;
				sum = avgDocLen();
				avgdoclen = sum / collectionsize;

				double weight = (0.4 + 0.6
						* (tf / (tf + 0.5 + 1.5 * (docLen / avgdoclen)))
						* Math.log(collectionsize / df) / Math.log(collectionsize));

				
				if (!docvector1.containsKey(docNo)) {
					HashMap<String, Double> temp = new HashMap<String, Double>();
					temp.put(word_key, weight);
					docvector1.put(docNo, temp);
				} else {

					HashMap<String, Double> temp1 = docvector1.get(docNo);
					if (temp1.containsKey(word_key)) {
						
					} else {
						temp1.put(word_key, weight);
					}
				}
				
/*				System.out.println("testting goin on1");
				for (Entry<Integer, HashMap<String, Double>> entry : docvector1.entrySet()) {
					int count = 0;
					int queryNo1 = entry.getKey();
					HashMap<String, Double> wordWeight = entry.getValue();
					//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
					//List<Entry<String, Double>> subList = list.subList(0, 5);
					System.out.println("*************************************");
					System.out.println("Q:" + queryNo1 + " " );
					System.out.println("*************************************");
					for (Entry<String, Double> entry1 : wordWeight.entrySet()) {
						System.out.println("testting goin on");
						String word = entry1.getKey();
						Double weight1 = entry1.getValue();
						System.out.println(" word "+ word);
						System.out.println(" weight1 "+ weight1);
						System.out.println("\n"+"Q:" + queryNo1 + " " );
						System.out.println("docno : " + docno+"cos_sim2 : " + cos_sim2);
						System.out.println();
						System.out.println("External Document Identifier: cranfield"
								+ docNo);
						System.out.println("Headline: " + title);
						System.out.println("*************************************");
					}

				}*/
				
			}


			


			private void cal_cosinesimilarity_W1() {
				// TODO Auto-generated method stub
				
			/*		for (Entry<Integer, HashMap<String, Double>> entry : queryvector.entrySet()) {
						int count = 0;
						int queryNo1 = entry.getKey();
						HashMap<String, Double> wordWeight = entry.getValue();
						for (Entry<Integer, HashMap<String, Double>> entry1 : weight1.entrySet()) {
							int DocNo = entry1.getKey();
							HashMap<String, Double> wordWeight1 = entry1.getValue();*/
				double cos_sim = 0;double weight_1;double weight_2;double collectionsize = 1400;
							for (int i = 1 ; i<=20 ; i++){
								 HashMap<String,Double> wordWeight2 = queryvector.get(i); 
								 for (Entry<String, Double> entry : wordWeight2.entrySet()){
				//				    	System.out.println("entry.getKeywordweight2query "+"i" + i + entry.getKey());
								    }
								 for (int j = 1 ; j <=collectionsize; j++){ 
									 HashMap <String,Double> wordWeight3 =  new HashMap<String,Double>() ;
									 wordWeight3=weight1.get(j);
									 cos_sim = 0;
									 /*test*/
									 if (!(wordWeight3 == null)){
									    for (Entry<String, Double> entry : wordWeight3.entrySet()){
					//				    	System.out.println("entry.getKeywordweight3 "+"j" + j + entry.getKey());
									    }/*test*/
									 for (Entry<String, Double> entry : wordWeight2.entrySet()){
					//					 System.out.println("entry.getKey "+ entry.getKey());
										 String word2 = entry.getKey();
										 if(word2!= null){
										 if (wordWeight3.containsKey(word2)){
											 
										 
											 weight_1 =wordWeight3.get(word2);
											 weight_2 = wordWeight2.get(word2);
											 cos_sim += weight_1 * weight_2 ; 
										 }
									 /////
										 else{
											 weight_1 = 0;
										 	weight_2 = wordWeight2.get(word2);
										 	cos_sim += weight_1 * weight_2 ;
										 }
									 }
									 }
									 }
									 if (!cosine_similarity.containsKey(i)) {
											HashMap<Integer, Double> temp = new HashMap<Integer, Double>();
											temp.put(j, cos_sim);
											cosine_similarity.put(i, temp);
										} else {

											HashMap<Integer, Double> temp1 = cosine_similarity.get(i);
											if (temp1.containsKey(j)) {
												
											} else {
												temp1.put(j, cos_sim);
											}
										}
								 }
								}
							
							for (Entry<Integer, HashMap<Integer, Double>> entry : cosine_similarity.entrySet()) {
							int count = 0;
							int queryNo1 = entry.getKey();
							HashMap<Integer, Double> wordWeight = entry.getValue();
							//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
							//List<Entry<String, Double>> subList = list.subList(0, 5);
							/*System.out.println("*************************************");
							System.out.println("Q:" + queryNo1 + " " );
							System.out.println("*************************************");*/
							for (Entry<Integer, Double> entry1 : wordWeight.entrySet()) {
								Integer docno = entry1.getKey();
								Double cos_sim1 = entry1.getValue();
								//System.out.println("\n"+"Q:" + queryNo1 + " " );
								//System.out.println("docno : " + docno+"cos_sim : " + cos_sim);
								//System.out.println();
								/*System.out.println("External Document Identifier: cranfield"
										+ docNo);
								System.out.println("Headline: " + title);
								System.out.println("*************************************");*/
							}
							 }
							
							
							
						
							
			}
			
			private void cal_cosinesimilarity_W2() {
				// TODO Auto-generated method stub
				
			/*		for (Entry<Integer, HashMap<String, Double>> entry : queryvector.entrySet()) {
						int count = 0;
						int queryNo1 = entry.getKey();
						HashMap<String, Double> wordWeight = entry.getValue();
						for (Entry<Integer, HashMap<String, Double>> entry1 : weight1.entrySet()) {
							int DocNo = entry1.getKey();
							HashMap<String, Double> wordWeight1 = entry1.getValue();*/
				double cos_sim1 = 0;double weight_1;double weight_2;double collectionsize = 1400;
							for (int i = 1 ; i<=20 ; i++){
								 HashMap<String,Double> wordWeight2 = queryvector1.get(i); 
								 for (Entry<String, Double> entry : wordWeight2.entrySet()){
				//				    	System.out.println("entry.getKeywordweight2query "+"i" + i + entry.getKey());
								    }
								 for (int j = 1 ; j <=collectionsize; j++){ 
									 HashMap <String,Double> wordWeight3 =  new HashMap<String,Double>() ;
									 wordWeight3=weight2.get(j);
									 cos_sim1 = 0;
									 /*test*/
									 if (!(wordWeight3 == null)){
									    for (Entry<String, Double> entry : wordWeight3.entrySet()){
					//				    	System.out.println("entry.getKeywordweight3 "+"j" + j + entry.getKey());
									    }/*test*/
									 for (Entry<String, Double> entry : wordWeight2.entrySet()){
					//					 System.out.println("entry.getKey "+ entry.getKey());
										 String word2 = entry.getKey();
										 if(word2!= null){
										 if (wordWeight3.containsKey(word2)){
											 
										 
											 weight_1 =wordWeight3.get(word2);
											 weight_2 = wordWeight2.get(word2);
											 cos_sim1 += weight_1 * weight_2 ; 
										 }
									 /////
										 else{
											 weight_1 = 0;
										 	weight_2 = wordWeight2.get(word2);
										 	cos_sim1 += weight_1 * weight_2 ;
										 }
									 }
									 }
									 }
									 if (!cosine_similarity1.containsKey(i)) {
											HashMap<Integer, Double> temp = new HashMap<Integer, Double>();
											temp.put(j, cos_sim1);
											cosine_similarity1.put(i, temp);
										} else {

											HashMap<Integer, Double> temp1 = cosine_similarity1.get(i);
											if (temp1.containsKey(j)) {
												
											} else {
												temp1.put(j, cos_sim1);
											}
										}
								 }
								}
							
							for (Entry<Integer, HashMap<Integer, Double>> entry : cosine_similarity1.entrySet()) {
							int count = 0;
							int queryNo1 = entry.getKey();
							HashMap<Integer, Double> wordWeight = entry.getValue();
							//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
							//List<Entry<String, Double>> subList = list.subList(0, 5);
							/*System.out.println("*************************************");
							System.out.println("Q:" + queryNo1 + " " );
							System.out.println("*************************************");*/
							for (Entry<Integer, Double> entry1 : wordWeight.entrySet()) {
								Integer docno = entry1.getKey();
								Double cos_sim2 = entry1.getValue();
								//System.out.println("\n"+"Q:" + queryNo1 + " " );
								//System.out.println("docno : " + docno+"cos_sim2 : " + cos_sim2);
								//System.out.println();
								/*System.out.println("External Document Identifier: cranfield"
										+ docNo);
								System.out.println("Headline: " + title);
								System.out.println("*************************************");*/
							}
							 }
							
							
							
						
							
			}			
			
	public static void main(String args[]) throws IOException {
		/*String cranfiledFolder = "C:/Users/Sumit Sikhwal/Documents/IR/Cranfield";
		String PathtostopWords = "C:/Users/Sumit Sikhwal/workspace/stopwords";
	//	String Pathtoqueries = "C:/Users/Sumit Sikhwal/workspace/Homework3_IR/hw3.queries";
		String Pathtoqueries = "C:/Users/Sumit Sikhwal/Documents/hw3.queries";*/
		String cranfiledFolder = args[0];
		String PathtostopWords = args[1];
	//	String Pathtoqueries = "C:/Users/Sumit Sikhwal/workspace/Homework3_IR/hw3.queries";
		String Pathtoqueries = args[2];
		RankedRetrieval retrieval = new RankedRetrieval(cranfiledFolder, PathtostopWords,
				Pathtoqueries);
		retrieval.avgDocLen();
		retrieval.readStopWords();
		retrieval.readQueries();
		retrieval.calculateWeight();
        retrieval.deleteFile(query_vector_W1_file);
        retrieval.deleteFile(query_vector_W2_file);
		retrieval.printqueryVector_W1();
		retrieval.printqueryVector_W2();
		retrieval.deleteFile(doc_vector_W1_file);
		retrieval.deleteFile(doc_vector_W2_file);
		//retrieval.printDocumentVector_W1();
		retrieval.cal_cosinesimilarity_W1();
		retrieval.cal_cosinesimilarity_W2();
		System.out.println("Rank W1");
		retrieval.findRank_W1();
		System.out.println("Rank W2");
		retrieval.findRank_W2();
		/*System.out.println("testting goin on1");*/
		/*for (Entry<Integer, HashMap<String, Double>> entry : docvector1.entrySet()) {
			int count = 0;
			int queryNo1 = entry.getKey();
			HashMap<String, Double> wordWeight = entry.getValue();
			//List<Entry<String, Double>> list = (List<Entry<String, Double>>) wordWeight;
			//List<Entry<String, Double>> subList = list.subList(0, 5);
			System.out.println("*************************************");
			System.out.println("Q:" + queryNo1 + " " );
			System.out.println("*************************************");
			for (Entry<String, Double> entry1 : wordWeight.entrySet()) {
				System.out.println("testting goin on");
				String word = entry1.getKey();
				Double weight = entry1.getValue();
				System.out.println("\n"+"Q:" + queryNo1 + " " );
				System.out.println("docno : " + docno+"cos_sim2 : " + cos_sim2);
				System.out.println();
				System.out.println("External Document Identifier: cranfield"
						+ docNo);
				System.out.println("Headline: " + title);
				System.out.println("*************************************");
			}
			 }*/
		//retrieval.
		/*System.out.println("Rank W1");
		retrieval.findRank(retrieval.weight1);
		System.out.println("Rank W2");
		retrieval.findRank(retrieval.weight2);*/
	}



	
}



	


	
