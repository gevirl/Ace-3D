/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.ArrayList;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;


/**
 *
 * @author gevirl
 */
public class SegmentedTiffDataSource extends TiffDataSource implements SegmentedDataSource{
    public SegmentedTiffDataSource(String tiff,Segmentation segmentation){
        super(tiff);
        this.segmentation = segmentation;
    }
/*
    public void saveAsXML(String file)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("Segmented");
        root.setAttribute("NumberOfPoints",Long.toString(this.getN()));
        root.setAttribute("NumberOfSegments", Integer.toString(segmentIndex.size()));
        for (Integer seg : segmentIndex.keySet()){
            List<Long> indexes = segmentIndex.get(seg);
            Element segment = new Element("Segment");
            segment.setAttribute("PointsInSegment",Integer.toString(indexes.size()));
            for (long n : indexes){
                Element pointEle = new Element("Point");
                Voxel vox = super.get(n);
                double[] point = vox.coords.toArray();
                StringBuilder builder = new StringBuilder();
                for (int d=0 ; d<point.length ; ++d){
                    if (d > 0 ){
                        builder.append(" ");
                    }
                    builder.append((int)point[d]);
                } 
                pointEle.addContent(builder.toString());
                segment.addContent(pointEle);
            }            
            root.addContent(segment);
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();
        
    }
*/
    public MicroClusterDataSource asMicroClusterDataSource(){
        MicroCluster[] micros = new MicroCluster[segmentation.getSegmentN()];
        for (int i=0 ; i<segmentation.getSegmentN() ; ++i){
            Voxel vox = this.getSegmentVoxel(i);            
            micros[i] = new SingleVoxelMicroCluster(vox);
           
        }       
        return new MicroClusterDataSource(micros);
    }
    @Override
    public ClusteredDataSource kMeansCluster(int nClusters, int nPartitions) throws Exception {
        double[] maxs = segmentation.getMaxs();
        double[] mins = segmentation.getMins();
        
        double[] dels = new double[dims.length];
        for (int d=0 ; d<dims.length ; ++d){
            dels[d] = (maxs[d]-mins[d])/nPartitions;
        }        
        // partition the voxels into separate lists
        int mx = (int)Math.pow(nPartitions,getD());
        ArrayList<Voxel>[] lists = new ArrayList[mx];
        for (int i=0 ; i<lists.length ; ++i){
            lists[i] = new ArrayList();
        } 
        for (int i=0 ; i<getSegmentN() ; ++i){
            Voxel vox = getSegmentVoxel(i);
            int index = region(vox.getPoint(), nPartitions,dels);
            lists[index].add(vox);
        } 
        // build the clustering threads
        double f = (double)getSegmentN()/(double)nClusters;   
        ArrayList<VoxelClusterer> clusterers = new ArrayList<>();
        for (int i=0 ; i<lists.length ; ++i){
            ArrayList<Voxel> list = lists[i];
            if (!list.isEmpty()){
                int nc = (int)((double)list.size()/f);
                VoxelClusterer clusterer = new VoxelClusterer(list,new KMeansPlusPlusClusterer(nc));
//                VoxelClusterer clusterer = new VoxelClusterer(list,new BalancedKMeansClusterer(nc));
                clusterers.add(clusterer);
            }
        }
        
        // do the clustering
        for (VoxelClusterer clusterer : clusterers){
            clusterer.start();
        }
        
        // wait for them all to finish
        for (VoxelClusterer clusterer : clusterers){
            clusterer.join();
        } 

        ClusteredDataSource ret =  new ClusteredDataSource(clusterers.toArray(new VoxelClusterer[0]),segmentation.getThreshold(),this.getD());
        ret.setPartition(mx);
        return ret;
    }
    // determine which region to put a given voxel
    protected int region(double[] p,int nPart,double[] dels){

        double[] mins = segmentation.getMins();
        int index = 0;
        int base = 1;
        for (int d=0 ; d<dims.length ; ++d){
            int i = (int)((p[d]-mins[d])/dels[d]);
            if (i == nPart){
                i = nPart-1;
            }
            index = index + i*base;
            base = base*nPart;
        }
        return index;
    }    
    // return the number of voxels in the segment
    @Override
    public int getSegmentN(){
        return segmentation.getSegmentN();
    }
    // return the ith voxel in the given segment
    @Override
    public Voxel getSegmentVoxel(int segIndex){
        // get the voxel from this tiff data source
        long voxIndex = segmentation.getVoxelIndex(segIndex);
        double prob = segmentation.getSegmentationProbability(segIndex);
        Voxel ret = super.get(voxIndex);
        ret.setAdjusted(prob);
        return ret;
    } 
    Segmentation segmentation;

    
}
