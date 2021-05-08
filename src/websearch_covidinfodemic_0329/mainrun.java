package websearch_covidinfodemic_0329;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;

public class mainrun {

	/**
	 * 1. collect user info from search data
	 * 1.1. how often did they search misinformation (what words? ), COVID search, and normal search? 
	 * [output] id - total counts of misinfo, covid search, total search
	 * 
	 * 2. compute mobility info from location data
	 * 2.1. estimate each user's home location --> get meshcode or something
	 * 2.2. ~~interpolation of mobility data~~
	 * 2.3. for each day, what were the users' TTD, RG, at home duration?
	 * 2.4. compute population density for each grid cell, 30 min intervals
	 * 2.5. compute social contact index for 30 minute intervals
	 * [output] id - homelocation meshcode - time series data of TTD, RG, at home duration, SCI 
	 * 
	 * 3. combine data sets by anonymizing IDs
	 * 3.1. make true ID - new ID number file 
	 * 3.2. replace output files with new ID numbers 
	 * 
	 * @throws IOException 
	 * @throws ParseException 
	 * 
	 */

	public static void main(String[] args) throws ParseException, IOException {

		//		String searchpath = "/mnt/home1/q_emotion/q_sum/";
		//		String gpspath    = "/mnt/log/covid/loc/";

		String root = "/mnt/tyabe/"; File root_f = new File(root); root_f.mkdir();
		String home = root+"infodemic_0329/"; File home_f = new File(home); home_f.mkdir();

		// parameters 
		String startdate = "20200101";
		String enddate   = "20200615";

		System.out.println("========= COLLECT DATA =========");

		// # 1 get search counts ----------------------------------------------
		File idwordcount = new File(home+"id_searchcounts.tsv");
		//		user_searchdata.get_searchdata(root,home,searchpath,startdate,enddate);
		//		System.out.println("done");

		// # 2.1 estimate home locations ----------------------------------------

		HashSet<String> IDs = user_homeloc.getIDs(idwordcount); // ~ 4,000,000 IDs in entire Japan
		System.out.println("Number of IDs: "+String.valueOf(IDs.size()));

		// parameters 
		//		String h_startdate = "20200106";
		//		String h_enddate   = "20200215";
		//		Integer chunks = Integer.valueOf(args[0]);

		// estimate home using MeanShift 
		File idhome_f = new File(home+"id_homelocs.csv");
		//		HashSet<String> doneIDs = new HashSet<String>();
		//		Integer beforecount = -1;
		//		Integer round = 1;
		//		while(doneIDs.size()>beforecount) {
		//			beforecount = doneIDs.size();
		//		user_homeloc.getHomes(IDs, h_startdate, h_enddate, gpspath, idhome_f, doneIDs, 4000000);
		//			System.out.println("Round "+String.valueOf(round)+"; finished "+String.valueOf(doneIDs.size())+
		//					" out of possible "+String.valueOf(IDs.size()));
		//			round+=1;
		//		}
		System.out.println("--- got home locations ---");

		// # 2.3 obtain individual mobility metrics --------------------------------
		//		HashMap<String, LonLat> idhome = user_mobilitymetric.getidhome(idhome_f);
		//		System.out.println("--- got id home "+String.valueOf(idhome.size()));
		//		String resdir  = home+"metrics_bydays/"; File resdir_f = new File(resdir); resdir_f.mkdir();
		//		user_mobilitymetric.runmetrics("20200508", enddate, gpspath, resdir, idhome);
		System.out.println("--- got mobility metrics!");


		// meshcode level 5 (250m), every 30 minute interval 
		//		Integer meshsize = 5;
		//		ArrayList<Integer> meshsizes = new ArrayList<Integer>();
		//		meshsizes.add(3); 
		//		meshsizes.add(4); 
		//		meshsizes.add(5); 
		//		meshsizes.add(6);

		//		for(Integer meshsize : meshsizes) {
		//			// # 2.4 get dynamic population distribution -------------------------------
		//			File dynamicpop = new File(home+"dynamic_meshpop_"+String.valueOf(meshsize)+".csv");
		//			mesh_dynamicpop.runMeshPop(startdate, enddate, gpspath, dynamicpop, meshsize, IDs);
		//
		//			// # 2.5 get SCI for each ID -----------------------------------------------
		//			File userSCIfile = new File(home+"user_SCI_"+String.valueOf(meshsize)+".csv");
		//			user_SCI.runSCImetric(dynamicpop, startdate, enddate, gpspath, meshsize, IDs, userSCIfile);
		//
		//			System.out.println("finished for mesh size "+String.valueOf(meshsize));
		//		}


		// 3. fake IDs
		File id_newid_f = new File(home+"id_newids.csv");
		HashMap<String, String> id_newid = connect_files.dummyIDs(idwordcount,id_newid_f);

		// 3.1. misinfo score for each ID 
		File id_misinfoscore_f = new File(home+"id_misinfoscore.csv");
		HashMap<String, Integer> id_misinfoscore = connect_files.ID_misinfoscore(idwordcount, id_misinfoscore_f);

		// 3.3. home code (population density and income) for each ID 
		// https://www5.cao.go.jp/keizai-shimon/kaigi/special/future/keizai-jinkou_data.html
		File idhomemesh_f = new File(home+"id_homelocs_meshcode.csv");
		HashMap<String, String> id_meshcode = connect_files.intomeshcode(idhome_f, idhomemesh_f);

		// 3.2. delta Rg, TTD, SCI for each ID 
		String resdir  = home+"metrics_bydays/";
		HashMap<String, String> id_rg = connect_files.getmetrics("rg",startdate, enddate, resdir);
		HashMap<String, String> id_ttd= connect_files.getmetrics("ttd",startdate, enddate, resdir);
		HashMap<String, String> id_disp= connect_files.getmetrics("disp",startdate, enddate, resdir);
		HashMap<String, String> id_sahr = connect_files.getmetrics("sahr",startdate, enddate, resdir);

		// 3.4. combine all data into one table 
		File id_rg_f = new File(home+"id_rg.csv");
		File id_ttd_f = new File(home+"id_ttd.csv");
		File id_disp_f = new File(home+"id_disp.csv");
		File id_sahr_f = new File(home+"id_sahr.csv");

		writeoutres(id_rg_f,id_newid,id_misinfoscore,id_meshcode,id_rg);
		writeoutres(id_ttd_f,id_newid,id_misinfoscore,id_meshcode,id_ttd);
		writeoutres(id_disp_f,id_newid,id_misinfoscore,id_meshcode,id_disp);
		writeoutres(id_sahr_f,id_newid,id_misinfoscore,id_meshcode,id_sahr);
		
		Integer negsamples = Integer.valueOf(args[0]);
		
		selectdata.select_subset(id_rg_f, negsamples);
		selectdata.select_subset(id_ttd_f, negsamples);
		selectdata.select_subset(id_disp_f, negsamples);
		selectdata.select_subset(id_sahr_f, negsamples);
		
	}


	public static void writeoutres(
			File out, 
			HashMap<String, String> id_newid, 
			HashMap<String, Integer> id_misinfoscore,
			HashMap<String, String> id_meshcode,
			HashMap<String, String> id_metric
			) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		for(String id : id_metric.keySet()) {
			String homemesh = "0";
			if(id_meshcode.containsKey(id)) {
				homemesh = id_meshcode.get(id);
			}
			String misinfoscore = "0";
			if(id_misinfoscore.containsKey(id)) {
				misinfoscore = String.valueOf(id_misinfoscore.get(id));
			}
			String metrics = "0";
			if(id_metric.containsKey(id)) {
				metrics = id_metric.get(id);
			}
			bw.write(id_newid.get(id)+","+homemesh+","+misinfoscore+","+metrics);
			bw.newLine();
		}
		bw.close();
	}



}




