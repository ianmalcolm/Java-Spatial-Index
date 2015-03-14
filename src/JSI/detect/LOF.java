/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.infomatiq.jsi.detect;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

//import java.text.ParseException;
//import java.util.Date;

/**
 *
 * @author ian
 */
public class LOF {

    // This hashmap caches the knn list generated during lof calculation
    // so that inquires for knn to the same point can be accelerate
    // at a 2nd time
    private HashMap<Rectangle, ArrayList<Rectangle>> knns = new HashMap<Rectangle, ArrayList<Rectangle>>();
    private Double factor = 1.0;
    private Point p;                    // test point
    private Rectangle rp;               // the rectangle of the test point
    private Integer minpts=0;           // k
    private SpatialIndex si;            // reference for r-tree
    private boolean REASONING = false;
    private Integer curdim = 0;
    private ArrayList<Double> con = new ArrayList<Double>();


    // Local outlier factor
    public static LOF lof(Point p,
            int k,
            ArrayList<Rectangle> rects,
            SpatialIndex si) {

        LOF mylof = new LOF();
        mylof.minpts = k;
        mylof.p = p;
        mylof.si = si;

        ArrayList<Rectangle> knn = mylof.si.nearestN(mylof.p, mylof.minpts);
        // knn[0] is the nearest neighbor

        Double lrdsum = 0.0;

        for (int i=0; i<knn.size(); i++) {
            Rectangle nbr = knn.get(i);
            // the sum of reachability distance
            lrdsum = lrdsum + mylof.lrd(nbr);
        }
        mylof.rp = new Rectangle(p);
        mylof.knns.put(mylof.rp, knn);
        mylof.factor = lrdsum / mylof.lrd(mylof.rp) / knn.size();
        return mylof;
    }

    // If a testpoint is said to be an outlier by lof(), then use reasoning()
    // to identify which dimension does the most contribution to this
    // anomaly. reasoning() return an ArrayList, in which each element represent
    // the contribution of a dimension to this anomaly.
    public void reasoning() {
        con.clear();
        REASONING=true;
        for (int j = 1;j<=p.getdim(); j++){
            this.curdim=j;

            ArrayList<Rectangle> knn = knns.get(rp);
            Double lrdsum = 0.0;

            for (int i = 0; i < knn.size(); i++) {
                Rectangle nbr = knn.get(i);
                // the sum of reachability distance
                lrdsum = lrdsum + lrd(nbr);
            }
            
            con.add(lrdsum / lrd(rp) / knn.size());

        }
//        REASONING=false;
    }

    public Double getcon(int dim) {
        return con.get(dim);
    }
    
    public Double getfactor() {
        return factor;
    }

    // Local reachability density
    private Double lrd(Rectangle r) {

        // The nearestN here may return more than k point, if some furthest 
        // points have the same distance to p. knn.get(knn.size()-1) is the furthest
        // point.
        ArrayList<Rectangle> knn = knns.get(r);
        if (knn == null) {
            assert REASONING == false;
            knn = si.nearestN(r.copys(), minpts);
            knns.put(r, knn);
        }

        Double rdsum = 0.0;
        for (int i=0; i<knn.size(); i++) {
            Rectangle nbr = knn.get(i);
            // the sum of reachability distance
            rdsum = rdsum + rd(r, nbr);
        }
        Double lrd = knn.size() / rdsum;
        return lrd;
    }

    // Reachability distance
    private Double rd(Rectangle r,
            Rectangle nbr) {
        Point nbp = nbr.copys();
        Point p = r.copys();
        // distance from p to its neighbor
        Double dnp = nbp.distance(p);
        // k-distance of the neighbor of point p
        Double kd = kd(nbr);
        // always return the larger one
        if (dnp > kd) {
            if (REASONING == true) {
                dnp = Math.sqrt(Math.pow(dnp,2)-Math.pow(nbp.get(curdim)-p.get(curdim),2));
                //dnp = Math.abs(nbp.get(curdim)-p.get(curdim));
            }
            return dnp;
        } else {
            return kd;
        }
    }

    // k-distance 
    private Double kd(Rectangle r) {
        // the last element in _knn must be the furthest kth rectangle
        Point p = r.copys();
        ArrayList<Rectangle> knn = knns.get(r);
        if (knn == null) {
            assert REASONING == false;
            knn = si.nearestN(p, minpts);
            knns.put(r, knn);
        }
        Point knbr = knn.get(knn.size() - 1).copys();
        Double dis = p.distance(knbr);
        if (REASONING == true) {
            dis = Math.sqrt(Math.pow(dis, 2) - Math.pow(p.get(curdim) - knbr.get(curdim), 2));
            //dis = Math.abs(p.get(curdim) - knbr.get(curdim));
        }
        return dis;
    }
}