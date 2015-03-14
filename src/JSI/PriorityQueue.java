//  PriorityQueue.java
//  Java Spatial Index Library
//  Copyright (C) 2008 aled@sourceforge.net
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
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
package com.infomatiq.jsi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * <p> Priority Queue that stores values as ints and priorities as floats. Uses
 * a Heap to sort the priorities; the values are sorted "in step" with the
 * priorities. </p> <p> A Heap is simply an array that is kept semi sorted; in
 * particular if the elements of the array are arranged in a tree structure; ie
 * </p>
 *
 * 00 / \ 01 02 / \ / \ 03 04 05 06 /\ /\ /\ /\ 07 08 09 10 11 12 13 14
 *
 * <p> then each parent is kept sorted with respect to it's immediate children.
 * E.g. 00 < 01, 00 < 02, 02 < 05, 02 < 06 </p> <p> This means that the array
 * appears to be sorted, as long as we only ever look at element 0. </p> <p>
 * Inserting new elements is much faster than if the entire array was kept
 * sorted; a new element is appended to the array, and then recursively swapped
 * with each parent to maintain the "parent is sorted w.r.t it's children"
 * property. </p> <p> To return the "next" value it is necessary to remove the
 * root element. The last element in the array is placed in the root of the
 * tree, and is recursively swapped with one of it's children until the "parent
 * is sorted w.r.t it's children" property is restored. </p> <p> Random access
 * is slow (eg for deleting a particular value), and is not implemented here -
 * if this functionality is required, then a heap probably isn't the right data
 * structure. </p>
 *
 * @author Aled Morris <aled@sourceforge.net>
 * @version 1.0b8
 */
public class PriorityQueue extends ArrayList<ObjSortedByDist>{

    public static final boolean SORT_ORDER_ASCENDING = true;
    public static final boolean SORT_ORDER_DESCENDING = false;
    private boolean sortOrder = SORT_ORDER_ASCENDING;
    private static boolean INTERNAL_CONSISTENCY_CHECKING = true;

    public PriorityQueue(boolean sortOrder) {
        super();
        this.sortOrder = sortOrder;
    }

    /**
     * @param p1
     * @param p2
     * @return true if p1 has an earlier sort order than p2.
     */
    private boolean sortsEarlierThan(Double p1, Double p2) {
        if (sortOrder == SORT_ORDER_ASCENDING) {
            return p1 < p2;
        }
        return p2 < p1;
    }

    // to insert a value, append it to the arrays, then
    // reheapify by promoting it to the correct place.
    public void add(Integer id, Double dist) {
        add(new ObjSortedByDist(id,dist));
        Collections.sort(this, new ObjSortedByDist(SORT_ORDER_ASCENDING));
    }

    public int getValue(int index) {
        assert index >=0 && index <size();
        return get(index).id;
    }

    public Double getPriority(int index) {
        assert index >=0 && index <size();
        return get(index).dist;
    }
    
    public int pop() {
        assert size()>0;
        int id = Integer.MIN_VALUE;
        if (size() > 0) {
            id = get(0).id;
            remove(0);
        }
        return id;
    }

    public void setSortOrder(boolean order) {
        this.sortOrder = order;
    }
}

class ObjSortedByDist implements Comparator<ObjSortedByDist> {
    
    public Integer id = null;
    public Double dist = Double.MAX_VALUE;
    private static final boolean SORT_ORDER_ASCENDING = PriorityQueue.SORT_ORDER_ASCENDING;
    private static final boolean SORT_ORDER_DESCENDING = PriorityQueue.SORT_ORDER_DESCENDING;
    private boolean sortOrder = SORT_ORDER_ASCENDING;
    
    public ObjSortedByDist(Integer id, Double dist) {
        this.id = id;
        this.dist = dist;
        sortOrder = SORT_ORDER_ASCENDING;
    }
    
    public ObjSortedByDist(boolean order) {
        sortOrder = order;
    }

    public ObjSortedByDist() {
        sortOrder = SORT_ORDER_ASCENDING;
    }

    public int compare(ObjSortedByDist o1, ObjSortedByDist o2) {
        if (sortOrder == SORT_ORDER_ASCENDING) {
            if (o1.dist > o2.dist) {
                return 1;
            } else if (o1.dist == o2.dist) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (o1.dist > o2.dist) {
                return -1;
            } else if (o1.dist == o2.dist) {
                return 0;
            } else {
                return 1;
            }

        }
    }

}