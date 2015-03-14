/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.infomatiq.jsi.detect;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.rtree.RTree;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ian
 */
public class Detect {

    public static void main(String[] args) {

        String trainingfile = "adaptability\\train.txt";
        String testingfile = "adaptability\\test.txt";
        boolean UPDATE = true;
        boolean NORMALIZATION = true;
        boolean VERBOSE = false;
        boolean REASONING = false;
        boolean RESCALING = false;
        int RTREE_SIZE = -1;
        int K = 30;
        double THRESHOLD_PERCENTAGE = 0.99;
        double THRESHOLD = 2.0;

        // command line argument parsing
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                usage();
                return;
            }
            if (args[i].equals("-train") && i + 1 < args.length) {
                i++;
                trainingfile = args[i];
                continue;
            }
            if (args[i].equals("-test") && i + 1 < args.length) {
                i++;
                testingfile = args[i];
                continue;
            }
            if (args[i].equals("-size") && i + 1 < args.length) {
                i++;
                RTREE_SIZE = Integer.parseInt(args[i]);
                continue;
            }
            if (args[i].equals("-update")) {
                UPDATE = true;
                continue;
            }
            if (args[i].equals("-norm")) {
                NORMALIZATION = true;
                continue;
            }
            if (args[i].equals("-v")) {
                VERBOSE = true;
                continue;
            }
            if (args[i].equals("-rescale")) {
                RESCALING = true;
                NORMALIZATION = true;
                continue;
            }
            if (args[i].equals("-k") && i + 1 < args.length) {
                i++;
                K = Integer.parseInt(args[i]);
                continue;
            }
            if (args[i].equals("-pt") && i + 1 < args.length) {
                i++;
                THRESHOLD_PERCENTAGE = Double.parseDouble(args[i]);
                continue;
            }
            if (args[i].equals("-at") && i + 1 < args.length) {
                i++;
                THRESHOLD = Double.parseDouble(args[i]);
                continue;
            }
            usage();
            return;
        }

        // Use rects as a database to store these training points
//      ArrayList<Date> times = new ArrayList<Date>();
//      DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        ArrayList<Rectangle> rects = new ArrayList<Rectangle>();
        ArrayList<Rectangle> rects_norm = new ArrayList<Rectangle>();
        SpatialIndex si = new RTree();
        SpatialIndex si_norm = new RTree();
        Rectangle bound;

        {
            System.out.printf("Preprocessing...\n");
            BufferedReader br = null;
            String line = null;
            try {
                br = new BufferedReader(new FileReader(trainingfile));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LOF.class.getName()).log(Level.SEVERE, null, ex);
            }

            while (true) {
                try {
                    if ((line = br.readLine()) == null) {
                        break;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(LOF.class.getName()).log(Level.SEVERE, null, ex);
                }
                String[] ele = line.split("\t");
                if (rects.size() > 0 && ele.length - 1 != rects.get(0).getdim()) {
                    System.out.printf("Warning: Point %s in training data is collected improperly.\n", ele[0]);
                    continue;
                }
                Point p = new Point();
                for (int i = 1; i < ele.length; i++) {
                    Double temp = Double.parseDouble(ele[i]);
                    p.add(temp);
                }
                Rectangle r = new Rectangle(p, p);
                rects.add(r);
                si.add(r);
//              times.add(df.parse(ele[0]));
                if (RTREE_SIZE > 0 && rects.size() >= RTREE_SIZE) {
                    si.delete(rects.get(0));
                    rects.remove(0);
                }
            }
            if (rects.size() < RTREE_SIZE || RTREE_SIZE < 1) {
                RTREE_SIZE = rects.size();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(LOF.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        // Normalization
        for (int i = 0; i < rects.size(); i++) {
            rects_norm.add(rects.get(i).copy());
        }
        bound = si.getBounds();
        if (NORMALIZATION == true) {
            for (int i = 0; i < rects_norm.size(); i++) {
                rects_norm.get(i).rescale(bound);
            }
        }

        // First read in MAXRECTS record into rects, then normalized it
        // From then on, before adding a Rectangle, Rescale it according
        // to previous normalization

        {
            System.out.printf("Adding points into R-Tree...\n");
            for (int i = 0; i < rects_norm.size(); i++) {
                si_norm.add(rects_norm.get(i));
            }

            if (THRESHOLD < 0.0) {
                Double[] value = new Double[si_norm.size()];
                for (int i = 0; i < value.length; i++) {
                    LOF trainlof = LOF.lof(rects_norm.get(i).copys(), K, rects_norm, si_norm);
                    value[i] = trainlof.getfactor();
                }
                Arrays.sort(value);
                THRESHOLD = value[(int) (value.length * THRESHOLD_PERCENTAGE) - 1];
                System.out.printf("MIN LOF %f, MAX LOF %f, %%%f is %f.\n", value[0], value[value.length - 1], THRESHOLD_PERCENTAGE * 100, THRESHOLD);
            } else {
                System.out.printf("MIN LOF NULL, MAX LOF NULL, THRESHOLD is %f.\n", THRESHOLD);
            }
            System.out.printf("trainingfile:%s, testingfile:%s\n", trainingfile, testingfile);
            System.out.printf("K=%d, UPDATE=%b, NORMALIZATION=%b, R-tree_size=%d\n", K, UPDATE, NORMALIZATION, si_norm.size());

        }

        {
            System.out.printf("Testing...\n");
            int anomaly = 0;
            BufferedReader br = null;
            String line = null;
            try {
                br = new BufferedReader(new FileReader(testingfile));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LOF.class.getName()).log(Level.SEVERE, null, ex);
            }

            while (true) {
                try {
                    if ((line = br.readLine()) == null) {
                        break;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(LOF.class.getName()).log(Level.SEVERE, null, ex);
                }
                String[] ele = line.split("\t");
                if (ele.length - 1 != si_norm.getdim()) {
                    System.out.printf("Warning: Point %s in testing data is collected improperly.\n", ele[0]);
                    continue;
                }
                Point p = new Point();
                for (int i = 1; i < ele.length; i++) {
                    p.add(Double.parseDouble(ele[i]));
                }
                Rectangle r = new Rectangle(p, p);
                Rectangle r_norm = r.copy();

                if (NORMALIZATION == true) {
                    r_norm.rescale(bound);
                }

                LOF testlof = LOF.lof(r_norm.copys(), K, rects_norm, si_norm);
                if (testlof.getfactor() >= THRESHOLD) {
                    anomaly++;
                    System.out.print(ele[0]);
                    System.out.printf("\t%f", testlof.getfactor());
                    if (REASONING == true) {
                        testlof.reasoning();
                        for (int i = 0; i < r_norm.getdim(); i++) {
                            System.out.printf("%.1f%%\t", testlof.getcon(i) / testlof.getfactor() * 100);
                        }
                    }
                    System.out.printf("\n");
                    continue;
                } else if (VERBOSE == true) {
                    System.out.print(ele[0]);
                    System.out.printf("\tnormal\t%f", testlof.getfactor());
                    System.out.printf("\n");
                }

                if (UPDATE == true) {
                    si_norm.delete(rects_norm.get(0));
                    si_norm.add(r_norm);
                    rects_norm.remove(0);
                    rects_norm.add(r_norm);
                    si.delete(rects.get(0));
                    si.add(r);
                    rects.remove(0);
                    rects.add(r);
                    if (RESCALING == true) {
                        if (!checkNorm(si_norm.getBounds(), 0.1)) {
                            System.out.println("Rescaling...");
                            bound = si.getBounds();
                            rects_norm.clear();
                            for (int i = 0; i < rects.size(); i++) {
                                rects_norm.add(rects.get(i).copy());
                            }
                            for (int i = 0; i < rects_norm.size(); i++) {
                                rects_norm.get(i).rescale(bound);
                            }
                            si_norm = new RTree();
                            for (int i = 0; i < rects_norm.size(); i++) {
                                si_norm.add(rects_norm.get(i));
                            }
                        }
                    }

                }
            }
            System.out.printf("%d anomalies found.\n", anomaly);

            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(LOF.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static boolean checkNorm(Rectangle si, double percent) {
        assert percent > 0;
        
        double maxwidth = Double.MIN_VALUE;
        double minwidth = Double.MAX_VALUE;
        for (int i=0; i<si.getdim(); i++) {
            double width = si.getWidth(i);
            if (maxwidth < width) {
                maxwidth = width;
            } else if (minwidth > width) {
                minwidth = width;
            }
        }
        if (Math.abs((maxwidth-minwidth)/minwidth) > percent) {
            return false;
        } else {
            return true;
        }
    }

    private static void usage() {
        System.out.println("Example: java -jar jsi.jar -train slg1_training.log -test slg1_testing.log -update -size 3000 -k 10 -pt 0.99");
        System.out.println("Example: java -jar jsi.jar -train slg1_training.log -test slg1_testing.log -size 3000 -k 10 -at 2.5");
        System.out.println("-train file for training data");
        System.out.println("-test file for testing data");
        System.out.println("-size the size of R-Tree");
        System.out.println("-k the number of nearest neighbor");
        System.out.println("-at the absolute threshold of LOF");
        System.out.println("-pt the threshold percentage of LOF");
        System.out.println("-update update the knowledge base of LOF");
        System.out.println("-v print LOF value whether it is an anomaly");
        System.out.println("-norm localy normalize each dimension to 0~1");
        System.out.println("-reasoning tell how much LOF drops if a dimension is taken out");
    }
}
