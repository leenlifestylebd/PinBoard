package com.pinboard.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PinboardIME extends InputMethodService {

    private View mInputView;
    private boolean mIsBangla = false;
    private boolean mIsCaps = false;
    private boolean mPinOpen = false;
    private int mTab = 0;
    private PinDatabase mDb;

    @Override
    public void onCreate() {
        super.onCreate();
        mDb = PinDatabase.getInstance(this);
    }

    @Override
    public View onCreateInputView() {
        mInputView = LayoutInflater.from(this)
                .inflate(R.layout.keyboard_view, null);
        setupAllKeys();
        return mInputView;
    }

    private void setupAllKeys() {
        // English letter keys
        int[] engIds = {
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v,
            R.id.key_b, R.id.key_n, R.id.key_m
        };
        String[] engLower = {
            "q","w","e","r","t","y","u","i","o","p",
            "a","s","d","f","g","h","j","k","l",
            "z","x","c","v","b","n","m"
        };
        String[] engUpper = {
            "Q","W","E","R","T","Y","U","I","O","P",
            "A","S","D","F","G","H","J","K","L",
            "Z","X","C","V","B","N","M"
        };
        String[] bnChars = {
            "১","২","৩","৪","৫","৬","৭","৮","৯","০",
            "ট","ঠ","ড","ঢ","ণ","ত","থ","দ","ধ",
            "ন","প","ফ","ব","ভ","ম","য"
        };

        for (int i = 0; i < engIds.length; i++) {
            final String lo = engLower[i];
            final String hi = engUpper[i];
            final String bn = bnChars[i];
            View k = mInputView.findViewById(engIds[i]);
            if (k != null) {
                k.setOnClickListener(v -> {
                    String ch = mIsBangla ? bn : (mIsCaps ? hi : lo);
                    commitText(ch);
                });
            }
        }

        // Bangla row 1
        int[] bnIds1 = {
            R.id.key_bn_1, R.id.key_bn_2, R.id.key_bn_3, R.id.key_bn_4,
            R.id.key_bn_5, R.id.key_bn_6, R.id.key_bn_7, R.id.key_bn_8,
            R.id.key_bn_9, R.id.key_bn_10
        };
        String[] bnChars1 = {"অ","ই","উ","এ","ও","ক","গ","ঙ","ছ","ঝ"};
        for (int i = 0; i < bnIds1.length; i++) {
            final String ch = bnChars1[i];
            View k = mInputView.findViewById(bnIds1[i]);
            if (k != null) k.setOnClickListener(v -> commitText(ch));
        }

        // Bangla rows 2 & 3
        int[] bnIds2 = {
            R.id.key_bn_11, R.id.key_bn_12, R.id.key_bn_13, R.id.key_bn_14,
            R.id.key_bn_15, R.id.key_bn_16, R.id.key_bn_17, R.id.key_bn_18,
            R.id.key_bn_19, R.id.key_bn_20, R.id.key_bn_21, R.id.key_bn_22,
            R.id.key_bn_23, R.id.key_bn_24, R.id.key_bn_25, R.id.key_bn_hasanta
        };
        String[] bnChars2 = {
            "ট","ঠ","ড","ণ","ত","দ","ন","প",
            "ব","ভ","ম","য","র","ল","শ","্"
        };
        for (int i = 0; i < bnIds2.length; i++) {
            final String ch = bnChars2[i];
            View k = mInputView.findViewById(bnIds2[i]);
            if (k != null) k.setOnClickListener(v -> commitText(ch));
        }

        // Backspace
        for (int id : new int[]{R.id.key_backspace, R.id.key_backspace_bn}) {
            View k = mInputView.findViewById(id);
            if (k != null) {
                k.setOnClickListener(v -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) ic.deleteSurroundingText(1, 0);
                    vib();
                });
                k.setOnLongClickListener(v -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) ic.deleteSurroundingText(8, 0);
                    return true;
                });
            }
        }

        // Space
        for (int id : new int[]{R.id.key_space, R.id.key_space_bn}) {
            View k = mInputView.findViewById(id);
            if (k != null) k.setOnClickListener(v -> commitText(" "));
        }

        // Enter
        for (int id : new int[]{R.id.key_enter, R.id.key_enter_bn}) {
            View k = mInputView.findViewById(id);
            if (k != null) k.setOnClickListener(v -> commitText("\n"));
        }

        // Caps
        View caps = mInputView.findViewById(R.id.key_caps);
        if (caps != null) caps.setOnClickListener(v -> {
            mIsCaps = !mIsCaps;
            ((TextView) caps).setAlpha(mIsCaps ? 1f : 0.5f);
            vib();
        });

        // Language toggle
        View lang = mInputView.findViewById(R.id.key_lang);
        if (lang != null) lang.setOnClickListener(v -> {
            mIsBangla = !mIsBangla;
            View eng = mInputView.findViewById(R.id.layout_english);
            View bn  = mInputView.findViewById(R.id.layout_bangla);
            if (eng != null) eng.setVisibility(mIsBangla ? View.GONE : View.VISIBLE);
            if (bn  != null) bn.setVisibility(mIsBangla ? View.VISIBLE : View.GONE);
            ((TextView) lang).setText(mIsBangla ? "EN" : "বাং");
            vib();
        });

        // Pin button
        View pin = mInputView.findViewById(R.id.key_pin);
        if (pin != null) pin.setOnClickListener(v -> togglePinPanel());

        // Settings
        View settings = mInputView.findViewById(R.id.key_settings);
        if (settings != null) settings.setOnClickListener(v -> {
            Intent i = new Intent(this, PinboardActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        // Pin panel tabs
        View tabAll   = mInputView.findViewById(R.id.tab_all);
        View tabText  = mInputView.findViewById(R.id.tab_text);
        View tabImage = mInputView.findViewById(R.id.tab_image);
        View tabCombo = mInputView.findViewById(R.id.tab_combo);
        if (tabAll   != null) tabAll.setOnClickListener(v   -> setTab(0));
        if (tabText  != null) tabText.setOnClickListener(v  -> setTab(1));
        if (tabImage != null) tabImage.setOnClickListener(v -> setTab(2));
        if (tabCombo != null) tabCombo.setOnClickListener(v -> setTab(3));

        // Combo option buttons
        View btnText   = mInputView.findViewById(R.id.btn_paste_text);
        View btnImg    = mInputView.findViewById(R.id.btn_paste_image);
        View btnBoth   = mInputView.findViewById(R.id.btn_paste_both);
        View btnCancel = mInputView.findViewById(R.id.btn_paste_cancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v ->
            hideComboBar());
    }

    private void commitText(String ch) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(ch, 1);
        vib();
    }

    private void togglePinPanel() {
        mPinOpen = !mPinOpen;
        View panel = mInputView.findViewById(R.id.pin_panel);
        if (panel != null) {
            panel.setVisibility(mPinOpen ? View.VISIBLE : View.GONE);
            if (mPinOpen) loadPins();
        }
    }

    private void setTab(int tab) {
        mTab = tab;
        int on  = 0xFFE94560;
        int off = 0xFF555577;
        setTabColor(R.id.tab_all,   tab == 0 ? on : off);
        setTabColor(R.id.tab_text,  tab == 1 ? on : off);
        setTabColor(R.id.tab_image, tab == 2 ? on : off);
        setTabColor(R.id.tab_combo, tab == 3 ? on : off);
        loadPins();
    }

    private void setTabColor(int id, int color) {
        TextView tv = mInputView.findViewById(id);
        if (tv != null) tv.setTextColor(color);
    }

    private void loadPins() {
        RecyclerView rv = mInputView.findViewById(R.id.pin_recycler);
        if (rv == null) return;
        if (rv.getLayoutManager() == null)
            rv.setLayoutManager(new LinearLayoutManager(this,
                    LinearLayoutManager.HORIZONTAL, false));

        List<PinItem> pins;
        switch (mTab) {
            case 1: pins = mDb.getTextPins(); break;
            case 2: pins = mDb.getImagePins(); break;
            case 3: pins = mDb.getPinsByType(PinItem.TYPE_TEXT_IMAGE); break;
            default: pins = mDb.getAllPins(); break;
        }

        rv.setAdapter(new PinAdapter(pins, this::onPinTap, this::onPinLong));
    }

    private void onPinTap(PinItem pin) {
        if (pin.isCombo()) {
            showComboBar(pin);
        } else if (pin.isTextOnly()) {
            commitText(pin.getTextContent());
        } else {
            copyImage(pin.getImagePath());
        }
    }

    private void onPinLong(PinItem pin) {
        mDb.deletePin(pin.getId());
        if (pin.hasImage()) new File(pin.getImagePath()).delete();
        loadPins();
        Toast.makeText(this, "Deleted: " + pin.getDisplayLabel(), Toast.LENGTH_SHORT).show();
    }

    private void showComboBar(PinItem pin) {
        View bar = mInputView.findViewById(R.id.combo_option_bar);
        if (bar == null) { commitText(pin.getTextContent()); return; }
        bar.setVisibility(View.VISIBLE);

        View bt = mInputView.findViewById(R.id.btn_paste_text);
        View bi = mInputView.findViewById(R.id.btn_paste_image);
        View bb = mInputView.findViewById(R.id.btn_paste_both);
        View bc = mInputView.findViewById(R.id.btn_paste_cancel);

        if (bt != null) bt.setOnClickListener(v -> { commitText(pin.getTextContent()); hideComboBar(); });
        if (bi != null) bi.setOnClickListener(v -> { copyImage(pin.getImagePath()); hideComboBar(); });
        if (bb != null) bb.setOnClickListener(v -> { commitText(pin.getTextContent()); copyImage(pin.getImagePath()); hideComboBar(); });
        if (bc != null) bc.setOnClickListener(v -> hideComboBar());
    }

    private void hideComboBar() {
        View bar = mInputView.findViewById(R.id.combo_option_bar);
        if (bar != null) bar.setVisibility(View.GONE);
    }

    private void copyImage(String path) {
        try {
            File f = new File(path);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newUri(getContentResolver(), "img", uri));
            Toast.makeText(this, "Image copied!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        vib();
    }

    private void vib() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(15);
        } catch (Exception ignored) {}
    }

    // ── Simple Adapter ─────────────────────────────────────────────────────

    interface OnPin  { void on(PinItem p); }

    static class PinAdapter extends RecyclerView.Adapter<PinAdapter.VH> {
        private final List<PinItem> pins;
        private final OnPin click, longClick;

        PinAdapter(List<PinItem> p, OnPin c, OnPin lc) {
            pins = p != null ? p : new ArrayList<>();
            click = c; longClick = lc;
        }

        @Override public int getItemViewType(int i) {
            PinItem p = pins.get(i);
            if (p.isCombo()) return 2;
            if (p.isImageOnly()) return 1;
            return 0;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int vt) {
            int lid = vt == 2 ? R.layout.item_pin_combo
                    : vt == 1 ? R.layout.item_pin_image
                    : R.layout.item_pin_text;
            View v = LayoutInflater.from(parent.getContext()).inflate(lid, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            PinItem p = pins.get(pos);
            if (h.label   != null) h.label.setText(p.getDisplayLabel());
            if (h.content != null && p.hasText()) h.content.setText(p.getTextContent());
            if (h.image   != null && p.hasImage())
                Glide.with(h.image.getContext()).load(new File(p.getImagePath()))
                        .centerCrop().into(h.image);
            h.itemView.setOnClickListener(v -> click.on(p));
            h.itemView.setOnLongClickListener(v -> { longClick.on(p); return true; });
        }

        @Override public int getItemCount() { return pins.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView label, content; ImageView image;
            VH(View v) {
                super(v);
                label   = v.findViewById(R.id.pin_label);
                content = v.findViewById(R.id.pin_content);
                image   = v.findViewById(R.id.pin_image);
            }
        }
    }
}
