import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;


public class IndexBuilding {
     static ArrayList stopwords_list ;
    /* stem words, filename , count */
    private static Map<String, HashMap<String,Integer>> stems;
    /* lemmas, filename , count */ 
    private static Map<String, HashMap<String,Integer>> lemmas;
    /* document freq for stems */
    private static HashMap<String,Integer> df = new HashMap<String,Integer>();
    /* document freq for lemmas */
    private  HashMap<String,Integer> df1 = new HashMap<String,Integer>();
    /* number of words in a document  */
    private static  HashMap<String,Integer> Doclength ;
    private static Map<String, HashMap<String,Integer>> maxFreq_stems;
    private StanfordLemmatizer stanfordLemma =  new StanfordLemmatizer();
    private List<String> list;

    public IndexBuilding() {
    			stems = new HashMap<String, HashMap<String,Integer>>();
    			list = new LinkedList<String>();
    			lemmas = new HashMap<String, HashMap<String,Integer>>();
    			maxFreq_stems = new HashMap<String, HashMap<String,Integer>>();
    			 Doclength = new HashMap<String,Integer>();
    		}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		if(args.length <= 0) {
			 System.out.println("No argument value "); 
		  }
			if (args.length == 1){
				System.out.println("No argument value for the stopwords file path ");
			}
		 //String path1 = "C:/Users/Sumit Sikhwal/Documents/IR/";
		 String path1 = args[0].toString();
		// System.out.println("args[0] "+args[0] );
		// System.out.println("args[1] "+args[1] );
		  File folder = new File(path1);
		  File[] listOfFiles = folder.listFiles();
		// File stopword_fl  = new File("C:/Users/Sumit Sikhwal/workspace/Homework2/stopwords");
		  File stopword_fl  = new File(args[1].toString());
		  String s = "";String s1 = "";String s2 = "";
		  int val = 0;int counter = 0; int singlecount = 1;int nooftokens = 0;
		  int counter1 = 0;
		  Integer intObj = new Integer(singlecount);
		  HashMap<String,Integer> uniquetokens =  new  HashMap<String,Integer>();
		  HashMap<String,Integer> uniquestems =  new  HashMap<String,Integer>();
		  long startTimeMs,taskTimeInMillis;
		  ArrayList token_list = new ArrayList();
		  ArrayList stopwords_list1 = new ArrayList() ;
		  IndexBuilding Obj = new IndexBuilding();
		  stopwords_list = new ArrayList();
		  stopwords_list = Obj.readstopwords(stopword_fl);
		  /* files  */
		  String v1_uncompressed = "v1_uncompressed_lemmas";
		  String v2_uncompressed = "v2_uncompressed_stems";
		  String max_tf_file     = "max_tf";
		  String doc_length_file =   "doc_length";
		  String v1_compress_file  = "v1_compress_lemmas";
		  String v2_compress_file = "v2_compress_stems";

		   for (File file : listOfFiles) {
		      if (file.isFile()) {
		    	  String FileName = file.getName().replace("cranfield", "");
		    	  String FilePath  = folder + "/" + file.getName();
		    	  Obj.readCranfieldData(FileName , FilePath );		         
		      }
	      }
		   startTimeMs = System.currentTimeMillis();
		   Obj.deleteFile(v2_uncompressed);
		   for (Entry<String, HashMap<String,Integer>> entry : stems.entrySet()) {
			   Map<String,Integer> docfreq    =stems.get(entry.getKey());
			   String data = "word -> "+entry.getKey()+"     "+ " doc freq =  "+docfreq.size() +"      "+ " filename "+entry.getValue();
			   Obj.writedata(data,v2_uncompressed);
			   Obj.writedata("\n",v2_uncompressed);
		   }
		    taskTimeInMillis = System.currentTimeMillis() - startTimeMs;
		   System.out.println(String.format("Time elapsed to build uncompressed Version 2 is %d milli Secs ",taskTimeInMillis)); 
		   
		   startTimeMs = System.currentTimeMillis();
		   Obj.deleteFile(v1_uncompressed);
		   for (Entry<String, HashMap<String,Integer>> entry : lemmas.entrySet()) {
			   Map<String,Integer> docfreq    =lemmas.get(entry.getKey());
 		  	   String data = "word -> "+entry.getKey()+"     "+" doc freq =  "+docfreq.size() +"     " +" filename "+entry.getValue();
			   Obj.writedata(data,v1_uncompressed);
			   Obj.writedata("\n",v1_uncompressed);
		   }
		   taskTimeInMillis = System.currentTimeMillis() - startTimeMs;
		   System.out.println(String.format("Time elapsed to build uncompressed Version 1 is %d milli Secs ",taskTimeInMillis)); 
	
		   Obj.deleteFile(max_tf_file);
		   Obj.max_tf(maxFreq_stems,max_tf_file);
		   Obj.deleteFile(doc_length_file);
		   Obj.doc_length(Doclength,doc_length_file);
		   Obj.deleteFile(v1_compress_file);
		   startTimeMs = System.currentTimeMillis();
		   Obj.V1_compress_dic(lemmas,v1_compress_file);
		   Obj.V1_compress_postfile(lemmas,v1_compress_file);
		  taskTimeInMillis = System.currentTimeMillis() - startTimeMs;
		  System.out.println(String.format("Time elapsed to build compressed Version 1 is %d milli Secs ",taskTimeInMillis)); 
		  startTimeMs = System.currentTimeMillis();
		  Obj.deleteFile(v2_compress_file);
		   Obj.V2_compress_dic(stems,v2_compress_file);
		   Obj.V2_compress_posting(stems,v2_compress_file);
		  taskTimeInMillis = System.currentTimeMillis() - startTimeMs;
		  System.out.println(String.format("Time elapsed to build compressed Version 2 is %d milli Secs ",taskTimeInMillis));
		  System.out.print("The size of the Index Version 1 uncompressed : ");
		  System.out.println((Obj.getFileSize(v1_uncompressed))+" Bytes");
		  System.out.print("The size of the Index Version 2 uncompressed : ");
		  System.out.println((Obj.getFileSize(v2_uncompressed))+ " Bytes");
		  System.out.print("The size of the Index Version 1 compressed : ");
		  System.out.println((Obj.getFileSize(v1_compress_file))+ " Bytes");
		  System.out.print("The size of the Index Version 2 compressed : ");
		  System.out.println((Obj.getFileSize(v2_compress_file))+" Bytes");

		  System.out.print("The number of inverted list in Index Version 1 uncompressed: ");
		  System.out.println(Obj.invertedListLength(lemmas));
		  System.out.print("The number of inverted list in Index Version 2 uncompressed: ");
		  System.out.println(Obj.invertedListLength(stems)); 
		  System.out.print("The number of inverted list in Index Version 1 compressed: ");
		  System.out.println(Obj.invertedListLength(lemmas)); 
		  System.out.print("The number of inverted list in Index Version 2 compressed: ");
		  System.out.println(Obj.invertedListLength(stems));
		  String[] searchwords= {"Reynolds","NASA","prandtl","flow","pressure","boundary","shock"};
		  Obj.dfTfLenforStems(stems,searchwords ); 
		  Obj.dfTfLenforlemmas(lemmas,searchwords );
	} 
	
		private void V1_compress_postfile(
			Map<String, HashMap<String, Integer>> lemmas3,
			String fileName) throws IOException {
		// TODO Auto-generated method stub
			List<String> lemma = new ArrayList<String>(lemmas3.keySet());
			String print,gammacode,hexcode,deltacode,hexcode1 = "";
	        Collections.sort(lemma);
	      
	        for(String word: lemma){
	            print ="";
	            gammacode = "";
	            hexcode =""; 
	            deltacode = "";
	            Map<String,Integer> documentMap = lemmas3.get(word);
	            print = word + "-"+documentMap.size()+"  ";
	            writedata(print,fileName);
	            List<String> Docs= new ArrayList<String>(documentMap.keySet());
	            Collections.sort(Docs);
	            writedata(Docs.get(0)+"|",fileName); 
	            int termfreq = 0;
	            for(int i =0;i < (Docs.size()-1);i++){
	             int doc1 = Integer.parseInt(Docs.get(i));
	             int doc2 = Integer.parseInt(Docs.get(i+1));
	             
	             
	             gammacode = gammacode + InttoGammacode(doc2-doc1);
	             }
	            // gammacode1 = new BigInteger(gammacode,2).toByteArray();
	            for(int i =0 ; i<Docs.size();i++ ){
	            	termfreq = documentMap.get(Docs.get(i));
	            	deltacode = deltacode + InttoDeltacode(termfreq);
	            }
	            //deltacode1 = Integer)(gammacode).toByteArray();
	             if(gammacode != "")
	             { 
	               hexcode = HexConversion(gammacode);
	             }    
	             if(deltacode != "")
	             { 
	               hexcode1 = HexConversion(gammacode);
	             } 
	             writedata(gammacode,fileName);
	             writedata(deltacode,fileName);
	             //writedata1(gammacode1,fileName);
	             //writedata1(deltacode1,fileName);
	             writedata("\n",fileName);
	 
	        }
		}
		
		
		private String InttoDeltacode(int termfreq) {
			// TODO Auto-generated method stub
			String binary = Integer.toBinaryString(termfreq);
	          int len = binary.length();
	          String gammaCode = InttoGammacode(len);
	          String offset = binary.substring(1,len);
	          
	          return (gammaCode + offset);
		}
		private void V2_compress_posting(
				Map<String, HashMap<String, Integer>> stems,
				String fileName) throws IOException {
			// TODO Auto-generated method stub
				List<String> stem = new ArrayList<String>(stems.keySet());
				String data,gammacode,hexcode,deltacode;
		        Collections.sort(stem);
		        for(String word: stem){
		            data ="";
		            gammacode = "";
		            hexcode =""; 
		            deltacode= "";
		            Map<String,Integer> documentMap = stems.get(word);
		            data = word + "-"+documentMap.size()+"  ";
		            writedata(data,fileName);
		            List<String> Docs= new ArrayList<String>(documentMap.keySet());
		            Collections.sort(Docs);
		            writedata(Docs.get(0)+"|",fileName); 	
		            int termfreq = 0;
		            for(int i =0;i < (Docs.size()-1);i++){
		            	int doc1 = Integer.parseInt(Docs.get(i));
		            	int doc2 = Integer.parseInt(Docs.get(i+1));
		              
		            	gammacode = gammacode + InttoGammacode(doc2-doc1);
		             }
		            for(int i =0 ; i<Docs.size();i++ ){
		            	termfreq = documentMap.get(Docs.get(i));
		            	deltacode = deltacode + InttoDeltacode(termfreq);
		            }
		             if(gammacode != "")
		             { 
		               hexcode = HexConversion(gammacode);
		             }    
		             
		             writedata(gammacode,fileName);
		             writedata(deltacode,fileName);
		             writedata("\n",fileName);
		 
		        }
			}
		private String HexConversion(String gammacode) {
			// TODO Auto-generated method stub
			 int i =0;int l=0;
			   String hexcode ="";
			   while(i < gammacode.length())
			   {
			      l = i +4;
			      if(gammacode.length() < l)
			      l = l - ( l - gammacode.length());

			      String subCode = gammacode.substring(i,l);
			      hexcode = hexcode + Integer.toHexString(Integer.parseInt(subCode,2));
			      i += 4;
			   }
			   return hexcode;
			}

		public void dfTfLenforStems(Map<String,HashMap<String,Integer>> map,String[] searchWords) throws IOException
		{
		    
		   String fileName = "";
		   StringBuffer sb = new StringBuffer();
		   List<String> words1  = new ArrayList<String>(map.keySet());
		   String data;
		   Collections.sort(words1);
		   for(String word : searchWords){
			   word = word.toLowerCase();
			   fileName = word + "_stem";
			    deleteFile(fileName);
			   Stemmer obj =  new Stemmer();
			
			   char  [] tokens1 =word.toCharArray();
			   for( int j = 0 ; j<tokens1.length;j++){
				
				   obj.add(tokens1[j]);	
			   }
			   obj.stem();
			   String u;
			   u = obj.toString();
	        
		    
		    data ="";   
		    if(words1.contains(u))
		    {
		      Map <String,Integer> docfreq = map.get(u);
		      data = u +" - "+docfreq.size()+"   ";
		      //System.out.print(" the df for the term "+ u +" (stem)  :"+data );
		       writedata(data,fileName);
		      List<String> Doc = new ArrayList<String>(docfreq.keySet());
		      Collections.sort(Doc);
		      int count = 0;
		      for(String DocumentName : Doc){
		          count += docfreq.get(DocumentName);
		         
		          writedata(DocumentName+"="+count+"  ",fileName);
		           sb.append(DocumentName);
		           sb.append(":");
		      }    
		     // System.out.print(" the tf for the term "+ u +" (stem)  : " +count+"  " );
		   }
		    
		   System.out.print(" Inverted list length for the term "+ u +" (stem)  : ");
		   System.out.println(sb.toString().getBytes().length+" Bytes");
		 }
		}
		
		public void dfTfLenforlemmas(Map<String,HashMap<String,Integer>> map,String[] searchWords) throws IOException
		{
			String line="";
			   String fileName = ""; 
			   StringBuffer sb = new StringBuffer();
			   List<String> key = new ArrayList<String>(map.keySet());
			   String data;
			   Collections.sort(key);
			  for(String term : searchWords)
			  line = line + term + " ";
			 
			  list = stanfordLemma.lemmatize(line);
			   for(String word : list){
			    word = word.toLowerCase();
			    fileName = word + "_lemma";
			    deleteFile(fileName);
			    data ="";
			    if(key.contains(word))
			    {
			      Map <String,Integer> docfreq = map.get(word);
			      data = word +"-"+docfreq.size()+"   ";
			      writedata(data,fileName);
			      List<String> Doc = new ArrayList<String>(docfreq.keySet());
			      Collections.sort(Doc);
			      for(String docName : Doc){
			         int count = docfreq.get(docName);
			         writedata(docName+"="+count+"  ",fileName);  
			         sb.append(docName);
			         sb.append("=");
			      }    
			   }
//		   String fileName = "";
			/*String str = "";
		   StringBuffer sb = new StringBuffer();
		   List<String> words_list  = new ArrayList<String>(map.keySet());
		   String data;
		   Collections.sort(words_list);
		   for(String word : searchWords){
			   //word = word.toLowerCase();
		      str = str + word + " ";
		      list = stanfordLemma.lemmatize(str);
			  
	        
		      for(String word1:list){
		    data ="";   
		    	if(words_list.contains(word1))
		    	{
		    		Map <String,Integer> docfreq = map.get(word1);
		    		data = "  "+docfreq.size()+"   ";
		    		System.out.print(" the df for the term "+ word1+" (lemma)  :"+data );
		  //    writedata(data,fileName);
		    		List<String> Doc = new ArrayList<String>(docfreq.keySet());
		    		Collections.sort(Doc);
		    		int count = 0;
		    		for(String DocumentName : Doc){
		    			 count += docfreq.get(DocumentName);
		    			//System.out.print(" the tf for "+ word+" lemma  :"+DocumentName+"  "+count+"  " );
		      //   writedata(DocumentName+"->"+count+"  ",fileName);
		    			sb.append(DocumentName);
		    			sb.append(":");
		      }    
		    		System.out.print(" the tf for the term "+ word1 +" (lemma)  : " +count+"  " );
		   }*/
		      
		   System.out.print(" Inverted list length for the term "+ word+" lemma  : ");
		   System.out.println(sb.toString().getBytes().length+" Bytes");
		   }
		}
		
		private String InttoGammacode(int k) {
			// TODO Auto-generated method stub
			String  Binvalue = Integer.toBinaryString(k);
			int len = Binvalue.length();
			String offset = Binvalue.substring(1, len);
			String unary_value = "";
			for (int i = 0; i < offset.length(); i++) {
				unary_value = unary_value + "1";
			}
			unary_value = unary_value + "0";
			return (unary_value + offset);
			
		}
		private void V1_compress_dic(Map<String, HashMap<String, Integer>> lemmas2,
			String fileName) throws IOException {
		// TODO Auto-generated method stub
			List<String> words = new ArrayList<String>(new HashSet(lemmas.keySet()));
			Collections.sort(words);
			for(int i = 0 ; i < words.size();i++){
				//System.out.println("words print"+ words.get(i));
			}
			int blocksize = 8;
			int counter = 0;
			String words2[] = new String[8]; 
			while(counter < words.size() ){
				for (int i = 0; i < blocksize; i++) {
	                if ( counter >= words.size())
	                break;  
	                words2[i] = words.get(counter);
	                counter++;
	            }
				  boolean flag = true;
			      int counter3 = 0;
			     
			String first_word = words2[0];
			for(int i = 0; i<first_word.length();i++)
	        {
	             char ch = first_word.charAt(i);
	             for(int j = 1;j < blocksize;j++)
	              {
	                   if( ch != words2[j].charAt(i) || words2[j].length() < i )
	                   {
	                        flag = false;   
	                        break;
	                   }
	              }
	                   if(flag)
	                   counter3++; 
	                   else 
	                   break;            
	        }
	        StringBuffer sb = new StringBuffer();
	        sb.append(first_word.length());
	        sb.append(first_word.substring(0, counter3));
	        sb.append('*');
	        sb.append(first_word.substring(counter3));
	        for (int i = 1; i < blocksize; i++) {
	              String code = words2[i].substring(counter3);
	              sb.append(code.length());
	              sb.append(code);
	            }
	            writedata(sb.toString(),fileName);
	            writedata("\n",fileName);
	       }

		}
			/* compress of stems dictionary */	
		private void V2_compress_dic(Map<String, HashMap<String, Integer>> stems,
				String fileName) throws IOException {
			// TODO Auto-generated method stub
				//List<String> words = new ArrayList<String>(new HashSet(stems.keySet()));
				List<String> words = new ArrayList<String>(new HashSet(stems.keySet()));
				Collections.sort(words);
				for(int i = 0 ; i < words.size();i++){
					//System.out.println("words print"+ words.get(i));
				}
				int blocksize = 8;
				int counter = 0;
				String words2[] = new String[8]; 
				while(counter < words.size() ){
					for (int i = 0; i < blocksize; i++) {
		                if ( counter >= words.size())
		                break;  
		                words2[i] = words.get(counter);
		                counter++;
		            }
					  boolean flag = true;
				      int counter3 = 0;
				     
				String first_word = words2[0];
				for(int i = 0; i<first_word.length();i++)
		        {
		             char ch = first_word.charAt(i);
		             for(int j = 1;j < blocksize;j++)
		              {
		                   if( ch != words2[j].charAt(i) || words2[j].length() < i )
		                   {
		                        flag = false;   
		                        break;
		                   }
		              }
		                   if(flag)
		                   counter3++; 
		                   else 
		                   break;            
		        }
		        StringBuffer sb = new StringBuffer();
		        sb.append(first_word.length());
		        sb.append(first_word.substring(0, counter3));
		        sb.append('*');
		        sb.append(first_word.substring(counter3));
		        for (int i = 1; i < blocksize; i++) {
		              String code = words2[i].substring(counter3);
		              sb.append(code.length());
		              sb.append(code);
		            }
		            writedata(sb.toString(),fileName);
		            writedata("\n",fileName);
		       }
				
			}	
	
		private void doc_length(HashMap<String, Integer> doclength,
			String doc_length_file) throws IOException {
		// TODO Auto-generated method stub
			   for(Entry<String,Integer> entry : doclength.entrySet()){
				   String data = "Document   " + entry.getKey() + "     No of word occurences "+ entry.getValue();
				   writedata(data,doc_length_file);
				   writedata("\n",doc_length_file);
				   
			   }
	}
		/*Writes data into max_tf file */
		  private void max_tf(Map<String, HashMap<String, Integer>> maxFreq_stems,
			String max_tf_file) throws IOException {
		// TODO Auto-generated method stub
			  int max_value,value;
			  String word ;
			  writedata(" Document no : most frequent word  : Frequency "+"\n",max_tf_file);
			  for(Entry<String,HashMap<String,Integer>> entry : maxFreq_stems.entrySet()){
				  StringBuffer sb1 = new StringBuffer();
				  sb1.append(entry.getKey());
				  sb1.append(':'+"   ");
				  max_value = 0;
				  word = "";
				  HashMap<String,Integer> WordFreq = entry.getValue();
				  for(Entry<String,Integer> entry2 : WordFreq.entrySet())
				     {
				             value =  (int)(entry2.getValue());
				           
				          if( max_value < value) 
				          {
				             max_value = value;
				             word = entry2.getKey()+": "+"   ";
				          } 
				     }
				     sb1.append(word);
				     sb1.append(max_value);
				     
				     writedata(sb1.toString(),max_tf_file);
				     writedata("\n",max_tf_file);
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
		
		private void writedata1(byte[] code, String fileName) throws IOException {
			// TODO Auto-generated method stub
			  // TODO Auto-generated method stub
			  File myFile = new File(fileName);
			  FileWriter fw = new FileWriter(myFile,true);
	          PrintWriter pw = new PrintWriter(fw);
	          pw.print(code);
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

		  ArrayList<String> readstopwords(File stopword_fl) throws IOException{
			  
		  
			  BufferedReader br1 = new BufferedReader(new FileReader(stopword_fl.toString()));
			  String line;
			  ArrayList<String> stopwords = new ArrayList();
			  //int i = 0;
			  while((line = br1.readLine())!= null){
				  	
				    stopwords.add(line);
				  
			  }
			  return stopwords;
		  }
		  
	
	private void readCranfieldData(String FileName, String FilePath) throws IOException {
		// TODO Auto-generated method stub
		FileReader fr = new FileReader(FilePath);
        BufferedReader br1 = new BufferedReader(fr);
        String line;
        while((line = br1.readLine())!= null){
            
        	String line2 = line.replaceAll("[<[/]?.*>\\n]","").trim().toLowerCase();
		  	Stemsfromwords(line2,FileName);
		  	Lemmasfromwords(line2,FileName);
		  	
		  
	  }        
        
	}

	private  void Stemsfromwords(String line, String fileName) {
		// TODO Auto-generated method stub
		String line1=line.replaceAll("[.'//d]+","");
		String regexp = "[\\s,;:()/.'\\n\\t?-]+";
		String line4=line.replaceAll("\\d","" );
		String words[]= line4.split(regexp);
		
		for (int i = 0 ; i < words.length; i++){
			if (!words[i].isEmpty()){
				if(!Doclength.containsKey(fileName))
				   { 
				       Doclength.put(fileName,1);
				   }
				   else
				   {
				       int count = Doclength.get(fileName) + 1;
				       Doclength.put(fileName,count);
				   }
				if(!stopwords_list.contains(words[i])){
				 
			 
					Stemmer obj =  new Stemmer();
				
					char  [] tokens1 =words[i].toCharArray();
					for( int j = 0 ; j<tokens1.length;j++){
					
						obj.add(tokens1[j]);	
					}
					obj.stem();
					String u;
					u = obj.toString();
					String stem_word=u.replaceAll("[^a-zA-Z0-9]","");
					if (!stem_word.isEmpty()){
						if (!stems.containsKey(stem_word)){
							HashMap<String,Integer> temp  =  new  HashMap<String,Integer>();
		        		temp.put(fileName, 1);
		        		df.put(stem_word,1);
		        		stems.put(stem_word,temp);
						}
						else{
							HashMap<String,Integer> temp1 =  new  HashMap<String,Integer>();
							temp1 = stems.get(stem_word);
							if (!temp1.containsKey(fileName)){
		        		
								df.put(stem_word,df.get(stem_word)+1 );
								temp1.put(fileName,1);
								stems.put(stem_word, temp1);
							}
							else{
								temp1.put(fileName, temp1.get(fileName)+1);
								stems.put(stem_word,temp1);
							}
						}
		        	
						if(!maxFreq_stems.containsKey(fileName))
						{
							HashMap<String,Integer> temp5 = new HashMap<String,Integer>();
							temp5.put(stem_word,1);
							maxFreq_stems.put(fileName,temp5);
						}
						else
						{
							HashMap<String,Integer> temp6 = maxFreq_stems.get(fileName);
							if(temp6.containsKey(stem_word))
							{
								int count3 = temp6.get(stem_word) + 1;
								temp6.put(stem_word,count3); 
							}
							else
								temp6.put(stem_word,1);
						}
		       }
		        }
		        
		}
	}
	}
	

	private  void Lemmasfromwords(String line, String fileName) {
		// TODO Auto-generated method stub
		
		String line4=line.replaceAll("[']","");
		
		list = stanfordLemma.lemmatize(line4);
	
		for(int i = 0 ; i < list.size();i++ ){
			  String word5 =list.get(i).trim();
			  String word6 =word5.replaceAll("[,\\d]","");
			if (word6.contains("-")){
				String	Hyphen_words []=word6.split("-");
				for (int j = 0 ; j<Hyphen_words.length;j++ ){
					lemmascollection(Hyphen_words[j],fileName);
				}
			}	
			else{
				word6.replaceAll("[^a-zA-Z0-9]","");
				//System.out.println()
				lemmascollection(word6,fileName);
			}
		}
	}
	public double getFileSize(String fileName)
	{
	   File file = new File(fileName);
	   double bytes = file.length();
	   return bytes;

	} 
	/* length of inverted List .**/
	public int invertedListLength(Map<String,HashMap<String,Integer>> map )
	{
	    return map.size();

	}
	private void lemmascollection(String word, String fileName) {
	
		if (!word.isEmpty()){
		
			if(!stopwords_list.contains(word)){
				if (!lemmas.containsKey(word)){
				       HashMap<String,Integer> temp2  =  new  HashMap<String,Integer>();
				        	
				        temp2.put(fileName, 1);
				        df1.put(word,1);
				        lemmas.put(word,temp2);
				 }
		      else{
				    HashMap<String,Integer> temp3 =  new  HashMap<String,Integer>();
				        	
				    temp3 = lemmas.get(word);
				    if (!temp3.containsKey(fileName)){
				        		
				    	df1.put(word,df1.get(word)+1 );
				        temp3.put(fileName,1);
				        lemmas.put(word, temp3);
				      }
				      else{
				    	  temp3.put(fileName, temp3.get(fileName)+1);
				    	  lemmas.put(word,temp3);
				        }
				  }
				
			}

	}
}
}

	 

