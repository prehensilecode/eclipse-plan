/*
 * Structure.java
 */

/*  This file is part of EclipsePlan.
 *
 *  Copyright (C) 2008  Dana-Farber/Brigham & Women's Cancer Center
 *
 *  EclipsePlan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  EclipsePlan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  
 *  NOTE: This program is not to be used for ANY clinical purpose, or
 *        in any situation which will affect patient care. This program
 *        is to be used only for research purposes.
 *
 *  Author: David Chin <dwchin@lroc.harvard.edu>  
 */

/* $Id: Structure.java 253 2008-06-24 18:25:24Z dwchin $ */

package edu.harvard.lroc.eclipseplan;

import java.io.*;


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.vecmath.*;

import com.archimed.dicom.*;

// Rather annoying aspect of the RS file: the data for each structure is
// split over 3 sequence objects.
//     -  StructureSetROISequence
//     -  ROIContourSequence
//     -  RTROIObservationsSequence
// they use the ReferencedROINumber to tell us which structure the data 
// belong to.
// 
// We are going to ignore the RTROIObservationsSequence, which tells us
// the kind of each structure, i.e. ORGAN, AVOIDANCE, etc.
//
// So, we have to make 2 passes per structure. Since the number of structures
// will be fairly small (typically less than 20), let's just read in all the
// data, tag them with the right structure name, and then pass them to the
// Structure constructor.

/**
 * Represents a single structure or Region Of Interest (ROI). An RS file will 
 * contain one or more structures.
 * @author David Chin
 * @version $Revision: 253 $
 */
public class Structure {
    /**
     * 
     * @param structDcm Structure meta-data: ROI Name and ROI Number
     * @param contourSeqDcm Contour sequence 
     */
    public Structure(DicomObject structDcm, DicomObject contourSeqDcm) {
        this.structDcm = structDcm;
        this.contourSeqDcm = contourSeqDcm;
        
        try {
            this.name = this.structDcm.getS(DDict.dROIName);
            this.number = this.structDcm.getI(DDict.dROINumber);
            
            // populate the Vector<Point3d> containing the structure data
            
            int nContours = this.contourSeqDcm.getSize(DDict.dContourSequence);
            DicomObject contour = null;
            Double x, y, z;
            int nPoints = 0;
            for (int i = 0; i < nContours; ++i) {
                if (this.debug_p)
                    System.out.println("Contour #" + i);
                
                contour = this.contourSeqDcm.getSequenceItem(DDict.dContourSequence, i);
                nPoints = contour.getI(DDict.dNumberOfContourPoints);
                
                if (this.debug_p)
                    System.out.println("    nPoints = " + nPoints);
                
                for (int j = 0; j < nPoints; ++j) {
                    x = new Double(contour.getS(DDict.dContourData, 3*j));
                    y = new Double(contour.getS(DDict.dContourData, 3*j + 1));
                    z = new Double(contour.getS(DDict.dContourData, 3*j + 2));
                    this.points.add(new Point3d(x,y,z));
                }
                
                this.points.trimToSize();
            }
            
            if (this.debug_p) {
                System.out.println(this.contourSeqDcm.getSize(DDict.dContourSequence));
                System.out.println("==========================");
            }
        } catch (Exception ex) {
            Logger.getLogger(Structure.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.findBoundingBox();
    }
    
    /**
     * Extracts bounding box from points data.
     */
    private void findBoundingBox() {
        // TODO: implement this
        int nPoints = this.points.size();
        Double xmin, xmax, ymin, ymax, zmin, zmax;
        Vector<Double> x = new Vector<Double>(nPoints);
        Vector<Double> y = new Vector<Double>(nPoints);
        Vector<Double> z = new Vector<Double>(nPoints);
        
        for (int i = 0; i < nPoints; ++i) {
            x.add(i, this.points.get(i).x);
            y.add(i, this.points.get(i).y);
            z.add(i, this.points.get(i).z);
        }
        
        xmin = 0.; xmax = 0.;
        ymin = 0.; ymax = 0.;
        zmin = 0.; zmax = 0.;
        for (int i = 0; i < nPoints; ++i) {
            if (x.get(i) < xmin) 
                xmin = x.get(i);
            
            if (x.get(i) > xmax) 
                xmax = x.get(i);
            
            if (y.get(i) < ymin)
                ymin = y.get(i);
            
            if (y.get(i) > ymax)
                ymax = y.get(i);
            
            if (z.get(i) < zmin)
                zmin = z.get(i);
            
            if (z.get(i) > zmax)
                zmax = z.get(i);
        }
        
        Point3d min = new Point3d(xmin, ymin, zmin);
        Point3d max = new Point3d(xmax, ymax, zmax);
        this.boundingBox = new BoundingBox(min, max);
    }
    
    /** Debug flag */
    private boolean debug_p = false;

    
    /** The actual points forming the structure. */
    private Vector<Point3d> points = new Vector<Point3d>();
    
    // TODO: not necessary?
    /** Dicom object of this structure. */
    private DicomObject structDcm;

    /** Dicom object of the contour sequence. */
    private DicomObject contourSeqDcm;


    /**
     * 
     * @return Dicom object of structure
     */
    public DicomObject getStructDcm() {
        return structDcm;
    }

    /**
     * 
     * @return Dicom object of the contours.
     */
    public DicomObject getContourSeqDcm() {
        return contourSeqDcm;
    }
    
    
    
    /** ROI name. */
    private String name;
    
    /** ROI number. */
    private Integer number;
    
    /**
     * 
     * @return ROI name
     */
    public String getName() {
        return name;
    }
    
    /**
     *
     * @return ROI number
     */
    public Integer getNumber() {
        return number;
    }
    
    @Override
    public String toString() {
        String rep = "";
        rep += "(ROI name: " + this.name + "; ";
        rep += " ROI number: " + this.number  + "; ";
        rep += " No. of points: " + this.points.size() + ")";
        
        return rep;
    }
    
    /** Bounding box */
    private BoundingBox boundingBox;

    /**
     * 
     * @return Bounding box
     */
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
    
    
    
    /** Simple test program
     * @param args 
     */
    public static void main(String[] args) {
        TreeMap<String, Structure> structMap = new TreeMap<String, Structure>();
        TreeMap<String, Integer> nameNumberMap = new TreeMap<String, Integer>();
        DicomObject structFileDcm = null;
        DicomObject struct = null;
        DicomObject contourSeq = null;
        FileInputStream fin = null;
        
        // these next two maps are keyed by ROI Number
        TreeMap<Integer, DicomObject> structMetaData = new TreeMap<Integer, DicomObject>();
        TreeMap<Integer, DicomObject> structContours = new TreeMap<Integer, DicomObject>();
        
        try {
            fin = new FileInputStream("/home/dwchin/66666B/RS.1.2.246.352.71.4.1039211570.2388931.20080402133708.dcm");
            DicomReader dcmRead = new DicomReader();
            structFileDcm = dcmRead.read(fin);
            
            int seqlen = structFileDcm.getSize(DDict.dStructureSetROISequence);
            for (int i = 0; i < seqlen; ++i) {
                struct = structFileDcm.getSequenceItem(DDict.dStructureSetROISequence, i);
                struct.dumpVRs(System.out);
                structMetaData.put(struct.getI(DDict.dROINumber), struct);
                nameNumberMap.put(struct.getS(DDict.dROIName), struct.getI(DDict.dROINumber));
                contourSeq = structFileDcm.getSequenceItem(DDict.dROIContourSequence, i);
                
                structContours.put(struct.getI(DDict.dROINumber), contourSeq);
                
                System.out.println("= = = = = = = = = =");
            }
            
            for (String n : nameNumberMap.navigableKeySet()) {
                Integer num = nameNumberMap.get(n);  // ROI Number
                structMap.put(n, 
                        new Structure(structMetaData.get(num),
                                      structContours.get(num)));
            }
            
            System.out.println(structMap);
        } catch (IOException ex) {
            Logger.getLogger(Structure.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DicomException ex) {
            Logger.getLogger(Structure.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Logger.getLogger(Structure.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        for (String s : structMap.navigableKeySet()) {
            System.out.println(structMap.get(s).toString());
            System.out.println(structMap.get(s).getBoundingBox().toString());
        }
        
    }
}
