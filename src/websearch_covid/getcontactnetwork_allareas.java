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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.Mesh;
import parameters.cityinfo;

public class getcontactnetwork_allareas{

	protected static final SimpleDateFormat DATE = new SimpleDateFormat("yyyyMMdd");//change time format
	protected static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	public static void main(String[] args) throws ParseException, IOException {

		//		String gpspath = "/mnt/log/covid/loc/";
		String userpath = "/mnt/log/covid/user/";

		/**
		 * @PARAMETERS
		 * - meshsize 
		 */
		String meshsize = "6";
		String city = "tokyo";

		//		String outputversion = "v1";

		// spatial parameters
		ArrayList<LonLat> lonlats = cityinfo.getcityinfo(city);
		LonLat minp = lonlats.get(0);
		LonLat maxp = lonlats.get(1);

		Double score_thres = Double.parseDouble(args[0]);
		String startdate = args[1];
		String enddate   = args[2];
		//		String startdate = year+"0201";
		//		String enddate   = "20200716";

		Integer gobackdays = Integer.valueOf(args[3]);

		Integer agg_meshsize = Integer.valueOf(args[4]);

		String skip = "no";
		if(args.length==6) {
			skip = args[5];
		}

		String citylabel = city;
		/// write into my directory
		//		String rootpath = "/home/t-tyabe/COVID_websearch/"; 
		String rootpath = "/mnt/COVID_websearch_tyabe/"; 
		File rootfile = new File(rootpath); if(!rootfile.exists()) { rootfile.mkdir(); }
		String thispath = rootpath+citylabel+"/"; 
		File thisfile = new File(thispath); if(!thisfile.exists()) { thisfile.mkdir(); }

		File meshlistf = new File(thispath+"meshlist_1009.csv");
		HashSet<String> meshlist = getmeshlist(meshlistf);

		String idpath   = thispath+"idhomes/"; File idfilepath = new File(idpath); if(!idfilepath.exists()) { idfilepath.mkdir(); }
		String respath = thispath+"contactnetwork_allareas_1011/"; File resdir = new File(respath); resdir.mkdir();
		String respath_fordays = thispath+"bydays/"; File resdir2 = new File(respath_fordays); resdir2.mkdir();

		File SCI_target = new File(respath+"SCI_K"+String.valueOf((int)Math.round(score_thres))+
				"_D"+String.valueOf(gobackdays)+"_M"+String.valueOf(agg_meshsize)+"_target_allareas.csv");
		File SCI_dummy  = new File(respath+"SCI_K"+String.valueOf((int)Math.round(score_thres))+
				"_D"+String.valueOf(gobackdays)+"_M"+String.valueOf(agg_meshsize)+"_dummy_allareas.csv");
		File web_count  = new File(respath+"websearch_K"+String.valueOf((int)Math.round(score_thres))+
				"_D"+String.valueOf(gobackdays)+"_M"+String.valueOf(agg_meshsize)+"_allareas.csv");
		File ID_count  = new File(respath+"totalusers_K"+String.valueOf((int)Math.round(score_thres))+
				"_D"+String.valueOf(gobackdays)+"_M"+String.valueOf(agg_meshsize)+"_allareas.csv");

		if(skip.equals("skip")) {
			System.out.println("===== Skipped the entire process and going to WIDE function");
		}
		else {
			Date start_date_date = DATE.parse(startdate);
			Date end_date_date   = DATE.parse(enddate);
			Date date = start_date_date;
			while((date.before(end_date_date))||(date.equals(end_date_date))){

				String date_str = DATE.format(date);
				Date next_date = getresidents.nextday_date(date);

				System.out.println("======== starting "+date_str);

				// get ids that were in Tokyo on this day
				File idlist = new File(idpath+"ids_"+date_str+".csv");
				HashMap<String, LonLat> finalids = new HashMap<String, LonLat>();

				if(!idlist.exists()) {
					System.out.println("##### id-homes didnt exist for "+date_str+"; RUN CONTACT NETWORK BEFORE THIS!!!");
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
						System.out.println("##### mid-file didnt exist for "+date_str+"; RUN CONTACT NETWORK BEFORE THIS!!!\"");
					}

					HashSet<String> COVIDusers = contactindex.getCOVIDids_hardthreshold_multi(userpath, date_str, gobackdays, score_thres);
					System.out.println("-- COVID users: "+String.valueOf(COVIDusers.size()));
					System.out.println("cross-checked users: "+String.valueOf(contactindex.crosscheck(COVIDusers, targetids_mesh)));

					// 1. weighted SCI
					// get indexes for all meshes 
					//					contactindex.time_aggr_allmesh(date_str,targetids_mesh,COVIDusers,dt_mesh_ids,
					//							SCI_target,SCI_dummy,web_count,ID_count,maxp,minp,agg_meshsize);
					// get indexes in small meshes for fewer selected meshes
					contactindex.time_aggr_allmesh_limitmesh(date_str,targetids_mesh,COVIDusers,dt_mesh_ids, meshlist,
							SCI_target,SCI_dummy,web_count,ID_count,maxp,minp,agg_meshsize);

					System.out.println("-- finished writing out aggregated measures: target");
				}
				else {
					System.out.println("**** Skipped "+date_str+" because # of IDs = 0 ****");
				}

				System.out.println("======== done "+date_str);
				date = next_date;
			}
			//	}
		}

		Integer meshdigits = meshlevel2digits(agg_meshsize);

		//	public static void main(String[] args) throws IOException {
		File out0 = new File(SCI_target.getAbsolutePath().split(".csv")[0]+"_wide.csv");
		reshapefile(SCI_target,out0,meshdigits);

		File out = new File(SCI_dummy.getAbsolutePath().split(".csv")[0]+"_wide.csv");
		reshapefile(SCI_dummy,out,meshdigits);

		File out2 = new File(web_count.getAbsolutePath().split(".csv")[0]+"_wide.csv");
		reshapefile(web_count,out2,meshdigits);

		File out3 = new File(ID_count.getAbsolutePath().split(".csv")[0]+"_wide.csv");
		reshapefile(ID_count,out3,meshdigits);

	}

	public static Integer meshlevel2digits(Integer meshlevel) {
		Integer digits = 8;
		if(meshlevel==6) {
			digits = 11;
		}
		else if(meshlevel==5) {
			digits = 10;
		}
		else if(meshlevel==4) {
			digits = 9;
		}
		else if(meshlevel==3) {
			digits = 8;
		}
		else if(meshlevel==2) {
			digits = 6;
		}
		else if(meshlevel==1) {
			digits = 4;
		}
		return digits; 
	}


	public static void sum_widefile(File in, File out) throws IOException {
		TreeMap<String, Integer> date_data = new TreeMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		String date_hours = "";
		while((line=br.readLine())!= null) {
			String[] tokens = line.split(",");
			String meshcode = tokens[0];
			if(meshcode.equals("meshcode")) { // line of date and times
				date_hours = line.split("meshcode,")[1]; // update line of 
			}
			else {
				for(int i=1; i<line.split(",").length; i++) {
					String lab = date_hours.split(",")[i-1];
					if(date_data.containsKey(lab)) {
						Integer sum = date_data.get(lab)+ Integer.valueOf(line.split(",")[i]);
						date_data.put(lab, sum);
					}
					else {
						date_data.put(lab, Integer.valueOf(line.split(",")[i]));
					}
				}
			}
		}
		br.close();
		System.out.println("done putting into map");

		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		for(String d : date_data.keySet()) {
			bw.write(d+","+date_data.get(d));
		}
		bw.close();
	}




	public static void reshapefile(File in, File out,Integer meshdigits) throws IOException {
		HashMap<String, TreeMap<String, String>> mesh_date_data = new HashMap<String, TreeMap<String, String>>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		String date_hours = "";
		while((line=br.readLine())!= null) {
			String[] tokens = line.split(",");
			String meshcode = tokens[0];
			if(meshcode.equals("meshcode")) { // line of date and times
				date_hours = line.split("meshcode,")[1]; // update line of 
			}
			else {
				//				System.out.println(line);
				String dateline = line.substring(meshdigits+1,line.length());
				if(mesh_date_data.containsKey(meshcode)) {
					mesh_date_data.get(meshcode).put(date_hours, dateline);
				}
				else {
					TreeMap<String, String> tmp = new TreeMap<String, String>();
					tmp.put(date_hours, dateline);
					mesh_date_data.put(meshcode, tmp);
				}
			}
		}
		br.close();
		System.out.println("done putting into map");

		String samplemeshcode = "";
		for(String mesh : mesh_date_data.keySet()) {
			samplemeshcode = mesh;
			break;
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		// write out date time line
		TreeMap<String, String> one = mesh_date_data.get(samplemeshcode);
		bw.write("meshcode");
		Set set2 = one.entrySet();
		Iterator j = set2.iterator();
		// Display elements
		while(j.hasNext()) {
			Map.Entry me = (Map.Entry)j.next();
			bw.write(","+me.getKey());
		}
		bw.newLine();

		// write out mesh, population data 
		for(String mesh : mesh_date_data.keySet()) {
			bw.write(mesh);
			Set set = mesh_date_data.get(mesh).entrySet();
			Iterator i = set.iterator();
			// Display elements
			while(i.hasNext()) {
				Map.Entry me = (Map.Entry)i.next();
				//		    	System.out.println(me.getKey());
				bw.write(","+me.getValue());
			}
			bw.newLine();
		}
		bw.close();
	}


	public static HashMap<String, HashSet<String>> gethotspotmesh(File in) throws IOException{
		HashMap<String, HashSet<String>> area_meshes = new HashMap<String, HashSet<String>>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!= null) {
			String[] tokens = line.split("\t");
			String area = tokens[0];
			String meshes = tokens[1];
			if(meshes.contains("AND")) {
				HashSet<String> meshlist = new HashSet<String>();
				for(String m : meshes.split("AND")) {
					meshlist.add(m);
				}
				area_meshes.put(area, meshlist);
			}
			else {
				HashSet<String> meshlist = new HashSet<String>();
				meshlist.add(meshes);
				area_meshes.put(area, meshlist);
			}
		}
		br.close();
		return area_meshes;
	}


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

	public static HashSet<String> getmeshlist(File in) throws IOException{
		HashSet<String> meshlist = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(in));
		String line = null;
		while((line=br.readLine())!= null) {
			meshlist.add(line);
		}
		br.close();
		return meshlist;
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

	//	public static void individual_aggr( //個々人で再集計する
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
