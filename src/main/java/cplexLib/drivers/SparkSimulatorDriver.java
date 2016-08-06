/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cplexLib.drivers;

import static cplexLib.constantsAndParams.Constants.*;
import static cplexLib.constantsAndParams.Parameters.LOG_FOLDER;
import static cplexLib.constantsAndParams.Parameters.MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE;
import static cplexLib.constantsAndParams.Parameters.PARTITION_ID;
import cplexLib.dataTypes.*;
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
 * 
 * pretend line we have P partitions and solve each one for the same time slice
 * 
 * report the final solution, and % time wasted on each partition
 * 
 */
public class SparkSimulatorDriver {
    
    private static Logger logger=Logger.getLogger(SparkSimulatorDriver.class);
    final static int ITER_TIME= 2*60 ;// 2 minutes
    final static int NUM_PARTITIONS= 39 ;// 
    final static int  MAX_ITERATIONS  =   THOUSAND  ; 
    
    
    //pretend we have 39 bags, one in each partition
    private static    List<ActiveSubtreeCollection> partitionList = new ArrayList<ActiveSubtreeCollection>();
    
    public SparkSimulatorDriver() {
        
        
         

                
    }
    
    public static void main(String[] args) throws Exception {
        int iteration = ZERO;
        Solution incumbent = new Solution();
        
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        logger.addAppender(new FileAppender(layout,LOG_FOLDER+SparkSimulatorDriver.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
         
        
        
        for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
            if (index==ZERO){
                partitionList.add(new ActiveSubtreeCollection(new NodeAttachment()));
            }else {
                partitionList.add(new ActiveSubtreeCollection());
            }
        }
        
        //solve these 39 bags forever , until all bags are empty
        for (;iteration<MAX_ITERATIONS;iteration++){
            if (allPartitionsEmpty()) break;
            
            //abort computation in case of error
            if (incumbent.isError() || incumbent.isUnbounded()) break;
            
            logger.info("Starting iteration "+iteration);
            
            //solve every partition for time slice
            MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE= getMAxKidsPerTree(iteration);
            for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
                ActiveSubtreeCollection astc = partitionList.get(index);
                logger.info("Solving partition "+index + " having "+astc.getNumberOFTrees() + " trees");
                astc.solve(Instant.now().plusMillis(THOUSAND*getTimeSlice(iteration)) , incumbent, iteration,index );
                
                 
                //clean up partition
                astc.cleanCompletedAndDiscardedTrees();
            }
            
            for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
                Solution partitionSolution = partitionList.get(index).getSolution() ;
                if ( ZERO != (new SolutionComparator()).compare(incumbent, partitionSolution)){
                    //we have found a better solution

                    //update our copies
                    incumbent = partitionSolution;  
                    logger.info("Best incumbent updated to "+incumbent.getObjectiveValue());
                    
                    //we will abort solution process if error
                    if (incumbent.isError() ||incumbent.isUnbounded())  break;

                }

            }
            
            
            //we will abort solution process if error
            if (incumbent.isError() ||incumbent.isUnbounded())  break;

            
            //farm and redistribute nodes, unless all trees are done
            //
              
            int treesLeft = ZERO;
            for ( ActiveSubtreeCollection astc : partitionList){
                 treesLeft +=astc.cleanCompletedAndDiscardedTrees();
                 logger.info("Number of tree , easy nodes, hard nodes, in partition is " +astc.getNumberOFTrees() + " ," + astc.getTotalLeafNodesInPartition(true)
                 + " ," + astc.getTotalLeafNodesInPartition(false));
            }
          
            
            //we only farm at the end of iter 0
            if (ZERO==iteration && treesLeft>ZERO) {
                dofarming();
            }
            
            
        }
        
        //print solution
         logger.info("Best soln found "+incumbent);
         
        //print time usage % for every partition
        for ( ActiveSubtreeCollection astc : partitionList){
            logger.info("Time slice utilization percent = " + TEN*TEN*astc.totalTimeUsedForSolving/astc.totalTimeAllocatedForSolving);
        }
        
    }
    
    //remove all nodes from tree 0 on partition 0 , and distribute randomly across all partitions
    private static void dofarming() throws Exception{
        int nextpartition = ZERO;
        
        ActiveSubtreeCollection astc =  partitionList.get(ZERO);
        String guid = astc.getActiveSubtreeIDs().get(ZERO);
        
        List <NodeAttachment> farmOutNodes = astc.farmOutNodes(guid, ZERO);
        int countOfFarmOutNodes = farmOutNodes.size();
        while (countOfFarmOutNodes > ZERO){
            NodeAttachment node = farmOutNodes.remove(countOfFarmOutNodes-ONE);
            //add it to a random partition
            partitionList.get((nextpartition++)%NUM_PARTITIONS).add(node);
            countOfFarmOutNodes--;
        }
    }
    
    private static boolean allPartitionsEmpty() throws Exception {
        boolean retval = true;
        for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
            if ( ZERO < partitionList.get(index).cleanCompletedAndDiscardedTrees()) retval = false;
        }
        return retval;
    }
    
    private static int getTimeSlice (int iter){
        return iter == ZERO ? ITER_TIME : ITER_TIME*TWO;
    }
    private static int getMAxKidsPerTree (int iter){
        return iter == ZERO ? MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE : MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE*TEN*TEN*THOUSAND;
    }
}
