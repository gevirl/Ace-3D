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
public class SegmentationNode {
    static public SegmentationNode read(DataInputStream inp)throws Exception {
        SegmentationNode node = new SegmentationNode();
        byte[] bytes = new byte[8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        inp.read(bytes, 0, 2);
        node.tau = byteBuffer.getShort(0);
        
        inp.read(bytes, 0, 4);
        node.order = byteBuffer.getInt(0);
        if (node.order != -1){
            System.out.println(node.order);
        }
        inp.read(bytes, 0, 4);
        node.parent = byteBuffer.getInt(0);        
        
        node.child1 = -1;
        node.child2 = -1;
        return node;
    }
    short tau;
    int order;
    int parent;
    int child1;
    int child2;
    
}
