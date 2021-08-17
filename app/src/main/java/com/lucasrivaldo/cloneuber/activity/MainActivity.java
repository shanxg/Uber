package com.lucasrivaldo.cloneuber.activity;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.UserFirebase;
import com.lucasrivaldo.cloneuber.model.User;

import static com.lucasrivaldo.cloneuber.config.ConfigurateFirebase.TYPE_PASSENGER;
import static com.lucasrivaldo.cloneuber.config.ConfigurateFirebase.TYPE_DRIVER;

public class MainActivity extends AppCompatActivity {

    public static String GOOGLE_MAPS_KEY;
    public static String MY_TAG_ERROR = "USERTESTERROR";
    public static String MY_TAG_TEST = "USERTESTAPP";

    private String lastUserType;

    private FirebaseAuth authRef;

    private Button buttonRegister, buttonSignIn;
    private TextInputEditText inputTextUserEmail, inputTextUserPW;

    private TextView textButtonSwitchDriver, textButtonSwitchPass;
    private Switch switchType;

    private ImageView imageLogo;
    private LinearLayout movLayout, progressBarOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBarOpen = findViewById(R.id.progressBarOpen);
        TextView textProgress = findViewById(R.id.progressText);
        textProgress.setText(getResources().getString(R.string.text_signin_auth));

        GOOGLE_MAPS_KEY = getResources().getString(R.string.google_api_key);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));

        authRef = ConfigurateFirebase.getFirebaseAuth();
        if (authRef.getCurrentUser() != null)
            UserFirebase.getLastLogin(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        progressBarOpen.setVisibility(View.GONE);

                        lastUserType = dataSnapshot.getValue(String.class);

                        Class aClass = lastUserType.equals(TYPE_DRIVER) ?
                                UberDriverActivity.class : UberPassengerActivity.class;

                        startActivity(new Intent(MainActivity.this, aClass));
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        else {
            progressBarOpen.setVisibility(View.GONE);
            findViewById(R.id.buttonsLayout).setVisibility(View.VISIBLE);
            loadInterface();
            setClickListeners();
        }
    }

    private void loadInterface(){
        buttonSignIn = findViewById(R.id.buttonSignIn);
        buttonRegister = findViewById(R.id.buttonRegister);


        inputTextUserPW = findViewById(R.id.inputTextUserPW);
        inputTextUserEmail = findViewById(R.id.inputTextUserEmail);

        imageLogo = findViewById(R.id.imageLogo);
        movLayout = findViewById(R.id.movLayout);

        textButtonSwitchDriver = findViewById(R.id.textButtonSwitchDriver);
        textButtonSwitchPass = findViewById(R.id.textButtonSwitchPass);
        switchType = findViewById(R.id.switchType);
        setLogTypeTextColor();

    }


    private boolean validateText(String uEmailText, String uPWText){

        if (uEmailText.isEmpty()) {

            throwToast("User email text is empty", true);
            return false;

        }else if (uPWText.isEmpty()){

            throwToast("User password text is empty", true);
            return false;

        }else
            return true;
    }

    private void authUser(String uEmailText, String uPWText){

        authRef.signInWithEmailAndPassword(uEmailText, uPWText)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        String userId = task.getResult().getUser().getUid();
                        checkUserType(userId);
                    }else {

                        try { throw task.getException(); }

                        catch (Exception e)
                        { throwToast(e.getMessage(), true); }
                    }
                });
    }

    private void checkUserType(String userId){
        DatabaseReference usersRef = ConfigurateFirebase.getFireDBRef()
                                                .child(ConfigurateFirebase.USERS);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String logType = getUserType();

                    if(dataSnapshot.child(logType).hasChild(userId))
                        startUberActivity();
                    else
                        loginValidationAlert(dataSnapshot, logType, userId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

    }



    /** ##############################      CLICK LISTENERS     ############################### **/

    private void setClickListeners(){

        buttonSignIn.setOnClickListener(view ->{

            if(imageLogo.getVisibility()==View.VISIBLE)
                toggleLoginInterface(true);
            else {

                String uEmailText = inputTextUserEmail.getText().toString();
                String uPWText = inputTextUserPW.getText().toString();

                if(validateText(uEmailText, uPWText))
                    authUser(uEmailText, uPWText);
            }

        });

        buttonRegister.setOnClickListener
                (view -> startActivity
                        (new Intent(MainActivity.this, RegisterActivity.class)));

        textButtonSwitchDriver.setOnClickListener
                (view ->{
                    switchType.setChecked(true);
                    setLogTypeTextColor();
                });

        textButtonSwitchPass.setOnClickListener
                (view ->{
                    switchType.setChecked(false);
                    setLogTypeTextColor();
                });
        switchType.setOnCheckedChangeListener
                ((compoundButton, isChecked) -> setLogTypeTextColor());

    }


    /** ##############################     ACTIVITY PROCESSES    ############################## **/

    @Override
    public boolean onSupportNavigateUp() {
        toggleLoginInterface(false);
        return true;
    }

    /** #################################        HELPERS       ################################ **/

    private void startUberActivity(){

        Class aClass = getUserType().equals(TYPE_DRIVER) ?
                UberDriverActivity.class : UberPassengerActivity.class;

        startActivity(new Intent(MainActivity.this, aClass));
        finish();
    }

    private void loginValidationAlert(DataSnapshot dataSnapshot, String logType, String userId){

        // ###############################     DISMISS LISTENER     ###############################

        DialogInterface.OnDismissListener dismissListener = dialogInterface ->{

            String difType = switchType.isChecked() ? TYPE_PASSENGER : TYPE_DRIVER;

            User user = dataSnapshot.child(difType).child(userId).getValue(User.class);
            user.setType(logType);

            DatabaseReference snapRef = dataSnapshot.child(logType).child(userId).getRef();
            snapRef.setValue(user).addOnCompleteListener
                    (task -> {
                        if(task.isSuccessful()) {
                            String textType = logType.equals(TYPE_DRIVER) ?
                                    "Diver" : "Passenger";
                            throwToast
                                    (textType + " registration successful",
                                            false);

                            startUberActivity();
                        }
                    });


        } ;

        // ################################     DIALOG BUILDER     ################################

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        builder.setTitle("Account not found:");

        builder.setMessage("You might have not selected the correct user type,"
                         + " try to switch the user type to sign in again, or "
                         + "if you want to sign in as "+getUserType()+", click confirm. \n"+ "\n"
                + "If you dont have an account, cancel this dialog and click in register button.");


        builder.setPositiveButton("Confirm",
                (dialogInterface, i) -> dialogInterface.dismiss() );

        builder.setNegativeButton("No",
                (dialog, which) ->
                        throwToast("Try signing in again: \n"
                                +"   Remember to select which type of user you are.\n"
                                +"             (Passenger or Driver)",
                                true));

        AlertDialog alert = builder.create();
        alert.setOnDismissListener(dismissListener);
        alert.show();


    }


    private void toggleLoginInterface(boolean isOpening){

        if (isOpening) {

            imageLogo.setVisibility(View.INVISIBLE);

            if (imageLogo.getVisibility() == View.INVISIBLE) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                movLayout.setVisibility(View.VISIBLE);
            }
        }else {

            movLayout.setVisibility(View.INVISIBLE);

            if (movLayout.getVisibility() == View.INVISIBLE) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);

                imageLogo.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getUserType(){
        return  switchType.isChecked() ? TYPE_DRIVER :  TYPE_PASSENGER;
    }

    private void setLogTypeTextColor(){
        if (switchType.isChecked()) {
            textButtonSwitchDriver.setTextColor(getResources().getColor(R.color.btnSignIn));
            textButtonSwitchPass.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            textButtonSwitchPass.setTextColor(getResources().getColor(R.color.btnSignIn));
            textButtonSwitchDriver.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void throwToast(String message, boolean isLong){
        int  lenght = Toast.LENGTH_LONG;
        if(!isLong)
            lenght = Toast.LENGTH_SHORT;

        Toast.makeText
                (this, message, lenght).show();

    }
}
