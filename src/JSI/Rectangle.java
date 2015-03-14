//   Rectangle.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited
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

/**
 * Currently hardcoded to 2 dimensions, but could be extended.
 *
 * @author aled@sourceforge.net
 * @version 1.0b8
 */
public class Rectangle {

    /**
     * use primitives instead of arrays for the coordinates of the rectangle, to
     * reduce memory requirements.
     */
//    public Double minX, minY, maxX, maxY;
    // s,t two point construct a rectangle. s always be the min point
    private Point s;
    private Point t;

    public Rectangle() {
        this.s = new Point();
        this.t = new Point();
    }

    // treate rectangle as a point
    public Rectangle(Point u) {
        this.s = null;
        assert u.getdim() > 1 : "Rectangle init Dimension Error";
        s = u;
        t = u;
    }

    public Rectangle(Point u, Point v) {
        this.s = null;
        assert u.getdim() > 1 : "Rectangle init Dimension Error";
        assert v.getdim() > 1 : "Rectangle init Dimension Error";
        assert u.getdim() == v.getdim() : "Rectangle init Dimension Error";
        s = this.getminp(u, v);
        t = this.getmaxp(u, v);

    }

    public Rectangle(Rectangle r) {
        this.s = null;
        s = r.copys();
        t = r.copyt();
    }

    public Point copys() {
        return s.copy();
    }

    public Point copyt() {
        return t.copy();
    }

    private Point getminp(Point u, Point v) {
        assert u.getdim() > 1 : "Rectangle getminp Dimension Error";
        assert v.getdim() > 1 : "Rectangle getminp Dimension Error";
        assert u.getdim() == v.getdim() : "Rectangle getminp Dimension match Error";
        Point p = new Point();
        for (int i=0; i<u.getdim(); i++) {
            if (u.get(i) > v.get(i)) {
                p.add(v.get(i));
            } else {
                p.add(u.get(i));
            }
        }
        return p;
    }

    public Point getmaxp(Point u, Point v) {
        assert u.getdim() > 1 : "Rectangle getmaxp Dimension Error";
        assert v.getdim() > 1 : "Rectangle getmaxp Dimension Error";
        assert u.getdim() == v.getdim() : "Rectangle getmaxp Dimension match Error";
        Point p = new Point();
        for (int i=0; i<u.getdim(); i++) {
            if (u.get(i) < v.get(i)) {
                p.add(v.get(i));
            } else {
                p.add(u.get(i));
            }
        }
        return p;
    }

    /**
     * Make a copy of this rectangle
     *
     * @return copy of this rectangle
     */
    public Rectangle copy() {
        assert getdim() > 1 : "R init incorrectly before use!";
        return new Rectangle(s, t);
    }

    public void clear() {
        s.clear();
        t.clear();
    }

    public int getdim() {
        if (s == null) {
            return 0;
        } else {
            return s.getdim();
        }
    }

    /**
     * Determine whether an edge of this rectangle overlies the equivalent edge
     * of the passed rectangle
     */
    public boolean edgeOverlaps(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert this.getdim() == r.getdim() : "Rectangle edgeOverlaps Dimension match Error";

        Point rs = r.copys();
        Point rt = r.copyt();
        for (int i=0; i<getdim(); i++) {
            if (rs.get(i) == s.get(i)) {
                return true;
            }
            if (rt.get(i) == t.get(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether this rectangle intersects the passed rectangle
     *
     * @param r The rectangle that might intersect this rectangle
     *
     * @return true if the rectangles intersect, false if they do not intersect
     */
    public boolean intersects(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert this.getdim() == r.getdim() : "Rectangle intersects Dimension match Error";

        Point rs = r.copys();
        Point rt = r.copyt();
        for (int i=0; i<getdim(); i++) {
            if (rs.get(i) > t.get(i)) {
                return false;
            }
            if (rt.get(i) < s.get(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert this.getdim() == r.getdim() : "Rectangle contains Dimension match Error";

        Point rs = r.copys();
        Point rt = r.copyt();
        for (int i=0; i<getdim(); i++) {
            if (rt.get(i) > t.get(i)) {
                return false;
            }
            if (rs.get(i) < s.get(i)) {
                return false;
            }
        }
        return true;

    }

    /**
     * Determine whether this rectangle is contained by the passed rectangle
     *
     * @param r The rectangle that might contain this rectangle
     *
     * @return true if the passed rectangle contains this rectangle, false if it
     * does not
     */
    public boolean containedBy(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";

        Point rs = r.copys();
        Point rt = r.copyt();
        for (int i=0; i<getdim(); i++) {
            if (rt.get(i) < t.get(i)) {
                return false;
            }
            if (rs.get(i) > s.get(i)) {
                return false;
            }
        }
        return true;

    }

    /**
     * Return the distance between this rectangle and the passed point. If the
     * rectangle contains the point, the distance is zero.
     *
     * @param p Point to find the distance to
     *
     * @return distance between this rectangle and the passed point.
     */
    public Double getdis(Point p) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert getdim() == p.getdim() : "R & P distance Dimension match Error";

        Double disSq = 0.0;

        for (int i=0; i<this.getdim(); i++) {
            Double temp = p.get(i) - s.get(i);
            if (temp < 0) {
                disSq += (temp * temp);
            }
            temp = p.get(i) - t.get(i);
            if (temp > 0) {
                disSq += (temp * temp);
            }
        }
        Double result = Math.sqrt(disSq);
        assert !Double.isInfinite(result);
        assert result >= 0.0;

        return result;
    }

    /**
     * Return the distance between this rectangle and the passed rectangle. If
     * the rectangles overlap, the distance is zero.
     *
     * @param r Rectangle to find the distance to
     *
     * @return distance between this rectangle and the passed rectangle
     */
    public Double getdis(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert getdim() == r.getdim() : "R & R distance Dimension match Error";

        Double disSq = 0.0;
        for (int i=0; i<getdim(); i++) {
            Double greatestMin = Math.max(s.get(i), r.copys().get(i));
            Double leastMax = Math.min(t.get(i), r.copyt().get(i));
            if (greatestMin > leastMax) {
                disSq += (Double) Math.pow((greatestMin - leastMax), 2);
            }
        }
        Double result = Math.sqrt(disSq);
        assert !Double.isInfinite(result);
        assert result >= 0.0;
        return result;
    }

    public Double getWidth(int i) {
        assert i>-1 && i<getdim();
        return t.get(i)-s.get(i);
    }
    /**
     * Calculate the area by which this rectangle would be enlarged if added to
     * the passed rectangle. Neither rectangle is altered.
     *
     * @param r Rectangle to union with this rectangle, in order to compute the
     * difference in area of the union and the original rectangle
     *
     * @return enlargement
     */
    public Double enlargement(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert this.getdim() == r.getdim() : "R enlargement Dimension match Error";
        Double result = union(r).area()- area();
        assert !Double.isInfinite(result);
        assert result >= 0.0;
        return result;
    }

    /**
     * Compute the area of this rectangle.
     *
     * @return The area of this rectangle
     */
    public Double area() {
        assert getdim() > 1 : "R init incorrectly before use!";
        Double result = 1.0;
        for (int i=0; i<getdim(); i++) {
            result *= t.get(i) - s.get(i);
        }
        assert !Double.isInfinite(result);
        assert result >= 0.0;
        return result;
    }

    /**
     * Computes the union of this rectangle and the passed rectangle, storing
     * the result in this rectangle.
     *
     * @param r Rectangle to add to this rectangle
     */
    public void enlarge(Rectangle r) {
        if (getdim() > 0) {
            assert getdim() == r.getdim() : "R enlarge R Dimension match Error";
        } else {
            assert getdim() == 0:"R init incorrectly before use!";
        }
        enlarge(r.copys());
        enlarge(r.copyt());
    }

    public void enlarge(Point p) {
        if (getdim() > 0) {
            assert getdim() == p.getdim() : "R enlarge p dimension match error!";
            for (int i=0; i<getdim(); i++) {
                if (s.get(i) > p.get(i)) {
                    s.set(i, p.get(i));
                }
                if (t.get(i) < p.get(i)) {
                    t.set(i, p.get(i));
                }
            }
        } else {
            assert getdim() == 0 : "R init incorrectly before use!";
            s = p.copy();
            t = p.copy();
        }

        assert getdim() == p.getdim() : "R enlarge p dimension match failed!";
    }

    public Double distanceSq(Point p) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert this.getdim() == p.getdim() : "R & p distanceSq dimension match error!";
        Double result = 0.0;
        for (int i=0; i<getdim(); i++) {
            if (s.get(i) > p.get(i)) {
                Double temp = s.get(i) - p.get(i);
                result += (temp * temp);
            }
            if (t.get(i) < p.get(i)) {
                Double temp = p.get(i) - t.get(i);
                result += (temp * temp);
            }
        }
        assert !Double.isInfinite(result);
        assert result >= 0.0;
        return result;
    }

    /**
     * Find the the union of this rectangle and the passed rectangle. Neither
     * rectangle is altered
     *
     * @param r The rectangle to union with this rectangle
     */
    public Rectangle union(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";
        assert this.getdim() == r.getdim() : "R & R union Dimension Error";
        Rectangle union = this.copy();
        union.enlarge(r);
        return union;
    }

    /**
     * Determine whether this rectangle is equal to a given object. Equality is
     * determined by the bounds of the rectangle.
     *
     * @param o The object to compare with this rectangle
     */
    public boolean equals(Rectangle r) {
        assert getdim() > 1 : "R init incorrectly before use!";
        if (r.getdim() != getdim()) {
            return false;
        }
        for (int i=0; i<getdim(); i++) {
            if (s.get(i) != r.copys().get(i)) {
                return false;
            }
            if (t.get(i) != r.copyt().get(i)) {
                return false;
            }
        }
        return true;
    }
    
    public void rescale(Rectangle r) {
        assert getdim() == r.getdim();
        s.rescale(r);
        t.rescale(r);
    }

    public Double MINDIST(Point p) {
        assert getdim() > 1 : "Rectangle init incorrectly before use!";
        assert getdim() == p.getdim() : "P & r MINDIST dimension match error!";
        double result = 0.0;
        double temp;
        for (int i=0; i<getdim(); i++) {
            if (p.get(i) < s.get(i)) {
                temp = s.get(i) - p.get(i);
                result += temp * temp;
            } else
            if (t.get(i) < p.get(i)) {
                temp = p.get(i) - t.get(i);
                result += temp * temp;
            }
        }
        assert !Double.isInfinite(result);
        assert result >= 0.0;

        return result;
    }

}
