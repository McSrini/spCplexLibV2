package cplexLib.constantsAndParams;

import java.io.Serializable;
import static cplexLib.constantsAndParams.Constants.*;
 

public class Parameters implements Serializable{
    
    //should move to properties file
    
    //this is the file we are solving
    public static final String SAV_FILENAME="F:\\temporary files here\\timtab1.mps";    
    
    //do not allow any subtree to grow bigger than this
    // can be increased by the driver
    public static int  MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE  =  TWO* THOUSAND; 
     
    public static double  RELATIVE_MIP_GAP = ZERO;
   
    //how much more time do we give hard nodes, compared to easy nodes?
    //default is 5 times more
    public static double  HARD_NODE_TIME_FACTOR = FIVE;
    
    //do not bother solving any tree for less than 1 second
    public  static final  double MIN_TIME_SLICE_FOR_TREE_SECONDS =     ONE; 
        
    //search strategy
    public static boolean  DEPTH_FIRST_SEARCH = false;
    
    //the partition on which this library (i.e. the ActiveSubtree and supporting objects) live
    //this is used for logging
    public static String LOG_FOLDER="F:\\temporary files here\\";
    public static int  PARTITION_ID = ONE;
    
}
