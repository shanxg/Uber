package com.lucasrivaldo.cloneuber.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.UserFirebase;
import com.lucasrivaldo.cloneuber.model.User;

import static com.lucasrivaldo.cloneuber.config.ConfigurateFirebase.TYPE_DRIVER;
import static com.lucasrivaldo.cloneuber.config.ConfigurateFirebase.TYPE_PASSENGER;

public class RegisterActivity extends AppCompatActivity {

    private User user;

    private TextInputEditText inputRegTextUserName, inputRegTextUserPw, inputRegTextUserEmail;
    private TextView textButtonRegSwitchDriver, textButtonRegSwitchPass;
    private Button buttonRegUser;
    private Switch switchRegType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Register your account");
        getSupportActionBar().show();

        user = new User();

        loadInterface();
        setClickListeners();


    }

    private void loadInterface(){

        inputRegTextUserName = findViewById(R.id.inputRegTextUserName);
        inputRegTextUserPw = findViewById(R.id.inputRegTextUserPw);
        inputRegTextUserEmail = findViewById(R.id.inputRegTextUserEmail);

        textButtonRegSwitchDriver = findViewById(R.id.textButtonRegSwitchDriver);
        textButtonRegSwitchPass = findViewById(R.id.textButtonRegSwitchPass);

        buttonRegUser = findViewById(R.id.buttonRegUser);
        switchRegType = findViewById(R.id.switchRegType);
        setRegTypeTextColor();

    }

    private boolean validateText(String userName, String userEmail, String textPw){

        if (userName.isEmpty()) {

            throwToast("Empty name: \n   Write your name to register.", true);
            return false;

        }else if (userEmail.isEmpty()) {

            throwToast("Empty email: \n   Write your email to register.", true);
            return false;

        }else if (textPw.isEmpty()) {

            throwToast("Empty password: \n   Write your password to register.", true);
            return false;

        }else
            return true;
    }

    private void regUser(String userName, String userEmail, String textPw){

        ConfigurateFirebase.getFirebaseAuth()
                .createUserWithEmailAndPassword(userEmail, textPw)
                .addOnCompleteListener(task -> {

                    if( task.isSuccessful()) {

                        String uType = getUserType();
                        String uID = task.getResult().getUser().getUid();


                        user.setType(uType);
                        user.setId(uID);
                        user.setName(userName);
                        user.setEmail(userEmail);
                        user.setPw(textPw);

                        boolean isComplete = user.save();

                        if(isComplete) {
                            UserFirebase.updateUserProfName(userName);

                            Class aClass = uType.equals(TYPE_DRIVER) ?
                                    UberDriverActivity.class : UberPassengerActivity.class;

                            uType = uType.equals(TYPE_DRIVER) ? "Diver": "Passenger";


                            throwToast(uType+" registration successful", false);
                            startActivity(new Intent(this, aClass));
                            finish();
                        }

                    }else {

                        try { throw task.getException(); }

                        catch (Exception e)
                        { throwToast(e.getMessage(), true); }
                    }

                });
    }

    /** ##############################      CLICK LISTENERS     ############################### **/

    private void setClickListeners(){

        buttonRegUser.setOnClickListener(view->{

            String userName = inputRegTextUserName.getText().toString();
            String userEmail = inputRegTextUserEmail.getText().toString();
            String textPw = inputRegTextUserPw.getText().toString();


            if(validateText(userName, userEmail, textPw))
                regUser(userName, userEmail, textPw);

        });

        textButtonRegSwitchDriver.setOnClickListener
                (view -> {
                    switchRegType.setChecked(true);
                    setRegTypeTextColor();
                });

        textButtonRegSwitchPass.setOnClickListener
                (view ->{
                    switchRegType.setChecked(false);
                    setRegTypeTextColor();
                });

        switchRegType.setOnCheckedChangeListener
                ((compoundButton, isChecked) -> setRegTypeTextColor());

    }

    /** ##############################     ACTIVITY PROCESSES    ############################## **/

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** #################################        HELPERS       ################################ **/

    private String getUserType(){
        return  switchRegType.isChecked() ? TYPE_DRIVER :  TYPE_PASSENGER;
    }

    private void setRegTypeTextColor(){
        if (switchRegType.isChecked()) {
            textButtonRegSwitchDriver.setTextColor(getResources().getColor(R.color.btnSignIn));
            textButtonRegSwitchPass.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            textButtonRegSwitchPass.setTextColor(getResources().getColor(R.color.btnSignIn));
            textButtonRegSwitchDriver.setTextColor(getResources().getColor(android.R.color.white));
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
