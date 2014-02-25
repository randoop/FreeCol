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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.BooleanOptionUI;
import net.sf.freecol.client.gui.option.FileOptionUI;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the map generator options.
 *
 * @see MapGeneratorOptions
 * @see OptionGroup
 */
public final class MapGeneratorOptionsDialog extends OptionsDialog {

    private static final Logger logger = Logger.getLogger(MapGeneratorOptionsDialog.class.getName());

    private static final FileFilter fsgFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".fsg");
            }
        };


    /**
     * Creates a dialog to set the map generator options.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param mgo The map generator option group.
     * @param editable Whether the options may be edited.
     */
    public MapGeneratorOptionsDialog(FreeColClient freeColClient,
                                     boolean editable) {
        super(freeColClient, editable,
            freeColClient.getGame().getMapGeneratorOptions(),
            Messages.message(MapGeneratorOptions.getXMLElementTagName()),
            FreeColDirectories.MAP_GENERATOR_OPTIONS_FILE_NAME,
            MapGeneratorOptions.getXMLElementTagName());

        if (isEditable()) {
            loadDefaultOptions();
            // TODO: The update should be solved by PropertyEvent.
            File mapDirectory = FreeColDirectories.getMapsDirectory();
            if (mapDirectory.isDirectory()) {
                File[] files = mapDirectory.listFiles(fsgFilter);
                Arrays.sort(files, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            return f1.getName().compareTo(f2.getName());
                        }
                    });
                JPanel mapPanel = new JPanel();
                for (final File file : files) {
                    JButton mapButton = makeMapButton(file);
                    if (mapButton == null) continue;
                    mapButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                updateFile(file);
                            }
                        });
                    mapPanel.add(mapButton);
                }

                JScrollPane scrollPane = new JScrollPane(mapPanel,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                scrollPane.getViewport().setOpaque(false);
                // TODO: find out how to do this properly
                scrollPane.setMinimumSize(new Dimension(400, 110));

                panel.add(scrollPane);
            }
        }
        initialize();
    }


    /**
     * Update the selected map file.
     *
     * The option UI may not have been created if we just click on the
     * map, because the text field is under mapGeneratorOptions.import.
     * Once the import submenu is opened, it is better to use the
     * option UIs under it.
     *
     * @param file The new map <code>File</code>.
     */
    private void updateFile(File file) {
        final OptionGroup mgo = freeColClient.getGame()
            .getMapGeneratorOptions();
        OptionGroupUI mgoUI = getOptionUI();
        FileOptionUI foui = (FileOptionUI)mgoUI
            .getOptionUI(MapGeneratorOptions.IMPORT_FILE);
        if (foui == null) {
            FileOption fileOption = (FileOption)mgo
                .getOption(MapGeneratorOptions.IMPORT_FILE);
            BooleanOption iTerrain = (BooleanOption)mgo
                .getOption(MapGeneratorOptions.IMPORT_TERRAIN);
            BooleanOption iBonuses = (BooleanOption)mgo
                .getOption(MapGeneratorOptions.IMPORT_BONUSES);
            BooleanOption iRumour = (BooleanOption)mgo
                .getOption(MapGeneratorOptions.IMPORT_RUMOURS);
            BooleanOption iSettlement = (BooleanOption)mgo
                .getOption(MapGeneratorOptions.IMPORT_SETTLEMENTS);
            fileOption.setValue(file);
            iTerrain.setValue(true);
            iBonuses.setValue(false);
            iRumour.setValue(false);
            iSettlement.setValue(false);
            mgoUI.reset();
        } else {
            foui.setValue(file);
            BooleanOptionUI terrainUI = (BooleanOptionUI)mgoUI
                .getOptionUI(MapGeneratorOptions.IMPORT_TERRAIN);
            BooleanOptionUI bonusesUI = (BooleanOptionUI)mgoUI
                .getOptionUI(MapGeneratorOptions.IMPORT_BONUSES);
            BooleanOptionUI rumourUI = (BooleanOptionUI)mgoUI
                .getOptionUI(MapGeneratorOptions.IMPORT_RUMOURS);
            BooleanOptionUI settlementsUI = (BooleanOptionUI)mgoUI
                .getOptionUI(MapGeneratorOptions.IMPORT_SETTLEMENTS);
            terrainUI.setValue(true);
            bonusesUI.setValue(false);
            rumourUI.setValue(false);
            settlementsUI.setValue(false);
        }
    }

    /**
     * Make a map button for a given map file.
     *
     * @param file The map <code>File</code>.
     * @return A <code>JButton</code> if the map is readable, or null
     *     on failure.
     */
    private JButton makeMapButton(File file) {
        String mapName = file.getName().substring(0, file.getName()
                                                         .lastIndexOf('.'));
        JButton mapButton = new JButton(Messages.message("freecol.map."
                + mapName));
        try {
            FreeColSavegameFile savegame = new FreeColSavegameFile(file);
            Image thumbnail = ImageIO.read(savegame
                .getInputStream(FreeColSavegameFile.THUMBNAIL_FILE));
            mapButton.setIcon(new ImageIcon(thumbnail));
            try {
                Properties properties = new Properties();
                properties.load(savegame
                    .getInputStream(FreeColSavegameFile.SAVEGAME_PROPERTIES));
                mapButton.setToolTipText(properties.getProperty("map.width")
                    + "\u00D7"
                    + properties.getProperty("map.height"));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to load savegame.", e);
                return null;
            }
            mapButton.setHorizontalTextPosition(JButton.CENTER);
            mapButton.setVerticalTextPosition(JButton.BOTTOM);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to read thumbnail.", e);
        }
        return mapButton;
    }


    // Override OptionsDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup getResponse() {
        OptionGroup value = super.getResponse();
        if (value != null) {
            freeColClient.getPreGameController().updateMapGeneratorOptions();
            if (isEditable() && !freeColClient.isMapEditor()) {
                saveDefaultOptions();
            }
        }
        return value;
    }
}
