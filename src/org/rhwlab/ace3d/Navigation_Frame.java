package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class Navigation_Frame extends JFrame implements PlugIn,InvalidationListener {
    public Navigation_Frame(ImagedEmbryo emb,SynchronizedMultipleSlicePanel p){
        super();
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.setTitle("Navigation Trees");
        this.embryo = emb;
        this.panel = p;
        this.getContentPane().setLayout(new BorderLayout());
        
        treePanel = new NavigationTreePanel(embryo);
        JScrollPane treeScroll = new JScrollPane(treePanel);
        
        headPanel = new NavigationHeaderPanel();
        headPanel.setTreePanel(treePanel);
        treePanel.setHeadPanel(headPanel);       
        this.add(headPanel,BorderLayout.NORTH); 
        

//        MouseListener ml = new MouseAdapter() {
//            public void mousePressed(MouseEvent e) {
//                if(SwingUtilities.isRightMouseButton(e)){
//                    int selRow = lineageTree.getRowForLocation(e.getX(), e.getY());
//                    TreePath selPath = lineageTree.getPathForLocation(e.getX(), e.getY());
//                    lineageTree.setSelectionPath(selPath); 
//                    if (selRow>-1){
//                        lineageTree.setSelectionRow(selRow);
//                    }
//                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)lineageTree.getLastSelectedPathComponent();
//                    selectCell(node);
//                }
//            };
//        };
//        lineageTree.addMouseListener(ml);      
        JScrollPane rootsScroll = new JScrollPane(lineageTree);
       
        nucsRoot = new DefaultMutableTreeNode("All Nuclei",true);
        nucsTree = new JTree(nucsRoot);
        nucsTree.setCellRenderer(new NucleusRenderer());
        nucsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    
        nucsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)nucsTree.getLastSelectedPathComponent();
                selectCell(node);
            }
        });
        JScrollPane nucsScroll = new JScrollPane(nucsTree);
        Dimension prefdim = nucsScroll.getPreferredSize();
        prefdim.setSize(200, 100);
        nucsScroll.setPreferredSize(prefdim);
        
        // Terminal nuclei tree
        deathsRoot = new DefaultMutableTreeNode("Terminal Nuclei",true);  
        deathsTree = new JTree(deathsRoot);
        deathsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); 
        deathsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)deathsTree.getLastSelectedPathComponent();
                if (node == null)return;
                if (node.isLeaf()){
                    Nucleus nuc = (Nucleus)node.getUserObject();
                    embryo.setSelectedNucleus(nuc);
                    panel.changeTime(nuc.getTime());
                    panel.changePosition(nuc.getCenter());
                }                
            }
        });
        JScrollPane deathsScroll = new JScrollPane(deathsTree);   
        
        // Root tree
        allRoots = new DefaultMutableTreeNode("Roots", true);
        allRootsTree = new JTree(allRoots);
        allRootsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        allRootsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)allRootsTree.getLastSelectedPathComponent();
                if (node == null)return;
                if (node.isLeaf()){
                    Nucleus nuc = (Nucleus)node.getUserObject();
                    panel.embryo.setSelectedNucleus(nuc);                    
                }                
            }
        });        
        JScrollPane allScroll = new JScrollPane(allRootsTree); 
        
        // Sublineage tree
        lineageRoot = new DefaultMutableTreeNode("Sublineage Tree Display",true);
        lineageTree = new JTree(lineageRoot);
        lineageTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);    
        lineageTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                ChangeEvent event = new ChangeEvent(lineageTree);
                treePanel.stateChanged(event);
            }
        });
//        String[] lineages = {"P0","AB","P1","ABa","ABp","EMS","P2"};
//        //DefaultMutableTreeNode lineageNode = null;
//        for (int i = 0; i < lineages.length -1; i++) {
//            DefaultMutableTreeNode node = new DefaultMutableTreeNode(lineages[i]);
//            lineageRoot.add(node);
//        }
        lineageTree.setModel(new DefaultTreeModel(lineageRoot));
        JScrollPane lineageScroll = new JScrollPane(lineageTree);

        JSplitPane deathInactiveSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,deathsScroll,allScroll);
        prefdim = deathInactiveSplit.getPreferredSize();
        prefdim.setSize(2*prefdim.width, prefdim.height);
        deathInactiveSplit.setPreferredSize(prefdim);
        deathInactiveSplit.setDividerLocation(200);
        
        JSplitPane triSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,deathInactiveSplit,lineageScroll);
        prefdim = triSplit.getPreferredSize();
        prefdim.setSize(prefdim.width, prefdim.height);
        triSplit.setPreferredSize(prefdim);        
        triSplit.setDividerLocation(300);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,nucsScroll,triSplit);
        split.setDividerLocation(180);
        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,split,treeScroll);
        split2.setDividerLocation(400);
        this.add(split2,BorderLayout.CENTER);
        
        pack();
    }

    @Override
    public void run(String arg) {
        this.setSize(1920, 531);
        this.setLocation(0, 627);
        this.setVisible(true);
    }


    @Override
    public void invalidated(Observable observable) {
        
        // get the selected roots
        TreePath[] selectedPaths = lineageTree.getSelectionPaths();
        nucsRoot.removeAllChildren();
        allRoots.removeAllChildren();
        deathsRoot.removeAllChildren();
        lineageRoot.removeAllChildren();
        NucleusFile nucFile = embryo.getNucleusFile();
        if (nucFile == null) { 
            return;
        }
        
        // making the all nuclei tree
        int currentTime = Navigation_Frame.this.panel.getTime();
        Nucleus selectedNucleus = embryo.getNucleusFile().getSelected();
        DefaultMutableTreeNode currentTimeNode = null;
        DefaultMutableTreeNode selectedNode = null;
        Set<Integer> times = nucFile.getAllTimes();
        for (Integer time : times){
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            DefaultMutableTreeNode timeNode = null;
            if (nucFile.isCurated(time)){
                timeNode = new DefaultMutableTreeNode(String.format("%d: Curated(%d)",time,nucs.size()));
            }else {
                timeNode = new DefaultMutableTreeNode(String.format("%d: Auto(%d)",time,nucs.size()));
            }
            nucsRoot.add(timeNode);
            for (Nucleus nuc : nucs){
                DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
                timeNode.add(nucNode);  
                if (nuc.equals(selectedNucleus)){
                    selectedNode = nucNode;
                }
            }
            if (time == currentTime){
                currentTimeNode = timeNode;
            }
        }    
        DefaultTreeModel nucsModel = new DefaultTreeModel(nucsRoot);
        nucsTree.setModel(nucsModel);        
        
        // making the roots tree
        String reg = "[0-9]+";
        Pattern pattern = Pattern.compile(reg);
        TreeMap<Integer,Set<Nucleus>> rootMap = embryo.getRootNuclei();
        for (Integer t : rootMap.keySet()){
            for (Nucleus root : rootMap.get(t)){
                if (pattern.matcher(Character.toString(root.getCellName().charAt(0))).matches()) {
                    addFirstNucToNode(root, allRoots);
                } else {
                    addFirstNucToNode(root,lineageRoot);
                }
            }
        }
        allRootsTree.setModel(new DefaultTreeModel(allRoots));      
        lineageTree.setModel(new DefaultTreeModel(lineageRoot));
        
        
        // reselect the previous selected nuclei
        ArrayList<TreePath> foundList = new ArrayList<>();
        if (selectedPaths != null){
            for (TreePath path : selectedPaths){
                DefaultMutableTreeNode lastNode= (DefaultMutableTreeNode)path.getLastPathComponent();
                String nucName = ((Nucleus)lastNode.getUserObject()).getName();
                DefaultMutableTreeNode found = (DefaultMutableTreeNode)this.findNucleus(nucName, lineageRoot);
                if (found != null){
                    foundList.add(new TreePath(found.getPath()));
                }
            }
            selectedPaths = foundList.toArray(new TreePath[0]);
            lineageTree.setSelectionPaths(selectedPaths);
        }
        
        // making the terminal nuclei tree
        for (Integer time : times){
            Set<Nucleus> nucs = nucFile.getLeaves(time);
            if (!nucs.isEmpty() && time != nucFile.getAllTimes().size()){
                DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(String.format("Time:%d",time));
                deathsRoot.add(timeNode);
                for (Nucleus nuc : nucs){
                    DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
                    timeNode.add(nucNode);                
                }
            }
        }        
        deathsTree.setModel(new DefaultTreeModel(deathsRoot));
        for (int i = 0; i < deathsTree.getRowCount(); i++) {
            deathsTree.expandRow(i);
        }

        // making the inactive nuclei tree
//        for (Integer time : times){
//            Set<Nucleus> nucs=nucFile.getRemnants(time,500);
//            if (!nucs.isEmpty()){
//                DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(String.format("Time:%d",time));
//                inactiveRoot.add(timeNode);
//                for (Nucleus nuc : nucs){
//                    DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
//                    timeNode.add(nucNode);                
//                }
//            }
//        }        
//        inactiveTree.setModel(new DefaultTreeModel(inactiveRoot));        
        
        

        // make the current time visible
        TreeNode[] nodes = null;
        if (selectedNode != null && currentTime==selectedNucleus.getTime()){
            nodes = nucsModel.getPathToRoot(selectedNode);
        }        
        else if (currentTimeNode != null){           
            nodes = nucsModel.getPathToRoot(currentTimeNode);
        }
        if (nodes != null){
            TreePath path = new TreePath(nodes);
            nucsTree.setExpandsSelectedPaths(true);
            nucsTree.setSelectionPath(path);
            nucsTree.makeVisible(path);
            nucsTree.scrollPathToVisible(path);
        }
        treePanel.stateChanged(new ChangeEvent(lineageTree));
        this.invalidate();
        
    }
    private void addFirstNucToNode(Nucleus firstNuc,DefaultMutableTreeNode node){
        DefaultMutableTreeNode cellNode = new DefaultMutableTreeNode(firstNuc);
        node.add(cellNode);
        Nucleus lastNuc = firstNuc.lastNucleusOfCell();
        if (lastNuc.isDividing()){
            Nucleus[] next = lastNuc.nextNuclei();
            addFirstNucToNode(next[0],cellNode);
            addFirstNucToNode(next[1],cellNode);
        }

        
    }
    private TreeNode findNucleus(String name,DefaultMutableTreeNode node){
        Object obj = node.getUserObject();
        if (obj instanceof Nucleus){
            String nodeName = ((Nucleus)obj).getName();
            if (nodeName.equals(name)){
                return node;
            }
        }
        if (node.isLeaf()){
            return null;
        }
        for (int i=0 ; i<node.getChildCount() ; ++i){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            TreeNode ret = findNucleus(name,child);
            if (ret != null){
                return ret;
            }
        }
        return null;
    }
    
    private void selectCell(DefaultMutableTreeNode node) {
    
        if (node == null)return;
            if (node.isLeaf()){
                Nucleus nuc = (Nucleus)node.getUserObject();
                embryo.setSelectedNucleus(nuc);
                panel.changeTime(nuc.getTime());
                panel.changePosition(nuc.getCenter());
            } else{
                String timeLabel = (String)node.getUserObject();
                try {
                    int t = Integer.valueOf(timeLabel.substring(5,timeLabel.indexOf(' ')));
                    panel.changeTime(t);
                } catch (Exception exc){}
            }
    }
    static Nucleus getSelectedInactive(){
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)inactiveTree.getLastSelectedPathComponent();
        if (node == null)return null;
        if (node.isLeaf()){
            return (Nucleus)node.getUserObject();
        }  
        return null;        
    }
    public BufferedImage getTreeImage(){
        return treePanel.getCompositeImage();
    }
            
            
    ImagedEmbryo embryo;
    SynchronizedMultipleSlicePanel panel;
    NavigationHeaderPanel headPanel;
    NavigationTreePanel treePanel;
    
    //DefaultMutableTreeNode rootsRoot;
    DefaultMutableTreeNode nucsRoot;
    DefaultMutableTreeNode deathsRoot;
    //DefaultMutableTreeNode inactiveRoot;
    DefaultMutableTreeNode allRoots;
    DefaultMutableTreeNode lineageRoot;
    
    JTree nucsTree;
    JTree deathsTree;
    static JTree inactiveTree;
    JTree allRootsTree;
    JTree lineageTree;
    
    public class NucleusRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus){

            NucleusRenderer comp = (NucleusRenderer)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object obj = ((DefaultMutableTreeNode)value).getUserObject();
            if (leaf && obj instanceof Nucleus){
                nuc = (Nucleus)obj;
                if (nuc.isDividing()){
                    comp.setForeground(Color.red);
                } else if (nuc.isLeaf()){
                    comp.setForeground(Color.blue);
                } else {
                    comp.setForeground(Color.black);
                } 
            }            
            return comp;
        }
        
        Nucleus nuc;
    }
}
