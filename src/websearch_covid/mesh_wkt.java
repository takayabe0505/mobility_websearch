package websearch_covid;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import jp.ac.ut.csis.pflow.geom.Mesh;

public class mesh_wkt {

	public static void main(String[] args) throws IOException {
		File in  = new File("C:/users/yabec/desktop/meshes_6.csv");
		File out = new File("C:/users/yabec/desktop/meshes_6_wkt.csv");
		BufferedReader br = new BufferedReader(new FileReader(in));
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		String line = null;
		while((line=br.readLine())!=null) {
			String[] tokens = line.split("\t");
			String meshcode = tokens[1];
			Mesh mesh = new Mesh(meshcode);
			Rectangle2D.Double rect = mesh.getRect();
			String wkt      = String.format("POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))",
					rect.getMinX(),rect.getMinY(),
					rect.getMinX(),rect.getMaxY(),
					rect.getMaxX(),rect.getMaxY(),
					rect.getMaxX(),rect.getMinY(),
					rect.getMinX(),rect.getMinY());
			bw.write(meshcode+"\t"+wkt);
			bw.newLine();
		}
		bw.close();
		br.close();
	}
	
//	public static void main(String[] args) throws IOException {
//		File in  = new File("C:/users/yabec/desktop/meshes.csv");
//		File out = new File("C:/users/yabec/desktop/meshes_wkt.csv");
//		BufferedReader br = new BufferedReader(new FileReader(in));
//		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
//		String line = null;
//		while((line=br.readLine())!=null) {
//			String[] tokens = line.split("\t");
//			String meshcode = tokens[1];
//			Mesh mesh = new Mesh(meshcode);
//			Rectangle2D.Double rect = mesh.getRect();
//			String wkt      = String.format("POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))",
//					rect.getMinX(),rect.getMinY(),
//					rect.getMinX(),rect.getMaxY(),
//					rect.getMaxX(),rect.getMaxY(),
//					rect.getMaxX(),rect.getMinY(),
//					rect.getMinX(),rect.getMinY());
//			bw.write(meshcode+"\t"+wkt);
//			bw.newLine();
//		}
//		bw.close();
//		br.close();
//	}

}
