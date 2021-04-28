package websearch_covidinfodemic;

import static java.util.Collections.*;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;
import org.atilika.kuromoji.Tokenizer.Builder;
import org.atilika.kuromoji.Tokenizer.Mode;


public class searchwords {

	/**
	 * code for PNAS Brief Paper on COVID-19 infodemic and its impacts on mobility compliance 
	 * タスク：コロナに関する検索で最も多く検索されてる単語を抽出
	 * 2020.11.21
	 * @throws IOException 
	 * 
	 */

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	//	public static void main(String[] args) throws IOException, ParseException {
	//
	//		// parameters 
	//		String datadir = "/mnt/home1/q_emotion/q_sum/";
	//		String root = "/mnt/tyabe/"; File root_f = new File(root); root_f.mkdir();
	//		String home = root+"infodemic/"; File home_f = new File(home); home_f.mkdir();
	//		String resdir  = home+"search_bydays/"; File resdir_f = new File(resdir); resdir_f.mkdir();
	//		String startdate = "20200101";
	//		String enddate   = "20200915";
	//
	//		// date,rank(1-100),word,#num
	//		File out = new File(resdir+"top100search.tsv");
	//
	//		Builder builder = Tokenizer.builder();
	//		builder.mode(Mode.SEARCH);
	//		Tokenizer search = builder.build();
	//
	//		Date start_date_date = DATE.parse(startdate);
	//		Date end_date_date   = DATE.parse(enddate);
	//		Date date = start_date_date;
	//		while((date.before(end_date_date))||(date.equals(end_date_date))){
	//			String date_str = DATE.format(date);
	//			Date next_date = getresidents.nextday_date(date);
	//			System.out.println("==== starting "+date_str);
	//
	//			// get ids that were in Tokyo on this day
	//			File querydata = new File(datadir+date_str+"_japan.tsv");
	//			if(querydata.exists()) {
	//				getTopSearch(querydata, search, date_str, out);
	//			}
	//			date = next_date;
	//		}
	//	}

	//	 testing 形態素解析
	public static void main(String[] args) throws IOException, ParseException {
		Builder builder = Tokenizer.builder();
		builder.mode(Mode.SEARCH);
		Tokenizer search = builder.build();
		ArrayList<String> res = getmorph("中国", search);
		System.out.println(res);
	}

	// get morphemes of search 
	// Kuromoji: http://www.mwsoft.jp/programming/lucene/kuromoji.html
	public static ArrayList<String> getmorph(
			String query, 
			Tokenizer tokenizer
			){
		List<Token> tokens = tokenizer.tokenize(query);
		ArrayList<String> words = new ArrayList<String>();
		for(Token tok : tokens) {
			System.out.println("allFeaturesArray : " + Arrays.asList(tok.getAllFeaturesArray()));
			String hinshi = tok.getAllFeaturesArray()[0];
			String type   = tok.getAllFeaturesArray()[1];
			String tango  = tok.getAllFeaturesArray()[6];
			if((hinshi.equals("名詞")) || (hinshi.equals("動詞"))) {
				if(!type.equals("接尾")) {
					words.add(tango);
				}
				else {
					if(words.isEmpty()) {
						words.add(tango);
					}
					else{
						String before = words.get(words.size()-1);
						String after = before + tango;
						words.remove(words.size()-1);
						words.add(after);
					}}}}
		return words;
	}


	// get search count data for this day
	public static void getTopSearch(
			File in, 
			Tokenizer tokenizer,
			String yyyymmdd,
			File out
			) throws IOException{
		HashMap<String, Integer> word_count = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in),"UTF-8"));
		String line = null;
		while((line=br.readLine())!=null) {
			String[] words  = line.split("\u0001");
			if((words.length>=5)&&(line.substring(0,4).equals("2020"))) {
				String query    = words[3];
				if((query.contains("コロナ"))||(query.contains("corona"))||(query.contains("covid"))) {
					// 分かち書き＋集計
					ArrayList<String> res = getmorph(query, tokenizer);
					for(String r : res) {
						if(word_count.containsKey(r)) {
							Integer newcount = word_count.get(r)+1;
							word_count.put(r, newcount);
						}
						else {
							word_count.put(r, 1);
						}
					}
				}
			}
			else {
				System.out.println("-- skipped error line: "+String.valueOf(words.length)+
						"; "+line.substring(0,4)+"; "+line);
			}
		}
		br.close();

		// write out to file
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out,true),"UTF-8"));
		// word_count を頻度で並べ替え
		List<Map.Entry<String, Integer>> sorted_map = word_count.entrySet().stream()
				.sorted(reverseOrder(Map.Entry.comparingByValue()))
				.collect(Collectors.toList());

		int rank = 0;
		for(Map.Entry<String, Integer> w: sorted_map) {
			if(rank<=5000) {
				bw.write(yyyymmdd+"\t"+String.valueOf(rank)+"\t"+w.getKey()+"\t"+String.valueOf(w.getValue()));
				bw.newLine();
			}
			rank+=1;
		}
		bw.close();
	}

}


