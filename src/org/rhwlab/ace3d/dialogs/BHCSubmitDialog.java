/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.rhwlab.BHC.Nuclei_Identification;
import org.rhwlab.BHC.Nuclei_IdentificationCLI;
import org.rhwlab.ace3d.Ace3D_Frame;

/**
 *
 * @author gevirl
 */
public class BHCSubmitDialog extends JDialog {
    public BHCSubmitDialog(Ace3D_Frame owner,String initDir,int[] dims){
        super(owner,false);
        this.setTitle("Submit To Grid");
        this.setSize(450, 300);
        this.setLocationRelativeTo(owner);    
        this.getContentPane().setLayout(new BorderLayout());
        
        panel = new BHCSubmitPanel(initDir, dims);
        this.getContentPane().add(panel,BorderLayout.CENTER);
        
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        JButton ok = new JButton("Submit");
        ok.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                File seriesDir = panel.getSeriesDirectory();
                File bhcDir = new File(seriesDir,"BHC");
                TreeMap<Integer,String[]> files = Nuclei_IdentificationCLI.getMVRFiles(seriesDir.getPath(),panel.getStartTime(),panel.getEndTime());
                try {
                    Nuclei_Identification.submitTimePoints(panel.isWaterston(),bhcDir,files,panel.getForce(),panel.getCores(),panel.getMemory(),panel.getAlpha(),panel.getVariance(),panel.getDegrees(),panel.getProb(),panel.getBoundingBox());
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        buttons.add(ok);
        
 //       JButton cancel = new JButton("Cancel");
 //       buttons.add(cancel);
        
        this.getContentPane().add(buttons,BorderLayout.SOUTH);
    }
    BHCSubmitPanel panel;
}
