package com.tareq.employee;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

/**
 * utility class for the entire application
 */
public class EmployeeUtil {

    //static file locations
    public static final String exportCSVFileName = "EmployeeData.csv";
    public static final String exportZipFileName = "EmployeeImages.zip";
    public static final String exportFolderName = "EmployeeData";

    private EmployeeUtil(){

    }

    /**
     * Returns the internal data/Images folder location. If no such directory, then it is created first.
     * @param context   the application context
     * @return          the String path
     */
    public static String getInternalImagesPath(Context context){
        String path = context.getApplicationInfo().dataDir + File.separator + "Images" + File.separator;
        File fl = new File(path);
        if (!fl.exists())
            fl.mkdir();
        return path;
    }

    /**
     * Deletes a image file by it's name from the internal directory.  If no such file, then the operation is ignored.
     * @param context   the application context
     * @param id        the image name
     */
    public static void deleteInternalImage(Context context, int id){
        String fullPath = getInternalImagesPath(context);
        File file = new File(fullPath + id +".png");
        file.delete();
    }


    /**
     * Checks if the requested permission is already granted or not.
     * @param activity      the activity
     * @param permission    the manifest permission
     * @param REQ_CODE      requested code to keep track the request source
     * @return              true means permission is granted already, otherwise false
     */
    public static boolean isPermissionGranted(AppCompatActivity activity, String permission, final int REQ_CODE) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(permission)
                    == PackageManager.PERMISSION_GRANTED) {

                return true;
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{permission}, REQ_CODE);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            // Log.v(TAG,"Permission is granted1");
            return true;
        }
    }

    /**
     * Returns the path of the external directory which is the root path for the app
     * @return  the file path in string
     */
    public static String getExportFolderPath(){
        File rootDir = Environment.getExternalStorageDirectory();
        File employeeDir = new File(rootDir, EmployeeUtil.exportFolderName);
        if (!employeeDir.exists()) {
            employeeDir.mkdirs();
        }
        return employeeDir.getPath();
    }
}
