package com.tareq.employee;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CursorAdapter;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.opencsv.CSVWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class HomeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    //views
    private RecyclerView employeeRecyclerView;
    private SwipeRefreshLayout parentSwipeRefreshLayout;

    //recycler view helpers
    private EmployeeAdapter adapter;
    private FloatingActionButton employeeAddFloatingButton;
    private List<Employee> employeeList = new ArrayList<>();

    //request codes
    private final int REQ_EXT_READ = 1;
    public static final int MODE_UPDATE = 1, MODE_DELETE = 2, MODE_COMPLETED = 3, MODE_SHOW_ALL = 4, MODE_INSERT=5, MODE_IMPORT=6;


    private int indexToUpdate = -1, mode = MODE_SHOW_ALL;
    private Employee newEmployeeData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //initialization
        employeeAddFloatingButton = findViewById(R.id.add_employee_floating_button);
        employeeRecyclerView = findViewById(R.id.employee_recycler_view);
        employeeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        parentSwipeRefreshLayout = findViewById(R.id.parent_refresh_layout);

        //swipe refreshLayout setup
        parentSwipeRefreshLayout.setRefreshing(true);

        //recyclerview setup
        adapter = new EmployeeAdapter(this, employeeList);
        employeeRecyclerView.setAdapter(adapter);


        //database reader callback setup
        getLoaderManager().initLoader(0, null, HomeActivity.this);


        //click call back setup
        employeeAddFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode=MODE_INSERT;
                gotoAddDataActivity();
            }
        });


        parentSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.notifyDataSetChanged();
                parentSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void gotoAddDataActivity() {
        Intent i = new Intent(getBaseContext(), AddDataActivity.class);
        startActivityForResult(i,MODE_INSERT);
    }


    //called when initialization is invoked of the Loader
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        Loader<Cursor> loader = new CursorLoader(this, DatabaseContentProvider.CONTENT_URI, null, null, null, null);
        return loader;
    }


    //called on each database update
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, final Cursor data) {

        if (mode == MODE_SHOW_ALL) {
            employeeList.clear();
            loadDataToRecyclerView(data);
        } else if (mode == MODE_DELETE) {
            employeeList.remove(indexToUpdate);
            adapter.notifyItemRemoved(indexToUpdate);
            adapter.notifyItemRangeChanged(indexToUpdate, adapter.getItemCount());
            mode = MODE_COMPLETED;
        } else {
            //do nothing, wait for the child activity end, and it itself
        }

    }

    public void notifyWillChange(int mode, int index) {
        indexToUpdate = index;
        this.mode = mode;
    }

    public void notifyWillChange(int mode) {
        this.mode = mode;
    }

    //loading data to recycler view
    private void loadDataToRecyclerView(final Cursor data) {



        new Thread(new Runnable() {
            @Override
            public void run() {
                //reading each row
                while (data.moveToNext()) {
                    //parsing the index of column of a row
                    int index0 = data.getColumnIndex(DatabaseOpenHelper.EMPLOYEE_ID);
                    int index1 = data.getColumnIndex(DatabaseOpenHelper.EMPLOYEE_NAME);
                    int index2 = data.getColumnIndex(DatabaseOpenHelper.EMPLOYEE_AGE);
                    int index3 = data.getColumnIndex(DatabaseOpenHelper.EMPLOYEE_GENDER);

                    //parsing data from the index
                    int id = data.getInt(index0);
                    String name = data.getString(index1);
                    int age = data.getInt(index2);
                    int gender = data.getInt(index3);

                    //creating new employee and add to list
                    Employee contact = new Employee(id, name, age, gender);
                    employeeList.add(contact);
                }

                //updating the UI from the other thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        //turning of the refresh layout as well as lock it
                        parentSwipeRefreshLayout.setRefreshing(false);
                        //parentSwipeRefreshLayout.setEnabled(false);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }


    //adding the import/export logo into into the actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_action_bar, menu);
        return true;
    }

    //actionbar item selection callbacks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_import:
                gotoImportDataFromCSVActivity();
                return true;
            case R.id.action_export:
                if (EmployeeUtil.isPermissionGranted(HomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQ_EXT_READ)) {
                    exportData();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //showing the importing window
    private void gotoImportDataFromCSVActivity() {
        Intent i = new Intent(getBaseContext(), ImportActivity.class);
        mode=MODE_SHOW_ALL;
        startActivity(i);
    }

    //exporting the csv and zip
    private void exportData() {
        //getting the saving path
        final File pathToSave = new File(EmployeeUtil.getExportFolderPath());

        //showing loading dialog
        final ProgressDialog dialog = ProgressDialog.show(HomeActivity.this, "",
                "Exporting. Please wait...", true);
        dialog.show();

        //thread for the huge writing task, writing csv and also the images in the zip
        new Thread(new Runnable() {
            @Override
            public void run() {
                //if both operation is success
                if (exportDataToCSV(pathToSave) && makeZip(pathToSave)) {
                    //UI updating from the the other thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, "Exported to \\0\\EmployeeData", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });


            }
        }).start();
    }

    //returns true if success to write the csv
    private boolean exportDataToCSV(final File pathToSave) {
        //no data in the list, so return
        if (employeeList.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(HomeActivity.this, "Nothing to export!", Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }

        try {
            //creating the empty csv file in the path
            String csvFileNameStr = EmployeeUtil.exportCSVFileName;
            File csvFile = new File(pathToSave, csvFileNameStr);
            csvFile.createNewFile();

            //initializing the csv writer
            CSVWriter writer;
            FileWriter csvFileWriter = new FileWriter(csvFile);
            writer = new CSVWriter(csvFileWriter);

            //the first column is for the column information
            String[] columnName = {DatabaseOpenHelper.EMPLOYEE_NAME, DatabaseOpenHelper.EMPLOYEE_AGE, DatabaseOpenHelper.EMPLOYEE_GENDER};
            writer.writeNext(columnName);

            //write all of the employee data available in the current list
            for (Employee emp : employeeList) {
                String[] employeeDataList = {emp.getName(), String.valueOf(emp.getAge()), emp.getGenderStr()};
                writer.writeNext(employeeDataList);
            }


            //deinitialization of the csv writer
            writer.flush();
            writer.close();
            return true;

        } catch (final Exception e) {
            //showing the error while writing csv in UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(HomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return false;

        }
    }


    //returns true if success to write the zip
    private boolean makeZip(File pathToSave) {
        //getting all the available files in the internal directory image path
        String pathStr = EmployeeUtil.getInternalImagesPath(this);
        File imageDirectoryFile = new File(pathStr);
        File[] imageFileList = imageDirectoryFile.listFiles();

        //if no images in the directory, exit now
        if (imageFileList.length == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(HomeActivity.this, "No images found!", Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }

        //creating empty zip file in the directory
        String zipFileNameStr = EmployeeUtil.exportZipFileName;
        File zipFile = new File(pathToSave, zipFileNameStr);

        try {
            //initializing buffer for reading
            final int BUFFER = 2048;
            BufferedInputStream origin = null;
            byte data[] = new byte[BUFFER];

            //initializing stream for writing
            FileOutputStream dest = new FileOutputStream(zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));


            //reading & writing each images
            for (int i = 0; i < imageFileList.length; i++) {
                FileInputStream fi = new FileInputStream(imageFileList[i]);
                origin = new BufferedInputStream(fi, BUFFER);

                //zip name is the sequence number in the directory
                ZipEntry entry = new ZipEntry(String.valueOf(i + 1) + ".png");
                out.putNextEntry(entry);
                int count;
                //adding to zip
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            //deint the stream
            out.close();
            return true;
        } catch (final Exception e) {
            //error while writing zip
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(HomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return false;

        }
    }

    //invoked when user accepts or denies permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_EXT_READ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportData();
            } else {
                Toast.makeText(HomeActivity.this, "Write Permission is required to export the data!", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void updateNewData(Intent data) {
        indexToUpdate = data.getIntExtra(AddDataActivity.TEXT_POSITION, -1);
        int id = data.getIntExtra(AddDataActivity.TEXT_ID, -1);
        String newName = data.getStringExtra(AddDataActivity.TEXT_NAME);
        int newAge = data.getIntExtra(AddDataActivity.TEXT_AGE, -1);
        int newGender = data.getIntExtra(AddDataActivity.TEXT_GENDER, -1);
        Employee updatedEmp = new Employee(id, newName, newAge, newGender);

        if(mode==MODE_UPDATE) {

            employeeList.set(indexToUpdate, updatedEmp);
            adapter.notifyItemChanged(indexToUpdate);
        } else if(mode==MODE_INSERT){

            employeeList.add(updatedEmp);
            adapter.notifyItemInserted(employeeList.size());
        }
        mode=MODE_COMPLETED;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Collect data from the intent and use it
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case MODE_INSERT:
                case MODE_UPDATE:
                    updateNewData(data);
                    break;
            }
        }
    }
}