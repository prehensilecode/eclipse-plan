/* MLCLeafGeometry.java */

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

/* $Id$ */

package edu.harvard.lroc.eclipseplan;

/** Geometry of individual MLC leaves. 
 *
 * @author David Chin
 * @version $Revision$
 */
public class MLCLeafGeometry {
    
    /** Parameter names are as in the documentation for DYNVMLC
     * 
     * @param leafWidth
     * @param leafGap 
     * @param wTongue
     * @param hTongue
     * @param zTongue
     * @param wGroove
     * @param hGroove
     * @param zGroove
     * @param wTip
     * @param hTip
     * @param tipGap
     * @param wSup
     * @param hSup
     * @param zSup
     * @param wRail
     * @param zHole
     * @param hHole
     * @param endType 
     */
    public MLCLeafGeometry(double leafWidth, double leafGap, double wTongue, double hTongue,
            double zTongue, double wGroove, double hGroove, double zGroove,
            double wTip, double hTip, double tipGap, double wSup, double hSup,
            double zSup, double wRail, double zHole, double hHole, String endType) {
        
        this.leafWidth = leafWidth;
        this.leafGap = leafGap;
        this.wTongue = wTongue;
        this.hTongue = hTongue;
        this.zTongue = zTongue;
        this.wGroove = wGroove;
        this.hGroove = hGroove;
        this.zGroove = zGroove;
        this.wTip = wTip;
        this.hTip = hTip;
        this.tipGap = tipGap;
        this.wSup = wSup;
        this.hSup = hSup;
        this.zSup = zSup;
        this.wRail = wRail;
        this.zHole = zHole;
        this.hHole = hHole;
        this.endType = endType;
    }
    
    
    // variable names correspond to the DYNVMLC component module names
    
    private Double leafWidth;
    
    private Double leafGap;
    
    private Double wTongue;
    
    private Double hTongue;
    
    private Double zTongue;
    
    private Double wGroove;
    
    private Double hGroove;
    
    private Double zGroove;
    
    private Double wTip;
    
    private Double hTip;
    
    private Double tipGap;
    
    private Double wSup;
    
    private Double hSup;
    
    private Double zSup;
    
    private Double wRail;
    
    private Double zHole;
    
    private Double hHole;
    
    private String endType;
}

