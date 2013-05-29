/* Material.java */

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

/** 
 * Encapsulates data which defines a material in a phantom,
 * i.e. min and max Hounsfield units, and min and max mass density    
 *
 * @author David Chin
 * @version $Revision$
 */
public class Material {
    public Material(String n) throws MaterialException {
        this.name = n;

        if (this.name.equalsIgnoreCase("air700icru")) {
            this.minHU = 0;
            this.maxHU = 50;
            this.minDens = 0.001;
            this.maxDens = 0.044;
        } else if (this.name.equalsIgnoreCase("lung700icru")) {
            this.minHU = 50;
            this.maxHU = 300;
            this.minDens = 0.044;
            this.maxDens = 0.302;
        } else if (this.name.equalsIgnoreCase("icrutissue700icru")) {
            this.minHU = 300;
            this.maxHU = 1125;
            this.minDens = 0.302;
            this.maxDens = 1.101;
        } else if (this.name.equalsIgnoreCase("icrpbone700icru")) {
            this.minHU = 1125;
            this.maxHU = 3000;
            this.minDens = 1.101;
            this.maxDens = 2.088;
        } else {
            throw new MaterialException("Unknown material: " + this.name);
        }
    }

    /** Name of material */
    private String name;
    
    /** Minimum Hounsfield number */
    private int minHU;
    
    /** Maximum Hounsfield number */
    private int maxHU;
    
    /** Minimum mass density (g/cm^3) */
    private double minDens;
    
    /** Maximum mass density (g/cm^3) */
    private double maxDens;
    
    /** estepe parameter */
    private double estepe;

    /** 
     * 
     * @return ESTEPE parameter for EGS
     */
    public double getEstepe() {
        return estepe;
    }

    /**
     * 
     * @return maximum density
     */
    public double getMaxDens() {
        return maxDens;
    }

    /**
     * 
     * @return maximum Hounsfield unit
     */
    public int getMaxHU() {
        return maxHU;
    }

    /**
     * 
     * @return minimum density
     */
    public double getMinDens() {
        return minDens;
    }

    /**
     * 
     * @return minimum Hounsfield unit
     */
    public int getMinHU() {
        return minHU;
    }

    /**
     * 
     * @return name
     */
    public String getName() {
        return name;
    }
}
