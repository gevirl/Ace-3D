/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.beans.value.ObservableValue;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.apache.commons.math3.linear.RealMatrix;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class SelectedNucleusFrame extends JFrame implements PlugIn,javafx.beans.value.ChangeListener   {
    public SelectedNucleusFrame(Ace3D_Frame owner,ImagedEmbryo emb){
        this.embryo = emb;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        this.setLocationRelativeTo(owner);  
//        this.setLocationByPlatform(true);
        this.setTitle("Selected Nucleus");
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.X_AXIS));
        npPanel = new NucleusPropertiesPanel();
        embryo.addListener(npPanel);
        mainPanel.add(npPanel);
        
        radiusControl = new RadiusControlPanel(owner);
        radiusControl.setEmbryo(embryo);
        embryo.addListener(radiusControl);
        mainPanel.add(radiusControl);
/*        
        MarkedNucleiPanel nucPanel = new MarkedNucleiPanel();
        embryo.addListener(nucPanel);
        panel.add(nucPanel);       
 */       
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4,3));
        
        JButton unselectButton = new JButton("Unselect");
        unselectButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                embryo.setSelectedNucleus(null);
            }
        });
        buttonPanel.add(unselectButton);
        
        JButton unmarkButton = new JButton("Unmark");
        unmarkButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                embryo.setMarked(null);
            }
        });
        buttonPanel.add(unmarkButton);        
/*        
        JButton calcExp = new JButton("Calc Expression");
        calcExp.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> datasets = Ace3D_Frame.datasetsSelected();
                if (datasets.isEmpty()) return;
                Nucleus nuc = embryo.selectedNucleus();
                TimePointImage tpi = embryo.getTimePointImage(datasets.get(0), nuc.getTime());
                double[][] eigen = nuc.getEigenVectors();
                try {
                    double exp = embryo.calculateExpression(nuc,tpi,eigen);
                    embryo.setExpression(nuc, exp);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        buttonPanel.add(calcExp);
*/       
        unlink = new JButton("Unlink");
        unlink.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    embryo.getNucleusFile().unlinkNucleus(embryo.selectedNucleus(),true);
                }
            }
        });
        buttonPanel.add(unlink);
       
        link = new JButton("Link");
        link.setEnabled(false);
        buttonPanel.add(link);
        link.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                embryo.formLink();

            }
        });
        
        JButton join = new JButton("Join");
        buttonPanel.add(join);
        join.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (embryo.getMarked()!=null){
                        embryo.joinSelectedToMarked();
                    } else {
                        embryo.joinSelectedNucleus();
                    }
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        
        JButton split = new JButton("Split");
        buttonPanel.add(split);
        split.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    embryo.splitSelectedNucleus();
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });      
        
        JButton remove = new JButton("Remove Nucleus");
        buttonPanel.add(remove); 
        remove.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    embryo.getNucleusFile().removeNucleus(embryo.selectedNucleus(),true);
                }
            }
        });    
        
        JButton removeCell = new JButton("Remove Cell");
        buttonPanel.add(removeCell);
        removeCell.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    embryo.getNucleusFile().removeCell(embryo.selectedNucleus(),true);
                }
            }
        });
        
        JButton nameChildren = new JButton("Toggle Cell Name");
        buttonPanel.add(nameChildren);
        nameChildren.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    ((NamedNucleusFile)embryo.getNucleusFile()).toggleCellName(embryo.selectedNucleus(), true);
                }
            }
        });
        
        JButton confirm = new JButton("Orient Embryo");
        buttonPanel.add(confirm);
        confirm.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    RealMatrix r =((NamedNucleusFile)embryo.getNucleusFile()).orientEmbryo(embryo.selectedNucleus().getTime());
                    NamedNucleusFile.setOrientation(r);
                    embryo.nameAllRoots();
                    embryo.notifyListeners();
                }
            }
        });
        
        content.add(mainPanel);
        content.add(buttonPanel);
        this.setContentPane(content);
        pack();
    }

    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        Nucleus sel = embryo.selectedNucleus();
        if (sel != null){
            if (sel.getParent()!= null){
                unlink.setEnabled(true);
            } else {
                unlink.setEnabled(false);
                if (embryo.getMarked() != null){
                    link.setEnabled(true);
                }else {
                    link.setEnabled(false);
                }
            }
        }
        npPanel.invalidated(embryo);
        radiusControl.invalidated(embryo);
    }
    @Override
    public void run(String arg) {
        this.setSize(456,495);
        this.setLocationByPlatform(false);
        this.setLocation(1468, 0);
        this.setVisible(true);
    }    

    ImagedEmbryo embryo;  
    NucleusPropertiesPanel npPanel;
    RadiusControlPanel radiusControl;
    
    JButton unlink;
    JButton link;
}
