/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;


/**
 * The panel that allows a user to pay for a new unit in Europe.
 */
public class NewUnitPanel extends FreeColPanel {

    /** The main label for the panel. */
    private final JLabel question;

    /** The buttons to display. */
    private final List<JButton> buttons = new ArrayList<JButton>();

    /** The unit types corresponding to the buttons. */
    private final List<UnitType> units = new ArrayList<UnitType>();

    /** A comparator for unit prices. */
    private final Comparator<UnitType> priceComparator;

    /** Is there at least one available unit? */
    private boolean shouldEnable = false;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param layout The <code>LayoutManager</code> to use.
     * @param label The label for the panel.
     * @param units A list of <code>UnitType</code>s to offer.
     */
    public NewUnitPanel(FreeColClient freeColClient, LayoutManager layout,
                        String label, List<UnitType> units) {
        super(freeColClient, layout);

        final Europe europe = getMyPlayer().getEurope();

        this.question = new JLabel(label);
        this.units.addAll(units);
        this.priceComparator = new Comparator<UnitType>() {
            public int compare(final UnitType u1, final UnitType u2) {
                return europe.getUnitPrice(u1) - europe.getUnitPrice(u2);
            }
        };

        okButton.setText(Messages.message("newUnitPanel.ok"));

        update();
    }

    /**
     * Updates this panel's labels so that the information it displays
     * is up to date.
     */
    public void update() {
        removeAll();

        final Player player = getMyPlayer();
        final Europe europe = player.getEurope();

        add(question, "span, wrap 20");

        // The prices may have changed, recreate the buttons
        Collections.sort(units, priceComparator);
        buttons.clear();
        for (UnitType ut : units) {
            int price = europe.getUnitPrice(ut);
            boolean enable = player.checkGold(price);
            JButton newButton = new JButton();
            newButton.setLayout(new MigLayout("wrap 2", "[60]", "[30][30]"));
            ImageIcon icon = getLibrary()
                .getUnitImageIcon(ut, "model.role.default", !enable, 0.66);
            JLabel name = localizedLabel(ut.getNameKey());
            name.setEnabled(enable);
            JLabel gold = localizedLabel(StringTemplate.template("goldAmount")
                .addAmount("%amount%", price));
            gold.setEnabled(enable);
            newButton.setEnabled(enable);
            newButton.add(new JLabel(icon), "span 1 2");
            newButton.add(name);
            newButton.add(gold);
            newButton.setActionCommand(ut.getId());
            newButton.addActionListener(this);
            buttons.add(newButton);
            add(newButton, "grow");
        }

        add(okButton, "newline 20, span, tag ok");

        setSize(getPreferredSize());
        revalidate();

        shouldEnable = player.checkGold(europe.getUnitPrice(units.get(0)));
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            super.actionPerformed(event);
        } else {
            UnitType unitType = getSpecification().getUnitType(command);
            getController().trainUnitInEurope(unitType);
            // Close early if there is nothing affordable remaining.
            getGUI().updateEuropeanSubpanels();
            if (!shouldEnable) getGUI().removeFromCanvas(this);            
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        
        removeAll();
        for (JButton jb : buttons) {
            if (jb != null) jb.setLayout(null);
        }
        buttons.clear();            
    }
}