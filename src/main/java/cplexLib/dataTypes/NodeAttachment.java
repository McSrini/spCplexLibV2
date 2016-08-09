package cplexLib.dataTypes;

import java.io.Serializable;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static cplexLib.constantsAndParams.Constants.*;
 

/**
 * 
 * @author srini
 *
 * This object is attached to every tree node, and can be used to reconstruct migrated leaf nodes.
 * 
 * It contains branching variable bounds, and other useful information.
 * 
 */
public class NodeAttachment implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    //every time there is a branching on a variable, we update on of these lists with the
    //new bound corresponding to the branching condition
    //
    //the key is the Variable name
    //
    //Note that this list may also have bounds on non-branching variables 
    //
    private Map< String, Double > upperBounds  = new HashMap< String, Double >();
    private Map< String, Double > lowerBounds = new HashMap< String, Double >();
    
    public NodeAttachmentMetadata nodeMetadata;
    
    //constructors    
    public NodeAttachment () {
      
        nodeMetadata = new NodeAttachmentMetadata();
    }
    
    public NodeAttachment ( boolean easy,  Map< String, Double > upperBounds, 
            Map< String, Double > lowerBounds,  int distanceFromOriginalRoot, int distanceFromSubtreeRoot, double parentsBranchingTime) {
         
        this.upperBounds = new HashMap< String, Double >();
        this.lowerBounds = new HashMap< String, Double >();
        for (Entry <String, Double> entry : upperBounds.entrySet()){
            this.upperBounds.put(entry.getKey(), entry.getValue()  );
        }
        for (Entry <String, Double> entry : lowerBounds.entrySet()){
            this.lowerBounds.put(entry.getKey(), entry.getValue()  );
        }
        
        nodeMetadata = new NodeAttachmentMetadata();
        nodeMetadata.isEasy = easy;
        nodeMetadata.distanceFromOriginalRoot=distanceFromOriginalRoot;
        nodeMetadata.distanceFromSubtreeRoot=distanceFromSubtreeRoot;
        
        nodeMetadata.timeTakenByParentToBranch =parentsBranchingTime;
    }
    

    public String toString() {
        String result = nodeMetadata.distanceFromOriginalRoot + NEWLINE;
        result += nodeMetadata.distanceFromSubtreeRoot+ NEWLINE;
        for (Entry entry : upperBounds.entrySet()) {
            result += entry.getKey()+BLANKSPACE + entry.getValue()+ NEWLINE;
                    
        }
        for (Entry entry : lowerBounds.entrySet()) {
            result += entry.getKey()+BLANKSPACE + entry.getValue()+ NEWLINE;
                    
        }
        return result;
    }
    
    public int getDepthFromOriginalRoot(  ){
        return nodeMetadata.distanceFromOriginalRoot  ;
    }
    
    public void setDepthFromSubtreeRoot(int depth ){
        this.nodeMetadata.distanceFromSubtreeRoot = depth;
    }
    
    public int getDepthFromSubtreeRoot(){
        return nodeMetadata.distanceFromSubtreeRoot  ;
    }
    
    public double getTimeTakenUntilBranching() {
        return  this.nodeMetadata.solutionTimeUsedSofar ;
    }
    public double getParentsBranchingTime() {
        return nodeMetadata.timeTakenByParentToBranch ;
    }
    
    public double getStartTimeOfCurrentSolutionTimeslice() {        
        return this.nodeMetadata.startTimeOfCurrentSolutionTimeslice;
    }
    public double getEndTimeOfCurrentSolutionTimeslice() {        
        return this.nodeMetadata.endTimeOfCurrentSolutionTimeslice;
    }
    
    public void setStartTimeOfCurrentSolutionTimeslice(double time ) {        
          this.nodeMetadata.startTimeOfCurrentSolutionTimeslice=time;
    }
    public void setEndTimeOfCurrentSolutionTimeslice(double time) {        
         this.nodeMetadata.endTimeOfCurrentSolutionTimeslice=time;
    }
    public double getTimeForWhichNodeHasAlreadyBeenSolved() {        
        return this.nodeMetadata.solutionTimeUsedSofar;
    }
    
    public void setTimeForWhichNodeHasAlreadyBeenSolved(double time ) {        
          this.nodeMetadata.solutionTimeUsedSofar=time;
    }
    
    public void setEasy(){
        nodeMetadata.isEasy = true;
    }
    
    public boolean isEasy(){
        return nodeMetadata.isEasy  ;
    }

    public Map< String, Double >   getUpperBounds   () {
        return  upperBounds ;
    }

    public Map< String, Double >   getLowerBounds   () {
        return  lowerBounds ;
    }

    
    
}
