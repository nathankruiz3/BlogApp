package com.nathankruiz3.blog.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nathankruiz3.blog.R;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class CreateAccountActivity extends AppCompatActivity {

    private static final int GALLERY_CODE = 2;

    private EditText firstNameActET, lastNameActET, emailActET, passwordActET, confirmPasswordActET;
    private Button createAccountButton;
    private ImageButton pfpImageButton;
    private Uri pfpImageUri = null;
    private Uri resultUri = null;

    private DatabaseReference dbRef;
    private FirebaseDatabase db;
    private FirebaseAuth auth;
    private StorageReference storageRef;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference().child("MUsers");

        auth = FirebaseAuth.getInstance();

        storageRef = FirebaseStorage.getInstance().getReference().child("MBlog_Pfps");

        dialog = new ProgressDialog(this);

        firstNameActET = (EditText) findViewById(R.id.firstNameActET);
        lastNameActET = (EditText) findViewById(R.id.lastNameActET);
        emailActET = (EditText) findViewById(R.id.emailActET);
        passwordActET = (EditText) findViewById(R.id.passwordActET);
        confirmPasswordActET = (EditText) findViewById(R.id.confirmPasswordActET);
        createAccountButton = (Button) findViewById(R.id.createAccountButton);
        pfpImageButton = (ImageButton) findViewById(R.id.pfpImageButton);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });

        pfpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_CODE);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
            pfpImageUri = data.getData();

            CropImage.activity(pfpImageUri)
                    .setAspectRatio(1,1)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);


        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();

                pfpImageButton.setImageURI(resultUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    private void createNewAccount() {

        final String firstName = firstNameActET.getText().toString().trim();
        final String lastName = lastNameActET.getText().toString().trim();
        final String email = emailActET.getText().toString().trim();
        final String password = passwordActET.getText().toString();
        final String confirmPassword = confirmPasswordActET.getText().toString();

        if (password.equals(confirmPassword)) {

            if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)
                    && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {

                dialog.setMessage("Creating Account...");
                dialog.show();

                auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        if (authResult != null) {

                            final StorageReference filePath = storageRef.child("MBlog_Pfps").child(resultUri.getLastPathSegment());

                            Task<Uri> urlTask = filePath.putFile(resultUri).continueWithTask(
                                    new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                        @Override
                                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                                            if (!task.isSuccessful()) {
                                                throw task.getException();
                                            }

                                            return filePath.getDownloadUrl();
                                        }
                                    }
                            ).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {

                                        Uri downloadUrl = task.getResult();

                                        String userID = auth.getCurrentUser().getUid();
                                        DatabaseReference currentUserDb = dbRef.child(userID);
                                        currentUserDb.child("firstname").setValue(firstName);
                                        currentUserDb.child("lastname").setValue(lastName);
                                        currentUserDb.child("pfp").setValue(downloadUrl.toString());
                                        currentUserDb.child("email").setValue(email);

                                        dialog.dismiss();

                                        // Send users to PostListActivity
                                        Intent intent = new Intent(CreateAccountActivity.this, PostListActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);


                                    }
                                }
                            });


                        }
                    }
                });

            }
        } else {
            Toast.makeText(CreateAccountActivity.this, "Passwords Do Not Match!", Toast.LENGTH_LONG).show();
        }
    }
}