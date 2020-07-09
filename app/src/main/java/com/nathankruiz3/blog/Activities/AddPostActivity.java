package com.nathankruiz3.blog.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nathankruiz3.blog.Model.Blog;
import com.nathankruiz3.blog.R;

import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    private static final int GALLERY_CODE = 1;

    private ImageButton addPostImage;
    private EditText newPostTitle, newPostDescription;
    private Button postButton;
    private Uri mImageUri;

    private DatabaseReference dbRef;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private StorageReference storageRef;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        dialog = new ProgressDialog(this);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference().child("MBlog");
        storageRef = FirebaseStorage.getInstance().getReference();

        addPostImage = (ImageButton) findViewById(R.id.addImageButton);
        newPostTitle = (EditText) findViewById(R.id.postTitleET);
        newPostDescription = (EditText) findViewById(R.id.postDescriptionET);
        postButton = (Button) findViewById(R.id.postButton);

        addPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_CODE);
            }
        });

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Posting to db
                startPosting();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {

            mImageUri = data.getData();
            Log.v("ImageUri", mImageUri.toString());
            addPostImage.setImageURI(mImageUri);

        }

    }

    private void startPosting() {

        dialog.setMessage("Posting to blog...");
        dialog.show();

        final String titleVal = newPostTitle.getText().toString().trim();
        final String descriptionVal = newPostDescription.getText().toString().trim();


        if (!TextUtils.isEmpty(titleVal) && !TextUtils.isEmpty(descriptionVal)
                && mImageUri != null) {
            // Start uploading

            // Adding the photo to Firebase Storage
            final StorageReference filepath = storageRef.child("MBlog_images").child(mImageUri.getLastPathSegment());

            Task<Uri> urlTask = filepath.putFile(mImageUri).continueWithTask(
                    new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            return filepath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUrl = task.getResult();

                        DatabaseReference newPost = dbRef.push();

                        Map<String, String> dataToSave = new HashMap<>();

                        // Naming musk be exactly the same as Blog object properties
                        dataToSave.put("title", titleVal);
                        dataToSave.put("description", descriptionVal);
                        dataToSave.put("image", downloadUrl.toString());
                        dataToSave.put("timestamp", String.valueOf(java.lang.System.currentTimeMillis()));
                        dataToSave.put("userID", user.getUid());

                        newPost.setValue(dataToSave);

                        dialog.dismiss();

                        Intent intent = new Intent(AddPostActivity.this, PostListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    } else {
                        Log.v("Download Url Error", String.valueOf(task.getException()));
                    }
                }
            });
        }
    }
}