package it.jdark.android.firebase.authentication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class EmailPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();

    TextView status, detail;
    EditText email, password;

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_password);

        // Views
        status = (TextView) findViewById(R.id.status);
        detail = (TextView) findViewById(R.id.details);
        email = (EditText) findViewById(R.id.email_input_field);
        password = (EditText) findViewById(R.id.password_input_field);

        // Buttons
        findViewById(R.id.email_password_signin_btn).setOnClickListener(this);
        findViewById(R.id.email_password_create_btn).setOnClickListener(this);
        findViewById(R.id.email_passowrd_signout_btn).setOnClickListener(this);
        findViewById(R.id.email_password_verify_btn).setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    updateUI(user);
                    Log.d(TAG, "onAuthStateChanged: Signed in -> " +user.getUid());
                } else {
                    updateUI(null);
                    Log.d(TAG, "onAuthStateChanged: Signed out");
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

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.email_password_create_btn) {
            createAccount(email.getText().toString(), password.getText().toString());
        } else if (i == R.id.email_password_signin_btn) {
            signIn(email.getText().toString(), password.getText().toString());
        } else if (i == R.id.email_passowrd_signout_btn) {
            signOut();
        } else if (i == R.id.email_password_verify_btn) {
            sendEmailVerification();
        }
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);

        if (!validateForm()) {
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail: onComplete ->" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Log.w(TAG, "onComplete: signInWithCredential -> " + "EMAIL ALREADY USED!");
                                Toast.makeText(EmailPasswordActivity.this, "Email Used already with different credential.", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.w(TAG, "signInWithCredential", task.getException());
                                Toast.makeText(EmailPasswordActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                            updateUI(null);
                            signOut();
                        }
                    }
                });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail: onComplete -> " + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail: Failed", task.getException());
                            Toast.makeText(EmailPasswordActivity.this, R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                        }

                        if (!task.isSuccessful()) {
                            status.setText(R.string.auth_failed);
                        }
                    }
                });
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

    private void sendEmailVerification() {
        findViewById(R.id.email_password_verify_btn).setEnabled(false);

        // Send verification email
        // [START send_email_verification]
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        // Re-enable button
                        findViewById(R.id.email_password_verify_btn).setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(EmailPasswordActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "sendEmailVerification", task.getException());
                            Toast.makeText(EmailPasswordActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }



    private boolean validateForm() {
        boolean valid = true;

        String mail = email.getText().toString();
        if (TextUtils.isEmpty(mail)) {
            email.setError("Required.");
            valid = false;
        } else {
            email.setError(null);
        }

        String passwd = password.getText().toString();
        if (TextUtils.isEmpty(passwd)) {
            password.setError("Required.");
            valid = false;
        } else {
            password.setError(null);
        }
        return valid;
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            status.setText(getString(R.string.email_password_status_form,
                    user.getEmail(), user.isEmailVerified()));
            detail.setText(getString(R.string.email_password_firebase_status_form, user.getUid()));

            findViewById(R.id.inputs_layout).setVisibility(View.GONE);
            findViewById(R.id.signin_layout).setVisibility(View.GONE);
            findViewById(R.id.signed_layout).setVisibility(View.VISIBLE);

            findViewById(R.id.email_password_verify_btn).setEnabled(!user.isEmailVerified());
        } else {
            status.setText(R.string.sign_out_text);
            detail.setText(null);

            findViewById(R.id.inputs_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.signin_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.signed_layout).setVisibility(View.GONE);
        }
    }

}
