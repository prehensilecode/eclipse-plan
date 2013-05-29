/*
 * BeamEGSinput.java
 *
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



/*
 * $Id: BeamEGSinput.java 253 2008-06-24 18:25:24Z dwchin $ 
 */

package edu.harvard.lroc.eclipseplan;

/**
 * Encapsulates data needed to create a BEAMnrc input file
 * @author David Chin
 * @version $Revision: 253 $
 */
public class BeamEGSinput {
    
    /** Creates a new instance of BeamEGSinput */
    public BeamEGSinput() {
    }

    /**
     * Array of beams for the run.
     */
    private Beam[] beam;

    /**
     * Indexed getter for property beam.
     * @param index Index of the property.
     * @return Value of the property at <CODE>index</CODE>.
     */
    public Beam getBeam(int index) {
        return this.beam[index];
    }

    /**
     * Indexed setter for property beam.
     * @param index Index of the property.
     * @param beam New value of the property at <CODE>index</CODE>.
     */
    public void setBeam(int index, Beam beam) {
        this.beam[index] = beam;
    }

}
