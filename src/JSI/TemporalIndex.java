/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.infomatiq.jsi;

import java.util.Date;

/**
 *
 * @author ian
 */
public interface TemporalIndex {
    
    public void add(Rectangle r, Date time, int id);
    
    public void delete(int id);
    
    public int newest();
    
    public int oldest();
}
