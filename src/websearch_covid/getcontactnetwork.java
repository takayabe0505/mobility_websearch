package websearch_covid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.Mesh;
import parameters.cityinfo;

public class getcontactnetwork {

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	public static void main(String[] args) throws ParseException, IOException {

		String gpspath = "/mnt/log/covid/loc/";
		String userpath = "/mnt/log/covid/user/";

		/**
		 * @PARAMETERS
		 * - meshsize 
		 */
		String meshsize = "6";
		String city = "tokyo";
		
		String outputversion = "v3";

		System.out.println("======== Starting "+city+" ========");

		// spatial parameters
		ArrayList<LonLat> lonlats = cityinfo.getcityinfo(city);
		LonLat minp = lonlats.get(0);
		LonLat maxp = lonlats.get(1);

		//		String year = "2020";
		String startdate = args[1];
		String enddate   = args[2];
		//		String startdate = year+"0201";
		//		String enddate   = "20200716";

		String citylabel = city;
		/// write into my directory
		//		String rootpath = "/home/t-tyabe/COVID_websearch/"; 
		String rootpath = "/mnt/COVID_websearch_tyabe/"; 
		File rootfile = new File(rootpath); if(!rootfile.exists()) { rootfile.mkdir(); }
		String thispath = rootpath+citylabel+"/"; 
		File thisfile = new File(thispath); if(!thisfile.exists()) { thisfile.mkdir(); }

		String idpath   = thispath+"idhomes/"; File idfilepath = new File(idpath); if(!idfilepath.exists()) { idfilepath.mkdir(); }
		String respath = thispath+"contactnetwork/"; File resdir = new File(respath); resdir.mkdir();
		String respath_fordays = thispath+"bydays/"; File resdir2 = new File(respath_fordays); resdir2.mkdir();

		Double score_thres = Double.parseDouble(args[0]);

		File SCI_target = new File(respath+"SCI_"+String.valueOf((int)Math.round(score_thres))+"_target_"+outputversion+".csv");
		File SCI_dummy  = new File(respath+"SCI_"+String.valueOf((int)Math.round(score_thres))+"_dummy_"+outputversion+".csv");
		File web_count  = new File(respath+"websearch_"+String.valueOf((int)Math.round(score_thres))+"_score_"+outputversion+".csv");
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(web_count));

		Date start_date_date = DATE.parse(startdate);
		Date end_date_date   = DATE.parse(enddate);
		Date date = start_date_date;
		while((date.before(end_date_date))||(date.equals(end_date_date))){

			String date_str = DATE.format(date);
			Date next_date = getresidents.nextday_date(date);

			System.out.println("======== "+city+" starting "+date_str);

			// get ids that were in Tokyo on this day
			File idlist = new File(idpath+"ids_"+date_str+".csv");
			HashMap<String, LonLat> finalids = new HashMap<String, LonLat>();

			if(!idlist.exists()) {
				System.out.println("-- making IDs because id-homes didnt exist... "+date_str);
				HashSet<String> ashikiriIDs = getresidents.ashikiriIDs(minp,maxp,gpspath,respath,date); 
				System.out.println(" ... ashikiri: "+String.valueOf(ashikiriIDs.size())+" "+date_str);
				// get night time logs of above IDs on this day
				HashMap<String, HashMap<String, LonLat>> id_datetime_ll = getresidents.getnightlogsofashikiriIDs(
						date, 
						gpspath, 
						ashikiriIDs
						);
				finalids = getresidents.gethomelocs(id_datetime_ll, maxp, minp, idlist);
			}
			else {
				System.out.println("-- id-homes available ... "+date_str);
				finalids = getidhomes(idlist);
			}
			HashMap<String, String> targetids_mesh = gettargetids(idlist, meshsize);
			System.out.println("-- got target ids-mesh: "+String.valueOf(targetids_mesh.size()));
			System.out.println("-- got target ids: "+String.valueOf(finalids.size()));

			if(targetids_mesh.size()>0) {
				File tmpout = new File(respath_fordays+date_str+"time_mesh_number_mesh"+meshsize+"_nohome.csv");
				HashMap<String, HashMap<String, HashSet<String>>> dt_mesh_ids = new HashMap<String, HashMap<String, HashSet<String>>>();
				if(tmpout.exists()) {
					dt_mesh_ids = contactindex.getdata(tmpout);
					System.out.println("-- found spatio-temporal matrix!!");
				}
				else {
					// read data and store HashMap<time, HashMap<meshcode,ArrayList<020ids>> --> 250m mesh x 30 minutes
					dt_mesh_ids = gettargetlogs(gpspath, finalids, date_str, meshsize);
					System.out.println("-- got logs of target ids "+String.valueOf(dt_mesh_ids.size()));
					// read entire data and store ids into hashmap 
					getotherlogs(gpspath, dt_mesh_ids, finalids, date_str, meshsize);
					System.out.println("-- got logs of target ids (with others) "+String.valueOf(dt_mesh_ids.size()));
					writeout(dt_mesh_ids, tmpout);
					System.out.println("-- finished writing out spatio-temporal matrix");
				}
				
				File COVIDusers_file = new File(userpath+date_str+"_user.tsv");
				HashSet<String> COVIDusers = contactindex.getCOVIDids_hardthreshold(COVIDusers_file, score_thres);
				System.out.println("-- COVID users: "+String.valueOf(COVIDusers.size()));
				Integer crosscheckIDs = contactindex.crosscheck(COVIDusers, targetids_mesh);
				HashSet<String> dummyIDs = contactindex.getdummyIDs(targetids_mesh, crosscheckIDs);
				System.out.println("cross-checked users: "+String.valueOf(contactindex.crosscheck(COVIDusers, targetids_mesh))+
						", dummy: "+String.valueOf(dummyIDs.size()));

				bw2.write(date_str+","+String.valueOf(COVIDusers.size()));
				bw2.newLine();

				// 1. weighted SCI
				contactindex.time_aggr(date_str,targetids_mesh,COVIDusers,dt_mesh_ids,SCI_target);
				contactindex.time_aggr(date_str,targetids_mesh,dummyIDs,dt_mesh_ids,SCI_dummy);
				
				System.out.println("-- finished writing out aggregated measures: target");

			}
			else {
				System.out.println("**** Skipped "+date_str+" because # of IDs = 0 ****");
			}
			System.out.println("======== "+city+" done "+date_str);
			date = next_date;
		}
		bw2.close();
	}
	//	}

	public static HashMap<String, LonLat> getidhomes(File in) throws IOException{
		HashMap<String, LonLat> ids = new HashMap<String, LonLat>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!= null) {
			String[] tokens = line.split(",");
			String id = tokens[0];
			LonLat p = new LonLat(Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
			ids.put(id, p);
		}
		br.close();
		return ids;
	}


	public static HashMap<String, String> gettargetids(File in, String meshsize) throws IOException{
		HashMap<String, String> ids = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!= null) {
			String[] tokens = line.split(",");
			String id = tokens[0];
			String mesh = new Mesh(Integer.valueOf(meshsize), Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2])).getCode();
			ids.put(id, mesh);
		}
		br.close();
		return ids;
	}

	public static HashMap<String, HashMap<String, HashSet<String>>> gettargetlogs(
			String gpspath,
			HashMap<String, LonLat> ids_inside,
			String date_str, 
			String meshsize
			) throws NumberFormatException, IOException, ParseException{
		File in = new File(gpspath+date_str+".tsv");
		HashMap<String, HashMap<String, HashSet<String>>> dt_mesh_ids = new HashMap<String, HashMap<String, HashSet<String>>>();
		if(in.exists()) { if(in.length()>0) { 
			//			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(in));
			BufferedReader br1 = new BufferedReader(new FileReader(in));
			String line1 = null;
			while((line1=br1.readLine())!=null){
				try {
					String[] tokens = line1.split("\t");
					if(tokens.length>=7){
						String id_br1 = tokens[0];
						if(!id_br1.equals("null")){ if(id_br1.length()>0){
							if(ids_inside.containsKey(id_br1)){ // inside ashikiri IDs?
								String unixtime = tokens[4];
								Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
								DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
								String datetime = DATETIME.format(currentDate);
								Integer hour = Integer.valueOf(datetime.split(" ")[1].split(":")[0]);
								Integer mins = Integer.valueOf(datetime.split(" ")[1].split(":")[1]);
								Integer half = 0;
								if(mins>=30) {
									half = 1;
								}
								String timeslot = String.valueOf(hour*2+half);
								Double lon = Double.parseDouble(tokens[3]);
								Double lat = Double.parseDouble(tokens[2]);
								String mesh = new Mesh(Integer.valueOf(meshsize), lon, lat).getCode();
								if(dt_mesh_ids.containsKey(timeslot)) {
									if(dt_mesh_ids.get(timeslot).containsKey(mesh)) {
										dt_mesh_ids.get(timeslot).get(mesh).add(id_br1);
									}
									else{
										HashSet<String> tmp = new HashSet<String>();
										tmp.add(id_br1);
										dt_mesh_ids.get(timeslot).put(mesh, tmp);
									}
								}
								else {
									HashSet<String> tmp = new HashSet<String>();
									tmp.add(id_br1);
									HashMap<String, HashSet<String>> tmp2 = new HashMap<String, HashSet<String>>();
									tmp2.put(mesh, tmp);
									dt_mesh_ids.put(timeslot, tmp2);
								}}}}}
				}
				catch (ArrayIndexOutOfBoundsException  e){
					System.out.println("OUT OF BOUNDS EXCEPTION ----");
					System.out.println(line1);
					System.out.println("----");
				}
				catch (Exception  e){
					System.out.println("OTHER ERROR IN LINE ----");
					System.out.println(line1);
					System.out.println(e.getMessage());
					System.out.println("----");				
				}
			}
			br1.close();
			//			gzip.close();
		}}
		else {
			System.out.println("File doesnt exist "+date_str);
		}
		return dt_mesh_ids;
	}


	public static HashMap<String, HashMap<String, HashSet<String>>> getotherlogs(
			String gpspath,
			HashMap<String, HashMap<String, HashSet<String>>> dt_mesh_ids,
			HashMap<String, LonLat> targetids,
			String date_str, 
			String meshsize
			) throws NumberFormatException, IOException, ParseException{
		File in = new File(gpspath+date_str+".tsv");
		if(in.exists()) { if(in.length()>0) { 
			//			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(in));
			BufferedReader br1 = new BufferedReader(new FileReader(in));
			String line1 = null;
			while((line1=br1.readLine())!=null){
				try {
					String[] tokens = line1.split("\t");
					if(tokens.length>=7){
						String id_br1 = tokens[0];
						if(!id_br1.equals("null")){ if(id_br1.length()>0){
							if(!targetids.containsKey(id_br1)){ // inside ashikiri IDs?
								String unixtime = tokens[4];
								Date currentDate = new Date (Long.parseLong(unixtime)*((long)1000)); // UTC time
								DATETIME.setTimeZone(TimeZone.getTimeZone("GMT+9"));
								String datetime = DATETIME.format(currentDate);
								Integer hour = Integer.valueOf(datetime.split(" ")[1].split(":")[0]);
								Integer mins = Integer.valueOf(datetime.split(" ")[1].split(":")[1]);
								Integer half = 0;
								if(mins>=30) {
									half = 1;
								}
								String timeslot = String.valueOf(hour*2+half);
								if(dt_mesh_ids.containsKey(timeslot)) {
									Double lon = Double.parseDouble(tokens[3]);
									Double lat = Double.parseDouble(tokens[2]);
									String mesh = new Mesh(Integer.valueOf(meshsize), lon, lat).getCode();
									if(dt_mesh_ids.get(timeslot).containsKey(mesh)) {
										dt_mesh_ids.get(timeslot).get(mesh).add(id_br1);
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
					System.out.println(e.getMessage());
					System.out.println("----");				
				}
			}
			br1.close();
			//			gzip.close();
		}}
		else {
			System.out.println("File doesnt exist "+date_str);
		}
		return dt_mesh_ids;
	}


	public static void writeout(HashMap<String, HashMap<String, HashSet<String>>> data, File out) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		for(String datetime: data.keySet()) {
			for(String mesh: data.get(datetime).keySet()) {
				bw.write(datetime+"\t"+mesh+"\t");
				for(String id: data.get(datetime).get(mesh)) {
					bw.write(id+",");
				}
				bw.newLine();
			}
		}
		bw.close();
	}

	//	public static void individual_aggr( //???????????????????????????
	//			HashSet<String> targetids, 
	//			HashMap<String, HashMap<String, HashSet<String>>> data, 
	//			File out) {
	//		HashMap<String, HashMap<String, Integer>> id_time_count = new HashMap<String, HashMap<String, Integer>>();
	//		for(String targetid: targetids) {
	//			for(String ts : data.keySet()) {
	//				HashMap<String, HashSet<String>> tmp = data.get(ts);
	//				for(String mesh : tmp.keySet()) {
	//					if(tmp.get(mesh).contains(targetid)){
	//						Integer count = tmp.get(mesh).size();
	//						// input into result map
	//						if(id_time_count.containsKey(targetid)) {
	//							id_time_count.get(targetid).put(ts, count);
	//						}
	//						else {
	//							HashMap<String, Integer> time_count = new HashMap<String, Integer>();
	//							time_count.put(ts, count);
	//							id_time_count.put(targetid, time_count);
	//						}}}}}
	//		
	//	}

	public static String timeslot2time(String timeslot) {
		Integer ts = Integer.valueOf(timeslot);
		String hour = String.format("%02d", ts/2);
		String mins = "00";
		if(ts%2==1) {
			mins = "30";
		}
		String res = hour+":"+mins+":00";
		return res;
	}



}
