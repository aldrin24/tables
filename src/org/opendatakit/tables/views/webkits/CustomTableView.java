/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.views.webkits;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebViewClient;


public class CustomTableView extends CustomView {

    private static final String DEFAULT_HTML =
        "<html><body>" +
        "<p>No filename has been specified.</p>" +
        "</body></html>";

    private Activity mActivity;
    private Map<String, Integer> colIndexTable;
    private TableProperties tp;
    private UserTable table;
    private String filename;

    private CustomTableView(Activity activity, String filename) {
        super(activity);
        this.mActivity = activity;
        this.filename = filename;
        colIndexTable = new HashMap<String, Integer>();
    }

    public static CustomTableView get(Activity activity, TableProperties tp,
            UserTable table, String filename) {
        CustomTableView ctv = new CustomTableView(activity, filename);
        ctv.set(tp, table);
        return ctv;
    }

    private void set(TableProperties tp, UserTable table) {
        this.tp = tp;
        this.table = table;
        colIndexTable.clear();
        ColumnProperties[] cps = tp.getColumns();
        for (int i = 0; i < cps.length; i++) {
            colIndexTable.put(cps[i].getDisplayName(), i);
            String smsLabel = cps[i].getSmsLabel();
            if (smsLabel != null) {
                colIndexTable.put(smsLabel, i);
            }
        }
    }

    ////////////////////////////// TEST ///////////////////////////////

    public static CustomTableView get(Activity activity, TableProperties tp, 
        UserTable table, String filename, int index) {
    	CustomTableView ctv = new CustomTableView(activity, filename);
    	// Create a new table with only the row specified at index.
    	// Create all of the arrays necessary to create a UserTable.
    	String[] rowIds = new String[1];
    	String[] headers = new String[table.getWidth()];
    	String[][] data = new String[1][table.getWidth()];
    	String[] footers = new String[table.getWidth()];
    	// Set all the data for the table.
    	rowIds[0] = table.getRowId(index);
    	for (int i = 0; i < table.getWidth(); i++) {
    		headers[i] = table.getHeader(i);
    		data[0][i] = table.getData(index, i);
    		footers[i] = table.getFooter(i);
    	}
    	UserTable singleRowTable = new UserTable(rowIds, headers, data, footers);

    	ctv.set(tp, singleRowTable);
    	return ctv;
    }
    
    /**
     * Returns a custom view based on the list of indexes. The rows will be
     * ordered by the order of the list of indexes.
     * 
     * @param context
     *          The context that wants to display this custom view.
     * @param tp
     *          The table properties of the table being displayed.
     * @param table
     *          The full table that we want to display a portion of.
     * @param filename
     *          The filename of the view we want to create.
     * @param indexes
     *          The indexes, of what rows, and in what order, we want to show
     *          them.
     * @return The custom view that represents the indexes in the table.
     */
    public static CustomTableView get(Activity activity, TableProperties tp, UserTable table,
        String filename, List<Integer> indexes) {
      CustomTableView ctv = new CustomTableView(activity, filename);
      // Create all of the arrays necessary to create a UserTable.
      String[] rowIds = new String[indexes.size()];
      String[] headers = new String[table.getWidth()];
      String[][] data = new String[indexes.size()][table.getWidth()];
      String[] footers = new String[table.getWidth()];
      // Set all the data for the table.
      for (int i = 0; i < table.getWidth(); i++) {
        headers[i] = table.getHeader(i);
        for (int j = 0; j < indexes.size(); j++) {
          rowIds[j] = table.getRowId(indexes.get(j));
          data[j][i] = table.getData(indexes.get(j), i);
        }
        footers[i] = table.getFooter(i);
      }
      UserTable multiRowTable = new UserTable(rowIds, headers, data, footers);

      ctv.set(tp, multiRowTable);
      return ctv;
    }

    public void setOnFinishedLoaded(WebViewClient client) {
    	webView.setWebViewClient(client);
    }

    //////////////////////////// END TEST /////////////////////////////

    public void display() {
      // Load a basic screen as you're getting the other stuff ready to
      // clear the old data.
        webView.addJavascriptInterface(new TableControl(mActivity), "control");
        webView.addJavascriptInterface(new TableData(tp, table), "data");
        if (filename != null) {
            load(FileProvider.getAsUrl(getContext(), new File(filename)));
        } else {
            loadData(DEFAULT_HTML, "text/html", null);
        }
        initView();
    }

    private class TableControl extends Control {

        public TableControl(Activity activity) {
            super(activity);
        }

        @SuppressWarnings("unused")
        public boolean openItem(int index) {
            Controller.launchDetailActivity(mActivity, tp, table, index);
            return true;
        }
    }
}
