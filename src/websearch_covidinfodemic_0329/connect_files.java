package websearch_covidinfodemic_0329;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import jp.ac.ut.csis.pflow.geom.Mesh;

public class connect_files {

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format

	public static HashMap<String, String> dummyIDs(
			File in, 
			File out
			) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		HashSet<String> ids = new HashSet<String>();
		while((line=br.readLine())!=null) {
			String id = line.split("\t")[0];
			ids.add(id);
		}
		br.close();

		HashMap<String, String> id_newid = new HashMap<String, String>();
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		Integer num = 1;
		for(String id : ids) {
			bw.write(id+","+String.valueOf(num));
			id_newid.put(id, String.valueOf(num));
			bw.newLine();
			num+=1;
		}
		bw.close();

		return id_newid;
	}


	public static HashMap<String, String> intomeshcode(File in, File out) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(in));
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		HashMap<String, String> id_meshcode = new HashMap<String, String>();
		String line = null;
		while((line=br.readLine())!=null) {
			String[] tokens = line.split(",");
			String id = tokens[0];
			Double lon = Double.parseDouble(tokens[1]);
			Double lat = Double.parseDouble(tokens[2]);
			String meshcode = new Mesh(6, lon, lat).getCode();
			bw.write(id+","+meshcode);
			bw.newLine();
			id_meshcode.put(id, meshcode);
		}
		br.close();
		bw.close();
		return id_meshcode;
	}


	public static HashMap<String, Integer> ID_misinfoscore(
			File in, 
			File out
			) throws IOException {

		HashSet<String> words = new HashSet<String>();
		words.add("???");words.add("??????");words.add("??????");words.add("??????");words.add("??????");
		words.add("????????????");words.add("??????");words.add("???");

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(in),"UTF-8"));
		String line = null;
		HashMap<String, Integer> id_score = new HashMap<String, Integer>();
		while((line=br.readLine())!=null) {
			String tokens[] = line.split("\t");
			String id = tokens[0];
			String word = tokens[1];
			if(words.contains(word)) {
				Integer count = Integer.valueOf(tokens[2]);
				if(id_score.containsKey(id)) {
					Integer newcount = count+id_score.get(id);
					id_score.put(id, newcount);
				}
				else {
					id_score.put(id, count);
				}
			}
		}
		br.close();

		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		for(String id : id_score.keySet()) {
			bw.write(id+","+String.valueOf(id_score.get(id)));
			bw.newLine();
		}
		bw.close();

		return id_score;
	}


	public static HashMap<String, String> getmetrics(
			String metric, // choose "rg","ttd","disp","sahr"
			String startdate, 
			String enddate,
			String resdir
			) throws ParseException, IOException {

		HashMap<String, HashMap<String, Double>> id_date_value   = new HashMap<String, HashMap<String, Double>>();

		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			Date next_date = utils.nextday_date(date);
			File id_metrics = new File(resdir+date_str+"_metrics.csv");
			metricintomap(id_metrics, date_str, id_date_value, metric);
			System.out.println("--- DONE metrics for "+date_str);
			date = next_date;
		}

		HashMap<String, String> id_metricline = new HashMap<String, String>();
		for(String id : id_date_value.keySet()) {
			String rg   = computedaily(id, id_date_value, startdate, enddate);
			id_metricline.put(id, rg);
		}

		return id_metricline;
	}

	public static String computebefaft(
			String id, 
			HashMap<String, HashMap<String, Double>> in
			) throws ParseException {
		String res = "0,0";
		if(in.containsKey(id)) {
			Double beftmp = 0d;
			Integer beftmpcount = 0;
			Double afttmp = 0d;
			Integer afttmpcount = 0;
			HashMap<String, Double> thisid = in.get(id);
			for(String datestr : thisid.keySet()) {
				Date date = DATE.parse(datestr);
				if(date.before(DATE.parse("20200201"))) {
					beftmp+=thisid.get(datestr);
					beftmpcount+=1;
				}
				else {
					afttmp+=thisid.get(datestr);
					afttmpcount+=1;	
				}
			}
			Double befres = 0d;
			Double aftres = 0d;
			if(beftmpcount>0) {
				befres = beftmp/(double)beftmpcount;
			}			
			if(afttmpcount>0) {
				aftres = afttmp/(double)afttmpcount;
			}
			res = String.valueOf(befres)+","+String.valueOf(aftres);
		}
		return res;
	}

	// TODO
	public static String computedaily(
			String id, 
			HashMap<String, HashMap<String, Double>> in,
			String startdate,
			String enddate
			) throws ParseException {
		HashMap<String, Double> thisid = in.get(id);
		String res = "";
		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			String val = "null";
			if(thisid.containsKey(date_str)) {
				Double value = ((double)Math.round(thisid.get(date_str) * 10))/10;
				val = String.valueOf(value);
			}
			res = res+","+val;
			Date next_date = utils.nextday_date(date);
			date = next_date;
		}
		res = res.substring(1);
		return res;
	}


	public static void metricintomap(
			File in,
			String date_str, 
			HashMap<String, HashMap<String, Double>> id_date_value,
			String val
			) throws IOException {
		Integer ind = 3; // when "rg"
		if(val.equals("ttd")) {
			ind = 4;
		}
		else if(val.equals("disp")) {
			ind = 5;
		}
		else if(val.equals("sahr")) {
			ind = 6; 
		}
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!=null) {
			String[] tokens = line.split(",");
			String id = tokens[0];
			Double value = Double.parseDouble(tokens[ind]);
			if(id_date_value.containsKey(id)) {
				id_date_value.get(id).put(date_str, value);
			}
			else {
				HashMap<String, Double> tmp = new HashMap<String, Double>();
				tmp.put(date_str, value);
				id_date_value.put(id, tmp);
			}
		}
		br.close();
	}


}
