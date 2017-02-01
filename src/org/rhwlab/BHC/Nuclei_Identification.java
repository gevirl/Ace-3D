/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.io.PrintStream;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rhwlab.dispim.datasource.BoundingBox;
import org.rhwlab.dispim.datasource.ByteHDF5DataSource;
import org.rhwlab.dispim.datasource.FloatHDF5DataSource;
import org.rhwlab.dispim.datasource.MicroClusterDataSource;
import org.rhwlab.dispim.datasource.Segmentation;
import org.rhwlab.dispim.datasource.SegmentedTiffDataSource;
import org.rhwlab.dispim.datasource.TiffDataSource;

/**
 *
 * @author gevirl
 */
public class Nuclei_Identification implements Runnable {
    public Nuclei_Identification(String dir,String lineageTiff,String segSource,String force,double alpha,double s,int nu,int thresh,BoundingBox box){
        System.out.println(lineageTiff);
        System.out.println(segSource);
        this.box = box;
        this.segmentedSource = segSource;
        segSource = cleanName(segSource);
        this.lineageTff = lineageTiff;
        this.directory = new File(dir);
        File segFile = new File(segSource);
        this.baseName = baseName(segFile.getName());
        this.force = force;
        this.alpha = alpha;
        this.segThresh = thresh;
        this.S = s;
        // parse the time from the filename
        this.time = getTime(segFile.getName());
        this.nu = nu;
        
    }
    static public String baseName(String fileName){
        return fileName.substring(0, fileName.indexOf("."));
    }
    static public int getTime(String fileName){
        int time = -1;
        Pattern p = Pattern.compile("TP(\\d{1,4})_");
        Matcher m = p.matcher(fileName);
        boolean matched = m.find();
        if (matched){
            time = Integer.valueOf(m.group(1));
        } 
        return time;
    }
    static String cleanName(String name){
        String ret  = name.replace(',', '_');
        return ret.replace(' ', '_');        
    }

    // return the tree and cluster file names for a given base name and threshold
    static File[] xmlFiles(File directory,String baseName,int thresh){
        File[] ret = new File[2];
        String s = Integer.toString(thresh);
        String microClusterFileName = cleanName(baseName +s + "_Clusters.xml");
        
        ret[0] = new File(directory,microClusterFileName);
        String BHCTreeFileName = cleanName(baseName + s +"_BHCTree.xml");
        ret[1] = new File(directory,BHCTreeFileName);
        
        
        return ret;
    }
    @Override
    public void run() {

        // determine the file names
        File[] xmls = xmlFiles(directory,baseName,segThresh);
        File microClusterFile = xmls[0];
        File BHCTreeFile = xmls[1];

        if (!microClusterFile.exists()  || (force!=null && force.toLowerCase().contains("kmeans"))){
            try {
                SegmentedTiffDataSource segSource;
                if (segmentedSource.endsWith("h5")){
                    if (segmentedSource.contains("Prob")){
                        segSource = new SegmentedTiffDataSource(lineageTff,new Segmentation(new FloatHDF5DataSource(new File(segmentedSource),"exported_data",100.0,1),segThresh,box));
                    }else {
                        segSource = new SegmentedTiffDataSource(lineageTff,new Segmentation(new ByteHDF5DataSource(new File(segmentedSource),"exported_data"),segThresh,box));
                    }
                } else {
                    segSource = new SegmentedTiffDataSource(lineageTff,new Segmentation(new TiffDataSource(segmentedSource),segThresh,box));
                }
                
//                SegmentedTiffDataSource segSource = new SegmentedTiffDataSource(segmentedTiff,backgroundSegment); 
                this.runMicroCluster(segSource,microClusterFile);
                this.runBHC(microClusterFile,BHCTreeFile);
//                this.runTreeCut(BHCTreeFile,gmmFile);
                return;
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(1);
            }
        }
        
        if (!BHCTreeFile.exists() || (force !=null && force.toLowerCase().contains("bhc") )){
            try {
                this.runBHC(microClusterFile,BHCTreeFile);
//                this.runTreeCut(BHCTreeFile,gmmFile);
                return;                
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(2);
            }
        }
 /*       
        if (!gmmFile.exists() || force){
            try {
                this.runTreeCut(BHCTreeFile, gmmFile);

            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(3);
            }
        }
        */
    }
    private void runMicroCluster(SegmentedTiffDataSource segSource,File microClusterFile)throws Exception {
        int nVoxels = segSource.getSegmentN();
        int nClusters = clusterCount(nVoxels);
        int nPartitions = Math.max(1,(int)Math.ceil(Math.pow(nClusters/1000.0,1.0/3.0)));                
        segSource.kMeansCluster( nClusters, nPartitions).saveAsXML(microClusterFile.getPath());
    }
    
    private void runBHC(File microClusterFile,File BHCTreeFile)throws Exception {
        MicroClusterDataSource microDataSource = new MicroClusterDataSource(microClusterFile.getPath());
        runBHC(microDataSource,BHCTreeFile);
    }
    
    public void runBHC(MicroClusterDataSource microDataSource,File BHCTreeFile)throws Exception {
        ThreadedAlgorithm alg;
//        MicroCluster4DDataSource microDataSource = new MicroCluster4DDataSource(microClusterFile.getPath());
//        do {
            alg = new ThreadedAlgorithm();
            alg.setTime(time);
            alg.setSource(microDataSource);
            double[] precision = new double[microDataSource.getD()];
            precision[0] = precision[1] = precision[2] = S;  //20
    //        precision[3] = 200.0;
            alg.setPrecision(precision);
            alg.setNu(nu); //10
      //      double alpha = Math.pow(2.0*nClusters,2.0);

            alg.setAlpha(alpha);
            alg.run();
  //          alpha = 10.*alpha;
  //      } while (alg.getFinalCluster().getPosterior()!=0.0);
        
        alg.saveResultAsXML(BHCTreeFile.getPath());        
    }
    private SegmentedTiffDataSource segmentedSource(){
        SegmentedTiffDataSource segSource;
        if (segmentedSource.endsWith("h5")){
            if (segmentedSource.contains("Prob")){
                segSource = new SegmentedTiffDataSource(lineageTff,new Segmentation(new FloatHDF5DataSource(new File(segmentedSource),"exported_data",100.0,1),segThresh,box));
            }else {
                segSource = new SegmentedTiffDataSource(lineageTff,new Segmentation(new ByteHDF5DataSource(new File(segmentedSource),"exported_data"),segThresh,box));
            }
        } else {
            segSource = new SegmentedTiffDataSource(lineageTff,new Segmentation(new TiffDataSource(segmentedSource),segThresh,box));
        }
        return segSource;
    }
    public void runSampled()throws Exception {
        // determine the file names
        File[] xmls = xmlFiles(directory,baseName,segThresh);
        File BHCTreeFile = xmls[1];        
        SegmentedTiffDataSource segSource = segmentedSource();
        MicroClusterDataSource mcSource = segSource.asMicroClusterDataSource();
        this.runBHC(mcSource, BHCTreeFile);
        
    }
    private void runTreeCut(File BHCTreeFile,File gmmFile) throws Exception {
 //       BHCTree tree = new BHCTree(BHCTreeFile.getPath());
 //       tree.saveCutAtThresholdAsXML(gmmFile.getPath(),.5);
/*        
        Double[] post = tree.allPosteriors().toArray(new Double[0]);
        for (int i=0 ; i<post.length ; ++i){
            if (post[i] >= 0.5){
                tree.saveCutAtThresholdAsXML(gmmFile.getPath(),post[i-1]);
                break;
            }
        }
*/        
    }
/*    
    static public void runBHCStudy(File microClusterFile)throws Exception {
        double[] alphas = new double[8];
        alphas[0] = 1000.0;
        for (int i=1 ; i<alphas.length ; ++i){
            alphas[i] = 10.0*alphas[i-1];
        }
        double[] precisions = new double[10];
        precisions[0] = 50.0;
        for (int i=1 ; i<precisions.length ; ++i){
            precisions[i] = precisions[i-1] + 50.0;
        }
        MicroClusterDataSource microDataSource = new MicroClusterDataSource(microClusterFile.getPath());
        PrintStream stream = new PrintStream(microClusterFile.getPath()+".study");
        for (double alpha : alphas){
            for (double prec : precisions){
                ThreadedAlgorithm alg = new ThreadedAlgorithm();
                alg.setSource(microDataSource); 
                alg.setPrecision(prec);
                alg.setNu(20);
                alg.init(alpha);
                alg.run();
                BHCTree tree = alg.resultAsBHCTree();
                Double[] post = tree.allPosteriors().toArray(new Double[0]);
                for (int i=0 ; i<post.length ; ++i){
                    if (post[i] >= 0.5){
                        Element ele = tree.cutTreeAtThreshold(post[i-1]);
                        List<Element> eleList = ele.getChildren("GaussianMixtureModel");
                        stream.printf("%e\t%f\t%d\n",alpha,prec,eleList.size());
                        break;
                    }
                }
                stream.flush();
            }
        }
    }
    */
    // determine the number of microclusters to form given the number of voxels in the segmented tiff
    static int clusterCount(int nVox){
        int ret = nVox/microClusterSize;
        if (ret < minMicroClusters){
            ret = minMicroClusters;
        } else if (ret > maxMicroClusters){
            ret = maxMicroClusters;
        }
        return ret;
    }
/*    
    static public void submitStudyTimes(File directory,TreeMap<Integer,String[]> tiffs)throws Exception {
        if (tiffs.isEmpty()) return;
        
        File scriptFile = new File(directory,"SubmitStudyTimes.sh");
       
        PrintStream scriptStream = new PrintStream(scriptFile);
        scriptStream.printf("cd %s\n", directory.getPath());
        
        for (int time : tiffs.keySet()){
            String[] names = tiffs.get(time);
            String fileName = new File(names[1]).getName();
            String baseName = baseName(fileName);
            File[] xmls = xmlFiles(directory,baseName);

           
            scriptStream.printf("qsub -e %s -o %s %s.qsub\n",directory.getPath(),directory.getPath(),baseName);
            
            // write the qsub file
            PrintStream qsubStream = new PrintStream(new File(directory,baseName+".qsub"));
            qsubStream.println("#$ -S /bin/bash");
            qsubStream.println("#$ -l mfree=40G");
            
     //       qsubStream.println("#$ -l h=w014");
            qsubStream.println("#$ -pe serial 1-10");
            qsubStream.println("cd /nfs/waterston/Ace3D");
            qsubStream.println("PATH=/nfs/waterston/jdk1.8.0_102/bin:$PATH");
            qsubStream.println("JAVA_HOME=/nfs/waterston/jdk1.8.0_102");
            qsubStream.println("M2_HOME=/nfs/waterston/apache-maven-3.3.9");
            qsubStream.print("/nfs/waterston/apache-maven-3.3.9/bin/mvn \"-Dexec.args=-Xms36000m -Xmx36000m -classpath %classpath org.rhwlab.BHC.Nuclei_Identification ");
            qsubStream.printf("-study -first %d -last %d -segTiff \"%s\"  -lineageTiff \"%s\" -dir %s  ",time,time,names[1],names[0],directory.getPath());

            qsubStream.print("\" -Dexec.executable=/nfs/waterston/jdk1.8.0_102/bin/java -Dexec.classpathScope=runtime org.codehaus.mojo:exec-maven-plugin:1.2.1:exec");
            qsubStream.println();
            qsubStream.close();
        }
        scriptStream.close();
         scriptFile.setExecutable(true, false);
 
        // start the submission script
        ProcessBuilder pb = new ProcessBuilder("ssh","grid.gs.washington.edu",scriptFile.getPath());
        Process p = pb.start();        
    }
    */
    static public void submitTimePoints(boolean water,File directory,TreeMap<Integer,String[]> tiffs,String force,int cores,int memory,double alpha,double S,int nu,int th,BoundingBox box)throws Exception {
        if (tiffs.isEmpty()) return;
        directory.mkdir();
        directory.setWritable(true, false);
        
        System.getProperty("user.name");
        long t = System.currentTimeMillis();
        File scriptFile = new File(directory,System.getProperty("user.name")+Long.toString(t)+"Submit.sh");
        scriptFile.setWritable(true,false);
        
        PrintStream scriptStream = new PrintStream(scriptFile);
        scriptStream.printf("cd %s\n", directory.getPath());
        scriptStream.println("rm -f *qsub.e*");
        scriptStream.println("rm -f *qsub.o*");
        scriptStream.println("rm -f *qsub.pe*");
        scriptStream.println("rm -f *qsub.po*");
        for (int time : tiffs.keySet()){
            String[] names = tiffs.get(time);
            if (names[0]==null || names[1] == null){
                continue;
            }            
            String fileName = new File(names[1]).getName();
            String baseName = baseName(fileName);
            File[] xmls = xmlFiles(directory,baseName,th);

            if (xmls[0].exists() && xmls[1].exists() && force==null){
                continue;
            }
           
            scriptStream.printf("qsub -e %s -o %s \"%s.qsub\"\n",directory.getPath(),directory.getPath(),baseName);
            
            // write the qsub file
            File qsubFile = new File(directory,baseName+".qsub");
            qsubFile.setWritable(true,false);
            PrintStream qsubStream = new PrintStream(qsubFile);
            qsubStream.println("#$ -S /bin/bash");
            qsubStream.printf("#$ -l mfree=%sG\n",memory);
            qsubStream.println("#$ -l h_rt=9:0:0");
     //       qsubStream.println("#$ -l h=w014");
            qsubStream.printf("#$ -pe serial %d\n",cores);
            if (!water){
                qsubStream.println("#$ -P sage");
            }
            qsubStream.println("cd /nfs/waterston/Ace3D");
            qsubStream.println("PATH=/nfs/waterston/jdk1.8.0_102/bin:$PATH");
            qsubStream.println("JAVA_HOME=/nfs/waterston/jdk1.8.0_102");
            qsubStream.println("M2_HOME=/nfs/waterston/apache-maven-3.3.9");
            qsubStream.printf("/nfs/waterston/apache-maven-3.3.9/bin/mvn \"-Dexec.args=-Xms%sG -Xmx%sG  ",cores*memory,cores*memory);
            qsubStream.print(" -classpath %classpath org.rhwlab.BHC.Nuclei_Identification ");
            qsubStream.printf("-segThresh %d -S %f -nu %d -alpha %f -segTiff \'%s\'  -lineageTiff \'%s\' -bhcDir %s  ",th,S,nu,alpha,names[1],names[0],directory.getPath());
            if (force!=null){
                qsubStream.printf(" -force %s ",force);
            }
            if (box.getMin(0)!=null){
                qsubStream.printf(" -xMin %f", box.getMin(0));
            }
            if (box.getMin(1)!=null){
                qsubStream.printf(" -yMin %f", box.getMin(1));
            }
            if (box.getMin(2)!=null){
                qsubStream.printf(" -zMin %f", box.getMin(2));
            }
            if (box.getMax(0)!=null){
                qsubStream.printf(" -xMax %f", box.getMax(0));
            }
            if (box.getMax(1)!=null){
                qsubStream.printf(" -yMax %f", box.getMax(1));
            }
            if (box.getMax(2)!=null){
                qsubStream.printf(" -zMax %f", box.getMax(2));
            }            
            qsubStream.print("\" -Dexec.executable=/nfs/waterston/jdk1.8.0_102/bin/java -Dexec.classpathScope=runtime org.codehaus.mojo:exec-maven-plugin:1.2.1:exec");
            qsubStream.println();
            qsubStream.close();
        }
        scriptStream.close();
        scriptFile.setExecutable(true, false);
        // start the submission script
        ProcessBuilder pb = new ProcessBuilder("ssh","grid.gs.washington.edu",scriptFile.getPath());
        Process p = pb.start(); 
        }
    
    static public void main(String[] args)throws Exception {
        Nuclei_IdentificationCLI cli = new Nuclei_IdentificationCLI();
        cli.process(args, true);
        
        if (cli.getQsub()){
            TreeMap<Integer,String[]> tiffs = cli.getTiffs(cli.getLineTiff(),cli.getSegmentTiff(),cli.getFirstTime(),cli.getLastTime());
//            submitTimePoints(new File(cli.getBHCDirectory()),tiffs,cli.getForce(),cli.getMemory(),cli.getAlpha(),cli.getS(),cli.getNu(),cli.getSegThresh());
        } else {
            Nuclei_Identification objectID = new Nuclei_Identification
                (cli.getBHCDirectory(),cli.getLineTiff(),cli.getSegmentTiff(),cli.getForce(),cli.getAlpha(),cli.getS(),cli.getNu(),cli.getSegThresh(),cli.getBoundingBox());
            objectID.run();
        }
/*                
        File dir = new File("/net/waterston/vol2/home/gevirl/rnt-1/segmented");
        for (File file : dir.listFiles()){
            System.out.println(file.getPath());
            if (file.getName().equals("img_TL016_Simple_Segmentation.tiff")){
                Nuclei_Identification objectID = new Nuclei_Identification(file.getPath(),true);
                objectID.run();
            }
        }
*/
        int iuasdfisd=0;
    }
//    SegmentedTiffDataSource segSource;
    String lineageTff;
    File directory;
    String baseName;
    String segmentedSource;
    double alpha;
    double S;
    int nu;
    int segThresh;
    int time=-1;

    String force;
    BoundingBox box;
//    static int backgroundSegment = 1;
 //   static int nucleiSegment = 2;
    
    static int microClusterSize = 50;  // voxels per micro cluster
    static int maxMicroClusters = 10000;
    static int minMicroClusters = 500;
}
