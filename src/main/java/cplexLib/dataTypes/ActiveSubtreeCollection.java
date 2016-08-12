/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cplexLib.dataTypes;

import static cplexLib.constantsAndParams.Constants.LOG_FILE_EXTENSION;
import static cplexLib.constantsAndParams.Constants.THOUSAND;
import static cplexLib.constantsAndParams.Constants.ZERO;
import static cplexLib.constantsAndParams.Parameters.HARD_NODE_TIME_FACTOR; 
import static cplexLib.constantsAndParams.Parameters.LOG_FOLDER;
import static cplexLib.constantsAndParams.Parameters.MIN_TIME_SLICE_FOR_TREE_SECONDS;
import static cplexLib.constantsAndParams.Parameters.PARTITION_ID;
import cplexLib.drivers.SparkSimulatorDriver;
import ilog.cplex.IloCplex;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author srini
 */
public class ActiveSubtreeCollection   {
    
    private static Logger logger=Logger.getLogger(ActiveSubtreeCollection.class);
    
    //list of subtrees
    private List <ActiveSubtree> activeSubtreeList = new ArrayList<ActiveSubtree> ();
    //corresponding list of time slices for each subtree
    private List <Double> timeSliceList = new ArrayList<Double> ();
    
    //best known LocalSolution
    private Solution bestKnownLocalSolution  = new Solution () ;
    private double bestKnownLocalOptimum =bestKnownLocalSolution.getObjectiveValue();
    
    //for statistics
    public double totalTimeAllocatedForSolving = ZERO;
    public double totalTimeUsedForSolving = ZERO;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  FileAppender(layout,LOG_FOLDER+ActiveSubtreeCollection.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
        } catch (IOException ex) {
            ///
        }
          
    }
 
    
    //Constructor
    public ActiveSubtreeCollection (  List<NodeAttachment> attachmentList) throws  Exception  {
        for (NodeAttachment node : attachmentList){
            activeSubtreeList.add(new ActiveSubtree(node));
        }
        
    }
    public ActiveSubtreeCollection (  NodeAttachment attachment ) throws  Exception  {
         activeSubtreeList.add(new ActiveSubtree(attachment));
         
    }
    public ActiveSubtreeCollection (   ) throws  Exception  {
        
    }
    
    public void add (  NodeAttachment attachment ) throws  Exception  {
         activeSubtreeList.add(new ActiveSubtree(attachment));
    }
    
    public void add (  List<NodeAttachment> attachmentList) throws  Exception  {
        for (NodeAttachment node : attachmentList){
            activeSubtreeList.add(new ActiveSubtree(node));
        }
        
    }
    
    public int getNumberOFTrees(){
        return this.activeSubtreeList.size();
    }
    
    public  Solution getSolution (){
        return    bestKnownLocalSolution ;
    }
    
    public long getTotalLeafNodesInPartition(boolean easy) {
        int count = ZERO ;
        for (int index = ZERO; index <activeSubtreeList.size(); index ++ ){
            count+=activeSubtreeList.get(index).getNumLeafNodes(easy);
        }
        
        return count;
    }
    
    //remove completed trees and return count of active trees left
    public int cleanCompletedAndDiscardedTrees ( ) throws Exception{
        List <Integer> positionsToCull = new ArrayList <Integer> ();
        for (int index = ZERO; index <activeSubtreeList.size(); index ++ ){
            if (activeSubtreeList.get(index).isDiscardable()|| (activeSubtreeList.get(index).isSolvedToCompletion()) ) positionsToCull.add(index);
        }
        
        //get indices in decreasing order
        Collections.reverse(  positionsToCull);
        
        int size = activeSubtreeList.size();
        for (int index: positionsToCull){
            ActiveSubtree removedTree= activeSubtreeList.remove(index);
            removedTree.end();
            size --;       
        }
        
        return size;
    }
    
    //find the number of active subtrees which have no kids, i.e. only sub-tree root
    public int findNumberOfActiveSubtreesWithNoChildren () throws Exception {
        int count = 0 ;
        for (ActiveSubtree tree: activeSubtreeList){
        
             if (!tree.isDiscardable() && ! (tree.isSolvedToCompletion()) ) {
                 if (tree.getPendingChildNodeCount()==ZERO) count ++;
             }
        }
        return count;
    }
    
    public List<String> getActiveSubtreeIDs () {
        List<String>  idList = new ArrayList<String> ();
        
        for (int index = ZERO ; index< activeSubtreeList.size(); index ++ ){
            ActiveSubtree subtree = activeSubtreeList.get(index);
            idList.add(subtree.getGUID());
        }
        return idList ;
    }
    
    public List <NodeAttachmentMetadata> getNodeMetaData() {
        List <NodeAttachmentMetadata> metaDataList = new ArrayList <NodeAttachmentMetadata>();
        for (ActiveSubtree tree: activeSubtreeList){
            metaDataList.addAll(tree.getMetadataForLeafNodesPendingSolution().values());
        }
        return metaDataList;
    }
    
    
    public NodeAttachment farmOutNode (String treeGUID, String nodeID) {
        ActiveSubtree subtree = null;
        for (int index = ZERO ; index< activeSubtreeList.size(); index ++ ){
            subtree = activeSubtreeList.get(index);
            if (subtree.getGUID().equalsIgnoreCase(treeGUID)) break;
        }
        return subtree == null? null:subtree.removeUnsolvedLeafNode(nodeID);
    }
    
    public List <NodeAttachment> farmOutNodes (String treeGUID, int threshold) {
        ActiveSubtree subtree = null;
        for (int index = ZERO ; index< activeSubtreeList.size(); index ++ ){
            subtree = activeSubtreeList.get(index);
            if (subtree.getGUID().equalsIgnoreCase(treeGUID)) break;
        }
        return subtree == null? null:subtree.farmOutNodes(threshold);
    }
    
    public Solution solve  ( Instant endTimeOnWorkerMachine,         Solution bestKnownGlobalSolution , int iteratioNumber, int partitionNumber) throws Exception{
        
        
        
        logger.info(" iteration "+ iteratioNumber + " ,subtree collection " + partitionNumber +" ,solve Started at  " + Instant.now());
        
        double timeSliceForPartition = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
        this.totalTimeAllocatedForSolving += timeSliceForPartition;
        //logger.info("Solving collection  with endTimeOnWorkerMachine "+endTimeOnWorkerMachine);
       
        //update our copy
        bestKnownLocalSolution=bestKnownGlobalSolution;
        this.bestKnownLocalOptimum = bestKnownGlobalSolution.getObjectiveValue();
        
        for (int iteration = ZERO;;iteration++) {
            
            //check if time expired, or no trees left to solve
            double wallClockTimeLeft = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
            long count = countOfTreesLeftToSolve ();
            if (wallClockTimeLeft<= ZERO) break;
            if (count<= ZERO) break;
            
            if (bestKnownLocalSolution.isError() ||bestKnownLocalSolution.isUnbounded())  break;
            
            //solve  every tree once
            //logger.info("solving every tree once " );
            logger.debug("  solveEveryTreeOnce   iteration " + iteration);
            solveEveryTreeOnce (   endTimeOnWorkerMachine );
        }
        
        double timeWasted = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
        this.totalTimeUsedForSolving +=(timeSliceForPartition-timeWasted);
        
        logger.info(" iteration "+ iteratioNumber + " ,subtree collection " + partitionNumber +" ,solve Ended at  " + Instant.now());
        return this.bestKnownLocalSolution;
    }
    
    private long countOfTreesLeftToSolve () throws Exception{
        long count = ZERO ;
        for (int index = ZERO ; index< activeSubtreeList.size(); index ++ ){
            ActiveSubtree subtree = activeSubtreeList.get(index);
             if (!(subtree.isDiscardable() || subtree.isSolvedToCompletion()||subtree.isTooBig()))  count++;
        }
        return count;
    }
    
    /**
     * Solve each subtree ONCE , for some time
     * Try  to use up the  time slice supplied to the partition
     * Try to divide time fairly between each of the remaining subtrees
     * 
     * @param timeSliceInSeconds
     * @param bestKnownGlobalOptimum 
     */
    private void solveEveryTreeOnce ( Instant endTimeOnWorkerMachine  ) throws Exception{
        
        //calculate the time alloted to each subtree
        timeSliceList.clear();
        for (int index = ZERO ; index< activeSubtreeList.size(); index ++ ){
            ActiveSubtree subtree = activeSubtreeList.get(index);
            double timeSliceForThisSubtree= ZERO;
            if (!(subtree.isDiscardable() || subtree.isSolvedToCompletion()||subtree.isTooBig())) {
                timeSliceForThisSubtree= getTimeSliceForThisSubtree(    endTimeOnWorkerMachine, 
                                                                        getNumberOfTypeNodesInSubtree(subtree, true),getNumberOfTypeNodesInSubtree(subtree, false),
                                                                        getNumberOfTypeNodesInPartition (true) ,getNumberOfTypeNodesInPartition (false));
                                                                        
                
                
            }
            timeSliceList.add(timeSliceForThisSubtree);
            
        }
        
        //now solve the subtrees
        for (int index = ZERO ; index< activeSubtreeList.size(); index ++ ){
            
            if ( ZERO>= timeSliceList.get(index)) continue;
            ActiveSubtree subtree = activeSubtreeList.get(index);
            logger.debug(" solving "+subtree.getGUID() +" for "+ timeSliceList.get(index)+" seconds. Tree has this many unsolved kids " + 
                    subtree.getPendingChildNodeCount() );
            subtree.solve( timeSliceList.get(index), bestKnownLocalOptimum);
            
            Solution subTreeSolution = subtree.getSolution() ;
            if ( ZERO != (new SolutionComparator()).compare(bestKnownLocalSolution, subTreeSolution)){
                //we have found a better solution
                            
                //update our copies
                bestKnownLocalSolution = subTreeSolution;                
                bestKnownLocalOptimum = subTreeSolution.getObjectiveValue();
                logger.info("bestKnownLocalOptimum updated to "+bestKnownLocalOptimum );
                
                //we will abort solution process if error
                if (subTreeSolution.isError() ||subTreeSolution.isUnbounded())  break;
    
            }
             
        }
    } 
    
    private long getNumberOfTypeNodesInSubtree(ActiveSubtree subtree, boolean isEasy) {
        long count = ZERO ;
        Map<String, NodeAttachmentMetadata> metadataMap = subtree.getMetadataForLeafNodesPendingSolution();
        for (NodeAttachmentMetadata data : metadataMap.values()) {
            if ( isEasy==data.isEasy) count++;
        }
        return count ;
    }
    
    private long getNumberOfTypeNodesInPartition(  boolean isEasy) {
        long count = ZERO ;
        for (ActiveSubtree  subtree: activeSubtreeList ) {
            if (subtree.isDiscardable()) continue ;
            Map<String, NodeAttachmentMetadata> metadataMap = subtree.getMetadataForLeafNodesPendingSolution();
            for (NodeAttachmentMetadata data : metadataMap.values()) {
                if ( isEasy==data.isEasy) count++;
            }
        }         
        return count ;
    }
    
    //time slice for a subtree is a fraction of the remaining time left for this partition
    //the fraction is calculated using the number of leaf nodes in this subtree, divided by the number of leaf nodes left in the partition
    private double getTimeSliceForThisSubtree(Instant endTimeOnWorkerMachine, 
            long numberOfEasyNodesInSubTree, long numberOfHardNodesInSubtree,
            long easyNodesRemainingInPartition  ,long  hardNodesRemainingInPartition    ){
        
        double timeSliceForSubTree = ZERO;
        
        double wallClockTimeLeft = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
        
        if (wallClockTimeLeft >ZERO &&(ZERO<hardNodesRemainingInPartition+easyNodesRemainingInPartition) ) {
            
            //we still have some time left to solve trees on this partition
            
            timeSliceForSubTree =  wallClockTimeLeft * ( (HARD_NODE_TIME_FACTOR*numberOfHardNodesInSubtree + numberOfEasyNodesInSubTree) / 
                                                         (HARD_NODE_TIME_FACTOR *hardNodesRemainingInPartition+ easyNodesRemainingInPartition ) ) ;
            
            
            
        }  
        
        return  Math.max( timeSliceForSubTree,  MIN_TIME_SLICE_FOR_TREE_SECONDS ); 
        
    }// end method getTimeSliceForThisSubtree 
    
   
}
