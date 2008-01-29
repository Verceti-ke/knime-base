/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   14.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.border.Border;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;


/**
 * This Panel holds subpanels consisting of SortItems.
 * 
 * @see SortItem
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeDialogPanel2 extends JPanel {
    private static final long serialVersionUID = -1757898824881266019L;

    /**
     * The entry in the JComboBox for not sorting a column.
     */
    public static final DataColumnSpec NOSORT = new DataColumnSpecCreator(
            "- DO NOT SORT -", DataType.getType(DataCell.class)).createSpec();

    /*
     * Keeps track of the components on this JPanel
     */
    private Vector<SortItem> m_components;

    /*
     * The DataTableSpec
     */
    private DataTableSpec m_spec;
    
    /*
     * Flag for whether to perform the sorting in memory or not.
     */
    private boolean m_memory;
    
    /*
     * Corresponding checkbox
     */
    private JCheckBox m_memorycheckb;

    /**
     * Constructs a new empty JPanel used for displaying the three first
     * selected columns in the according order and the sorting order for each.
     * 
     */
    SorterNodeDialogPanel2() {
        BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
        super.setLayout(bl);
        m_components = new Vector<SortItem>();
        m_memory = false;
    }

    /**
     * Updates this panel based on the DataTableSpec, the list of columns to
     * include and the corresponding sorting order.
     * 
     * @param spec the DataTableSpec
     * @param incl the list to include
     * @param sortOrder the sorting order
     * @param nrsortitems the inital number of sortitems to be shown
     * @param sortInMemory whether to perform the sorting in memory or not
     */
    public void update(final DataTableSpec spec, final List<String> incl,
            final boolean[] sortOrder, final int nrsortitems,
            final boolean sortInMemory) {
        m_spec = spec;
        m_memory = sortInMemory;
        super.removeAll();
        m_components.removeAllElements();
        int interncounter = 0;

        if (spec != null) {
            Vector<DataColumnSpec> values = new Vector<DataColumnSpec>();
            values.add(NOSORT);
            for (int j = 0; j < spec.getNumColumns(); j++) {
                values.add(spec.getColumnSpec(j));
            }
            if ((incl == null) && (sortOrder == null)) {

                for (int i = 0; i < nrsortitems 
                && i < spec.getNumColumns(); i++) {
                    DataColumnSpec selected = 
                            (i == 0) ? values.get(i + 1) : values.get(0);
                    SortItem temp = new SortItem(i, values, selected, true);
                    super.add(temp);
                    m_components.add(temp);
                }
            } else {
                for (int i = 0; i < incl.size(); i++) {
                    String includeString = incl.get(i);
                    int toInclude = spec.findColumnIndex(includeString);
                    if (toInclude != -1) {
                        DataColumnSpec colspec = spec.getColumnSpec(toInclude);
                        SortItem temp =
                                new SortItem(interncounter, values, colspec,
                                        sortOrder[interncounter]);
                        super.add(temp);
                        m_components.add(temp);
                        interncounter++;
                    } else if (includeString.equals(NOSORT.getName())) {
                        SortItem temp = new SortItem(interncounter, values,
                                NOSORT, sortOrder[interncounter]);
                        super.add(temp);
                        m_components.add(temp);
                        interncounter++;
                    }
                }
            }
            Box buttonbox = Box.createHorizontalBox();
            Border addColumnBorder = BorderFactory
                    .createTitledBorder("Add columns");
            buttonbox.setBorder(addColumnBorder);
            int maxCols = m_spec.getNumColumns() - m_components.size();
            
            JButton addSortItemButton = new JButton("new columns");
            final JSpinner spinner = new JSpinner();
            SpinnerNumberModel snm;
            if (maxCols == 0) {
                snm = new SpinnerNumberModel(0, 0, maxCols, 1);
                spinner.setEnabled(false);
                addSortItemButton.setEnabled(false);
            } else {
                snm = new SpinnerNumberModel(1, 1, maxCols, 1);
            }
            spinner.setModel(snm);
            spinner.setMaximumSize(new Dimension(50, 25));
            spinner.setPreferredSize(new Dimension(50, 25));
            NumberEditor ne = (NumberEditor)spinner.getEditor();
            final JFormattedTextField spinnertextfield = ne.getTextField();
            // workaround to ensure same background color
            Color backColor = spinnertextfield.getBackground();
            // when spinner's text field is editable false
            spinnertextfield.setEditable(false);
            spinnertextfield.setBackground(backColor);
           

            addSortItemButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent ae) {
                    ArrayList<String> newlist = new ArrayList<String>();
                    for (int i = 0; i < m_components.size(); i++) {
                        SortItem temp = m_components.get(i);
                        newlist.add(temp.getSelectedColumn().getName());
                    }
                    int oldsize = m_components.size();
                    String temp = spinner.getValue().toString();
                    int newsize = Integer.parseInt(temp);
                    for (int n = oldsize; n < oldsize + newsize; n++) {
                        newlist.add(NOSORT.getName());
                    }
                    boolean[] oldbool = new boolean[oldsize];
                    boolean[] newbool = new boolean[oldsize + newsize];
                    // copy old values
                    for (int i = 0; i < m_components.size(); i++) {
                        SortItem temp2 = m_components.get(i);
                        newbool[i] = temp2.getSortOrder();
                    }
                    // create new values
                    for (int i = oldbool.length; i < newbool.length; i++) {
                        newbool[i] = true;
                    }
                    update(m_spec, newlist, newbool, (oldsize + newsize),
                            m_memory);
                }
            });
            buttonbox.add(spinner);
            buttonbox.add(Box.createHorizontalStrut(10));
            buttonbox.add(addSortItemButton);
            super.add(buttonbox);
            Box memorybox = Box.createHorizontalBox();
            m_memorycheckb = new JCheckBox("Sort in memory", m_memory);
            m_memorycheckb.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent ae) {
                    if (m_memorycheckb.isSelected()) {
                        m_memory = true;
                    } else {
                        m_memory = false;
                    }
                }
            });
            memorybox.add(m_memorycheckb);
            super.add(memorybox);
            revalidate();
        }
    }

    /**
     * Returns all columns from the include list.
     * 
     * @return a list of all columns from the include list
     */
    public List<String> getIncludedColumnList() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < m_components.size(); i++) {
            SortItem temp = m_components.get(i);
            if (!(temp.getSelectedColumn().equals(NOSORT))) {
                list.add(temp.getSelectedColumn().getName());
            }
        }
        return list;
    }

    /**
     * Returns the sortOrder array.
     * 
     * @return sortOrder
     */
    public boolean[] getSortOrder() {
        Vector<Boolean> boolvector = new Vector<Boolean>();
        for (int i = 0; i < m_components.size(); i++) {
            SortItem temp = m_components.get(i);
            if (!(temp.getSelectedColumn().equals(NOSORT))) {
                boolvector.add(temp.getSortOrder());
            }
        }
        boolean[] boolarray = new boolean[boolvector.size()];
        for (int i = 0; i < boolarray.length; i++) {
            boolarray[i] = boolvector.get(i);
        }
        return boolarray;
    }
    
    /**
     * @return whether to perform the sorting in memory or not.
     */
    public boolean sortInMemory() {
        return m_memory;
    }
}
