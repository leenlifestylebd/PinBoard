package com.pinboard.keyboard;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PinboardActivity extends AppCompatActivity {

    private RecyclerView    recyclerView;
    private ManagePinAdapter adapter;
    private PinDatabase     db;
    private List<PinItem>   allPins = new ArrayList<>();

    // pending image path when building a combo pin
    private String          pendingComboImagePath = null;

    private ActivityResultLauncher<String> imagePickerForImage;
    private ActivityResultLauncher<String> imagePickerForCombo;
    private ActivityResultLauncher<String> permLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinboard);

        db = PinDatabase.getInstance(this);

        recyclerView = findViewById(R.id.recycler_pins);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManagePinAdapter(this, allPins, this::onDeletePin);
        recyclerView.setAdapter(adapter);

        // ── FAB buttons ──────────────────────────────────────────────────
        findViewById(R.id.btn_add_text).setOnClickListener(v  -> showAddTextDialog());
        findViewById(R.id.btn_add_image).setOnClickListener(v -> requestImage(false));
        findViewById(R.id.btn_add_combo).setOnClickListener(v -> showAddComboDialog());

        // ── Image pickers ─────────────────────────────────────────────────
        imagePickerForImage = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) saveImageOnlyPin(uri); });

        imagePickerForCombo = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) { pendingComboImagePath = copyImageToStorage(uri); showComboTextDialog(); } });

        permLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted) pickImageNow(pendingComboImagePath == null); else Toast.makeText(this,"Permission needed",Toast.LENGTH_SHORT).show(); });

        loadPins();
    }

    // ── Load ──────────────────────────────────────────────────────────────

    private void loadPins() {
        allPins.clear();
        allPins.addAll(db.getAllPins());
        adapter.notifyDataSetChanged();
    }

    // ── Add Text-only ─────────────────────────────────────────────────────

    private void showAddTextDialog() {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_add_text, null);
        EditText etText  = dv.findViewById(R.id.et_pin_text);
        EditText etLabel = dv.findViewById(R.id.et_pin_label);
        new AlertDialog.Builder(this)
                .setTitle("📝 Add Text Pin")
                .setView(dv)
                .setPositiveButton("Save", (d, w) -> {
                    String t = etText.getText().toString().trim();
                    String l = etLabel.getText().toString().trim();
                    if (t.isEmpty()) { Toast.makeText(this,"Please enter text",Toast.LENGTH_SHORT).show(); return; }
                    db.insertPin(PinItem.textPin(t, l));
                    loadPins();
                    Toast.makeText(this,"Text pin added!",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Add Image-only ────────────────────────────────────────────────────

    private void requestImage(boolean forCombo) {
        if (!forCombo) pendingComboImagePath = null; // reset flag
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            pickImageNow(!forCombo);
        } else {
            permLauncher.launch(perm);
        }
    }

    private void pickImageNow(boolean isImageOnly) {
        if (isImageOnly) imagePickerForImage.launch("image/*");
        else             imagePickerForCombo.launch("image/*");
    }

    private void saveImageOnlyPin(Uri uri) {
        String path = copyImageToStorage(uri);
        if (path == null) return;
        EditText etLabel = new EditText(this);
        etLabel.setHint("Label (optional)");
        new AlertDialog.Builder(this)
                .setTitle("🖼 Add Label")
                .setView(etLabel)
                .setPositiveButton("Save", (d, w) -> {
                    db.insertPin(PinItem.imagePin(path, etLabel.getText().toString().trim()));
                    loadPins();
                    Toast.makeText(this,"Image pin added!",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Skip", (d,w) -> {
                    db.insertPin(PinItem.imagePin(path, ""));
                    loadPins();
                    Toast.makeText(this,"Image pin added!",Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ── Add Combo (Text + Image) ──────────────────────────────────────────

    private void showAddComboDialog() {
        // Step 1: user picks image first, then we ask for text
        new AlertDialog.Builder(this)
                .setTitle("📝🖼 Add Text + Image Pin")
                .setMessage("Step 1: Pick an image from your gallery.")
                .setPositiveButton("Pick Image", (d, w) -> requestImage(true))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Called after image is chosen — now ask for the text */
    private void showComboTextDialog() {
        if (pendingComboImagePath == null) return;
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_add_text, null);
        EditText etText  = dv.findViewById(R.id.et_pin_text);
        EditText etLabel = dv.findViewById(R.id.et_pin_label);
        new AlertDialog.Builder(this)
                .setTitle("Step 2: Enter text for this pin")
                .setView(dv)
                .setPositiveButton("Save", (d, w) -> {
                    String t = etText.getText().toString().trim();
                    String l = etLabel.getText().toString().trim();
                    if (t.isEmpty()) { Toast.makeText(this,"Please enter text",Toast.LENGTH_SHORT).show(); return; }
                    db.insertPin(PinItem.comboPin(t, pendingComboImagePath, l));
                    pendingComboImagePath = null;
                    loadPins();
                    Toast.makeText(this,"Text+Image pin added!",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (d,w) -> pendingComboImagePath = null)
                .show();
    }

    // ── Delete ────────────────────────────────────────────────────────────

    private void onDeletePin(PinItem pin) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Pin")
                .setMessage("Remove \"" + pin.getDisplayLabel() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (pin.hasImage()) new File(pin.getImagePath()).delete();
                    db.deletePin(pin.getId());
                    loadPins();
                    Toast.makeText(this,"Pin deleted",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Storage helper ────────────────────────────────────────────────────

    private String copyImageToStorage(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "PinboardImages");
            dir.mkdirs();
            File dest = new File(dir, "pin_" + UUID.randomUUID() + ".jpg");
            InputStream in  = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[4096]; int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close();
            return dest.getAbsolutePath();
        } catch (Exception e) {
            Toast.makeText(this,"Failed to save image: " + e.getMessage(),Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Manage Pins Adapter
    // ════════════════════════════════════════════════════════════════════════

    public interface DeletePinListener { void onDelete(PinItem pin); }

    public static class ManagePinAdapter extends RecyclerView.Adapter<ManagePinAdapter.ViewHolder> {

        private final android.app.Activity act;
        private final List<PinItem>        pins;
        private final DeletePinListener    listener;

        public ManagePinAdapter(android.app.Activity a, List<PinItem> p, DeletePinListener l) {
            act = a; pins = p; listener = l;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int vt) {
            View v = LayoutInflater.from(act).inflate(R.layout.item_manage_pin, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int pos) {
            PinItem pin = pins.get(pos);
            h.label.setText(pin.getDisplayLabel());
            h.type.setText(pin.getTypeLabel());

            // image preview
            if (pin.hasImage()) {
                h.image.setVisibility(View.VISIBLE);
                Glide.with(act).load(new File(pin.getImagePath())).centerCrop().into(h.image);
            } else {
                h.image.setVisibility(View.GONE);
            }

            // text preview
            if (pin.hasText()) {
                h.preview.setVisibility(View.VISIBLE);
                h.preview.setText(pin.getTextContent());
            } else {
                h.preview.setVisibility(View.GONE);
            }

            h.deleteBtn.setOnClickListener(v -> listener.onDelete(pin));
        }

        @Override public int getItemCount() { return pins.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView    label, type, preview;
            ImageView   image;
            ImageButton deleteBtn;
            ViewHolder(View v) {
                super(v);
                label     = v.findViewById(R.id.pin_label);
                type      = v.findViewById(R.id.pin_type);
                preview   = v.findViewById(R.id.pin_preview);
                image     = v.findViewById(R.id.pin_image_preview);
                deleteBtn = v.findViewById(R.id.btn_delete);
            }
        }
    }
}
