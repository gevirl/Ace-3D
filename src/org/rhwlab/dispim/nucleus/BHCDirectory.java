/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.rhwlab.BHC.BHCTree;

/**
 *
 * @author gevirl
 */
// the directory of BHCTrees
public class BHCDirectory {
    public BHCDirectory(File dir){
        this.dir = dir;
        open();
    }
    public BHCDirectory(Element xml){
        this.dir = new File(xml.getAttributeValue("path"));
        open();
    }
    final private void open(){
        if (!dir.isDirectory()){
            dir = dir.getParentFile();
        }
        File[] files = dir.listFiles();
        Pattern pat = Pattern.compile("TP(\\d{1,4})_.+Probabilities(\\d{1,3})_(BHCTree|Clusters).xml");
        for (File file : files){
            Matcher mat = pat.matcher(file.getName());
            if (mat.matches()){
                int time = Integer.valueOf(mat.group(1));
                int thresh = Integer.valueOf(mat.group(2));
                String type = mat.group(3);
                
                if (type.equals("Clusters")){
                    TreeMap<Integer,File> clusterFileMap = this.clusterFiles.get(time);
                    if (clusterFileMap == null){
                        clusterFileMap = new TreeMap<>();
                        this.clusterFiles.put(time, clusterFileMap);
                    }
                    clusterFileMap.put(thresh,file);
                } else if (type.equals("BHCTree")){
                    TreeMap<Integer,File> treeFilesMap = this.treeFiles.get(time);
                    if (treeFilesMap == null){
                        treeFilesMap = new TreeMap<>();
                        this.treeFiles.put(time,treeFilesMap);
                    }
                    treeFilesMap.put(thresh, file);
                }
                //System.out.println(file.getName());
            }

        }        
    }
    public Set<Integer> getThresholdProbs(int time){
        return  treeFiles.get(time).keySet();
    }

    public TreeMap<Integer,BHCTree> getTrees(int time)throws Exception {
        TreeMap<Integer,BHCTree> ret = new TreeMap<>();
        for (Integer p : this.getThresholdProbs(time)){
            ret.put(p,this.getTree(time, p));
        }
        return ret;
    }

    // get all the trees for a given time
    public BHCTree getTree(int time,int thresh)throws Exception {
        TreeMap<Integer,BHCTree> treesAtTime = this.bhcTrees.get(time);
        
        if (treesAtTime == null){
            // no cached trees for the given time, create an empty cache
            treesAtTime = new TreeMap<>();
            bhcTrees.put(time,treesAtTime);             
        }
        
        BHCTree tree = treesAtTime.get(thresh);
        if (tree == null){
            // no tree in cache, build tree from file
            TreeMap<Integer,File> filesAtTime = this.treeFiles.get(time);
            if (filesAtTime != null){
                File file = filesAtTime.get(thresh);
                if (file != null){  // is there a file for the given threshold
                    tree = new BHCTree(file.getPath(),time);  
                }
            }
        }
        return tree;
    }
    public File getDirectory(){
        return this.dir;
    }
    public Element toXML(){
        Element bhcEle = new Element("BHCTreeDirectory");
        bhcEle.setAttribute("path", dir.getPath()) ;    
        return bhcEle;
    }

    // convert file names to include segmentation threshold
    // this is one time use only
    static public void convert(File dir)throws Exception {
        Pattern pat = Pattern.compile("TP\\d{1,4}_.+ProbabilitiesClusters.xml");
        File[] files = dir.listFiles();
        for (File file : files){
            String name = file.getName();
            Matcher m = pat.matcher(name);
            if (m.matches()){
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                reader.close();
                
                String[] tokens = line.split(" ");
                for (String token : tokens){
                    if(token.contains("Segmentation")){
                        String[] items = token.split("=");
                        double thresh = Double.valueOf(items[1].substring(1, items[1].length()-1));
                        int iThresh = (int)thresh;
                        String clustName = name.replace("Probabilities", "Probabilities"+Integer.toString(iThresh)+"_");
                        String bhcName = clustName.replace("Clusters", "BHCTree");
                        System.out.printf("%s  --  %s\n",clustName,bhcName);
                        
                        File oldBHC = new File(dir,name.replace("Clusters", "BHCTree"));
                        if (oldBHC.exists()){
                            File newBHC = new File(dir,bhcName);
                            System.out.printf("%s to %s\n",oldBHC.getPath(),newBHC.getPath());
                            oldBHC.renameTo(newBHC);
                        }
                        
                        File newClust = new File(dir,clustName);
                        file.renameTo(newClust);
                    }
                }
            }
        }
    }
    static public void main(String[] args)throws Exception {
        
        BHCDirectory.convert(new File("/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/BHC"));
        BHCDirectory bhc = new BHCDirectory(new File("/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/BHC"));
        bhc.open();
        int iusdfui=0;
        
        
/*        
        File bhcNucFile = new File("/net/waterston/vol2/home/gevirl/rnt-1/xml/img_TL016_Simple_Segmentation.xml");
        BHCNucleusFile nucFile = new BHCNucleusFile(bhcNucFile);
        Ace3DNucleusFile ace3dFile = new Ace3DNucleusFile();
        ace3dFile.addBHC(nucFile);;
        BHCTreeDirectory bhcDir = new BHCTreeDirectory(new File("/net/waterston/vol2/home/gevirl/rnt-1/xml"));
        for (int i=0 ; i<25 ; ++i){
            ace3dFile = bhcDir.linkInNextTime(ace3dFile);
        }
        ace3dFile.bhcNucDir = new BHCNucleusDirectory(new File("/net/waterston/vol2/home/gevirl/rnt-1/xml"));
        ace3dFile.saveAs(new File("/net/waterston/vol2/home/gevirl/rnt-1/LinkedNuclei.json"));
        int ouahsdfuis=0;
*/
    }

    File dir;
//    TreeMap<Integer,TreeMap<Integer,FilePair>> filePairs = new TreeMap<>(); // files indexed by time and segmentation threshold
    TreeMap<Integer,TreeMap<Integer,File>> clusterFiles = new TreeMap<>();
    TreeMap<Integer,TreeMap<Integer,File>> treeFiles = new TreeMap<>();
    TreeMap<Integer,TreeMap<Integer,BHCTree>> bhcTrees = new TreeMap<>(); // trees read indexed by time and threshold
/*
    public class FilePair{
        public FilePair(File clusterFile,File treeFile){
            this.cluster = clusterFile;
            this.tree = treeFile;
        }
        File cluster;
        File tree;
    }
*/
}
