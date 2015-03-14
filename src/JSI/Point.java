//   Point.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited.
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
import java.util.Arrays;
import java.util.Collections;

/**
 * Currently hardcoded to 2 dimensions, but could be extended.
 *
 * @author aled@sourceforge.net
 * @version 1.0b8
 */
public class Point extends ArrayList<Double> {

    public Point(Double... coo) {
        super.addAll(Arrays.asList(coo));
    }

    public Point(Point t) {
        super.addAll(t);
    }

    public int getdim() {
        return size();
    }
    
    public Point copy() {
        return new Point(this);
    }
    
    public static Point getminp(Point u, Point v) {
        assert u.getdim() == v.getdim():"getminp dimension match error!";
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

    public static Point getmaxp(Point u, Point v) {
        assert u.getdim() == v.getdim():"getmaxp dimension match error!";
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

    public double getmaxdim() {
        double maxdim = Collections.max(this);
        return maxdim;
    }

    public double getmindim() {
        double mindim = Collections.min(this);
        return mindim;
    }
    
    public void print() {
        for (int i=0; i<getdim(); i++) {
            System.out.print(get(i));
            System.out.print("\t");
        }
    }
    
    public void set(Double... coo) {
        clear();
        addAll(Arrays.asList(coo));
    }
    
    // clone point t to this point
    public void set(Point t) {
        clear();
        addAll(t);
    }

    // get distance from point p
    public Double distance(Point p) {
        assert getdim() == p.getdim():"Point getdis Dimension Error!";
        double result = 0.0;
        double temp = 0;
        for (int i=0; i<getdim(); i++) {
            temp = get(i) - p.get(i);
            result += temp*temp;
        }
        result = Math.sqrt(result);

        assert !Double.isInfinite(result);
        assert result >= 0.0;
        return result;
    }

    // 
    public Double MINDIST(Rectangle r) {
        assert getdim() > 1 : "Point init incorrectly before use!";
        assert getdim() == r.getdim() : "P & r MINDIST dimension match error!";
        Point rs = r.copys();
        Point rt = r.copyt();
        double result = 0.0;
        double temp;
        for (int i=0; i<getdim(); i++) {
            if (get(i) < rs.get(i)) {
                temp = rs.get(i) - get(i);
                result += temp * temp;
            } else
            if (rt.get(i) < get(i)) {
                temp = get(i) - rt.get(i);
                result += temp * temp;
            }
        }
        assert !Double.isInfinite(result);
        assert result >= 0.0;

        return result;
    }

    // 
    public Double MINMAXDIST(Rectangle r) {
        assert getdim() > 1 : "Point init incorrectly before use!";
        assert getdim() == r.getdim() : "P & r MINDIST dimension match error!";
        Double[] rmk = new Double[getdim()];
        Double[] rmi = new Double[getdim()];
        Double[] result = new Double[getdim()];
        Point rs = r.copys();
        Point rt = r.copyt();
        double rmitemp;
        double rmktemp;
        double resulttemp = 0.0;
        double resultmin = Double.MAX_VALUE;

        for (int i=0; i<getdim(); i++) {
            // calculating |pi - rMi|^2
            if (get(i) <= ((rs.get(i) + rt.get(i)) / 2)) {
                rmitemp = rs.get(i);
            } else {
                rmitemp = rt.get(i);
            }
            double temp = get(i) - rmitemp;
            rmi[i] = temp*temp;

            // calculating |pi - rmk|^2
            if (get(i) >= ((rs.get(i) + rt.get(i)) / 2)) {
                rmktemp = rs.get(i);
            } else {
                rmktemp = rt.get(i);
            }
            temp = get(i) - rmktemp;
            rmk[i] = temp*temp;
        }

        // calculate all |pi - rmk|^2 + sigma(|pi - rMi|^2)
        for (int i=0; i < getdim(); i++) {
            resulttemp += rmi[i];
        }
        for (int i=0; i < getdim(); i++) {
            result[i] = resulttemp - rmi[i] + rmk[i];
        }

        // find the min result
        for (int i=0; i<getdim(); i++) {
            if (result[i] <= resultmin) {
                resultmin = result[i];
            }
        }

        assert !Double.isInfinite(resultmin);
        assert resultmin >= 0.0;

        return resultmin;
    }
    
    public void rescale(Rectangle r) {
        assert getdim() == r.getdim();
        Point rs = r.copys();
        Point rt = r.copyt();
        for (int i=0; i <getdim(); i++) {
            Double offset = rs.get(i);
            Double length = rt.get(i)-rs.get(i);
            this.set(i, this.get(i)-offset);
            
            // If span of bound in dimenstion i is 0, then do nothing
            // to scaling,
            if (length > 0.0) {
                this.set(i, this.get(i)/length);
            }
            assert !this.get(i).isInfinite();
            assert !this.get(i).isNaN();
        }
    }

}
