//   RTree.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited
//   Copyright (C) 2008-2010 aled@sourceforge.net
//  
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//  
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//  
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
package com.infomatiq.jsi.rtree;

import com.infomatiq.jsi.HeapSort;
import com.infomatiq.jsi.Point;
//import com.infomatiq.jsi.PriorityQueue;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Stack;

/**
 * <p>This is a lightweight RTree implementation, specifically designed for the
 * following features (in order of importance): <ul> <li>Fast intersection query
 * performance. To achieve this, the RTree uses only main memory to store
 * entries. Obviously this will only improve performance if there is enough
 * physical memory to avoid paging.</li> <li>Low memory requirements.</li>
 * <li>Fast add performance.</li> </ul></p>
 *
 * <p>The main reason for the high speed of this RTree implementation is the
 * avoidance of the creation of unnecessary objects, mainly achieved by using
 * primitive collections from the trove4j library.</p>
 *
 * @author aled@sourceforge.net
 * @version 1.0b8
 */
public class RTree implements SpatialIndex {

    private static final String version = "1.0b8";
    // parameters of the tree
    private int maxNodeEntries = 50;
    private int minNodeEntries = 20;
    // map of nodeId -> node object
    // TODO eliminate this map - it should not be needed. Nodes
    // can be found by traversing the tree.
    private HashMap<Rectangle, Node> rectMap = new HashMap<Rectangle, Node>();
    // internal consistency checking - set to true if debugging tree corruption
    private final static boolean INTERNAL_CONSISTENCY_CHECKING = true;
    // initialisation
    private Node root = null;
//    private int size = 0;
    // Enables creation of new nodes
    // Deleted node objects are retained in the nodeMap, 
    // so that they can be reused. Store the IDs of nodes
    // which can be reused.

    // List of nearestN rectangles
    // List of nearestN rectanges, used in the alternative nearestN implementation.
    /**
     * Constructor. Use init() method to initialize parameters of the RTree.
     */
    public RTree() {
        init(null);
    }

    //-------------------------------------------------------------------------
    // public implementation of SpatialIndex interface:
    //  init(Properties)
    //  add(Rectangle, int)
    //  delete(Rectangle, int)
    //  nearest(Point, TIntProcedure, Double)
    //  intersects(Rectangle, TIntProcedure)
    //  contains(Rectangle, TIntProcedure)
    //  size()
    //-------------------------------------------------------------------------
    /**
     * <p>Initialize implementation dependent properties of the RTree. Currently
     * implemented properties are: <ul> <li>MaxNodeEntries</li> This specifies
     * the maximum number of entries in a node. The default value is 10, which
     * is used if the property is not specified, or is less than 2.
     * <li>MinNodeEntries</li> This specifies the minimum number of entries in a
     * node. The default value is half of the MaxNodeEntries value (rounded
     * down), which is used if the property is not specified or is less than 1.
     * </ul></p>
     *
     * @see com.infomatiq.jsi.SpatialIndex#init(Properties)
     */
    public void init(Properties props) {
        if (props != null) {

            maxNodeEntries = Integer.parseInt(props.getProperty("MaxNodeEntries", "0"));
            minNodeEntries = Integer.parseInt(props.getProperty("MinNodeEntries", "0"));

            // Obviously a node with less than 2 entries cannot be split.
            // The node splitting algorithm will work with only 2 entries
            // per node, but will be inefficient.
            assert maxNodeEntries > 10;
            // The MinNodeEntries must be less than or equal to (int) (MaxNodeEntries / 2)
            if (minNodeEntries < 1 || minNodeEntries > maxNodeEntries / 2) {
                minNodeEntries = maxNodeEntries / 2;
            }
        }

        root = new Node(1, maxNodeEntries, minNodeEntries);

    }

    /**
     * @see com.infomatiq.jsi.SpatialIndex#add(Node, int)
     */
    public void add(Rectangle r) {
        Node rect = new Node(r);
        // pass a pararmeter 1 to level by default
        add(rect, 1);
        rectMap.put(r, rect);

        if (INTERNAL_CONSISTENCY_CHECKING) {
            assert checkConsistency();
        }
    }

    /**
     * Adds a new entry at a specified level in the tree
     */
    private void add(Node rect, int level) {
        if (size() > 0) {
            assert rect.getdim() == this.getdim() : "R-Tree Point dimension match error!";
        }
        // I1 [Find position for new record] Invoke ChooseLeaf to select a 
        // leaf node L in which to place r

        Node n = chooseNode(rect, level);
        Node newLeaf = null;

        // I2 [Add record to leaf node] If L has room for another entry, 
        // install E. Otherwise invoke SplitNode to obtain L and LL containing
        // E and all the old entries of L
        if (n.size() < maxNodeEntries) {
            n.addEntry(rect);

        } else {
            newLeaf = splitNode(n, rect);
        }


        // I3 [Propagate changes upwards] Invoke AdjustTree on L, also passing LL
        // if a split was performed
        Node newNode = adjustTree(n, newLeaf);

        // I4 [Grow tree taller] If node split propagation caused the root to 
        // split, create a new root whose children are the two resulting nodes.
        if (newNode != null) {
            Node oldRoot = root;
            root = new Node(root.getLevel()+1, maxNodeEntries, minNodeEntries);
            root.addEntry(newNode);
            root.addEntry(oldRoot);
//            assert root.getLevel() == newNode.getLevel() + 1;
//            assert root.getLevel() == oldRoot.getLevel() + 1;
        }
    }

    /**
     * @see com.infomatiq.jsi.SpatialIndex#delete(Rectangle, int)
     */
    public boolean delete(Rectangle r) {
        // FindLeaf algorithm inlined here. Note the "official" algorithm 
        // searches all overlapping entries. This seems inefficient to me, 
        // as an entry is only worth searching if it contains (NOT overlaps)
        // the rectangle we are searching for.
        //
        // Also the algorithm has been changed so that it is not recursive.

        // FL1 [Search subtrees] If root is not a leaf, check each entry 
        // to determine if it contains r. For each entry found, invoke
        // findLeaf on the node pointed to by the entry, until r is found or
        // all entries have been checked.

        Node rect = rectMap.get(r);
        if (rect == null) {
            return false;
        }
        
        assert rect.getLevel() == 0;
        Node parent = rect.getParent();
        parent.deleteEntry(rect);
        rectMap.remove(r);
        condenseTree(parent);

        // shrink the tree if possible (i.e. if root node has exactly one entry,and that 
        // entry is not a leaf node, delete the root (it's entry becomes the new root)
        while (root.size()==1 && root.getLevel()>1) {
            root = root.get(0);
        }
        root.setParent(null);

        if (INTERNAL_CONSISTENCY_CHECKING) {
            assert checkConsistency();
        }

        return true;
    }

    /**
     *
     * @param p
     * @param k
     * @return
     */
    public ArrayList<Rectangle> nearestN(Point p, int k) {
        HeapSort<Rectangle> knn = new HeapSort<Rectangle>(HeapSort.SORT_ORDER_DESCENDING);
        nearestN(p, k, root, knn);

        if (size() >= k) {
            assert knn.size() >= k;
        } else {
            assert knn.size() == size();
        }

        ArrayList<Rectangle> result = new ArrayList<Rectangle>();
        knn.setSortOrder(HeapSort.SORT_ORDER_ASCENDING);
        while (knn.size() != 0) {
            result.add(knn.pop());
        }
        return result;
    }

    private void nearestN(Point p, int k, Node n, HeapSort<Rectangle> knn) {

        assert k > 0;

        if (!n.isLeaf()) {

            // Generate Active Branch List, sort ABL based on ordering metric values
            HeapSort<Node> ABL = new HeapSort<Node>(HeapSort.SORT_ORDER_DESCENDING);
            genBranchList(p, n, ABL);

            // Perform Downward Pruning (may discard all branches)
            pruneBranchList(k, knn, ABL);

            // Iterate through the Active Branch List
            while (ABL.size() != 0) {
                // Recursively visit child nodes
                ABL.setSortOrder(HeapSort.SORT_ORDER_ASCENDING);
                Node child = ABL.pop();
                nearestN(p, k, child, knn);
                // Perform Upward Pruning
                ABL.setSortOrder(HeapSort.SORT_ORDER_DESCENDING);
                pruneBranchList(k, knn, ABL);
            }
        } else {
            double furthest = Double.MAX_VALUE;
            if (knn.size() >= k) {
                furthest = knn.gettopval();
            }
            for (int i=0; i<n.size(); i++) {
                // use Node.MINDIST(Point) instead of Point.MINDIST(Node)
                // to improve performance
                double rectdist = n.get(i).MINDIST(p);
                if (furthest >= rectdist) {
                    knn.insert(n.get(i), rectdist);
                }
            }
            knn.prune(k);
        }
    }

    private void genBranchList(Point p, Node n, HeapSort<Node> ABL) {
        for (int i=0; i<n.size(); i++) {
            // use Node.MINDIST(Point) instead of Point.MINDIST(Node)
            // to improve performance
            Double dist = n.get(i).MINDIST(p);
            ABL.insert(n.get(i), dist);
        }
    }

    private void pruneBranchList(int k, HeapSort<Rectangle> knn, HeapSort<Node> ABL) {
        if (knn.size() >= k) {
            Double maxdist = knn.gettopval();
            while (ABL.size() > 0) {
                if (ABL.gettopval() > maxdist) {
                    ABL.pop();
                } else {
                    break;
                }
            }
        }

    }

    /**
     * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, TIntProcedure)
     */
    public boolean intersects(Rectangle r) {
        return root.intersects(r);
    }

    /**
     * @see com.infomatiq.jsi.SpatialIndex#contains(Rectangle, TIntProcedure)
     */
    public ArrayList<Rectangle> contains(Rectangle r) {
        // find all rectangles in the tree that are contained by the passed rectangle
        // written to be non-recursive (should model other searches on this?)
        // Depth-First Traversal
        ArrayList<Rectangle> result = new ArrayList<Rectangle>();
        Stack<Node> parent = new Stack<Node>();
        Stack<Integer> child = new Stack<Integer>();
        parent.push(root);
        child.push(0);


        // TODO: possible shortcut here - could test for intersection with the 
        // MBR of the root node. If no intersection, return immediately.

        while (parent.size() > 0) {
            Node n = parent.peek();

            if (!n.isLeaf()) {
                // go through every entry in the index node to check
                // if it intersects the passed rectangle. If so, it 
                // could contain entries that are contained.
                boolean intersects = false;
                for (int i = child.peek(); i < n.size(); i++) {
                    if (r.intersects(n.get(i))) {
                        parent.push(n.get(i));
                        child.pop();
                        child.push(i); // this becomes the start index when the child has been searched
                        child.push(0);
                        intersects = true;
                        break; // ie go to next iteration of while()
                    }
                }
                if (intersects) {
                    continue;
                }
            } else {
                // go through every entry in the leaf to check if 
                // it is contained by the passed rectangle
                for (int i = 0; i < n.size(); i++) {
                    if (r.contains(n.get(i))) {
                        result.add(n.get(i).getRef());
                    }
                }
            }
            parent.pop();
            child.pop();
        }
        return result;
    }

    /**
     * @see com.infomatiq.jsi.SpatialIndex#contains(Rectangle, TIntProcedure)
     */
    /**
     * @see com.infomatiq.jsi.SpatialIndex#size()
     */
    public int size() {
        return rectMap.size();
    }

    /**
     * @see com.infomatiq.jsi.SpatialIndex#getBounds()
     */
    public Rectangle getBounds() {
        Rectangle bounds = null;

        if (root.size() > 0) {
            return root.getMBR();
        }
        return null;
    }

    public int getdim() {
        return root.getdim();
    }

    /**
     * @see com.infomatiq.jsi.SpatialIndex#getVersion()
     */
    public String getVersion() {
        return "RTree-" + version;
    }
    //-------------------------------------------------------------------------
    // end of SpatialIndex methods
    //-------------------------------------------------------------------------



    /**
     * Split a node. Algorithm is taken pretty much verbatim from Guttman's
     * original paper.
     *
     * @return new node object.
     */
    private Node splitNode(Node n, Node rect) {
        // [Pick first entry for each group] Apply algorithm pickSeeds to 
        // choose two entries to be the first elements of the groups. Assign
        // each to a group.
        // The parameter newId stands for the id of the new rectange

//        System.arraycopy(initialEntryStatus, 0, entryStatus, 0, maxNodeEntries);

        assert n.size() == maxNodeEntries : "Error:Not enough entires before spliting a Node!";

        Node newNode = new Node(n.getLevel(), maxNodeEntries, minNodeEntries);

        // for the purposes of picking seeds, take the MBR of the node to include
        // the new rectangle as well.
        n.enlarge(rect);

        ArrayList<Node> seed = pickSeeds(n, rect);
        Node highestLowIndex = seed.get(0);
        Node lowestHighIndex = seed.get(1);

        ArrayList<Node> tempentries = n.copyEntry();
        tempentries.add(rect);

        n.clearEntry();

        // highestLowIndex is the seed for the new node.
        // lowestHighIndex is the seed for the original node. 

        if (highestLowIndex == null) {
            newNode.addEntry(tempentries.get(tempentries.size()-1));
            tempentries.remove(tempentries.get(tempentries.size()-1));
        } else {
            newNode.addEntry(highestLowIndex);
            tempentries.remove(highestLowIndex);
        }
        if (lowestHighIndex == null) {
            n.addEntry(tempentries.get(tempentries.size() - 1));
            tempentries.remove(tempentries.get(tempentries.size() - 1));
        } else {
            n.addEntry(lowestHighIndex);
            tempentries.remove(lowestHighIndex);
        }
        
        assert tempentries.size() == this.maxNodeEntries + 1 - 2;

        // [Check if done] If all entries have been assigned, stop. If one
        // group has so few entries that all the rest must be assigned to it in 
        // order for it to have the minimum number m, assign them and stop. 
//        while (n.size() + newNode.size() < maxNodeEntries + 1) {
        while (tempentries.size() > 0) {
            if (maxNodeEntries + 1 - newNode.size() == minNodeEntries) {
                // assign all remaining entries to original node
                while (tempentries.size() > 0) {
                    n.addEntry(tempentries.get(0));
                    tempentries.remove(0);
                }
                break;
            }

            if (maxNodeEntries + 1 - n.size() == minNodeEntries) {
                // assign all remaining entries to new node
                while (tempentries.size() > 0) {
                    newNode.addEntry(tempentries.get(0));
                    tempentries.remove(0);
                }
                break;
            }

            // [Select entry to assign] Invoke algorithm pickNext to choose the
            // next entry to assign. Add it to the group whose covering rectangle 
            // will have to be enlarged least to accommodate it. Resolve ties
            // by adding the entry to the group with smaller area, then to the 
            // the one with fewer entries, then to either. Repeat from S2
            ArrayList<Node> next = pickNext(n, newNode, tempentries);

            Node nextNode = next.get(0);
            Node nextEntry = next.get(1);
            nextNode.addEntry(nextEntry);
            tempentries.remove(nextEntry);

        }
        assert n.size() + newNode.size() == maxNodeEntries + 1 : "Error:Node lost while spliting a Node!";
        return newNode;
    }

    /**
     * Pick the seeds used to split a node. Select two entries to be the first
     * elements of the groups
     */
    private ArrayList<Node> pickSeeds(Node n, Node rect) {
        // Find extreme rectangles along all dimension. Along each dimension,
        // find the entry whose rectangle has the highest low side, and the one 
        // with the lowest high side. Record the separation.
        Double maxNormalizedSeparation = -1.0; // initialize to -1 so that even overlapping rectangles will be considered for the seeds
        Double normalizedSeparation = -1.0;
        Integer highestLowIndex = -1;
        Integer lowestHighIndex = -1;
        Point s = rect.copys();
        Point t = rect.copyt();

        for (int i=0; i<s.getdim(); i++) {
            Double tempHighestLow = s.get(i);
            int tempHighestLowIndex = -1;
            Double tempLowestHigh = t.get(i);
            int tempLowestHighIndex = -1;
            Double len = n.copyt().get(i) - n.copys().get(i);

            for (int c = 0; c < n.size(); c++) {
                Double tempLow = n.get(c).copys().get(i);
                Double tempHigh = n.get(c).copyt().get(i);
                if (tempLow >= tempHighestLow) {
                    tempHighestLow = tempLow;
                    tempHighestLowIndex = c;
                }  // ensure that the same index cannot be both lowestHigh and highestLow
                if (tempHigh <= tempLowestHigh) {
                    tempLowestHigh = tempHigh;
                    tempLowestHighIndex = c;
                }

                // PS2 [Adjust for shape of the rectangle cluster] Normalize the separations
                // by dividing by the widths of the entire set along the corresponding
                // dimension

                if (len == 0) {
                    normalizedSeparation = 1.0;
                } else {
                    normalizedSeparation = (tempHighestLow - tempLowestHigh) / len;
                }

            }

            // PS3 [Select the most extreme pair] Choose the pair with the greatest
            // normalized separation along any dimension.
            // Note that if negative it means the rectangles overlapped. However still include
            // overlapping rectangles if that is the only choice available.
            if (normalizedSeparation >= maxNormalizedSeparation) {
                highestLowIndex = tempHighestLowIndex;
                lowestHighIndex = tempLowestHighIndex;
                maxNormalizedSeparation = normalizedSeparation;
            }
        }

        // At this point it is possible that the new rectangle is both highestLow and lowestHigh.
        // This can happen if all rectangles in the node overlap the new rectangle.
        // Resolve this by declaring that the highestLowIndex is the lowest Y and,
        // the lowestHighIndex is the largest X (but always a different rectangle)
        if (highestLowIndex == lowestHighIndex) {
            highestLowIndex = -1;
            Double tempMinY = s.getmindim();
            lowestHighIndex = 0;
            Double tempMaxX = n.get(0).copyt().getmaxdim();

            for (int c = 1; c < n.size(); c++) {
                double newmin = n.get(c).copys().getmindim();
                double newmax = n.get(c).copyt().getmaxdim();
                if (newmin < tempMinY) {
                    tempMinY = newmin;
                    highestLowIndex = c;
                } else if (newmax > tempMaxX) {
                    tempMaxX = newmax;
                    lowestHighIndex = c;
                }
            }
        }

        assert highestLowIndex != lowestHighIndex : "splitNode highestLowIndex equals lowestHighIndex!";

        ArrayList<Node> seed = new ArrayList<Node>();
        if (highestLowIndex != -1) {
            seed.add(n.get(highestLowIndex));
        } else {
            seed.add(null);
        }
        if (lowestHighIndex != -1) {
            seed.add(n.get(lowestHighIndex));
        } else {
            seed.add(null);
        }
        return seed;

    }

    /**
     * Pick the next entry to be assigned to a group during a node split.
     *
     * [Determine cost of putting each entry in each group] For each entry not
     * yet in a group, calculate the area increase required in the covering
     * rectangles of each group
     */
    private ArrayList<Node> pickNext(Node n, Node newNode, ArrayList<Node> rect) {
        Node nextNode = null; 
        Node nextEntry = null;
        Double maxDifference = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < rect.size(); i++) {

            Double nIncrease = n.enlargement(rect.get(i));
            Double newNodeIncrease = newNode.enlargement(rect.get(i));
            Double difference = Math.abs(nIncrease - newNodeIncrease);
            if (difference > maxDifference) {
                nextEntry = rect.get(i);
                if (nIncrease < newNodeIncrease) {
                    nextNode = n;
                } else if (newNodeIncrease < nIncrease) {
                    nextNode = newNode;
                } else if (n.area() < newNode.area()) {
                    nextNode = n;
                } else if (newNode.area() > n.area()) {
                    nextNode = newNode;
                } else if (newNode.size() < maxNodeEntries / 2) {
                    nextNode = n;
                } else {
                    nextNode = newNode;
                }
                maxDifference = difference;
            }
        }
        assert nextNode != null;
        assert nextEntry != null;
        ArrayList<Node> next = new ArrayList<Node>();
        next.add(nextNode);
        next.add(nextEntry);
        return next;
    }


    /**
     * Used by delete(). Ensures that all nodes from the passed node up to the
     * root have the minimum number of entries.
     *
     * Note that the parent and parentEntry stacks are expected to contain the
     * nodeIds of all parents up to the root.
     */
    private void condenseTree(Node n) {
        // CT1 [Initialize] Set n=l. Set the list of eliminated
        // nodes to be empty.
        assert n.getLevel() == 1;
        Stack<Node> eliminatedNode = new Stack<Node>();

        // CT2 [Find parent entry] If N is the root, go to CT6. Otherwise 
        // let P be the parent of N, and let En be N's entry in P  
        while (n != root) {
            Node parent = n.getParent();
            assert parent.contains(n);

            // CT3 [Eliminiate under-full node] If N has too few entries,
            // delete En from P and add N to the list of eliminated nodes
            if (n.size() < minNodeEntries) {
                eliminatedNode.push(n);
                parent.deleteEntry(n);
            } else {
                // CT4 [Adjust covering rectangle] If N has not been eliminated,
                // adjust EnI to tightly contain all entries in N
            }

            // CT5 [Move up one level in tree] Set N=P and repeat from CT2
            n = parent;
            n.setMBR();
        }

        // CT6 [Reinsert orphaned entries] Reinsert all entries of nodes in set Q.
        // Entries from eliminated leaf nodes are reinserted in tree leaves as in 
        // Insert(), but entries from higher level nodes must be placed higher in 
        // the tree, so that leaves of their dependent subtrees will be on the same
        // level as leaves of the main tree
        while (eliminatedNode.size() > 0) {
            Node e = eliminatedNode.pop();
            for (int j = 0; j < e.size(); j++) {
                add(e.get(j), e.getLevel());
            }
        }
    }

    /**
     * Used by add(). Chooses a leaf to add the rectangle to.
     */
    private Node chooseNode(Node rect, int level) {
        // CL1 [Initialize] Set N to be the root node
        Node n = root;

        // CL2 [Leaf check] If N is a leaf, return N
        while (n.getLevel() != level) {

            // CL3 [Choose subtree] If N is not at the desired level, let F be the entry in N 
            // whose rectangle FI needs least enlargement to include EI. Resolve
            // ties by choosing the entry with the rectangle of smaller area.
            Double leastEnlargement = Double.MAX_VALUE;
            Node leastEnlargementNode = null;
            for (int i = 0; i < n.size(); i++) {
                Double enlargement = n.get(i).enlargement(rect);
                if ((enlargement < leastEnlargement)
                        || ((enlargement == leastEnlargement)
                        && (n.get(i).area() < leastEnlargementNode.area()))) {
                    leastEnlargementNode = n.get(i);
                    leastEnlargement = enlargement;
                }
            }

            // CL4 [Descend until a leaf is reached] Set N to be the child node 
            // pointed to by Fp and repeat from CL2
            n = leastEnlargementNode;
        }
        
        return n;
    }

    /**
     * Ascend from a leaf node L to the root, adjusting covering rectangles and
     * propagating node splits as necessary.
     */
    private Node adjustTree(Node n, Node nn) {
        // AT1 [Initialize] Set N=L. If L was split previously, set NN to be 
        // the resulting second node.

        // AT2 [Check if done] If N is the root, stop
        while (n != root) {

            // AT3 [Adjust covering rectangle in parent entry] Let P be the parent 
            // node of N, and let En be N's entry in P. Adjust EnI so that it tightly
            // encloses all entry rectangles in N.
            Node parent = n.getParent();
            parent.setMBR();


            // AT4 [Propagate node split upward] If N has a partner NN resulting from 
            // an earlier split, create a new entry Enn with Ennp pointing to NN and 
            // Enni enclosing all rectangles in NN. Add Enn to P if there is room. 
            // Otherwise, invoke splitNode to produce P and PP containing Enn and
            // all P's old entries.
            Node newNode = null;
            if (nn != null) {
                if (parent.size() < maxNodeEntries) {
                    parent.addEntry(nn);
                } else {
                    newNode = splitNode(parent, nn);
                }
            }

            // AT5 [Move up to next level] Set N = P and set NN = PP if a split 
            // occurred. Repeat from AT2
            n = parent;
            nn = newNode;

        }

        return nn;
    }

    /**
     * Check the consistency of the tree.
     *
     * @return false if an inconsistency is detected, true otherwise.
     */
    public boolean checkConsistency() {
        if (root == null) {
            System.out.println("The root node is null!");
            return false;
        }

        return root.checkConsistency();
    }
}
