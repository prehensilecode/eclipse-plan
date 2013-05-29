/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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


package edu.harvard.lroc.eclipseplan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.j3d.BoundingBox;
import javax.vecmath.Point3d;

/**
 * @author David Chin
 * @version $Revision$
 */
public class DentalConvert {
    public DentalConvert() {
        
    }
    
    private StructureSet structs;
    
    private Phantom phant;
    
    private EgsPhant egsPhant;
    
    static public void main(String[] args) {
        String plandir = "A086160";
        DentalConvert dc = new DentalConvert();
        System.out.println(plandir);
        
        CTImageList ctimages = new CTImageList("A086160");
        dc.phant = new Phantom(ctimages);
        
        System.out.println(dc.phant.getBoundingBox().toString());   
        
        // get bounding box from console input
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        
        System.out.print("Enter x0: ");
        String x0str = null;
        try {
            x0str = console.readLine();
        } catch (IOException ex) {
            System.err.println(ex);
        }
        
        System.out.print("Enter y0: ");
        String y0str = null;
        try {
            y0str = console.readLine();
        } catch (IOException ex) {
            System.err.println(ex);
        }
        
        System.out.print("Enter z0: ");
        String z0str = null;
        try {
            z0str = console.readLine();
        } catch (IOException ex) {
            System.err.println(ex);
        }
        
        System.out.print("Enter x1: ");
        String x1str = null;
        try {
            x1str = console.readLine();
        } catch (IOException ex) {
            System.err.println(ex);
        }
        
        System.out.print("Enter y1: ");
        String y1str = null;
        try {
            y1str = console.readLine();
        } catch (IOException ex) {
            System.out.println(ex);
        }
        
        System.out.print("Enter z1: ");
        String z1str = null;
        try {
            z1str = console.readLine();
        } catch (IOException ex) {
            System.out.println(ex);
        }
        
        // TODO: bounds check for x0str,y0,z0, x1,y1,z1
        Double x0 = Double.parseDouble(x0str);
        Double y0 = Double.parseDouble(y0str);
        Double z0 = Double.parseDouble(z0str);
        Double x1 = Double.parseDouble(x1str);
        Double y1 = Double.parseDouble(y1str);
        Double z1 = Double.parseDouble(z1str);
        BoundingBox bbox = new BoundingBox(new Point3d(x0, y0, z0), 
                                           new Point3d(x1, y1, z1));
        
        dc.phant.resize(bbox);  
                
        EgsPhant ep = new EgsPhant(dc.phant);
        
        try {
            ep.writeFile("A086160_foobar.egsphant");
        } catch (Exception ex) {
            Logger.getLogger(DentalConvert.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

}
