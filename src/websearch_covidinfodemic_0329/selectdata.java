package websearch_covidinfodemic_0329;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

public class selectdata {

//	public static void main(String[] args) throws IOException {
		
		
	public static void select_subset(
			File in, 
			Integer negsamples
			) throws NumberFormatException, IOException {	

//		String root = "/mnt/tyabe/"; File root_f = new File(root); root_f.mkdir();
//		String home = root+"infodemic_0329/"; File home_f = new File(home); home_f.mkdir();
//		File id_allmeasures     = new File(home+"id_allmeasures.csv");
//		File id_allmeasures_sub = new File(home+"id_allmeasures_subset.csv");
//		Integer negsamples = Integer.valueOf(args[0]);

		File out = new File(in.getPath().split(".csv")[0]+"_subset.csv");
		
		BufferedReader br1 = new BufferedReader(new FileReader(in));
		String line1 = null;
		Integer ids = 0;
		HashSet<String> over1mesh = new HashSet<String>();
		while((line1=br1.readLine())!=null) {
			String[] tokens = line1.split(",");
			Integer count = Integer.valueOf(tokens[2]);
			if(!tokens[1].equals("0")) {
				if(count>0) {
					ids+=1;
					String mesh = tokens[1].substring(0,8);
					over1mesh.add(mesh);
				}
			}
		}
		br1.close();

		System.out.println("IDs with over 1 misinfo search: "+String.valueOf(ids));

		Integer negcount = 0;
		BufferedReader br = new BufferedReader(new FileReader(in));
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		String line = null;
		while((line=br.readLine())!=null) {
			String[] tokens = line.split(",");
			Integer count = Integer.valueOf(tokens[2]);
			if(!tokens[1].equals("0")) {
				if(count>0) {
					bw.write(line);
					bw.newLine();
				}
				else if(count==0) {
					String mesh = tokens[1].substring(0,8);
					if(over1mesh.contains(mesh)) {
						if(negcount<negsamples) {
							bw.write(line);
							bw.newLine();
							negcount+=1;
						}
					}
				}
			}
		}
		br.close();
		bw.close();
	}

}
