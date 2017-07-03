package it.jdark.android.firebase.authentication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    TextView status, detail;
    EditText phoneNumber;
    CountryCodePicker ccp;

    OnCompleteListener<AuthResult> mCompleteListener;

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    String verificationCode;
    PhoneAuthProvider.ForceResendingToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        status = findViewById(R.id.status);
        detail = findViewById(R.id.details);
        ccp = findViewById(R.id.ccp);
        phoneNumber = findViewById(R.id.phone_number);

        findViewById(R.id.resend_code).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.register_signin_button).setOnClickListener(this);
        findViewById(R.id.send_verify_code).setOnClickListener(this);

        // Callbacks from Firebase phone number registration process
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                Log.i(TAG, "onVerificationCompleted: Completed!");
                Toast.makeText(getApplicationContext(), "logging...", Toast.LENGTH_SHORT).show();
                mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(mCompleteListener);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed ", e);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    phoneNumber.setError("Invalid phone number.");
                } else if (e instanceof FirebaseAuthException) {
                    Toast.makeText(getApplicationContext(), "Authentication by phone Disable!", Toast.LENGTH_LONG).show();
                }  else if (e instanceof FirebaseTooManyRequestsException) {
                        Toast.makeText(getApplicationContext(), "Quota exceeded.", Toast.LENGTH_SHORT).show();
                }
                // Hide verification layout if any error has came
                findViewById(R.id.validateLayout).setVisibility(View.GONE);
                findViewById(R.id.resend_code).setEnabled(false);
            }

            @Override
            public void onCodeSent(String verificationID, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verificationID, forceResendingToken);
                Log.i(TAG, "onCodeSent: " + verificationID);
                findViewById(R.id.validateLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.resend_code).setEnabled(true);
                verificationCode = verificationID;
                token = forceResendingToken;
            }
        };

        mAuth = FirebaseAuth.getInstance();

        // Authenticator listener
        // Update the UI according the current log in user
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mAuth.getCurrentUser();
                Log.w(TAG, "onAuthStateChanged: " +user);
                if (user != null) {
                    updateUI(user);
                    Log.d(TAG, "onAuthStateChanged: Signed in -> " +user.getUid());
                } else {
                    updateUI(null);
                    Log.d(TAG, "onAuthStateChanged: Signed out");
                }
            }
        };

        // Listener to Sign in process (signInWithCredential)
        // Allow to perform some action once the login process is completed
        // (tipically update UI <- which is handle in that example by the AuthStateListener)
        mCompleteListener = new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.w(TAG, "onComplete: OK! " + task.getException());
                } else {
                    Log.w(TAG, "onComplete: FAIL! ");
                    Log.w(TAG, "message ->" + task.getException());
                    // Catch some errors to show back few information about the reason of fail
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        if (task.getException().getMessage().contains("The verification ID")) {
                            Toast.makeText(getApplicationContext(), "implementation error!", Toast.LENGTH_SHORT).show();
                        } else if (task.getException().getMessage().contains("The sms verification code")) {
                            ((EditText) findViewById(R.id.verify_code)).setError("Invalid code.");
                            Toast.makeText(getApplicationContext(), "invalid Code!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        };
    }


    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


    private void updateUI(FirebaseUser user) {
        if (user != null) {
            status.setText(getString(R.string.phone_number_status_form,
                    user.getPhoneNumber()));
            detail.setText(getString(R.string.phone_number_detail_form, user.getUid()));

            findViewById(R.id.inputLayout).setVisibility(View.GONE);
            findViewById(R.id.signinLayout).setVisibility(View.GONE);
            findViewById(R.id.signoutLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.validateLayout).setVisibility(View.GONE);
        } else {
            status.setText(R.string.sign_out_text);
            detail.setText(null);

            findViewById(R.id.inputLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.signinLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.signoutLayout).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.register_signin_button) {
            createAccount(ccp.getSelectedCountryCodeWithPlus(), phoneNumber.getText().toString());
        } else if (i == R.id.resend_code) {
            ResendCode(ccp.getSelectedCountryCodeWithPlus(), phoneNumber.getText().toString());
        } else if (i == R.id.sign_out_button) {
            signOut();
        } else if (i == R.id.send_verify_code) {
            sendValidationCode();
        }

    }

    private void createAccount(String prefix, String phoneNumber) {
        Log.i(TAG, "createAccount: Phone Number -> "+prefix+"-"+phoneNumber + " Token -> " + token);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(prefix+phoneNumber,60, TimeUnit.SECONDS, this, mCallbacks);
    }

    private void signOut() {
        mAuth.signOut();
    }

    private void ResendCode(String prefix, String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(prefix+phoneNumber,60, TimeUnit.SECONDS, this, mCallbacks, token);
    }

    private void sendValidationCode() {
        String code = ((EditText) findViewById(R.id.verify_code)).getText().toString();
        if (!code.isEmpty() && verificationCode!= null) {
            Log.w(TAG, "sendValidationCode: sms code ->" + PhoneAuthProvider.getCredential(verificationCode, code).getSmsCode());
            mAuth.signInWithCredential(PhoneAuthProvider.getCredential(verificationCode, code)).addOnCompleteListener(mCompleteListener);
        } else
            ((EditText) findViewById(R.id.verify_code)).setError("Required!");
    }


}
