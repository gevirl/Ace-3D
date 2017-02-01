/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.imagesource;

import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author gevirl
 */
public class SuperVoxel {
    public SuperVoxel(){
        
    }
    static public SuperVoxel read(DataInputStream inp)throws Exception {
        SuperVoxel sv = new SuperVoxel();
        byte[] bytes = new byte[8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN); 

        inp.read(bytes,0,4); // the time
        sv.time = byteBuffer.getInt(0);

        inp.read(bytes,0,8); // the datasize            
        sv.dataSize = byteBuffer.getLong(0);  

        // the image dimensions
        sv.dims = new long[3];
        inp.read(bytes,0,8); 
        sv.dims[0] = byteBuffer.getLong(0);
        inp.read(bytes,0,8); 
        sv.dims[1] = byteBuffer.getLong(0);
        inp.read(bytes,0,8); 
        sv.dims[2] = byteBuffer.getLong(0);
        
        inp.read(bytes,0,4);  // voxel count
        sv.voxelCount = byteBuffer.getInt(0);
        sv.voxels = new long[sv.voxelCount][];

        byte[] pixBytes = new byte[sv.voxelCount*8];
        inp.read(pixBytes);
        ByteBuffer pixBuf = ByteBuffer.wrap(pixBytes);
        pixBuf.order(ByteOrder.LITTLE_ENDIAN); 
        
        for (int i =0 ; i<sv.voxelCount ; ++i){
            long[] coord = new long[3];
            long index = pixBuf.getLong();
            for (int d =0 ; d<sv.dims.length-1 ; ++d){
                coord[d] = index % sv.dims[d];
                index = index - coord[d];
                index = index/sv.dims[d];
            }
            coord[sv.dims.length-1] = index;
            sv.voxels[i] = coord;

        }
        return sv;        
    }
    int time;
    long dataSize;
    long[] dims;
    int voxelCount;
    long[][]voxels;
}
