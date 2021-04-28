package websearch_covidinfodemic_0329;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class user_searchdata {

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");


	public static void get_searchdata(
			String root,
			String home,
			String datadir,
			String startdate, 
			String enddate
			) throws ParseException, IOException {

		// words identified as false information
		File infodemic_words_f = new File(root+"infodemic_words.csv");
		HashSet<String> infodemic_words = getwords(infodemic_words_f);

		HashMap<String, HashMap<String, Integer>> id_word_counts = new HashMap<String, HashMap<String, Integer>>();

		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			Date next_date = utils.nextday_date(date);
			System.out.println("==== starting "+date_str);

			// get ids that were in Tokyo on this day
			File querydata = new File(datadir+date_str+"_japan.tsv");
			if(querydata.exists()) {
				getUserSearchCounts(querydata, infodemic_words, id_word_counts);
			}
			date = next_date;
		}

		File idwordcount = new File(home+"id_searchcounts.tsv");
		writeout(id_word_counts, idwordcount);

	}

	public static HashSet<String> getwords(File in) throws IOException{
		HashSet<String> wordlist = new HashSet<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in),"UTF-8"));
		String line = null;
		while((line=br.readLine())!=null) {
			wordlist.add(line.split(",")[0]);
		}
		br.close();
		return wordlist;
	}


	// for each user, get daily count of misinfo search, covid search, total search 
	public static void getUserSearchCounts(
			File in,
			HashSet<String> wordlist,
			HashMap<String, HashMap<String, Integer>> id_type_counts
			) throws IOException{
		// id , misinfo count , covid search count, total search count
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in),"UTF-8"));
		String line = null;
		while((line=br.readLine())!=null) {
			String[] words  = line.split("\u0001");
			if((words.length>=5)&&(line.substring(0,4).equals("2020"))) {
				//				System.out.println("-- OK line: "+String.valueOf(words.length)+"; "+line.substring(0,4)+"; "+line);
				String id       = words[1];
				//			String device   = words[2]; //			if(device.equals("smartphone")) {
				String query    = words[3];
				if((query.contains("コロナ"))||(query.contains("covid-19"))||(query.contains("covid"))) {
					for(String w : wordlist) {
						if(query.contains(w)) {
							if(w.equals("")) {
								w="dummy";
							}
							//							String time = words[0].split(" ")[1];
							if(id_type_counts.containsKey(id)) {
								if(id_type_counts.get(id).containsKey(w)) {
									int newcount = id_type_counts.get(id).get(w)+1;
									id_type_counts.get(id).put(w,newcount);
								}
								else {
									id_type_counts.get(id).put(w, 1);
								}
							}
							else {
								HashMap<String, Integer> tmp = new HashMap<String, Integer>();
								tmp.put(w, 1);
								id_type_counts.put(id, tmp);
							}
						}
					}
				}
				else {
					String w = "noncovid";
					if(id_type_counts.containsKey(id)) {
						if(id_type_counts.get(id).containsKey(w)) {
							int newcount = id_type_counts.get(id).get(w)+1;
							id_type_counts.get(id).put(w,newcount);
						}
						else {
							id_type_counts.get(id).put(w, 1);
						}
					}
					else {
						HashMap<String, Integer> tmp = new HashMap<String, Integer>();
						tmp.put(w, 1);
						id_type_counts.put(id, tmp);
					}
				}
			}
			else {
				System.out.println("-- skipped error line: "+String.valueOf(words.length)+
						"; "+line.substring(0,4)+"; "+line);
			}
			//			}
		}
		br.close();
	}

	public static void writeout(
			HashMap<String, HashMap<String, Integer>> idwordcount, 
			File out
			) throws IOException {
		// write out to file
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out),"UTF-8"));
		for(String id: idwordcount.keySet()) {
			for(String word: idwordcount.get(id).keySet()) {
				bw.write(id+"\t"+word+"\t"+String.valueOf(idwordcount.get(id).get(word)));
				bw.newLine();
			}
		}
		bw.close();
	}



}
