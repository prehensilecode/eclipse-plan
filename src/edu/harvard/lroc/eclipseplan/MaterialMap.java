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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Emulate an immutable map of material names to material numbers. This is
 * a singleton.
 * @author David Chin
 * @version $Revision$
 */
public class MaterialMap {
    private MaterialMap() {
        // exists only to defeat instantiation
    }
    
    public static MaterialMap getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MaterialMap();
            
            map.put("AIR700ICRU", 1);
            map.put("LUNG700ICRU", 2);
            map.put("ICRUTISSUE700ICRU", 3);
            map.put("ICRPBONE700ICRU", 4);
            map.put("H2O700ICRU", 5);
        }
        
        return INSTANCE;
    }
    
    /** Singleton instance */
    private static MaterialMap INSTANCE = null;
    
    
    // All the map methods
    public static Integer get(String key) {
        return map.get(key);
    }
    
    public static Set<String> keySet() {
        return map.keySet();
    }
    
    public static Set<Map.Entry<String,Integer>> entrySet() {
        return map.entrySet();
    }
    
    public static Collection<Integer> values() {
        return map.values();
    }
    
    public static boolean containsKey(Object key) {
        return map.containsKey(key);
    }
    
    public static boolean containsValue(Object value) {
        return map.containsValue(value);
    }
    
    public static boolean isEmpty() {
        return map.isEmpty();
    }
    
    public static int size() {
        return map.size();
    }
    
    
    // the material map needs to have a predictable order because the list
    // of materials written to the header of the egsphant file must be 
    // ordered by the material numbers
    /** The actual map   */
    private static Map<String, Integer> map = new LinkedHashMap<String, Integer>();
    
    
    public static void main(String[] args) {
        MaterialMap m = MaterialMap.getInstance();
        
        for (String s : MaterialMap.keySet()) 
            System.out.println(s);
        
        System.out.println(MaterialMap.entrySet().toString());
    }
}
