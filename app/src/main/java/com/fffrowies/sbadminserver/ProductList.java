package com.fffrowies.sbadminserver;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fffrowies.sbadminserver.Common.Common;
import com.fffrowies.sbadminserver.Interface.ItemClickListener;
import com.fffrowies.sbadminserver.Model.Product;
import com.fffrowies.sbadminserver.ViewHolder.ProductViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.UUID;

import info.hoang8f.widget.FButton;

public class ProductList extends AppCompatActivity {

    RecyclerView recycler_product;
    RecyclerView.LayoutManager layoutManager;

    RelativeLayout rootLayout;

    FloatingActionButton fab;

    //Firebase
    FirebaseDatabase db;
    DatabaseReference productList;
    FirebaseStorage storage;
    StorageReference storageReference;

    String categoryId = "";

    FirebaseRecyclerAdapter<Product, ProductViewHolder> adapter;

    //Add New Product Layout
    MaterialEditText edtName, edtDescription, edtPrice, edtDiscount;
    FButton btnUpload, btnSelect;

    Product newProduct;

    Uri saveUri;

    ProgressBar progressBar;
    TextView txvProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        //Firebase
        db = FirebaseDatabase.getInstance();
        productList = db.getReference("Products");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //Init
        recycler_product = (RecyclerView) findViewById(R.id.recycler_product);
        recycler_product.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_product.setLayoutManager(layoutManager);

        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddProductDialog();
            }
        });

        //Get Intent here
        if (getIntent() != null) categoryId = getIntent().getStringExtra("CategoryId");

        if (!categoryId.isEmpty() && categoryId != null) {

            loadProductList(categoryId);
        }
    }

    private void loadProductList(String categoryId) {
        adapter = new FirebaseRecyclerAdapter<Product, ProductViewHolder>(
                Product.class,
                R.layout.product_item,
                ProductViewHolder.class,
                productList.orderByChild("categoryId").equalTo(categoryId)
        ) {
            @Override
            protected void populateViewHolder(ProductViewHolder viewHolder, Product model, int position) {
                viewHolder.product_name.setText(model.getName());
                Picasso.with(getBaseContext())
                        .load(model.getImage())
                        .into(viewHolder.product_image);

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Code late
                    }
                });
            }
        };
        adapter.notifyDataSetChanged();
        recycler_product.setAdapter(adapter);
    }

    private void showAddProductDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ProductList.this);
        alertDialog.setTitle("Add new Product");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_product_layout = inflater.inflate(R.layout.add_new_product, null);

        edtName = (MaterialEditText) add_product_layout.findViewById(R.id.edtName);
        edtDescription = (MaterialEditText) add_product_layout.findViewById(R.id.edtDescription);
        edtPrice = (MaterialEditText) add_product_layout.findViewById(R.id.edtPrice);
        edtDiscount = (MaterialEditText) add_product_layout.findViewById(R.id.edtDiscount);

        btnSelect = (FButton) add_product_layout.findViewById(R.id.btnSelect);
        btnUpload = (FButton) add_product_layout.findViewById(R.id.btnUpload);

        progressBar = (ProgressBar) add_product_layout.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.INVISIBLE);
        txvProgressBar = (TextView) add_product_layout.findViewById(R.id.txvProgressBar);

        //Event for buttons
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); //Let user select image from Gallery and save Uri of this image
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        alertDialog.setView(add_product_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //Set button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                //Create new Product
                if (newProduct != null) {
                    productList.push().setValue(newProduct);
                    Snackbar.make(rootLayout,"New product "+newProduct.getName()+" was added",Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), Common.PICK_IMAGE_REQUEST);
    }

    private void uploadImage() {

        if (saveUri != null) {
            progressBar.setVisibility(View.VISIBLE);
            txvProgressBar.setText("Uploading...");

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.INVISIBLE);
                            txvProgressBar.setText("");

                            Toast.makeText(ProductList.this, "Uploaded!!!", Toast.LENGTH_SHORT).show();

                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //set value for new product if image upload and we can get download link
                                    newProduct = new Product();
                                    newProduct.setName(edtName.getText().toString());
                                    newProduct.setDescription(edtDescription.getText().toString());
                                    newProduct.setPrice(edtPrice.getText().toString());
                                    newProduct.setDiscount(edtDiscount.getText().toString());
                                    newProduct.setCategoryId(categoryId);
                                    newProduct.setImage(uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.INVISIBLE);
                            txvProgressBar.setText("");

                            Toast.makeText(ProductList.this, "ERROR " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            txvProgressBar.setText("Uploaded " + progress + "%");
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            saveUri = data.getData();
            btnSelect.setText("Image Selected!");
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (item.getTitle().equals(Common.UPDATE)) {
            showUpdateProductDialog(adapter.getRef(item.getOrder()).getKey(), adapter.getItem(item.getOrder()));
        } else if (item.getTitle().equals(Common.DELETE)) {
            deleteProduct(adapter.getRef(item.getOrder()).getKey());
        }

        return super.onContextItemSelected(item);
    }

    private void deleteProduct(String key) {
        productList.child(key).removeValue();
    }

    private void showUpdateProductDialog(final String key, final Product item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ProductList.this);
        alertDialog.setTitle("Edit Product");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_product_layout = inflater.inflate(R.layout.add_new_product, null);

        edtName = (MaterialEditText) add_product_layout.findViewById(R.id.edtName);
        edtDescription = (MaterialEditText) add_product_layout.findViewById(R.id.edtDescription);
        edtPrice = (MaterialEditText) add_product_layout.findViewById(R.id.edtPrice);
        edtDiscount = (MaterialEditText) add_product_layout.findViewById(R.id.edtDiscount);

        //set default value for view
        edtName.setText(item.getName());
        edtDescription.setText(item.getDescription());
        edtPrice.setText(item.getPrice());
        edtDiscount.setText(item.getDiscount());

        btnSelect = (FButton) add_product_layout.findViewById(R.id.btnSelect);
        btnUpload = (FButton) add_product_layout.findViewById(R.id.btnUpload);

        progressBar = (ProgressBar) add_product_layout.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.INVISIBLE);
        txvProgressBar = (TextView) add_product_layout.findViewById(R.id.txvProgressBar);

        //Event for buttons
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); //Let user select image from Gallery and save Uri of this image
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeImage(item);
            }
        });

        alertDialog.setView(add_product_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //Set button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                //Update information
                item.setName(edtName.getText().toString());
                item.setDescription(edtDescription.getText().toString());
                item.setPrice(edtPrice.getText().toString());
                item.setDiscount(edtDiscount.getText().toString());

                productList.child(key).setValue(item);
                Snackbar.make(rootLayout,"Product "+item.getName()+" was edited",Snackbar.LENGTH_SHORT).show();
            }
        });
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void changeImage(final Product item) {

        if (saveUri != null) {
            progressBar.setVisibility(View.VISIBLE);
            txvProgressBar.setText("Uploading...");

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.INVISIBLE);
                            txvProgressBar.setText("");

                            Toast.makeText(ProductList.this, "Uploaded!!!", Toast.LENGTH_SHORT).show();

                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //set value for new category if image upload and we can get download link
                                    item.setImage(uri.toString());

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.INVISIBLE);
                            txvProgressBar.setText("");

                            Toast.makeText(ProductList.this, "ERROR " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            txvProgressBar.setText("Uploaded " + progress + "%");
                        }
                    });
        }
    }
}
