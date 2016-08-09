package cplexLib.callbacks;
 
import cplexLib.dataTypes.NodeAttachment;
import cplexLib.dataTypes.SubtreeMetaData;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static cplexLib.constantsAndParams.Constants.*;
import static cplexLib.constantsAndParams.Parameters.*;
import ilog.cplex.IloCplex.NodeId;
import java.io.IOException;
import org.apache.log4j.*; 

/**
 * 
 * @author srini
 * 
 * records solution start time for this node
 *
 */
public class NodeHandler extends IloCplex.NodeCallback{
         
    private Logger logger ;
    private boolean isLoggingInitialized =false ;
          
    //meta data of the subtree which we are monitoring
    private SubtreeMetaData treeMetaData;
    
    //keep note of when the solution time slice ends for this tree
    private double timeSliceEnd_millis ; // system millisec
    
    public NodeHandler (SubtreeMetaData metaData) throws IOException {
        this.  treeMetaData= metaData;
        
    }
    
    public void setTimeSlice(double seconds){
        timeSliceEnd_millis = THOUSAND * seconds + System.currentTimeMillis();
    }
 
    public double GetTimeSlice(){
        return timeSliceEnd_millis ; 
    }
    
    protected void main() throws IloException {
        
        //initialize logging 
        if (! isLoggingInitialized) try {
            initLogging(PARTITION_ID);
        } catch (Exception ex) {
            //logging will not be available for this class
        }
        
        if (ZERO<getNremainingNodes64()) {

                
            //get the node data for the node chosen for solving 
            NodeAttachment nodeData = (NodeAttachment) getNodeData(ZERO );
             
            if (nodeData==null ) { //it will be null for subtree root
                NodeAttachment subTreeRoot = treeMetaData.getRootNodeAttachment();
                nodeData=new NodeAttachment (   subTreeRoot.isEasy(),  
                        subTreeRoot.getUpperBounds(), 
                        subTreeRoot.getLowerBounds(),  
                        subTreeRoot.getDepthFromOriginalRoot(), 
                        ZERO, subTreeRoot.getParentsBranchingTime());         
            }

            //Mark the solution start time for this node, and end time for this solution time slice. 
            //Note that node may branch before end time of this time slice is reached
            //Note down solution time used so far 
            //This is required to cater for a node being solved across more than 1 driver cycles
            if(nodeData.getEndTimeOfCurrentSolutionTimeslice()<=ZERO){
                //first time this node picked up for solving
            }else{
                nodeData.setTimeForWhichNodeHasAlreadyBeenSolved(
                    nodeData.        getTimeForWhichNodeHasAlreadyBeenSolved()+
                    nodeData.getEndTimeOfCurrentSolutionTimeslice()
                    -nodeData.getStartTimeOfCurrentSolutionTimeslice());
                
            }
            nodeData.setStartTimeOfCurrentSolutionTimeslice(System.currentTimeMillis());
            nodeData.setEndTimeOfCurrentSolutionTimeslice(this.timeSliceEnd_millis);
            setNodeData(ZERO,nodeData);                   
                
        }        
    }//end main method

    private void initLogging(int partitionID) throws Exception{
        
        if (! isLoggingInitialized){
            
            //initialize the LOG4J logger
            logger=Logger.getLogger(this.getClass());
            logger.setLevel(Level.DEBUG);
            PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
            logger.addAppender(new RollingFileAppender(layout,LOG_FOLDER +this.getClass().getSimpleName()+partitionID+ LOG_FILE_EXTENSION ));
            logger.setAdditivity(false);
            isLoggingInitialized=true;
        }
         
    } 
}
