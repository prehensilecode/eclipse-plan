/*
 * Jaws.java
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

 /* $Id: Jaws.java 253 2008-06-24 18:25:24Z dwchin $*/

package edu.harvard.lroc.eclipseplan;

import com.archimed.dicom.*;
import java.io.*;

/** Represents asymmetric jaws.
 *
 * @author David Chin
 * @version $Revision: 253 $
 */
public class Jaws {
    /**
     * 
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     */
    public Jaws(double x1, double x2, double y1, double y2) {
        this.x[0] = x1;
        this.x[1] = x2;
        
        this.y[0] = y1;
        this.y[1] = y2;
    }
    
    /**
     * 
     * @param x
     * @param y
     */
    public Jaws(Double[] x, Double[] y) {
        this.x[0] = x[0];
        this.x[1] = x[1];
        
        this.y[0] = y[0];
        this.y[1] = y[1];
    }
    
    /**
     * 
     * @param ctrlPtDcm control point Dicom object
     * @throws DicomException 
     */
    public Jaws(DicomObject ctrlPtDcm) throws DicomException {

        int ssize = ctrlPtDcm.getSize(DDict.dBeamLimitingDevicePositionSequence);
        if (ssize < 2) {
            throw new DicomException("wrong number of jaw pairs: " + ssize);
        }

        //
        // the jaw positions are given in the first two beam limiting device poistions\            // decide which pair of jaws from the name
        // decide which pair of jaws from the name
        //
        DicomObject devPos = ctrlPtDcm.getSequenceItem(DDict.dBeamLimitingDevicePositionSequence, 0);

        if (devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("asymx") ||
                devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("x")) {
            this.x[0] = new Double(devPos.getS(DDict.dLeafJawPositions, 0));
            this.x[1] = new Double(devPos.getS(DDict.dLeafJawPositions, 1));
        } else if (devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("asymy") ||
                devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("y")) {
            this.y[0] = new Double(devPos.getS(DDict.dLeafJawPositions, 0));
            this.y[1] = new Double(devPos.getS(DDict.dLeafJawPositions, 1));
        }

        devPos = ctrlPtDcm.getSequenceItem(DDict.dBeamLimitingDevicePositionSequence, 1);

        if (devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("asymx") ||
                devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("x")) {
            this.x[0] = new Double(devPos.getS(DDict.dLeafJawPositions, 0));
            this.x[1] = new Double(devPos.getS(DDict.dLeafJawPositions, 1));
        } else if (devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("asymy") ||
                devPos.getS(DDict.dRTBeamLimitingDeviceType).equalsIgnoreCase("y")) {
            this.y[0] = new Double(devPos.getS(DDict.dLeafJawPositions, 0));
            this.y[1] = new Double(devPos.getS(DDict.dLeafJawPositions, 1));
        }     
    }

    /** X jaws positions */
    private Double[] x = new Double[2];
    
    /** Y jaws positions */
    private Double[] y = new Double[2];

    public Double[] getX() {
        return x;
    }

    public Double[] getY() {
        return y;
    }
    
    
    
    @Override
    public String toString() {
        String rep = "X jaws: (" + x[0] + ", " + x[1] + "); ";
        rep += "Y jaws: (" + y[0] + ", " + y[1] + ")";
        return rep;
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException, DicomException, Exception {
        Jaws j = new Jaws(1.1, 2.2, 3.3, 4.4);
        
        System.out.println(j.toString());
        
        File planFile = new File("/home/dwchin/A085414/RP.1.2.246.352.71.5.1039211570.191353.20080409095018.dcm");
        FileInputStream fin = null;
        DicomObject planDcm = null;
        
        // read in the plan file
      
        fin = new FileInputStream(planFile);
        BufferedInputStream bis = new BufferedInputStream(fin);
            
        DicomReader dcmReader = new DicomReader();
           
        planDcm = dcmReader.read(fin, true);     
        DicomObject beamSeq = (DicomObject) planDcm.get(DDict.dBeamSequence);
        int nBeams = planDcm.getSize(DDict.dBeamSequence);
        System.out.println("No. of beams: " + nBeams);
        for (int i = 0; i < nBeams; ++i) {
            System.out.println("Beam no. " + i);
            
            // the jaws are only specified in the 1st control point
            Jaws jaws = new Jaws(beamSeq.getSequenceItem(DDict.dControlPointSequence, 0));
            
            System.out.println(jaws.toString());
            System.out.println("= = = = = = = = = = =");
         }
    }
}
