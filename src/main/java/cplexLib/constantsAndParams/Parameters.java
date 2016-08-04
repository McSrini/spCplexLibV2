package cplexLib.constantsAndParams;

import java.io.Serializable;
import static cplexLib.constantsAndParams.Constants.*;
 

public class Parameters implements Serializable{
    
    //should move to properties file
    
    
    public static final String SAV_FILENAME="F:\\temporary files here\\timtab1.mps";    
    //do not allow any subtree to grow bigger than this
    public static int  MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE  =   TWO*TEN  ; //small number at the outset, icreased later
     
    public static double  RELATIVE_MIP_GAP = ZERO;
    
    //search strategy
    public static boolean  DEPTH_FIRST_SEARCH = false;
    
    //the partition on which this library (i.e. the ActiveSubtree and supporting objects) live
    //this is used for logging
    public static String LOG_FOLDER="F:\\temporary files here\\";
    public static int  PARTITION_ID = ONE;
    
}
