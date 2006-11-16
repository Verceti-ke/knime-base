/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   08.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.knime.base.util.coordinate.CoordinateMapping;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class LabelPaintUtil {
    
    private static final String DOTS = "..";
    
    private LabelPaintUtil() {
        
    }
    
    /**
     * Possible positions of the label.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public enum Position {
        /** Label on the left side of the coordinate. */
        LEFT,
        /** Label on the bottom of the coordinate. */
        BOTTOM,
        /** Label on the right side of the coordinate. */
        TOP,
        /** Label on top of the coordinate. */
        RIGHT;
    }
    
    /**
     * Rounds the passed double value by the rounding factor and returns a 
     * string representation of it. Typical usage for tooltips.
     * @param value the value to be rounded
     * @param roundingFactor the rounding factor use 100 for two numbers after
     * the comma, 1000 for three numbers after the comma and so on.
     * @return the string representation of the ronded double.
     */
    public static String getDoubleAsString(final double value, 
            final double roundingFactor) {
        long newVal = Math.round(value * roundingFactor);
        return "" + ((double)newVal / roundingFactor);
    }
    
    /**
     * 
     * @param label the label to display.
     * @param g the graphics object
     * @param availableSpace the available space
     * @param position the position
     * @param rotate whether labels are rotated or not.
     */
    public static void drawLabel(final String label, final Graphics2D g, 
            final Rectangle availableSpace, final Position position, 
            final boolean rotate) {
        if (position.equals(Position.LEFT)) {
            drawLeftLabel(label, g, availableSpace, rotate);
            return;
        }
        if (position.equals(Position.BOTTOM)) {
            drawBottomLabel(label, g, availableSpace, rotate);
            return;
        }
        if (position.equals(Position.RIGHT)) {
            drawRightLabel(label, g, availableSpace, rotate);
            return;
        }
        if (position.equals(Position.TOP)) {
            drawTopLabel(label, g, availableSpace, rotate);
        }
        
    }
    
    /**
     * Cuts the passed label until it fits into the desired length by cutting
     * out the middle of the label. The first three and the most possible part
     * of the length are retained. If some charaters are removed "..." is 
     * inserted.
     * @param label the label to cut
     * @param desiredLength the desired length.
     * @param fm the font metrics
     * @return a cutted label which fits into the desired length,
     */
    public static String cutLabel(final String label, final int desiredLength,
            final FontMetrics fm) {
        String newLabel = label;
        if (fm.stringWidth(label) > desiredLength) {
            // check if the first 3 and dots and the last 4 is still 
            // too long
            if (label.length() > 4) {
                newLabel = label.substring(0, 3) + DOTS
                    + label.substring(label.length() - 4, label.length());
            } else {
                return DOTS;
            }
            if (fm.stringWidth(newLabel) > desiredLength) {
                // if so check if the last 4 + dots is too long
                newLabel = DOTS + label.substring(label.length() - 4, 
                        label.length());
                if (fm.stringWidth(newLabel) > desiredLength) {
                    // if so return dots
                    return DOTS;
                } else {
                    return newLabel;
                }
            } else if (fm.stringWidth(newLabel) < desiredLength) {
                // if more space than the first 3 and the last 4 is 
                // available, increase the end until the string fills out the 
                // available length
                int lastIndex = label.length() - 4;
                int firstIndex = 3 + DOTS.length();
                int cutPos = 4;
                while (fm.stringWidth(newLabel) < desiredLength
                        && lastIndex > firstIndex) {
                    cutPos++;
                    lastIndex = label.length() - cutPos;
                    newLabel = label.substring(0, 3)  + DOTS
                    + label.substring(label.length() - cutPos, label.length());
                }
                return newLabel;
            }
            return newLabel;
        }
        return label;
    }
    
    /**
     * Returns true if any label is too long to be displayed in the available
     * space.
     * @param mappings the set of labels to be displayed.
     * @param availableSize the available width
     * @param fm the font metrics
     * @return true if labels should be rotated, false otherwise.
     */
    public static boolean rotateLabels(
            final CoordinateMapping[] mappings, 
            final int availableSize, final FontMetrics fm) {
        for (int i = 0; i < mappings.length; i++) {
            String label = mappings[i].getDomainValueAsString();
            if (fm.stringWidth(label) > availableSize) {
                return true;
            }
        }
        return false;
    }
    
    private static void drawLeftLabel(final String label, final Graphics2D g, 
            final Rectangle availableSpace, final boolean rotate) {
        // TODO: paint as left as possible if the string is short!
        String drawLabel = label;
//      if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            int xPos = availableSpace.x;
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel = cutLabel(label, availableSpace.width, 
                        g.getFontMetrics());
            } else {
                xPos = (availableSpace.x + availableSpace.width) 
                    - g.getFontMetrics().stringWidth(drawLabel);
            }
            // draw it
            g.drawString(drawLabel, xPos, 
                    availableSpace.y + availableSpace.height);
        } else {
            int alpha = 45; 
            double a = availableSpace.width;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            double hypo = Math.floor(a / Math.cos(Math.toRadians(alpha)));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo) {
                drawLabel = cutLabel(label, (int)Math.floor(hypo), 
                        g.getFontMetrics());
            }
            
//            int newX = (availableSpace.x + availableSpace.width) 
//                - g.getFontMetrics().stringWidth(drawLabel) 
//                + g.getFontMetrics().getHeight() / 2;
            double b = Math.sin(Math.toRadians(alpha)) * g.getFontMetrics()
                .stringWidth(drawLabel);
            int newX = (int)(availableSpace.width - b) - g.getFontMetrics()
                .getDescent();
            int newY = availableSpace.y + (int)(availableSpace.getHeight() - b);
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), newX, newY);
            g.drawString(drawLabel, newX, newY);
            g.setTransform(at);
        }
        
    }
    
    private static void drawBottomLabel(final String label, final Graphics2D g, 
            final Rectangle availableSpace, final boolean rotate) {
        String drawLabel = label;
//      if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel = cutLabel(label, availableSpace.width, 
                        g.getFontMetrics());
            }
            // draw it
            g.drawString(drawLabel, availableSpace.x, 
                    availableSpace.y);
        } else {
            // check if it has to be cutted
            int alpha = 45;
            double b = availableSpace.height;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            int hypo = (int)Math.floor(b / Math.cos(Math.toRadians(alpha)));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo 
                    - g.getFontMetrics().getHeight()) {
                drawLabel = cutLabel(label, hypo, g.getFontMetrics());
            }
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), availableSpace.x, 
                    availableSpace.y);
            g.drawString(drawLabel, availableSpace.x, 
                    availableSpace.y);
            g.setTransform(at);
        }        
        
    }
    
    private static void drawRightLabel(final String label, 
            final Graphics2D g, 
            final Rectangle availableSpace, final boolean rotate) {
        // TODO: top y position is still wrong
        String drawLabel = label;
//      if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel = cutLabel(label, availableSpace.width, 
                        g.getFontMetrics());
            }
            // draw it
            g.drawString(drawLabel, availableSpace.x, 
                    availableSpace.y + availableSpace.height);
        } else {
            int alpha = -45;
            // check if it has to be cutted
            int a = availableSpace.width;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            int hypo = (int)Math.floor(a / Math.cos(Math.toRadians(alpha)));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo) {
                drawLabel = cutLabel(label, hypo, g.getFontMetrics());
            }
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), availableSpace.x, 
                    availableSpace.y + availableSpace.height);
            g.drawString(drawLabel, availableSpace.x, 
                    availableSpace.y + availableSpace.height);
            g.setTransform(at);
        }
    }
    
    private static void drawTopLabel(final String label, 
            final Graphics2D g, 
            final Rectangle availableSpace, final boolean rotate) {
        // TODO: top y position is still wrong
        String drawLabel = label;
//      if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel = cutLabel(label, availableSpace.width, 
                        g.getFontMetrics());
            }
            // draw it
            g.drawString(drawLabel, availableSpace.x, 
                    availableSpace.y + availableSpace.height);
        } else {
            int alpha = -45;
            // hypthenuse = a / sin(alpha)
            // a = availableSpace.height
            // check if it has to be cutted
            double a = availableSpace.height;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            int hypo = (int)Math.abs(Math.floor(a 
                    / Math.sin(Math.toRadians(alpha))));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo) {
                drawLabel = cutLabel(label, hypo, g.getFontMetrics());
            }
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), availableSpace.x, 
                    availableSpace.y + availableSpace.height);
            g.drawString(drawLabel, availableSpace.x, 
                    availableSpace.y + availableSpace.height);
            g.setTransform(at);
        }
    }
    

}
