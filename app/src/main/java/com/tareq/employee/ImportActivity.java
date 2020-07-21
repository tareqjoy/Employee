package com.tareq.employee;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ImportActivity extends AppCompatActivity {

    //views
    private EditText csvPathEditText, zipPathEditText;
    private Button cancelButton, importButton, csvChooseButton, zipChooseButton;

    //request codes
    private final int CSV_SELECT_CODE = 1, ZIP_SELECT_CODE = 2;
    private final int REQ_EXT_READ_CSV = 1, REQ_EXT_READ_ZIP = 2;

    //file URI
    private Uri csvUri = null, zipUri = null;

    private int successParsedInt = 0, totalRowInt = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        //setting up the back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //initializing the views
        csvPathEditText = findViewById(R.id.csv_path_edit_text);
        zipPathEditText = findViewById(R.id.zip_path_edit_text);
        cancelButton = findViewById(R.id.cancel_button);
        importButton = findViewById(R.id.import_button);
        csvChooseButton = findViewById(R.id.csv_path_choose_button);
        zipChooseButton = findViewById(R.id.zip_path_choose_button);

        //setting up click call backs
        //cancel button should finish the activity
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //opens csv file chooser if permission is granted or request new permission
        csvChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EmployeeUtil.isPermissionGranted(ImportActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQ_EXT_READ_CSV)) {
                    //already granted
                    showFileChooser(CSV_SELECT_CODE);
                }
            }
        });

        //opens zip file chooser if permission is granted or request new permission
        zipChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EmployeeUtil.isPermissionGranted(ImportActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQ_EXT_READ_CSV)) {
                    //already granted
                    showFileChooser(ZIP_SELECT_CODE);
                }
            }
        });

        //start importing if input file path is okay
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    startImporting();
                }
            }
        });

    }

    //returns true if all inputs are correct
    private boolean validateInput() {
        if (TextUtils.isEmpty(csvPathEditText.getText().toString())) {
            Toast.makeText(ImportActivity.this, "Choose a csv file.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(zipPathEditText.getText().toString())) {
            Toast.makeText(ImportActivity.this, "Choose a zip file.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //start import from csv and zip
    private void startImporting() {
        //showing the loading dialog
        final ProgressDialog dialog = ProgressDialog.show(ImportActivity.this, "", "Importing. Please wait...", true);
        dialog.show();

        //reading is large task, must be used in thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //reading files using the URI found by file chooser
                readZipFile(zipUri);
                parseCSV(csvUri);

                //after successfully importing
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        csvPathEditText.setText("");
                        zipPathEditText.setText("");
                        dialog.dismiss();
                        showParseLog();
                    }
                });

            }
        }).start();
    }

    //showing statistics of importing report
    private void showParseLog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("From "+totalRowInt+" items, "+successParsedInt + " items successfully added.");
        builder.setCancelable(true);
        builder.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ImportActivity.this.finish();
                    }
                });

        AlertDialog alert11 = builder.create();
        alert11.show();

    }

    //parsing zip file from uri
    private boolean readZipFile(Uri from) {
        try {
            //initializing the stream for input
            ZipInputStream zipInputStream = new ZipInputStream(getContentResolver().openInputStream(from));
            ZipEntry zipEntry = null;

            //getting the extracting path
            String internalImagesPath = EmployeeUtil.getInternalImagesPath(this);

            //for ecch file in the zip
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {

                //extracting the file into the internal image folder but with temp_ prefix
                File file = new File(internalImagesPath + "temp_" + zipEntry.getName());

                //setting up output stream for writing
                FileOutputStream fout = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int length = 0;

                //writing the image buffer by buffer
                while ((length = zipInputStream.read(buffer)) > 0) {
                    fout.write(buffer, 0, length);
                }

                //deinit the streams
                zipInputStream.closeEntry();
                fout.close();

                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                bitmap = EmployeeUtil.scaleDown(bitmap,512);
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);


            }
            zipInputStream.close();
            return true;
        } catch (final Exception e) {
            //showing the error from other thread to UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ImportActivity.this, "Error while reading zip file", Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
    }


    //parsing csv file from uri
    private boolean parseCSV(Uri from) {
        try {
            //setting up input streams
            InputStream inputStream = getContentResolver().openInputStream(from);
            CSVReader dataRead = new CSVReader(new InputStreamReader(inputStream));
            String[] nextLine;

            //tracking row index
            int rowInt = 0;
            successParsedInt = 0;

            //traversing all csv rows
            while ((nextLine = dataRead.readNext()) != null) {

                //for the first row, the column description, should not be treated as data row
                if (nextLine[0].equals(DatabaseOpenHelper.EMPLOYEE_NAME)) {
                    continue;
                }

                //increase row index
                rowInt++;

                //for inserting into database
                ContentValues values = new ContentValues();

                //0 is name column
                values.put(DatabaseOpenHelper.EMPLOYEE_NAME, nextLine[0]);

                //1 is age column
                try {
                    //if it is a valid number
                    int ageInt = Integer.parseInt(nextLine[1]);
                    values.put(DatabaseOpenHelper.EMPLOYEE_AGE, ageInt);
                } catch (Exception e) {
                    //if not a valid int, ignore the entire row and also delete the corresponding extracted image
                    deleteTempImageFile(rowInt);
                    continue;
                }

                //2 is gender column
                //0 means male
                //1 means female
                if (nextLine[2].equalsIgnoreCase(Employee.MALE)) {
                    values.put(DatabaseOpenHelper.EMPLOYEE_GENDER, 0);
                } else if (nextLine[2].equalsIgnoreCase(Employee.FEMALE)) {
                    values.put(DatabaseOpenHelper.EMPLOYEE_GENDER, 1);
                } else {
                    //if not valid input, ignore entire row, delete the corresponding extracted image
                    deleteTempImageFile(rowInt);
                    continue;
                }

                //inserting the valid data and getting ID
                Uri contactUri = getContentResolver().insert(DatabaseContentProvider.CONTENT_URI, values);

                //parsing ID from uri
                String[] splitPath = contactUri.toString().split("/");
                String idStr = splitPath[splitPath.length - 1];

                //getting the internal image folder
                String saved = EmployeeUtil.getInternalImagesPath(this);

                //changing the temporary extracted image (temp_*.png) into normal image (*.png)
                File file = new File(saved + "temp_" + rowInt + ".png");
                if (!file.exists()) {
                    getContentResolver().delete(DatabaseContentProvider.CONTENT_URI, DatabaseOpenHelper.EMPLOYEE_ID + "=?",new String[]{ idStr});
                    continue;
                }
                File file1 = new File(saved + idStr + ".png");
                file.renameTo(file1);

                successParsedInt++;
            }
            //deleting
            deleteUnresolvedTempImages();
            totalRowInt = rowInt;
            return true;

        } catch (final Exception e) {
            //showing the error from other thread to UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ImportActivity.this, "Error while reading csv file", Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
    }

    //deletes temporary extracted image with it's name(ID) if csv parse error occurs
    private void deleteTempImageFile(int rowInt) {
        String saved = EmployeeUtil.getInternalImagesPath(this);
        File file = new File(saved + "temp_" + rowInt + ".png");
        file.delete();
    }

    //if extra temp image file left, this method will clear them forcefully
    private void deleteUnresolvedTempImages(){
        String pathStr = EmployeeUtil.getInternalImagesPath(this);
        File imageDirectoryFile = new File(pathStr);
        File[] imageFileList = imageDirectoryFile.listFiles();
        for(File imgFile:imageFileList){
            String fileNameStr = imgFile.getName();
            if(fileNameStr.startsWith("temp_")){
                imgFile.delete();
            }
        }
    }


    //showing file chooser for both CSV and ZIP
    private void showFileChooser(int selectionCode) {
        //setting up intents
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);

        //default path
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/EmployeeData/");

        if (selectionCode == CSV_SELECT_CODE) {
            //the chooser is opened for csv file selection
            i.setDataAndType(uri, "text/*");
            startActivityForResult(Intent.createChooser(i, "Open folder"), CSV_SELECT_CODE);
        } else {
            //the chooser is opened for zip file selection
            i.setDataAndType(uri, "application/zip");
            startActivityForResult(Intent.createChooser(i, "Open folder"), ZIP_SELECT_CODE);
        }
    }


    //invoked while user accepts or denies permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_EXT_READ_CSV:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted, open the file chooser for CSV
                    showFileChooser(CSV_SELECT_CODE);
                } else {
                    Toast.makeText(ImportActivity.this, "Read permission is required to import the files", Toast.LENGTH_LONG).show();
                }
                break;
            case REQ_EXT_READ_ZIP:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted, open the file chooser for ZIP
                    showFileChooser(ZIP_SELECT_CODE);
                } else {
                    Toast.makeText(ImportActivity.this, "Read permission is required to import the files", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //invoked while file is chosen from the picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CSV_SELECT_CODE:
                //csv file is chosen
                if (resultCode == RESULT_OK) {
                    csvPathEditText.setText(data.getData().getPath());
                    csvUri = data.getData();
                }
                break;
            case ZIP_SELECT_CODE:
                //zip file is chosen
                if (resultCode == RESULT_OK) {
                    zipPathEditText.setText(data.getData().getPath());
                    zipUri = data.getData();
                }
                break;
        }
    }

    //invoked while action bar back button is pressed
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
