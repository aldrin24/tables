package org.opendatakit.tables.sync.files;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewSettings;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

/**
 * The SyncAdapter for syncing files on the server to the phone. This is called
 * a sync, but in reality at this point in time it is just a way to download 
 * the most recent version of files from the server.
 * <p>
 * This is modeled off of TablesSyncAdapter and the manifest checking from 
 * Collect.
 * @author sudar.sam@gmail.com
 *
 */
public class FileSyncAdapter extends AbstractThreadedSyncAdapter {
  
  private static final String TAG = "FileSyncAdapter";
  
  private final Context context;
  
  public FileSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    this.context = context;
  }

  /**
   * Download the files from the server.
   */
  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    Log.d(TAG, "in onPerformSync");
    android.os.Debug.waitForDebugger();
    Preferences prefs = new Preferences(this.context);
    String aggregateUri = prefs.getServerUri();
    String authToken = prefs.getAuthToken();
    
    DbHelper dbh = DbHelper.getDbHelper(context);
    DataManager dm = new DataManager(dbh);
    TableProperties[] tableProperties = dm.getTablePropertiesForTablesSetToSync(
        KeyValueStore.Type.SERVER);
    for (TableProperties tableProp : tableProperties) {
      SyncUtilities.pullKeyValueEntriesForTable(context, aggregateUri, 
          authToken, tableProp);
      /*
       * We are going to hack something together for now that updates the list
       * and detail views for the table that we just pulled. Eventually we want
       * to be able to just pull the keys, put them in the key value store, and 
       * have this work with the settings. For instance, right now we pull
       * down entries and put them into the store. If we had a file, it will
       * say "list" as they key. However, Tables doesn't now how to deal with
       * that filename unless it is in the collection overview object. This 
       * object is stored in the key value store as well, but as a complicated
       * JSON object. We eventually want to move away from this and put have 
       * the JSON keys exist as keys themselves in the key value store, in 
       * which case, eventually, "list" or whatever the key would be when you
       * pull from the server would be able to be used directly. For now that
       * isn't how it works, however. We have the "list" key in the key value
       * store at this point (assuming that such a key was uploaded with a file
       * to the server). 
       * 
       * To make Tables know about it, however, we also need to set it in the
       * object that is stored in TableProperties. 
       * 
       * TODO: remove this special case hack for detail and list, and make it
       * all conform to the grand vision set forth above.
       */
      // So first we're going to check for list and detail keys. This means 
      // that one the server they must exist as "list" and "detail".
      try {
        SQLiteDatabase db = dbh.getReadableDatabase();
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStore kvs = kvsm.getStoreForTable(tableProp.getTableId(), 
            tableProp.getBackingStoreType());
        List<String> desiredKeys = new ArrayList<String>();
        desiredKeys.add(TableViewSettings.KEY_LIST_FILE);
        // now check the value. if there is an entry, we should set it.
        List<OdkTablesKeyValueStoreEntry> entries = 
            kvs.getEntriesForKeys(db, desiredKeys);
        if (entries.size() > 1) {
          Log.e(TAG, "query for " + TableViewSettings.KEY_LIST_FILE +
              " for table " + tableProp.getTableId() + " in the kvs of type " +
              tableProp.getBackingStoreType() + " returned " + entries.size()
              + " entries. It should be at most one.");
        }
        if (entries.size() > 0) {
          // this means we got something. Doing check > 0 rather than == 1 just
          // to try and fail more gracefully if something has gone wrong with
          // the set invariant.
          TableViewSettings ovViewSettings = 
              tableProp.getOverviewViewSettings();
          ovViewSettings.setCustomListFilename(entries.get(0).value);
        }
        // and now check for detail.
        desiredKeys.clear();
        desiredKeys.add(TableProperties.KEY_DETAIL_VIEW_FILE);
        entries = 
            kvs.getEntriesForKeys(db, desiredKeys);
        if (entries.size() > 1) {
          Log.e(TAG, "query for " + TableProperties.KEY_DETAIL_VIEW_FILE +
              " for table " + tableProp.getTableId() + " in the kvs of type " +
              tableProp.getBackingStoreType() + " returned " + entries.size()
              + " entries. It should be at most one.");
        }
        if (entries.size() > 0) {
          // this means we got something. Doing check > 0 rather than == 1 just
          // to try and fail more gracefully if something has gone wrong with
          // the set invariant.
          tableProp.setDetailViewFilename(entries.get(0).value);
        }
      } finally {
        //TODO: fix when to close db problem.
        // here we also want to try and stop syncing the file. This is b/c we
        // don't want to keep on synching in the background, or else you start
        // throwing errors about html issues. This might go away if we move
        // away from sync adapters as the pattern.
        ContentResolver.setIsSyncable(account, "org.opendatakit.tables.tablefilesauthority", 0);
        ContentResolver.setSyncAutomatically(account, "org.opendatakit.tables.tablefilesauthority", false);
      }
    }
    

  }

}