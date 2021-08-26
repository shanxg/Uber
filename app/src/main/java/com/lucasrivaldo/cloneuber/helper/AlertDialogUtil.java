package com.lucasrivaldo.cloneuber.helper;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

public class AlertDialogUtil {

    public static void permissionValidationAlert(String[] needPermissions, Context context){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Permissions denied:");
        builder.setMessage("To keep using the App, you need to accept the Requested permissions.");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirm",
                (dialog, which) -> SystemPermissions.validatePermissions
                        (needPermissions, (Activity) context, 1));

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void confirmTripPaymentAlertDialog
            (Context context, String tripText, DialogInterface.OnClickListener pListener){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Trip confirmation:");
        builder.setMessage(tripText);
        builder.setCancelable(false);
        builder.setPositiveButton("Confirm",pListener);
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {});

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void loginValidationAlert(Context context, String uType,
                                      DialogInterface.OnClickListener negListener,
                                      DialogInterface.OnDismissListener dismissListener){

        // ################################     DIALOG BUILDER     ################################

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        builder.setTitle("Account not found:");

        builder.setMessage("You might have not selected the correct user type,"
                + " try to switch the user type to sign in again, or "
                + "if you want to sign in as "+uType+", click confirm. \n"+ "\n"
                + "If you dont have an account, cancel this dialog and click in register button.");


        builder.setPositiveButton("Confirm",
                (dialogInterface, i) -> dialogInterface.dismiss() );

        builder.setNegativeButton("No", negListener);

        AlertDialog alert = builder.create();
        alert.setOnDismissListener(dismissListener);
        alert.show();


    }
}