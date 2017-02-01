/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.ImageJ;
import ij.macro.Interpreter;
import ij.plugin.PlugIn;
import ij.process.LUT;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.ace3d.dialogs.BHCSubmitDialog;
import org.rhwlab.ace3d.dialogs.BHCTreeCutDialog;
import org.rhwlab.ace3d.dialogs.PanelDisplay;
import org.rhwlab.dispim.HDF5DirectoryImageSource;
import org.rhwlab.dispim.ImageJHyperstackSource;
import org.rhwlab.dispim.ImageSource;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.TifDirectoryImageSource;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.BHCDirectory;
import org.rhwlab.dispim.nucleus.LinkedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class Ace3D_Frame extends JFrame implements PlugIn,ChangeListener  {
    public  Ace3D_Frame()  {
        
        this.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e){
 //               slicePanel.requestFocusInWindow();
                Ace3D_Frame.this.toFront();
                Ace3D_Frame.this.requestFocus();
            }            
        });
        
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        imagedEmbryo = new ImagedEmbryo();
        TimePointImage.setEmbryo(imagedEmbryo);
        
        contrastDialog = new DataSetsDialog(this,0,Short.MAX_VALUE);
        contrastDialog.setVisible(true);
        
        panel = new SynchronizedMultipleSlicePanel(this,3);
        panel.setEmbryo(imagedEmbryo);
//        imagedEmbryo.addListener(panel);
        this.add(panel);
        
        navFrame = new Navigation_Frame(imagedEmbryo,panel);       
        imagedEmbryo.addListener(navFrame);
        
        selectedNucFrame = new SelectedNucleusFrame(this,imagedEmbryo);
//        selectedNucFrame.setVisible(true);        
        buildMenu();
        this.pack();
   
        String homeDir = System.getProperty("user.home");
        File propFile = new File(homeDir,"Ace3D_Frame.properties");
        props.open(propFile.getPath());
        this.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                try {
                    close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        imagedEmbryo.getNucleusFile().addListener(navFrame);
        imagedEmbryo.getNucleusFile().addSelectionOberver(selectedNucFrame);
        imagedEmbryo.getNucleusFile().addSelectionOberver(panel); 
        
        viPlot = new VolumeIntensityPlot(imagedEmbryo);
        viDialog = new PanelDisplay(viPlot);
    }

    public void close() throws Exception {
        props.save();
        System.exit(0);
    }
    @Override
    public void run(String string) {
        this.setSize(1464,627);
        this.setVisible(true);
        navFrame.run(null);
        this.selectedNucFrame.run(null);
    }
    

    final void buildMenu(){
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        JMenuItem mvr = new JMenuItem("Open Series Directory");
        fileMenu.add(mvr);
        mvr.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String vsProp = props.getProperty("MVRDir");
                if (vsProp != null){
                    dirChooser.setSelectedFile(new File(vsProp));
                } 
                if (dirChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){   
                    File sel = dirChooser.getSelectedFile();
                    File mvrDir = new File(sel,"MVR_STACKS");
                    
                    HDF5DirectoryImageSource source = new HDF5DirectoryImageSource(mvrDir,"exported_data","Segmented",imagedEmbryo,false);
                    boolean segFound = source.open();
                    if (segFound){
                        
                        panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                            ,Math.min(source.getMaxTime(),panel.getMaxTime()) ); 

                        int lineChannel = source.getChannel();
                        int expChannel = 1;
                        if (lineChannel == 1){
                            expChannel = 2;
                        }

                        File typical = new File(mvrDir,String.format("TP1_Ch%d_Ill0_Ang0,90.tif",lineChannel));
                        TifDirectoryImageSource lineSource = new TifDirectoryImageSource(typical.getPath(),"Lineaging",imagedEmbryo,true);             
                        panel.setTimeRange(Math.max(lineSource.getMinTime(),panel.getMinTime())
                            ,Math.min(lineSource.getMaxTime(),panel.getMaxTime()) ); 

                        typical = new File(mvrDir,String.format("TP1_Ch%d_Ill0_Ang0,90.tif",expChannel));
                        TifDirectoryImageSource expSource = new TifDirectoryImageSource(typical.getPath(),"Expressing",imagedEmbryo,false);             
                        panel.setTimeRange(Math.max(expSource.getMinTime(),panel.getMinTime())
                            ,Math.min(expSource.getMaxTime(),panel.getMaxTime()) );    

                        bhc  = new BHCDirectory(new File(sel,"BHC"));
                        imagedEmbryo.getNucleusFile().setBHCTreeDirectory(bhc);                    

                        imagedEmbryo.notifyListeners();                    
                        props.setProperty("MVRDir",sel.getPath());
                    }else {
                        imagedEmbryo.clearSources();
                        JOptionPane.showMessageDialog(Ace3D_Frame.this, "No Segmentation probability hdf5 files found");
                    }
                }             
            }
        });
        fileMenu.addSeparator();
        
        JMenuItem virtStack = new JMenuItem("Open Lineaging TIFF Virtual Stack");
        virtStack.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String vsProp = props.getProperty("VirtualStack");
                if (vsProp != null){
                    fileChooser.setSelectedFile(new File(vsProp));
                } 
                if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){    
                    File sel = fileChooser.getSelectedFile();
                    String timeStr = setMinTime();
                    if (timeStr != null){
                        ImageJHyperstackSource source = new ImageJHyperstackSource(sel,Integer.valueOf(timeStr),"Lineaging",imagedEmbryo); 
                        panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                            ,Math.min(source.getMaxTime(),panel.getMaxTime()) );  
                        imagedEmbryo.notifyListeners();
                        props.setProperty("VirtualStack",sel.getPath());
                    }
                }                
            }
        });
        fileMenu.add(virtStack);
        
        JMenuItem segTifDir = new JMenuItem("Open Segmented TIF Directory");
        segTifDir.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String stProp = props.getProperty("SegTiffs");
                if (stProp != null){
                    fileChooser.setSelectedFile(new File(stProp));
                } 
                if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){    
                    File sel = fileChooser.getSelectedFile();
                    TifDirectoryImageSource source = new TifDirectoryImageSource(sel.getPath(),"Segmented",imagedEmbryo,true);
                        panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                            ,Math.min(source.getMaxTime(),panel.getMaxTime()) );
                    imagedEmbryo.notifyListeners();
                    props.setProperty("SegTiffs",sel.getPath());
                }                 
            }
        });
        fileMenu.add(segTifDir);
        
        JMenuItem bhcOpen = new JMenuItem("Open BHC Trees ");
        bhcOpen.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openBHCDir(null);
                    imagedEmbryo.notifyListeners();
                    
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(bhcOpen); 
        fileMenu.addSeparator();
        
        JMenuItem session = new JMenuItem("Open Existing Session");
        fileMenu.add(session);
        session.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String prop = props.getProperty("Session");
                if (prop != null){
                    fileChooser.setSelectedFile(new File(prop));
                }
                if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){    
                    File sel = fileChooser.getSelectedFile();
                    try {
                        openSession(sel);
                    } catch (Exception exc){
                        exc.printStackTrace();
                    }
                    props.setProperty("Session",sel.getPath());
                }
            }
        });
        
        JMenuItem saveAsSession = new JMenuItem("Save As Current Session");
        fileMenu.add(saveAsSession);
        saveAsSession.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    saveAsSession();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });  
        
        JMenuItem saveSession = new JMenuItem("Save Current Session");
        fileMenu.add(saveSession);
        saveSession.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (sessionXML != null){
                        saveSession(sessionXML);
                    }else {
                        saveAsSession();
                    }

                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.addSeparator();
/*        
        
        JMenuItem hyper = new JMenuItem("Import ImageJ Hyperstack");
        hyper.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String timeStr = setMinTime();
                if (timeStr != null){
                    source = new ImageJHyperstackSource();
                    source.open();
                    initToSource();
                    int firstTime = Integer.valueOf(timeStr);
                    source.setFirstTime(firstTime);
                    panel.setTimeRange(firstTime, source.getMaxTime());                    
                }
            }
        });
        fileMenu.add(hyper);

        JMenuItem superVoxel = new JMenuItem("Open TGMM SuperVoxel Binary Files");
        superVoxel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String svFile = props.getProperty("TGMMSuperVoxelBinaryFile");
                if (svFile != null){
                    sourceChooser.setSelectedFile(new File(svFile));
                } 
                if (sourceChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
                    props.setProperty("TGMMSuperVoxelBinaryFile",sourceChooser.getSelectedFile().getPath()); 
                    source = new TGMMSuperVoxelSource(sourceChooser.getSelectedFile().getPath());
                    source.open();
                    initToSource();
                }
                int fhsduis=0;
            }
        });
        fileMenu.add(superVoxel);
       
        JMenuItem open = new JMenuItem("Open Images from HDF5");
        open.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String hdf5File = props.getProperty("HDF5ImageFile");
                if (hdf5File != null){
                    ij.io.OpenDialog.setDefaultDirectory(new File(hdf5File).getParent());
                }
                source = new Hdf5ImageSource();
                if (source.open()){
                    props.setProperty("HDF5ImageFile", source.getFile());
                    initToSource();
                }
            }
        });
        fileMenu.add(open);
        

*/        
        JMenuItem calcExp = new JMenuItem("Calculate Expression");
        calcExp.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Ace3D_Frame.this.imagedEmbryo.calculateExpression();
            }
        });
        fileMenu.add(calcExp);
        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    close();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        fileMenu.add(exit);
      
        JMenu navigate = new JMenu("Navigate");
        JMenuItem toTime = new JMenuItem("To Time Point");
        toTime.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                moveToTime();
            }
        });
        navigate.add(toTime);
        
        menuBar.add(navigate);
        
        JMenu segmenting = new JMenu("Segmenting");
        menuBar.add(segmenting);
        
        JMenuItem cutItem = new JMenuItem("Current Time Point");
        cutItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    cutTree();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });       
        segmenting.add(cutItem); 
        
        JMenuItem autoSeg = new JMenuItem("Auto - Between Curations");
        segmenting.add(autoSeg);
        autoSeg.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
 //                   ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoSegmentBetweenCuratedTimes(getCurrentTime());
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });

        segmenting.addSeparator();
     
        JMenuItem scatter = new JMenuItem("Intensity/Volume Plot");
        scatter.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    viDialog.setVisible(true);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        segmenting.add(scatter);

        JMenuItem lineplot = new JMenuItem("Nuclei Probability Plot");
        lineplot.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    
                    int time = getCurrentTime();
                    TreeMap<Integer,BHCTree> trees = bhc.getTrees(time);
                    BHCTree tree = null;
                    if (trees.size() > 1){
                        Object resp = JOptionPane.showInputDialog(null,"Choose segmentation threshold","thresholds ",JOptionPane.INFORMATION_MESSAGE,
                            null, trees.keySet().toArray(),trees.firstKey());
                        tree = trees.get(resp);
                    } else {
                        tree = trees.firstEntry().getValue();
                    }
                    SegmentationLinePlot plot = new SegmentationLinePlot();
                    plot.setTree(tree);
                    PanelDisplay dialog = new PanelDisplay(plot);
                    dialog.setVisible(true);    
                   
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });        
        segmenting.add(lineplot);
        segmenting.addSeparator();
        
        JMenuItem remove = new JMenuItem("Remove Nuclei - Current Time");
        remove.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               Set<Nucleus> nucs = imagedEmbryo.getNuclei(getCurrentTime());
               for (Nucleus nuc : nucs){
                   imagedEmbryo.getNucleusFile().removeNucleus(nuc,false);
               }
               imagedEmbryo.notifyListeners();
            }
        });
        
        JMenuItem removeRange = new JMenuItem("Remove Nuclei - Time Range");
        removeRange.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                int timeTo = -1;
                while (timeTo == -1){
                    String ans = JOptionPane.showInputDialog("Enter last time to delete");
                    if (ans == null) return;
                    try {
                        timeTo = Integer.valueOf(ans.trim());
                    } catch (Exception exc){
                        JOptionPane.showMessageDialog(rootPane,"invalid entry");
                    }
                }
                for (int t=getCurrentTime();t<=timeTo;++t){
                    Set<Nucleus> nucs = imagedEmbryo.getNuclei(t);
                    for (Nucleus nuc : nucs){
                        imagedEmbryo.getNucleusFile().removeNucleus(nuc,false);
                    }
                }
               imagedEmbryo.notifyListeners();
            }
        }); 
        segmenting.add(remove);
        segmenting.add(removeRange);
        JMenuItem removeAll = new JMenuItem("Remove All Nuclei");
        removeAll.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Integer[] times = imagedEmbryo.getNucleusFile().getAllTimes().toArray(new Integer[0]);
                for (int i=0 ; i<times.length ; ++i){
                   Set<Nucleus> nucs = imagedEmbryo.getNuclei(times[i]);
                   for (Nucleus nuc : nucs){
                       imagedEmbryo.getNucleusFile().removeNucleus(nuc,false);
                   }                    
                }
                imagedEmbryo.notifyListeners();
            }
            
        });
        segmenting.add(removeAll);  
        segmenting.addSeparator();
        
        JMenuItem submitBHC = new JMenuItem("Submit to Grid");
        segmenting.add(submitBHC);
        submitBHC.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bhc.getDirectory() != null){
                    
                    int[] dims = TimePointImage.getIntDims();
                    submitDialog = new BHCSubmitDialog(Ace3D_Frame.this,bhc.getDirectory().getParent(),dims);
                    submitDialog.setVisible(true);
                }
            }
        });
/*        
        JMenuItem outlierItem = new JMenuItem("Resegment Outliers");
        outlierItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
//                    cutTreeOutlier();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });        
        selectedTimePoint.add(outlierItem);
 */       

        
        JMenu linking = new JMenu("Linking");
        menuBar.add(linking);
/*        
        JMenuItem linkItem = new JMenuItem("Auto Link");
        linking.add(linkItem);
        linkItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int fromTime = -1;
                    while (fromTime == -1){
                        try {
                            String ans = JOptionPane.showInputDialog("Enter time to link back to");
                            if (ans == null){
                                return;
                            }
                            fromTime = Integer.valueOf(ans);
                        } catch (Exception exc){}
                    }
                    ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoLink(fromTime, getCurrentTime());
                    
//                    ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoLinkBetweenCuratedTimes(getCurrentTime());
                } catch (Exception exc){
                    exc.printStackTrace();
                }
                
            }
        });
*/
        JMenuItem searchlinkItem = new JMenuItem("Auto Link - Tree Search");
        linking.add(searchlinkItem);
        searchlinkItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int fromTime = -1;
                    while (fromTime == -1){
                        try {
                            String ans = JOptionPane.showInputDialog("Enter time to link back to");
                            if (ans == null){
                                return;
                            }
                            fromTime = Integer.valueOf(ans);
                        } catch (Exception exc){}
                    }
                    TreeMap<Integer,Integer> probMap = mapTimesToThreshProbs(fromTime,getCurrentTime());
                    Set<Integer> timesSet = probMap.navigableKeySet();
                    Integer[] timesArray = timesSet.toArray(new Integer[0]);
                    Integer[] probsArray = new Integer[timesArray.length];
                    for (int i=0 ; i<timesArray.length;++i){
                        probsArray[i] = probMap.get(timesArray[i]);
                    }
                    ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).bestMatchAutoLink(timesArray,probsArray);
                    
//                    ((LinkedNucleusFile)imagedEmbryo.getNucleusFile()).autoLinkBetweenCuratedTimes(getCurrentTime());
                } catch (Exception exc){
                    exc.printStackTrace();
                }
                
            }
        });        
        JMenuItem unlink = new JMenuItem("Unlink Current Time");
        linking.add(unlink);
        unlink.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Set<Nucleus> nucs = imagedEmbryo.getNucleusFile().getNuclei(getCurrentTime());
                NucleusFile nucFile = imagedEmbryo.getNucleusFile();
                int count = 1;
                for (Nucleus nuc : nucs){
                    nucFile.unlinkNucleus(nuc, count == nucs.size());  // notify listeners at last nucleus
                    ++count;
                }
            }
        });        
        JMenu view = new JMenu("Annotations");
        menuBar.add(view);
        
        segmentedNuclei = new JCheckBoxMenuItem("Nuclei indicator");
        segmentedNuclei.setSelected(true);
        segmentedNuclei.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });
        view.add(segmentedNuclei);
        
        divisionIndicator = new JCheckBoxMenuItem("Division indicator");
        divisionIndicator.setSelected(true);
        divisionIndicator.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(divisionIndicator);
        
        sisters = new JCheckBoxMenuItem("Sister indicator");
        sisters.setSelected(false);
        sisters.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(sisters);

        locationIndicator = new JCheckBoxMenuItem("Location indicator");
        locationIndicator.setSelected(true);
        locationIndicator.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(locationIndicator);
        
        selectedLabeled = new JCheckBoxMenuItem("Label the Selected Nucleus");
        selectedLabeled.setSelected(false);
        selectedLabeled.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(selectedLabeled);
        
        nucleiLabeled = new JCheckBoxMenuItem("Label All the Nuclei");
        nucleiLabeled.setSelected(false);
        nucleiLabeled.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
            }
        });        
        view.add(nucleiLabeled);
        this.setJMenuBar(menuBar);        
    }
    private TreeMap<Integer,Integer> mapTimesToThreshProbs(int fromTime,int toTime){
        // determine which probabilities to use for each time point
        TreeMap<Integer,Integer> map = new TreeMap();
        for (int t=fromTime ; t<=toTime ; ++t){
            Set<Integer> probs = bhc.getThresholdProbs(t);
            Integer[] probArray = probs.toArray(new Integer[0]);
            if (probs.size()==1){
                map.put(t,probArray[0]);
            } else {
                // prompt for which to use
                Object resp = JOptionPane.showInputDialog(null,
                        String.format("For time = %d", t),"Choose probability threshold", JOptionPane.INFORMATION_MESSAGE, null, probArray, probArray[0]);
                if (resp == null) return null;
                map.put(t, (Integer)resp);
            }
        }   
        return map;
    }

    private void moveToTime(){
        boolean valid = false;
        while (!valid) {
            String timeStr = JOptionPane.showInputDialog("Enter the time:");
            if (timeStr == null) return;
            try {
                int time = Integer.valueOf(timeStr);
                panel.changeTime(time);
                valid = true;
            } catch (Exception exc){
                JOptionPane.showMessageDialog(this,"Not a valid time entry");
            }
        }
    }
    private String setMinTime(){
        String ret = null;
        boolean valid = false;
        while (!valid) {
            String timeStr = JOptionPane.showInputDialog("Enter the first time value:");
            if (timeStr == null) return ret;  // null return means user cancelled input
            try {

                valid = true;
                ret = timeStr;
                
            } catch (Exception exc){
                JOptionPane.showMessageDialog(this,"Not a valid time entry");
            }
        }
        return ret;        
    }
    public int getCurrentTime(){
        return panel.getTime();
    }
/*    
    private void openNucFile()throws Exception {
        String f = props.getProperty("NucFile");
        if (f != null){
            nucChooser.setSelectedFile(new File(f));
        }         
        if (nucChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            nucFile = new NamedNucleusFile(nucChooser.getSelectedFile());
            nucFile.open();

            if (imagedEmbryo != null){
                imagedEmbryo.setNucleusFile(nucFile);
            }            
            nucFile.addListener(navFrame);
            nucFile.addSelectionOberver(selectedNucFrame);
            nucFile.addSelectionOberver(panel);            
            
            props.setProperty("NucFile",nucFile.getFile().getPath());            

        }
    }
*/    
    // starting from BHC directory, no nucleus file exists yet
    private void openBHCDir(File sel)throws Exception {
        if (sel == null){
            String bhcProp = props.getProperty("BHC");
            if (bhcProp != null){
                fileChooser.setSelectedFile(new File(bhcProp));
            } 
            if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
                sel = fileChooser.getSelectedFile();

            }            
        }
        
        bhc  = new BHCDirectory(sel);
        imagedEmbryo.getNucleusFile().setBHCTreeDirectory(bhc);

        props.setProperty("BHC",sel.getPath());            
        
    } 
    private void openSession(File xml)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(xml);
        Element root = doc.getRootElement();   
        if (!root.getName().equals("Ace3DSession")){
            return;
        }
        
        Element bhcEle = root.getChild("BHCTreeDirectory");
        if (bhcEle != null){
            openBHCDir(new File(bhcEle.getAttributeValue("path")));
        }
        imagedEmbryo.fromXML(root.getChild("ImagedEmbryo"));
        for (ImageSource source : imagedEmbryo.getSources()){
            panel.setTimeRange(Math.max(source.getMinTime(),panel.getMinTime())
                ,Math.min(source.getMaxTime(),panel.getMaxTime()) );
        }
        imagedEmbryo.notifyListeners();  
        
        Element dsEle = root.getChild("DataSets");
        for (Element props : dsEle.getChildren("DataSetProperties")){
            String name = props.getAttributeValue("Name");
            DataSetProperties p = new DataSetProperties(props);
            dataSetProperties.put(name, p);
            contrastDialog.setProperties(name, p);
        }
        this.sessionXML = xml;
        
    }
    private void saveAsSession()throws Exception {
        JFileChooser sessionChooser=null;        
        if (sessionXML == null){

            if (bhc == null){
                sessionChooser = new JFileChooser();
            } else {
                sessionChooser = new JFileChooser(bhc.getDirectory());
            }

        }else {
            sessionChooser = new JFileChooser(sessionXML);
        }
        if (sessionChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
            saveSession(sessionChooser.getSelectedFile());
        } else {
            return;
        }                    
    }
    private void saveSession(File xml)throws Exception {
        
        Element root = new Element("Ace3DSession");
        if (bhc != null){
            root.addContent(bhc.toXML());
        }
       
        root.addContent(imagedEmbryo.toXML());
        
        Element dsProps = new Element("DataSets");
        for (String ds : dataSetProperties.keySet()){
            DataSetProperties props = dataSetProperties.get(ds);
            Element dsEle = props.toXML();
            dsEle.setAttribute("Name", ds);
            dsProps.addContent(dsEle);
        }
        root.addContent(dsProps);
        
        OutputStream stream = new FileOutputStream(xml);       
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();
        props.setProperty("Session",xml.getPath());
        sessionXML = xml;

    }
    
    private void submitAllTimePoints()throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        String segTiff = props.getProperty("SegmentedTIFF");
        if (segTiff != null){
            fileChooser.setSelectedFile(new File(segTiff));
        }
        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION){
            segTiff = fileChooser.getSelectedFile().getPath();
//            Nuclei_Identification.submitTimePoints(segTiff,1,2);
            props.setProperty("SegmentedTIFF",segTiff);
        }
    }
    private void cutTree()throws Exception {
        int time = this.getCurrentTime();
        BHCDirectory bhcTree = imagedEmbryo.getNucleusFile().getTreeDirectory();
        if (treeCutDialog == null){
            treeCutDialog = new BHCTreeCutDialog(this,this.imagedEmbryo);
        }
        treeCutDialog.setBHCTrees(bhcTree,time);
        treeCutDialog.setVisible(true);
        if (treeCutDialog.isOK()){
            double nextThresh = treeCutDialog.getThresh();
 //           bhcNucFile.setThreshold(nextThresh);
        }
    }
/*
    private void cutTreeOutlier()throws Exception {
        int time = this.getCurrentTime();
        BHCNucleusDirectory bhc = ((Ace3DNucleusFile)nucFile).getBHC();
        BHCNucleusFile bhcNucFile = bhc.getFileforTime(time); 
        BHCTree tree = imagedEmbryo.getBHCTree(time);
        BHCNucleusFile replace = bhcNucFile.cutTreeOutlier(tree);
        if (replace != null){
            ((Ace3DNucleusFile)this.nucFile).replaceTime(replace);
        }
    }
    private void saveAsNucFile()throws Exception {
        buildChooser();
        if (nucFile != null && nucFile.getFile()!=null){
            nucChooser.setSelectedFile(nucFile.getFile().getParentFile());
        } else {
            java.util.Properties pros = System.getProperties();
            Map<String,String> map = System.getenv();
            nucChooser.setSelectedFile(new File(System.getProperty("user.home")));
        }
        if (nucChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION){
            File f = nucChooser.getSelectedFile();
            if (nucFile.getFile()!=null && f.getPath().equals(nucFile.getFile().getPath())){

                if (JOptionPane.showConfirmDialog(rootPane,"Replace the original nucleus file?")==JOptionPane.OK_OPTION){
                    nucFile.saveAs(f);
                }
                
            } else {
                nucFile.saveAs(nucChooser.getSelectedFile());
            }
        }
    }
 */   


/*
    private void buildLutMenu(){
        lutMenu.removeAll();
        Iterator<DataSetDesc> dataSetIter = source.getDataSets().iterator();
        while(dataSetIter.hasNext()){
            ButtonGroup buttonGroup = new ButtonGroup();
            String dataset = dataSetIter.next().getName();
            JMenu dataSetMenu = new JMenu(dataset);
            lutMenu.add(dataSetMenu);
            Set<String> lutNames = lookUpTables.getLutNames();
            JCheckBoxMenuItem[] lutItems = new JCheckBoxMenuItem[lutNames.size()];
            int i=0;
            for (String lutName : lutNames){
                
                lutItems[i] = new JCheckBoxMenuItem(lutName);
                lutItems[i].setName(dataset);
                lutItems[i].addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JCheckBoxMenuItem checked = (JCheckBoxMenuItem)e.getSource();
                        if (checked.getState()){

                            dataSetLuts.put(checked.getName(),lookUpTables.getLUT(checked.getText()));
                            panel.showCurrentImage();
                        }
                    }
                });
                dataSetMenu.add(lutItems[i]);
                buttonGroup.add(lutItems[i]);
                if (lutName.equalsIgnoreCase("Gray")){
                    lutItems[i].setSelected(true);
                }
                ++i;
            }
            dataSetLuts.put(dataset,lookUpTables.getLUT("Gray"));
        }
    }
*/

    @Override
    public void stateChanged(ChangeEvent e) {
        panel.stateChanged(e);
    }   

    static public boolean labelNuclei(){
        return nucleiLabeled.getState();
    }
    static public boolean labelSelectedNucleus(){
        return selectedLabeled.getState();
    }
    static public boolean sistersIndicated(){
        return sisters.getState();
    }
    static public boolean divisionsIndicated(){
        return divisionIndicator.getState();
    }    
    static public boolean nucleiIndicated(){
        return segmentedNuclei.getState();
    }
    static public boolean locationIndicated(){
        return locationIndicator.getState();
    }

    static public List<String> datasetsSelected(){
        ArrayList<String> ret = new ArrayList<>();
        for (String key : dataSetProperties.keySet()){
            DataSetProperties props = dataSetProperties.get(key);
            if (props.selected){
                ret.add(key);
            }
        }
        return ret;
    }
    static public List<String> getAllDatsets(){
        ArrayList<String> ret = new ArrayList<>();
        for (String key : dataSetProperties.keySet()){
            ret.add(key);
        }
        return ret;        
    }
    static public DataSetProperties getProperties(String dataSet){
        return dataSetProperties.get(dataSet);
    }
    static public void setProperties(String dataset,DataSetProperties ps){
        dataSetProperties.put(dataset, ps);
    }
    
    static public LUT getLUT(String dataSet){
        return dataSetLuts.get(dataSet);
    }
    static public DataSetsDialog getDataSetsDialog(){
        return contrastDialog;
    }
    public ImagedEmbryo getEmbryo(){
        return this.imagedEmbryo;
    }
    
    File sessionXML;
    Properties props = new Properties();
    JMenu dataset;
    JMenu contrast;
    JMenu lutMenu;
    JMenu colorMenu;
    
    ImagedEmbryo imagedEmbryo;
    
    SynchronizedMultipleSlicePanel panel;
    SelectedNucleusFrame selectedNucFrame;
    JFileChooser fileChooser = new JFileChooser();
    JFileChooser dirChooser = new JFileChooser();
    static DataSetsDialog contrastDialog;
    Navigation_Frame navFrame;
    LookUpTables lookUpTables = new LookUpTables();
    BHCTreeCutDialog treeCutDialog;
    BHCSubmitDialog submitDialog;
    BHCDirectory bhc;
    VolumeIntensityPlot viPlot;
    PanelDisplay viDialog;
    
    static JCheckBoxMenuItem segmentedNuclei;
    static JCheckBoxMenuItem sisters;
    static JCheckBoxMenuItem locationIndicator;
    static JCheckBoxMenuItem divisionIndicator;
    static JCheckBoxMenuItem nucleiLabeled;
    static JCheckBoxMenuItem selectedLabeled;
    static JMenuItem[] colorChoices;
    static TreeMap<String,LUT> dataSetLuts = new TreeMap<>();
    
    static TreeMap<String,DataSetProperties> dataSetProperties = new TreeMap<>();
    
    static public void main(String[] args) {
        EventQueue.invokeLater(new Runnable(){
            @Override
            public void run() {
                Interpreter.batchMode=true;
                ImageJ ij = new ImageJ(ImageJ.NO_SHOW);
                try {
                    Ace3D_Frame frame = new Ace3D_Frame();
                    
                    frame.run(null);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
    }     


}
