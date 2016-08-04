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
    
    public NodeHandler (SubtreeMetaData metaData) throws IOException {
        this.  treeMetaData= metaData;
        
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
                        ZERO);         
            }

            //Mark the solution start time. 
            //Solution time may end up being an overestimate, since it could include spark iteration restart time.
            //To address this, we should also not edown the partition end time in this node, and use it to calculate effective start time.
            //ToDo for later

            if (  isLoggingInitialized) logger.info("Start time is " + System.currentTimeMillis());
            nodeData.setStartTimeFor_LP_Relaxation(System.currentTimeMillis());
            setNodeData(ZERO,nodeData);                   
                
        }        
    }//end main method

    private void initLogging(int partitionID) throws Exception{
        
        if (! isLoggingInitialized){
            
            //initialize the LOG4J logger
            logger=Logger.getLogger(this.getClass());
            logger.setLevel(Level.DEBUG);
            PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");     
            logger.addAppender(new RollingFileAppender(layout,LOG_FOLDER +this.getClass().getSimpleName()+partitionID+ LOG_FILE_EXTENSION ));
            
            isLoggingInitialized=true;
        }
         
    } 
}
