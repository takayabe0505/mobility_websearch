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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import jp.ac.ut.csis.pflow.geom.LonLat;

public class getMobilityPatterns {

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
		String gpspath = "/mnt/log/covid/loc/";
		//		String datadir = "/mnt/home1/q_emotion/q_sum/";
		String root = "/mnt/tyabe/"; File root_f = new File(root); root_f.mkdir();
		String home = root+"infodemic/"; File home_f = new File(home); home_f.mkdir();
		String resdir  = home+"metrics_bydays/"; File resdir_f = new File(resdir); resdir_f.mkdir();
		String startdate = "20200101";
		String enddate   = "20200915";

		File idhome_f = new File(home+"id_homelocs.csv");
		HashMap<String, LonLat> idhome = getidhome(idhome_f);
		System.out.println("--- got id home "+String.valueOf(idhome.size()));

		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){
			String date_str = DATE.format(date);
			Date next_date = utils.nextday_date(date);
			File gps1 = new File(gpspath+date_str+".tsv");
			File out = new File(resdir+date_str+"_metrics.csv");
			if((gps1.exists()) && (gps1.length()>0) ) {
				HashMap<String, HashMap<String, LonLat>> id_datetime_ll = getlogs(gps1, idhome);
				System.out.println("--- got logs "+date_str);
				getRG_TTD_disp(id_datetime_ll, out, idhome);
				System.out.println("--- DONE metrics for "+date_str);
			}
			date = next_date;
		}
	}

	public static HashMap<String, LonLat> getidhome(File in) throws NumberFormatException, IOException{
		HashMap<String, LonLat> idhome = new HashMap<String, LonLat>();
		BufferedReader br1 = new BufferedReader(new FileReader(in));
		String line1 = null;
		while((line1=br1.readLine())!=null){
			String[] tokens = line1.split(",");
			String id = tokens[0];
			LonLat p = new LonLat(Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
			idhome.put(id, p);
		}
		br1.close();
		return idhome;
	}


	public static HashMap<String, HashMap<String, LonLat>> getlogs(
			File in,
			HashMap<String, LonLat> ids_inside
			) throws NumberFormatException, IOException, ParseException{
		HashMap<String, HashMap<String, LonLat>> id_datetime_ll = new HashMap<String, HashMap<String, LonLat>>();
		BufferedReader br1 = new BufferedReader(new FileReader(in));
		String line1 = null;
		while((line1=br1.readLine())!=null){
			try {
				String[] tokens = line1.split("\t");
				if(tokens.length>=7){
					String id_br1 = tokens[0];
					if(!id_br1.equals("null")){ if(id_br1.length()>0){
						if(tokens[4].length()>=10){
							Double lon = Double.parseDouble(tokens[3]);
							Double lat = Double.parseDouble(tokens[2]);
							LonLat p = new LonLat(lon,lat);
							if(ids_inside.containsKey(id_br1)){ // inside ashikiri IDs?
								String unixtime = tokens[4];
								Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
								DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
								String datetime = DATETIME.format(currentDate);
								if(id_datetime_ll.containsKey(id_br1)) {
									id_datetime_ll.get(id_br1).put(datetime, p);
								}
								else {
									HashMap<String, LonLat> tmp = new HashMap<String, LonLat>();
									tmp.put(datetime, p);
									id_datetime_ll.put(id_br1, tmp);
								}}}}}}
			}
			catch (ArrayIndexOutOfBoundsException  e){
				System.out.println("OUT OF BOUNDS EXCEPTION ----");
				System.out.println(line1);
				System.out.println("----");
			}
			catch (Exception  e){
				System.out.println("OTHER ERROR IN LINE ----");
				System.out.println(line1);
				System.out.println("----");				
			}
		}
		br1.close();
		return id_datetime_ll;
	}


	public static void getRG_TTD_disp(
			HashMap<String, HashMap<String, LonLat>> id_date_ll,
			File out,
			HashMap<String, LonLat> idhome
			) throws IOException, ParseException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(out,true));
		Integer count = 0;
		for(String id : id_date_ll.keySet()) {
			HashMap<String, LonLat> date_dt_ll = id_date_ll.get(id);
			LonLat home = idhome.get(id);
			Double rg = rg(date_dt_ll);
			Double ttd = ttd(date_dt_ll);
			String disp = distfromhome(date_dt_ll, home);
			long outofhometime = outofhometime(date_dt_ll, home);
			bw.write(id+","+
					String.valueOf(home.getLon())+","+
					String.valueOf(home.getLat())+","+
					String.valueOf(rg)+","+
					String.valueOf(ttd)+","+
					String.valueOf(disp)+","+
					String.valueOf(outofhometime));
			bw.newLine();
			if((count<3)||(count%10000==0)) {
				System.out.println(String.valueOf(count)+","+id+","+
						String.valueOf(home.getLon())+","+
						String.valueOf(home.getLat())+","+
						String.valueOf(rg)+","+
						String.valueOf(ttd)+","+
						String.valueOf(disp)+","+
						String.valueOf(outofhometime));
			}
			count+=1;
		}
		bw.close();
	}

	public static Double ttd(HashMap<String, LonLat> data_thisid) throws ParseException {
		Double totaldistance = 0d;
		LonLat beforep = new LonLat(0d,0d);

		// in temporal order! 
		TreeMap<Date, LonLat> newentries = new TreeMap<Date, LonLat>();
		for(String date_str : data_thisid.keySet()) {
			Date date = DATETIME.parse(date_str);
			newentries.put(date, data_thisid.get(date_str));
		}

		Iterator<Entry<Date, LonLat>> entries = newentries.entrySet().iterator();
		while(entries.hasNext()) {
			Map.Entry entry = (Map.Entry)entries.next();
			LonLat p = (LonLat) entry.getValue();
			if(beforep.getLon()!=0d) {
				Double movement = p.distance(beforep)/1000d;
				totaldistance = totaldistance + movement;
				beforep = p;
			}
			else {
				beforep = p;
			}
		}
		return totaldistance;
	}

	public static long outofhometime(
			HashMap<String, LonLat> data_thisid, 
			LonLat home
			) throws ParseException {
		// in temporal order! 
		TreeMap<Date, LonLat> newentries = new TreeMap<Date, LonLat>();
		for(String date_str : data_thisid.keySet()) {
			Date date = DATETIME.parse(date_str);
			newentries.put(date, data_thisid.get(date_str));
		}

		long totaldistance = 0;
		LonLat beforep = new LonLat(0d,0d);
		Date beforetime = null;
		String isout = "no";

		Iterator<Entry<Date, LonLat>> entries = newentries.entrySet().iterator();
		while(entries.hasNext()) {
			Map.Entry entry = (Map.Entry)entries.next();
			Date now = (Date) entry.getKey();
			LonLat p = (LonLat) entry.getValue();
			if(beforep.getLon()!=0d) {
				if(p.distance(home)>200d) {
					if(isout.equals("yes")) {
						long diff = now.getTime() - beforetime.getTime();
						long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
						totaldistance = totaldistance + diffMinutes;
					}
					isout = "yes";
				}
				else {
					if(isout.equals("yes")) {
						long diff = now.getTime() - beforetime.getTime();
						long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
						totaldistance = totaldistance + diffMinutes;
					}
					isout = "no";
				}
			}
			beforep = p;
			beforetime = now;
		}
		return totaldistance;
	}


	public static String distfromhome(
			HashMap<String, LonLat> data_thisid, 
			LonLat home
			) {
		Double mindist = 10000000d;
		Double tmpdist = 0d;
		LonLat mindistloc = new LonLat(0d,0d);
		for(String d : data_thisid.keySet()) {
			//			Integer hour = Integer.valueOf(TIME.format(d).split(":")[0]);
			LonLat p = data_thisid.get(d);
			Double distance = p.distance(home)/1000d; // in km
			if(mindist>distance) {
				mindist = distance;
				mindistloc = p;
			}
			tmpdist = tmpdist + distance;
		}
		Double avgdist = 10000000d;
		if(data_thisid.size()>0) {
			avgdist = tmpdist/(double)data_thisid.size();
		}
		String result = String.valueOf(mindist)+","+String.valueOf(avgdist)+","+
				String.valueOf(mindistloc.getLon()+","+String.valueOf(mindistloc.getLat()));
		return result;
	}

	public static Double rg(HashMap<String, LonLat> data_thisid) {
		LonLat avg = getavg(data_thisid);
		Double tmp = 0d;
		for(String d : data_thisid.keySet()) {
			LonLat p = data_thisid.get(d);
			Double distance = p.distance(avg)/1000d; // in km
			tmp = tmp + Math.pow(distance,2d);
		}
		Double rg = Math.sqrt(tmp/(double)data_thisid.size());
		return rg;
	}

	public static LonLat getavg(HashMap<String, LonLat> data_thisid) {
		Double tmplon = 0d;
		Double tmplat = 0d;
		for(String d : data_thisid.keySet()) {
			LonLat p = data_thisid.get(d);
			tmplon = tmplon + p.getLon();
			tmplat = tmplat + p.getLat();
		}
		Double lon = tmplon / (double)data_thisid.size(); 
		Double lat = tmplat / (double)data_thisid.size(); 
		LonLat avg = new LonLat(lon,lat);
		return avg;
	}

	public static HashSet<String> getFakeIDs(
			String resdir, 
			String startdate, 
			String enddate
			) throws ParseException, IOException {
		HashSet<String> idss = new HashSet<String>();

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
								idss.add(id);
							}
						}
					}
				}
				br.close();
			}
			date = next_date;
		}
		return idss;
	}


}
