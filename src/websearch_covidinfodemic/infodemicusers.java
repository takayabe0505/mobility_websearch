package websearch_covidinfodemic;

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

import websearch_covid.getresidents;

public class infodemicusers {

	/**
	 * code for PNAS Brief Paper on COVID-19 infodemic and its impacts on mobility compliance 
	 * タスク：コロナに関する偽情報を検索しているユーザーの抽出
	 * 2020.11.19
	 * @throws IOException 
	 * 
	 */

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	public static void main(String[] args) throws IOException, ParseException {

		// parameters 
		String datadir = "/mnt/home1/q_emotion/q_sum/";
		String root = "/mnt/tyabe/"; File root_f = new File(root); root_f.mkdir();
		String home = root+"infodemic/"; File home_f = new File(home); home_f.mkdir();
		String resdir  = home+"search_bydays/"; File resdir_f = new File(resdir); resdir_f.mkdir();
		String startdate = "20200101";
		String enddate   = "20200915";

		// words identified as false information
		File infodemic_words_f = new File(root+"infodemic_words.csv");
		HashSet<String> infodemic_words = getwords(infodemic_words_f);

		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			Date next_date = getresidents.nextday_date(date);
			System.out.println("==== starting "+date_str);

			// get ids that were in Tokyo on this day
			File querydata = new File(datadir+date_str+"_japan.tsv");
			File out       = new File(resdir+date_str+"_falsesearch.tsv");
			if(querydata.exists()) {
				getSearchers(querydata, infodemic_words, date_str, out);
			}
			date = next_date;
		}
		
		// aggregate and collect data for anonymization
		File aggres = new File(home+"infodemic_date_count.tsv");
		collectresults(resdir, aggres);
		
	}


	// https://www.soumu.go.jp/main_content/000693295.pdf
	public static HashSet<String> getwords(File in) throws IOException{
		HashSet<String> wordlist = new HashSet<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in),"UTF-8"));
		String line = null;
		while((line=br.readLine())!=null) {
			wordlist.add(line);
		}
		br.close();
		return wordlist;
	}

	// get search count data for this day
	public static void getSearchers(
			File in, 
			HashSet<String> wordlist,
			String yyyymmdd,
			File out
			) throws IOException{
		HashMap<String, HashMap<String, HashSet<String>>> word_time_idset = new HashMap<String, HashMap<String, HashSet<String>>>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in),"UTF-8"));
		String line = null;
		while((line=br.readLine())!=null) {
			String[] words  = line.split("\u0001");
			if((words.length>=5)&&(line.substring(0,4).equals("2020"))) {
//				System.out.println("-- OK line: "+String.valueOf(words.length)+"; "+
//						line.substring(0,4)+"; "+line);
				String id       = words[1];
				//			String device   = words[2];
				//			if(device.equals("smartphone")) {
				String query    = words[3];
				if((query.contains("コロナ"))||(query.contains("covid-19"))||(query.contains("covid"))) {
					for(String w : wordlist) {
						if(query.contains(w)) {
							if(w.equals("")) {
								w="dummy";
							}
							String time = words[0].split(" ")[1];
							String timeslot = utils.time2slot(time);
							if(word_time_idset.containsKey(w)) {
								if(word_time_idset.get(w).containsKey(timeslot)) {
									word_time_idset.get(w).get(timeslot).add(id);
								}
								else {
									HashSet<String> tmp1 = new HashSet<String>();
									tmp1.add(id);
									word_time_idset.get(w).put(timeslot, tmp1);
								}
							}
							else {
								HashSet<String> tmp1 = new HashSet<String>();
								tmp1.add(id);
								HashMap<String, HashSet<String>> tmp2 = new HashMap<String, HashSet<String>>();
								tmp2.put(timeslot, tmp1);
								word_time_idset.put(w, tmp2);
							}
						}}}
			}
			else {
				System.out.println("-- skipped error line: "+String.valueOf(words.length)+
						"; "+line.substring(0,4)+"; "+line);
			}
			//			}
		}
		br.close();

		// write out to file
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out),"UTF-8"));
		for(String w: word_time_idset.keySet()) {
			if(w.equals("dummy")) {
				for(Integer h=0; h<=47; h++) {
					String d = String.valueOf(h);
					if(word_time_idset.get(w).containsKey(d)) {
						HashSet<String> idlist = word_time_idset.get(w).get(d);
						bw.write(w+"\t"+yyyymmdd+" "+utils.slot2time(d)+"\t"+String.valueOf(idlist.size())+"\t");
					}
					else {
						bw.write(w+"\t"+yyyymmdd+" "+utils.slot2time(d)+"\t"+"0"+"\t");
					}
					bw.newLine();
				}
			}
			else {
				for(Integer h=0; h<=47; h++) {
					String d = String.valueOf(h);
					if(word_time_idset.get(w).containsKey(d)) {
						HashSet<String> idlist = word_time_idset.get(w).get(d);
						bw.write(w+"\t"+yyyymmdd+" "+utils.slot2time(d)+"\t"+String.valueOf(idlist.size())+"\t");
						for(String id : idlist) {
							bw.write(id+",");
						}
					}
					else {
						bw.write(w+"\t"+yyyymmdd+" "+utils.slot2time(d)+"\t"+"0"+"\t");
					}
					bw.newLine();
				}
			}
		}
		bw.close();
	}

	public static void collectresults(
			String resdir,
			File out
			) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out),"UTF-8"));
		File[] files = new File(resdir).listFiles();
		for(File f : files) {
			if(f.getName().contains("falsesearch")) {
				String date = f.getName().split("_")[0];
				HashMap<String, Integer> word_count = new HashMap<String, Integer>();
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
				String line = null;
				while((line=br.readLine())!=null) {
					String[] tokens  = line.split("\t");
					String word = tokens[0];
					Integer count = Integer.valueOf(tokens[2]);
					if(word_count.containsKey(word)) {
						Integer newcount = word_count.get(word)+count;
						word_count.put(word, newcount);
					}
					else {
						word_count.put(word, count);
					}
				}
				br.close();
				
				for(String w : word_count.keySet()) {
					bw.write(w+"\t"+date+"\t"+String.valueOf(word_count.get(w)));
					bw.newLine();
				}
			}
		}
		bw.close();
	}
	

}
