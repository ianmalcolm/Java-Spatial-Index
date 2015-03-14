/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.infomatiq.jsi;

import java.util.ArrayList;

/**
 *
 * @author ian
 */
public class HeapSort<Type> {
    public static final boolean SORT_ORDER_ASCENDING = true;
    public static final boolean SORT_ORDER_DESCENDING = false;
    private ArrayList<Type> ele = null;
    private ArrayList<Double> val = null;
    private boolean sortOrder = SORT_ORDER_ASCENDING;
    private static boolean INTERNAL_CONSISTENCY_CHECKING = true;

    public HeapSort(boolean sortOrder) {
        this.sortOrder = sortOrder;
        ele = new ArrayList<Type>();
        val = new ArrayList<Double>();
    }

    /**
     * @param p1
     * @param p2
     * @return true if p1 has an earlier sort order than p2.
     */
    private boolean sortsEarlierThan(Double p1, Double p2) {
        if (sortOrder == SORT_ORDER_ASCENDING) {
            return p1 <= p2;
        }
        return p2 <= p1;
    }

    // to insert a value, append it to the arrays, then
    // reheapify by promoting it to the correct place.
    public void insert(Type element, Double value) {
        ele.add(element);
        val.add(value);

        promote(ele.size()-1, element, value);
    }

    private void promote(int index, Type element, Double value) {
        // Consider the index to be a "hole"; i.e. don't swap elements/values
        // when moving up the tree, simply copy the parent into the hole and
        // then consider the parent to be the hole.
        // Finally, copy the value/element into the hole.
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            Double parentValue = val.get(parentIndex);

            if (sortsEarlierThan(parentValue, value)) {
                break;
            }

            // copy the parent entry into the current index.
            ele.set(index, ele.get(parentIndex));
            val.set(index, parentValue);
            index = parentIndex;
        }

        ele.set(index, element);
        val.set(index, value);

        if (INTERNAL_CONSISTENCY_CHECKING) {
            check();
        }
    }

    public int size() {
        return ele.size();
    }

    public void clear() {
        ele.clear();
        val.clear();
    }

    public Type gettopele() {
        return ele.get(0);
    }

    public Double gettopval() {
        return val.get(0);
    }

    public void prune(int k) {

        ArrayList<Type> prunedEle = new ArrayList<Type>();
        Double prunedVal = 0.0;
        
        while (size() > k) {
            // normal case - we can simply remove the lowest priority (highest distance) entry
            Double value = gettopval();
            Type element = pop();

            // rare case - multiple items of the same priority (distance)
            if (value == gettopval()) {
                prunedEle.add(element);
                prunedVal = value;
            } else {
                prunedEle.clear();
            }
        }

        // if the saved values have the same distance as the
        // next one in the tree, add them back in.
        if (prunedEle.size() > 0 && prunedVal == gettopval()) {
            for (int i = 0; i < prunedEle.size(); i++) {
                insert(prunedEle.get(i), prunedVal);
            }
            prunedEle.clear();
        }

    }
    
    private void demote(int index, Type element, Double value) {
        int childIndex = (index * 2) + 1; // left child

        while (childIndex < ele.size()) {
            Double childValue = val.get(childIndex);

            if (childIndex + 1 < ele.size()) {
                Double rightValue = val.get(childIndex + 1);
                if (sortsEarlierThan(rightValue, childValue)) {
                    childValue = rightValue;
                    childIndex++; // right child
                }
            }

            if (sortsEarlierThan(childValue, value)) {
                val.set(index, childValue);
                ele.set(index, ele.get(childIndex));
                index = childIndex;
                childIndex = (index * 2) + 1;
            } else {
                break;
            }
        }

        ele.set(index, element);
        val.set(index, value);
    }

    // get the value with the lowest priority
    // creates a "hole" at the root of the tree.
    // The algorithm swaps the hole with the appropriate child, until
    // the last entry will fit correctly into the hole (ie is lower
    // priority than its children)
    public Type pop() {
        Type ret = ele.get(0);

        // record the value/priority of the last entry
        int lastIndex = ele.size() - 1;
        Type tempElement = ele.get(lastIndex);
        Double tempValue = val.get(lastIndex);

        ele.remove(lastIndex);
        val.remove(lastIndex);

        if (lastIndex > 0) {
            demote(0, tempElement, tempValue);
        }

        if (INTERNAL_CONSISTENCY_CHECKING) {
            check();
        }

        return ret;
    }

    public void setSortOrder(boolean sortOrder) {
        if (this.sortOrder != sortOrder) {
            this.sortOrder = sortOrder;
            // reheapify the arrays
            for (int i = (ele.size() / 2) - 1; i >= 0; i--) {
                demote(i, ele.get(i), val.get(i));
            }
        }
        if (INTERNAL_CONSISTENCY_CHECKING) {
            check();
        }
    }

    private void check() {
        // for each entry, check that the child entries have a lower or equal
        // priority

        for (int i = 0; i < ele.size() / 2; i++) {
            Double curValue = val.get(i);

            int leftIndex = (i * 2) + 1;
            if (leftIndex < ele.size()) {
                Double leftValue = val.get(leftIndex);
                assert sortsEarlierThan(curValue, leftValue) : "Internal error in HeapSort";
            }

            int rightIndex = (i * 2) + 2;
            if (rightIndex < ele.size()) {
                Double rightValue = val.get(rightIndex);
                assert sortsEarlierThan(curValue, rightValue) : "Internal error in HeapSort";
            }
        }
        assert ele.size() == val.size();
    }

}
