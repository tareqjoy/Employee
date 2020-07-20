package com.tareq.employee;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

/**
 * Helper class for Recycler view
 */
public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
    private Context mContext;
    private List<Employee> items;

    public EmployeeAdapter(Context mContext, List<Employee> items) {
        this.mContext = mContext;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Employee item = items.get(position);


        holder.nameTextView.setText(item.getName());
        holder.ageTextView.setText(String.valueOf(item.getAge()));
        holder.genderTextView.setText(item.getGenderStr());

        //setting up the image view for each row
        String internalImageFileStr = EmployeeUtil.getInternalImagesPath(mContext) + item.getId() + ".png";
        File imageFile = new File(internalImageFileStr);

        Picasso.get().invalidate(imageFile);
        Picasso.get().load(imageFile).into(holder.profileImageView);

     //   Picasso.get().im.into(holder.profileImageView);
        /*
        if (imageFile.exists()) {
            Log.e("tareqjoy","updated "+position);
            //picasso library for loading image

            holder.profileImageView.invalidate();
        } else {
            holder.profileImageView.setImageDrawable(null);
        }*/

        holder.parentCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pop up menu
                String[] options = {"Edit", "Delete"};
                showPopUpMenu(options, position, item);
            }
        });
    }

    //showing pop up menu
    private void showPopUpMenu(String[] options,final int position, final Employee item){
        //setting up the dialog acting as menu
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(item.getName());

        //callbacks on menu item press
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        //Edit pressed
                        gotoAddDataActivity(position,item);
                        break;
                    case 1:
                        //Delete pressed
                        showConfirmDialog(position, item);
                        break;
                }
            }
        });
        builder.show();
    }

    //goto AddDataActivity for updating
    private void gotoAddDataActivity(int position, Employee item) {
        //passing the information to the AddDataActivity
        Intent myIntent = new Intent(mContext, AddDataActivity.class);
        myIntent.putExtra(AddDataActivity.TEXT_MODE, AddDataActivity.MODE_EDIT);
        myIntent.putExtra(AddDataActivity.TEXT_POSITION, position);
        myIntent.putExtra(AddDataActivity.TEXT_NAME, item.getName());
        myIntent.putExtra(AddDataActivity.TEXT_GENDER, item.getGender());
        myIntent.putExtra(AddDataActivity.TEXT_AGE, item.getAge());
        myIntent.putExtra(AddDataActivity.TEXT_ID, item.getId());
        ((HomeActivity)mContext).notifyWillChange(HomeActivity.MODE_UPDATE);
        ((HomeActivity)mContext).startActivityForResult(myIntent,HomeActivity.MODE_UPDATE);
    }

    //showing confirm dialog for delete
    private void showConfirmDialog(final int position, final Employee item) {
        //setting up the callbacks
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //Yes pressed
                        deleteItem(position, item);
                        break;
                }
            }
        };

        //building the dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(mContext);
        builder.setMessage("Are you sure to delete?").setNegativeButton("Yes", dialogClickListener)
                .setPositiveButton("No", dialogClickListener).show();
    }


    //delete a item from database and also the image
    private void deleteItem(int position, Employee item){
        String s = String.valueOf(item.getId());
        String[] ids = new String[]{s};
        //items.remove(position);

        //deleting from database
        mContext.getContentResolver().delete(DatabaseContentProvider.CONTENT_URI, DatabaseOpenHelper.EMPLOYEE_ID + "=?", ids);

        //delete corresponding image
        EmployeeUtil.deleteInternalImage(mContext, item.getId());
        ((HomeActivity)mContext).notifyWillChange(HomeActivity.MODE_DELETE, position);
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * View holder for single row in Recycler View
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        //views
        public TextView nameTextView;
        public TextView ageTextView;
        public TextView genderTextView;
        public ImageView profileImageView;
        public CardView parentCardView;

        public ViewHolder(View itemView) {
            super(itemView);
            //initialization views
            nameTextView = itemView.findViewById(R.id.name_text_view);
            ageTextView = itemView.findViewById(R.id.age_text_view);
            genderTextView = itemView.findViewById(R.id.gender_text_view);
            profileImageView = itemView.findViewById(R.id.profile_image_view);
            parentCardView = itemView.findViewById(R.id.parent_card_view);

        }

    }
}
