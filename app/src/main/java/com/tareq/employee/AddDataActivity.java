package com.tareq.employee;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AddDataActivity extends AppCompatActivity {
    //views
    private ImageView profileImageView;
    private EditText nameEditText, ageEditText;
    private RadioGroup genderRadioGroup;
    private Button cancelButton, saveButton;

    //bitmap of the selected image
    private Bitmap profileImageBitmap = null;

    //request codes
    private final int REQ_EXT_READ = 1, PICK_FROM_FILE = 2;
    public final static int MODE_EDIT = 1, MODE_NEW = 2;
    public final static String TEXT_MODE = "mode", TEXT_NAME = "name", TEXT_AGE = "age", TEXT_GENDER = "gender",
            TEXT_ID = "id", TEXT_POSITION = "position";

    //variables for tracking the activity
    private int activityMode = MODE_NEW;
    private int idInt = -1, ageInt = -1, genderInt = -1, positionInt = -1;
    private String nameStr = "";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_data);

        //setting the back button on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //initializing the views
        profileImageView = findViewById(R.id.profile_image_view);
        nameEditText = findViewById(R.id.name_edit_text);
        ageEditText = findViewById(R.id.age_edit_text);
        genderRadioGroup = findViewById(R.id.gender_radio_group);
        cancelButton = findViewById(R.id.cancel_button);
        saveButton = findViewById(R.id.save_button);


        //bundle from the other activity, if the bundle is null, means the activity will act as adding new data (initially all field empty)
        //otherwise, it will act as updating old data (initially all field is filled)
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            saveButton.setEnabled(false);
            activityMode = extras.getInt(TEXT_MODE);
            nameStr = extras.getString(TEXT_NAME);
            ageInt = extras.getInt(TEXT_AGE);
            genderInt = extras.getInt(TEXT_GENDER);
            idInt = extras.getInt(TEXT_ID);
            nameEditText.setText(nameStr);
            ageEditText.setText(String.valueOf(ageInt));
            positionInt = extras.getInt(TEXT_POSITION, -1);
            ((RadioButton) genderRadioGroup.getChildAt(genderInt)).setChecked(true);
            showImageFromPath(String.valueOf(idInt));
        } else {
            makeSafeOperation();
        }

        //when the radio button is clicked the keyboard should be hidden
        genderRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                hideKeyboard();
            }
        });


        //cancel clicking button will show a confirm dialog
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                showConfirmDialog();
            }
        });

        //saving actions
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                //if all inputs are okay
                if (validInputs()) {
                    saveData();
                }
            }
        });

    }

    //returns true if all inputs are correct
    private boolean validInputs() {
        //getting the input data
        final String nameStr = nameEditText.getText().toString();
        final String ageStr = ageEditText.getText().toString();
        final int genderIndex = genderRadioGroup.indexOfChild(findViewById(genderRadioGroup.getCheckedRadioButtonId()));

        //validation, if null or empty
        if (TextUtils.isEmpty(nameStr)) {
            nameEditText.setError("Input name is too short");
            return false;
        }

        if (TextUtils.isEmpty(ageStr)) {
            ageEditText.setError("Input age is invalid");
            return false;
        } else {
            try {
                int t = Integer.parseInt(ageStr);
                if (t > 150 || t <= 0) {
                    ageEditText.setError("Invalid age");
                    return false;
                }
            } catch (Exception e) {
                ageEditText.setError("Age must be a number");
                return false;
            }

        }

        //no radio button is selected
        if (genderIndex == -1) {
            Toast.makeText(AddDataActivity.this, "Please select gender", Toast.LENGTH_SHORT).show();
            return false;
        }

        //no image has been set
        if (profileImageBitmap == null) {
            Toast.makeText(AddDataActivity.this, "Please add a profile image", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //saving data, insert to database, copy image to internal
    private void saveData() {
        //getting the input data
        final String nameStr = nameEditText.getText().toString();
        final String ageStr = ageEditText.getText().toString();
        final int genderIndex = genderRadioGroup.indexOfChild(findViewById(genderRadioGroup.getCheckedRadioButtonId()));


        //showing loading dialog
        ProgressDialog dialog = ProgressDialog.show(AddDataActivity.this, "", "Saving. Please wait...", true);
        dialog.show();

        //the large writing operation is done in other thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(DatabaseOpenHelper.EMPLOYEE_NAME, nameStr);
                values.put(DatabaseOpenHelper.EMPLOYEE_AGE, Integer.parseInt(ageStr));
                values.put(DatabaseOpenHelper.EMPLOYEE_GENDER, genderIndex);

                if (activityMode == MODE_NEW) {
                    //if new data is adding

                    //inserting into database
                    Uri contactUri = getContentResolver().insert(DatabaseContentProvider.CONTENT_URI, values);

                    //getting the ID
                    String[] splitPath = contactUri.toString().split("/");
                    String idStr = splitPath[splitPath.length - 1];

                    //saving the bitmap image with the name of the ID
                    saveImageFile(profileImageBitmap, idStr);

                    idInt = Integer.valueOf(idStr);

                    //showing success message in UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AddDataActivity.this, "New data added", Toast.LENGTH_LONG).show();
                        }
                    });

                } else {
                    //if old data is updating

                    //updating old data
                    getContentResolver().update(DatabaseContentProvider.CONTENT_URI, values, "id = ?", new String[]{String.valueOf(idInt)});

                    //saving the bitmap image with the name of the ID
                    saveImageFile(profileImageBitmap, String.valueOf(idInt));

                    //showing success message in UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AddDataActivity.this, "Data updated", Toast.LENGTH_LONG).show();
                        }
                    });

                }
                Intent intent = new Intent();
                intent.putExtra(TEXT_POSITION, positionInt);
                intent.putExtra(DatabaseOpenHelper.EMPLOYEE_ID, idInt);
                intent.putExtra(DatabaseOpenHelper.EMPLOYEE_NAME, nameStr);
                intent.putExtra(DatabaseOpenHelper.EMPLOYEE_AGE, Integer.parseInt(ageStr));
                intent.putExtra(DatabaseOpenHelper.EMPLOYEE_GENDER, genderIndex);
                setResult(RESULT_OK, intent);
                //finish this activity after success
                finish();
            }
        }).start();
    }

    //saving bitmap image with the given filename
    private void saveImageFile(Bitmap image, String filename) {

        try {
            //stream for reading the bitmap into byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            //stream for saving byte array
            OutputStream out;

            //creating an empty image file
            String internalImagePathStr = EmployeeUtil.getInternalImagesPath(AddDataActivity.this);
            File file = new File(internalImagePathStr + filename + ".png");
            file.createNewFile();

            //writing the byte array
            out = new FileOutputStream(file);
            out.write(byteArray);

            //deinit the stream
            out.close();
        } catch (final IOException e) {
            //showing error in UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AddDataActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    //hides the soft keyboard
    private void hideKeyboard() {
        //getting the current focus
        View view = AddDataActivity.this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //loading image file into the image view
    private void showImageFromPath(String id) {
        String imageFilePathStr = EmployeeUtil.getInternalImagesPath(AddDataActivity.this) + id + ".png";
        File imageFile = new File(imageFilePathStr);
        if (imageFile.exists()) {
            //if the image exist in the directory
            Picasso.get().load(imageFile).into(profileImageView, new Callback() {
                @Override
                public void onSuccess() {
                    makeSafeOperation();
                    profileImageBitmap = ((BitmapDrawable) profileImageView.getDrawable()).getBitmap();
                }

                @Override
                public void onError(Exception e) {
                    makeSafeOperation();
                }
            });

        } else {
            //Toast.makeText(AddDataActivity.this, "No image is found", Toast.LENGTH_LONG).show();
        }
    }

    private void makeSafeOperation() {
        //image picker will be shown on click and the keyboard must be hidden
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                //check if the app has permission for reading external storage, if not then prompt request
                if (EmployeeUtil.isPermissionGranted(AddDataActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQ_EXT_READ)) {
                    takePictureFromGallery();

                }
            }
        });
        saveButton.setEnabled(true);
    }

    //invoked while user grants or rejects permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_EXT_READ:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePictureFromGallery();
                } else {
                    Toast.makeText(AddDataActivity.this, "Read permission is required for adding image", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //open image chooser
    private void takePictureFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_FROM_FILE);
        saveButton.setEnabled(false);
    }

    //checks if some input data is modified
    private boolean dataChanged() {
        //getting the input data
        String nameStr = nameEditText.getText().toString();
        String ageStr = ageEditText.getText().toString();
        int genderIndex = genderRadioGroup.indexOfChild(findViewById(genderRadioGroup.getCheckedRadioButtonId()));


        if (activityMode == MODE_NEW) {
            //this activity acts as adding new, initial input was empty
            if (TextUtils.isEmpty(nameStr) && TextUtils.isEmpty(ageStr) && genderIndex == -1) {
                return false;
            }
            return true;
        } else {
            //this activity acts as updating old, initial is old data
            if (nameStr.equals(nameStr) && ageStr.equals(String.valueOf(ageInt)) && genderInt == genderIndex) {
                return false;
            }
            return true;
        }
    }

    //shows confirm alert dialog
    private void showConfirmDialog() {
        //if some data is edited
        if (dataChanged()) {
            //dialog's button click listener
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:

                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            //if user press YES then the activity closes
                            setResult(RESULT_CANCELED, null);
                            finish();
                            break;
                    }
                }
            };

            //building the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Data will be lost. Are you sure to exit?")
                    .setNegativeButton("Yes", dialogClickListener)
                    .setPositiveButton("No", dialogClickListener)
                    .show();
        } else {
            //no data is edited
            //user can finish the activity without showing dialog
            setResult(RESULT_CANCELED, null);
            finish();
        }
    }


    private void loadIntoImageView(final Uri selectedImageUri) {
        //loading image can be large task for large image, so thread is used
        new Thread(new Runnable() {
            @Override
            public void run() {
                //parsing URI to workable file descriptor
                ParcelFileDescriptor parcelFileDescriptor;
                try {
                    parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

                    //showing the image
                    profileImageBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    profileImageBitmap = EmployeeUtil.scaleDown(profileImageBitmap, 512);

                    //deinit the the file descriptor
                    parcelFileDescriptor.close();

                    //setting the image into image view in the UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            profileImageView.setImageBitmap(profileImageBitmap);
                        }
                    });

                } catch (Exception e) {
                    ;
                }
            }
        }).start();

    }

    //invoked while activity for result is used and the result is found
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_FROM_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    loadIntoImageView(data.getData());
                }
                saveButton.setEnabled(true);
                break;
        }


    }

    //invoked while navigation back button is pressed
    @Override
    public void onBackPressed() {
        showConfirmDialog();
        //super.onBackPressed();
    }

    //invoked while actionbar back button is pressed
    @Override
    public boolean onSupportNavigateUp() {
        showConfirmDialog();
        return true;
    }
}

