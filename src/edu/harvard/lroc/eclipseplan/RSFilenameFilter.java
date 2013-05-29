/* RSFilenameFilter.java */

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

/* $Id: RSFilenameFilter.java 253 2008-06-24 18:25:24Z dwchin $ */

package edu.harvard.lroc.eclipseplan;

import java.io.File;
import java.io.FilenameFilter;

/** 
 * Filter for Eclipse structure file names 
 * @author David Chin
 * @version $Revision: 253 $
 */
public class RSFilenameFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return (name.startsWith("RS_") || name.startsWith("RS.")) && 
                name.endsWith(".dcm");
    }

}
