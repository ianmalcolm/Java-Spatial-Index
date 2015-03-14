//   Node.java
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

import com.infomatiq.jsi.Rectangle;
import java.util.ArrayList;

/**
 * <p>Used by RTree. There are no public methods in this class.</p>
 *
 * @author aled.morris@infomatiq.co.uk
 * @version 1.0b8
 */
public class Node extends Rectangle {

    // It's user's responsibility to keep id as a key to index node

    private int level = 0;
    private Node parent = null;
    private ArrayList<Node> entries = null;
    private int maxEntries = 0;
    private int minEntries = 0;
    private Rectangle ref = null;

    // This constructor is used when a Rectangle is added into a tree
    Node(Rectangle r) {
        super(r);
        ref = r;
        level = 0;
    }

    Node(int level, int maxNodeEntries, int minNodeEntries) {
        super();

        this.level = level;
        entries = new ArrayList<Node>();
        this.maxEntries = maxNodeEntries;
        this.minEntries = minNodeEntries;
    }

    public void addEntry(Node n) {
        assert n != null:"Node n == null!";
        assert size() <= maxEntries;
        enlarge(n);
        entries.add(n);
        n.setParent(this);

    }

    public void clearEntry() {
        for (int i = 0; i < size(); i++) {
            entries.clear();
        }
        super.clear();
    }

    public boolean checkConsistency() {
        // Node self check
        boolean result = true;
        Rectangle r = new Rectangle();
        if (getLevel() == 0 && size() > 0) {
            System.out.printf("Error: Node %s, level %d, has child!\n", this, getLevel());
            result = false;
        }
        if (size() == 0 && getLevel() > 0 ) {
            System.out.print("Error: Node is empty but node is not level 0\n");
            return false;
        }
        if (getLevel() > 0 && !isRoot()) {
            if (entries.size() < minEntries) {
                System.out.printf("Error: Node %s, level %d, entry %d has too few children!\n", this, getLevel(), size());
                result = false;
            }
            if (entries.size() > maxEntries) {
                System.out.printf("Error: Node %s, level %d, entry %d has too many children!\n", this, getLevel(), size());
                result = false;
            }
        }
        for (int i=0; i<size(); i++) {

            if (this != get(i).getParent()) {
                System.out.printf("Error: Node %s, level %d, entry %d, its parent incorrent!\n", this, getLevel(), i);
                result = false;
            }
            if (getLevel() - 1 != get(i).getLevel()) {
                System.out.printf("Error: Node %s, level %d, entry %d, its level incorrent!\n", this, getLevel(), i);
                result = false;
            }

            if (getLevel() > 0) {
                if (get(i).checkConsistency() == false) {
                    result = false;
                }
            }
            r.enlarge(get(i).getMBR());
        }

        if (getLevel() > 0) {
            if (!getMBR().equals(r)) {
                System.out.printf("Error: Node %s, level %d, its MBR incorrent!\n", this, getLevel());
                result = false;
            }
        }


        return result;
    }
    // delete entry. This is done by setting it to null and copying the last entry into its space.
    // i is the index of Rectangle array

    public void deleteEntry(Node n) {
        entries.remove(n);
        setMBR();
    }

    public ArrayList<Node> copyEntry() {
        ArrayList<Node> copy = new ArrayList<Node>();
        copy.addAll(entries);
        return copy;
    }

    public void enlarge(Node n) {
        Rectangle original = n.getMBR();
        enlarge(n.getMBR());
        if (!original.equals(this)) {
            if (parent != null) {
                parent.enlarge(this);
            }
        }
    }

    // Here equal only means two nodes have the same MBR
    public boolean equals(Node n) {
        if (!equals(n.getMBR())) {
            return false;
        }

        return true;
    }

    public Rectangle getMBR() {
        return super.copy();
    }

    public int getLevel() {
        return this.level;
    }

//    public boolean intersects(Node n) {
//        return super.intersects(n.getMBR());
//    }

    public boolean isLeaf() {
        return (getLevel() == 1);
    }

    public boolean isRoot() {
        return (parent == null);
    }
    
    public Node getParent() {
        return parent;
    }
    
    public Rectangle getRef() {
        return ref;
    }

    public Node get(int i) {
        return entries.get(i);
    }

    public void setParent(Node newParent) {
        parent = newParent;
    }
    
    public int size() {
        if (entries == null) {
            return 0;
        }
        return entries.size();
    }

    public void setMBR() {
        Rectangle original = this.getMBR();
        super.clear();
        for (int i=0; i<size(); i++) {
            enlarge(entries.get(i));
        }
        if (!original.equals(this)) {
            if (parent != null) {
                parent.setMBR();
            }
        }
    }
}
