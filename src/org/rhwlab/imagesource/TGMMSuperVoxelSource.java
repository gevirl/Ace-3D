/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.imagesource;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeMap;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.jdom2.Element;
import org.rhwlab.dispim.DataSetDesc;
import org.rhwlab.dispim.DataSetDescImpl;
import org.rhwlab.dispim.ImageSource;
import org.rhwlab.dispim.TimePointImage;

/**
 *
 * @author gevirl
 */
public class TGMMSuperVoxelSource implements ImageSource {
    public TGMMSuperVoxelSource(String fName){
        File f = new File(fName);
        dir = f.getParentFile();
    }

    @Override
    public boolean open() {
        if (dir.isDirectory()){
            File[] files = dir.listFiles();
            for (File file : files){
                if (file.getPath().endsWith("bin")){
                    int i = file.getName().indexOf("_");
                    int time = Integer.valueOf(file.getName().substring(i-3, i));
                    fileNames.put(time,file.getPath());
                } else if (file.getPath().endsWith("svb")){
                    int i = file.getName().indexOf(".svb");
                    int time = Integer.valueOf(file.getName().substring(i-4, i));
                    fileNames.put(time,file.getPath());                    
                }
            }
            return true;
        }
        return false;
    }
    
    public void readBinaryFile(int time) throws Exception {
        file = fileNames.get(time);
        if (file == null) return; 
        
        DataInputStream inp = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 32768));
        
        // read the basic regions as supervoxels
        byte[] bytes = new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);            
        inp.read(bytes);  // the number of supervoxels            
        nSV =byteBuffer.getInt(0);
        long total = 0;
        superVoxels = new SuperVoxel[nSV];
        for (int i=0 ; i<nSV ; ++i){
            superVoxels[i] = SuperVoxel.read(inp);
            total = total + superVoxels[i].voxelCount;
        }
        
        // read the tree nodes
        inp.read(bytes);  // the number of nodes           
        int nNodes=byteBuffer.getInt(0);  
        nodes = new SegmentationNode[nNodes];
        for (int i=0 ; i<nNodes ; ++i){
            nodes[i] = SegmentationNode.read(inp);
            // form child links
            if (nodes[i].parent != -1){
                SegmentationNode parent = nodes[nodes[i].parent];
                if (parent.child1 == -1){
                    parent.child1 = i;
                } else {
                    parent.child2 = i;
                    if (nodes[parent.child1].tau > nodes[parent.child2].tau){
                        int temp = parent.child1;
                        parent.child1 = parent.child2;
                        parent.child2 = temp;
                    }
                }
            }
        }
        this.reportNodes();
        inp.close();
    }

    @Override
    public TimePointImage getImage(String dataset, int time) {

        try {
            ArrayImg image = null;
            this.readBinaryFile(time);
            if (file.endsWith("svb")){
                for (SuperVoxel sv : superVoxels){
                    image = this.addSuperVoxelToImage(image, sv);
                }
            } else {
                image = addNodeToImage(image,nodes[0],252);
            }
            float[] mn = new float[2];
            mn[0] = (float)0.0;
            mn[1] = (float)Short.MAX_VALUE;
            TimePointImage tpi = new TimePointImage(image,mn,time,dataset);
            return tpi;
        } catch (Exception exc){
            exc.printStackTrace();;
        }
        return null;
    }
    private ArrayImg addNodeToImage(ArrayImg arrayImg,SegmentationNode node,int threshold){
        if (node.child1!=-1) { 
            if (node.tau<threshold){
                // add all the leaves descending from this node
                arrayImg = addDescendents(arrayImg,node);
            }else {
                // see if any nodes from children subtrees can be added
                arrayImg = addNodeToImage(arrayImg,nodes[node.child1],threshold);
                arrayImg = addNodeToImage(arrayImg,nodes[node.child2],threshold);
            }
        }
        return arrayImg;
    }
    private ArrayImg addDescendents(ArrayImg arrayImg,SegmentationNode node){
        if (node.order != -1){
            SuperVoxel sv = superVoxels[node.order];
            arrayImg = addSuperVoxelToImage(arrayImg,sv);
        } else {
            arrayImg = addDescendents(arrayImg,nodes[node.child1]);
            arrayImg = addDescendents(arrayImg,nodes[node.child2]);
        }
        return arrayImg;
    }
    private ArrayImg addSuperVoxelToImage(ArrayImg arrayImg,SuperVoxel sv) {
        
        short black = 0;
        short white = Short.MAX_VALUE;
        if (arrayImg == null){
            arrayImg = new ArrayImgFactory().create(sv.dims, new UnsignedShortType());
            ArrayCursor cursor =arrayImg.cursor();
            cursor.fwd();
            while (cursor.hasNext()){
                UnsignedShortType nt = (UnsignedShortType)cursor.get();
                nt.setInteger(black);
                cursor.fwd();
            }
        }
        ArrayRandomAccess access = arrayImg.randomAccess();
        for (int i =0 ; i<sv.voxelCount ; ++i){
            access.setPosition(sv.voxels[i]);
            UnsignedShortType vox = (UnsignedShortType)access.get();
            vox.setInteger(white);
        }
        return arrayImg;
    }

    @Override
    public int getTimes() {
        return fileNames.size();
    }

    @Override
    public int getMinTime() {
        return fileNames.firstKey();
    }

    @Override
    public int getMaxTime() {
        return fileNames.lastKey();
    }

    @Override
    public String getFile() {
        return dir.getPath();
    }

    @Override
    public Collection<DataSetDesc> getDataSets() {
        HashSet<DataSetDesc> ret = new HashSet<>();
        DataSetDescImpl ds = new DataSetDescImpl();
        ds.setName("TGMMSuperVoxels");
        ret.add(ds);
        return ret;
    }
    public void reportNodes(){
        int i=0;
        for (SegmentationNode node : nodes){
            System.out.printf("Node:%s\tParent:%d\tC1:%d\tC2%d\tSV:%d\tTau:%d\n",i,node.parent,node.child1,node.child2, node.order,node.tau);
            ++i;
        }
    }
    String file;
    File dir;
    TreeMap<Integer,String> fileNames = new TreeMap<Integer,String>();
    int nSV; // number of super voxels
    SuperVoxel[] superVoxels;
    SegmentationNode[] nodes;

    @Override
    public void setFirstTime(int minTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element toXML() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
