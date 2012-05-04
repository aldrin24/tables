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
package org.opendatakit.tables.Activity.defaultopts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendatakit.tables.Database.ColumnProperty;
import org.opendatakit.tables.Database.DefaultsManager;
import org.opendatakit.tables.Database.TableProperty;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * An activity for setting query defaults.
 */
public class QueryDefaults extends Activity {
	
	private ColumnProperty cp;
	private DefaultsManager dm;
	private TableProperty tp;
	private Map<String, EditText> changedFields;
	
    /**
     * Called when the activity is first created.
     */
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences settings =
			PreferenceManager.getDefaultSharedPreferences(this);
		String tableID = settings.getString("ODKTables:tableID", "1");
		cp = new ColumnProperty(tableID);
		dm = new DefaultsManager(tableID);
		tp = new TableProperty(tableID);
		changedFields = new HashMap<String, EditText>();
		ScrollView sv = new ScrollView(this);
		sv.addView(getView());
		setContentView(sv);
	}
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
    }
	
	@Override
	protected void onPause() {
		super.onPause();
		saveState();
	}
	
	private void saveState() {
        for(String colName : changedFields.keySet()) {
			dm.setQueryColDefault(colName,
					changedFields.get(colName).getText().toString());
        }
        changedFields.clear();
	}
	
	/**
	 * @return a table of the columns and their options
	 */
	private View getView() {
		TableLayout table = new TableLayout(this);
		// adding the availability option (if necessary)
		addAvailOpt(table);
		// adding the per-column options
		Map<String, String> defVals = dm.getQueryColVals();
		Set<String> defInc = dm.getQueryIncCols();
		String gav = dm.getQueryAvailCol();
		for(String col : tp.getColOrderArrayList()) {
			addValOpt(table, col, defVals.get(col), defInc.contains(col), gav);
		}
		return table;
	}
	
	/**
	 * Adds the availability option to the table if there are any daterange
	 * columns.
	 * @param table the table view to add to
	 */
	private void addAvailOpt(TableLayout table) {
		TableRow.LayoutParams lp = new TableRow.LayoutParams(
				TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT, 1);
		lp.span = 2;
		// forming the spinner
		List<String> drCols = new ArrayList<String>();
		List<String> cols = tp.getColOrderArrayList();
		for(String colname : cols) {
			if(("Date Range").equals(cp.getType(colname))) {
				drCols.add(colname);
			}
		}
		if(drCols.isEmpty()) {return;}
		drCols.add(0, "None");
		Spinner drSpin = new Spinner(this);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item,
				drCols.toArray(new String[0]));
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		drSpin.setAdapter(adapter);
		int pos = drCols.indexOf(dm.getQueryAvailCol());
		if(pos < 0) {pos = 0;}
		drSpin.setSelection(pos);
		drSpin.setOnItemSelectedListener(new DRAListener(dm));
		// forming and adding the label
		TextView drLabel = new TextView(this);
		drLabel.setText("Query on availability in:");
		TableRow labelRow = new TableRow(this);
		drLabel.setLayoutParams(lp);
		labelRow.addView(drLabel);
		table.addView(labelRow);
		// adding the table row
		drSpin.setLayoutParams(lp);
		TableRow spinRow = new TableRow(this);
		spinRow.addView(drSpin);
		table.addView(spinRow);
	}
	
	/**
	 * Adds a column and its options to the view
	 * @param table the table view to add to
	 * @param col the column name
	 * @param val the default value
	 * @param inc whether the column is currently set to be included in all
	 *            responses
	 * @param gav the name of the column to base availability on (or null)
	 */
	private void addValOpt(TableLayout table, String col, String val,
			boolean inc, String gav) {
		TableRow.LayoutParams colNameParams = new TableRow.LayoutParams(
				TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT, 1);
		colNameParams.span = 2;
		TableRow.LayoutParams labelParams = new TableRow.LayoutParams(
				TableRow.LayoutParams.WRAP_CONTENT,
				TableRow.LayoutParams.WRAP_CONTENT, 0);
		TableRow.LayoutParams fieldParams = new TableRow.LayoutParams(
				TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT, 1);
		// the border row
		TableRow borRow = new TableRow(this);
		borRow.setMinimumHeight(3);
		borRow.setBackgroundColor(Color.DKGRAY);
		table.addView(borRow);
		// the column name row
		TextView colName = new TextView(this);
		colName.setLayoutParams(colNameParams);
		colName.setText(col);
		TableRow nameRow = new TableRow(this);
		nameRow.addView(colName);
		table.addView(nameRow);
		// the value row
		TextView valLabel = new TextView(this);
		valLabel.setLayoutParams(labelParams);
		valLabel.setText("Value: ");
		EditText valField = new EditText(this);
		valField.setLayoutParams(fieldParams);
		valField.setText(val);
		valField.setOnKeyListener(new TextListener(col, valField));
		TableRow valRow = new TableRow(this);
		valRow.addView(valLabel);
		valRow.addView(valField);
		table.addView(valRow);
		// the include row
		TextView incLabel = new TextView(this);
		incLabel.setLayoutParams(labelParams);
		incLabel.setText("Include? ");
		CheckBox incCheck = new CheckBox(this);
		incCheck.setLayoutParams(fieldParams);
		incCheck.setChecked(inc);
		incCheck.setOnClickListener(new IncListener(col, incCheck, dm));
		TableRow incRow = new TableRow(this);
		incRow.addView(incLabel);
		incRow.addView(incCheck);
		table.addView(incRow);
	}
	
	private class TextListener implements OnKeyListener {
		private String colname;
		private EditText field;
		public TextListener(String colname, EditText field) {
			this.colname = colname;
			this.field = field;
		}
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if((event.getAction() == KeyEvent.ACTION_DOWN) &&
					!changedFields.containsKey(colname)) {
				changedFields.put(colname, field);
			}
			return false;
		}
	}
	
	private class IncListener implements OnClickListener {
		private String colname;
		private CheckBox check;
		private DefaultsManager dm;
		public IncListener(String colname, CheckBox check,
				DefaultsManager dm) {
			this.colname = colname;
			this.check = check;
			this.dm = dm;
		}
		@Override
		public void onClick(View v) {
			dm.setQueryIncCol(colname, check.isChecked());
		}
	}
	
	private class DRAListener implements AdapterView.OnItemSelectedListener {
		DefaultsManager dm;
		protected DRAListener(DefaultsManager dm) {
			this.dm = dm;
		}
		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			if(position == 0) {
				update("");
			} else {
				update(parent.getItemAtPosition(position).toString());
			}
		}
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			update("");
		}
		private void update(String val) {
			dm.setQueryAvailCol(val);
		}
	}
	
}