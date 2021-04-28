package websearch_covidinfodemic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class Mobilitymetrics {

	/**
	 * code for PNAS Brief Paper on COVID-19 infodemic and its impacts on mobility compliance 
	 * ã‚¿ã‚¹ã‚¯ï¼šinfodemicå¯¾è±¡è€… / ã‚³ãƒ­ãƒŠã�«é–¢ã�—ã�¦æ¤œç´¢ã�—ã�Ÿäºº / ã��ã‚Œä»¥å¤–ã�®äººã�§åˆ†ã�‘ã‚‹
	 * 		human mobility index (TTD, Rg, SAH time, SCI) ã�Œã€�ã‚³ãƒ­ãƒŠå‰�ã�«æ¯”ã�¹ã�¦ã�©ã‚Œã��ã‚‰ã�„æ¸›ã�£ã�Ÿã�‹ã‚’è¦‹ã‚‹
	 * 2020.11.25
	 * @Taka 
	 */

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat TIME     = new SimpleDateFormat("HH:mm:ss");
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	public static void main(String[] args) throws IOException, ParseException {

		// parameters 
		//		String gpspath = "/mnt/log/covid/loc/";
		//		String datadir = "/mnt/home1/q_emotion/q_sum/";
		String root = "/mnt/tyabe/"; File root_f = new File(root); root_f.mkdir();
		String home = root+"infodemic/"; File home_f = new File(home); home_f.mkdir();
		String searchdir  = home+"search_bydays/";
		String resdir  = home+"metrics_bydays/"; File resdir_f = new File(resdir); resdir_f.mkdir();
		String startdate = "20200101";
		String enddate   = "20200915";

		HashMap<String, HashSet<String>> fakeid_dates = getFakeIDs(searchdir, startdate, enddate);
		System.out.println("--- DONE getting IDs");

		HashSet<String> days = new HashSet<String>();
		days.add("20200407");days.add("20200505");days.add("20200526");
		
		for(String date_str : days) {
			File out = new File(resdir+date_str+"_metrics.csv");
			File out_anony = new File(resdir+date_str+"_metrics_anony.csv");
			if((out.exists()) && (out.length()>0) ) {
				BufferedReader br = new BufferedReader(new FileReader(out));
				BufferedWriter bw = new BufferedWriter(new FileWriter(out_anony));
				String line1 = null;
				while((line1=br.readLine())!=null){
					String[] tokens = line1.split(",");
					String id = tokens[0];
					Integer count = 0;
					if(fakeid_dates.containsKey(id)) {
						count = fakeid_dates.get(id).size();
					}
					bw.write(String.valueOf(count)+","+tokens[3]+","+tokens[4]+","+
							tokens[5]+","+tokens[6]+","+tokens[9]);
					bw.newLine();
				}
				System.out.println("--- DONE metrics for "+date_str);
				br.close();
				bw.close();
			}
		}
	}

	public static HashMap<String, HashSet<String>> getFakeIDs(
			String resdir, 
			String startdate, 
			String enddate
			) throws ParseException, IOException {
		HashMap<String, HashSet<String>> id_dates = new HashMap<String, HashSet<String>>();

		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			Date next_date = utils.nextday_date(date);
			System.out.println("==== starting "+date_str);

			File searchids = new File(resdir+date_str+"_falsesearch.tsv");
			if(searchids.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(searchids));
				String line = null;
				while((line=br.readLine())!=null) {
					String[] tokens = line.split("\t");
					String word = tokens[0];
					if(!word.equals("dummy")) {
						String num = tokens[2];
						if(!num.equals("0")) {
							String ids = tokens[3];
							ids = ids.substring(0,ids.length()-1);
							for(String id : ids.split(",")) {
								if(id_dates.containsKey(id)) {
									id_dates.get(id).add(date_str);
								}
								else {
									HashSet<String> dates = new HashSet<String>();
									dates.add(date_str);
									id_dates.put(id, dates);
								}
							}
						}
					}
				}
				br.close();
			}
			date = next_date;
		}
		return id_dates;
	}

}
