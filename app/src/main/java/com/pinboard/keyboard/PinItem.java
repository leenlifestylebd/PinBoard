package com.pinboard.keyboard;

public class PinItem {
    // Pin types
    public static final int TYPE_TEXT       = 0;
    public static final int TYPE_IMAGE      = 1;
    public static final int TYPE_TEXT_IMAGE = 2;  // text + image combo

    private long   id;
    private int    type;
    private String textContent;   // text portion (null if image-only)
    private String imagePath;     // image file path (null if text-only)
    private String label;
    private long   timestamp;
    private int    order;

    public PinItem() {}

    /** Text-only pin */
    public static PinItem textPin(String text, String label) {
        PinItem p = new PinItem();
        p.type        = TYPE_TEXT;
        p.textContent = text;
        p.label       = label;
        p.timestamp   = System.currentTimeMillis();
        return p;
    }

    /** Image-only pin */
    public static PinItem imagePin(String imagePath, String label) {
        PinItem p = new PinItem();
        p.type      = TYPE_IMAGE;
        p.imagePath = imagePath;
        p.label     = label;
        p.timestamp = System.currentTimeMillis();
        return p;
    }

    /** Text + Image combo pin */
    public static PinItem comboPin(String text, String imagePath, String label) {
        PinItem p = new PinItem();
        p.type        = TYPE_TEXT_IMAGE;
        p.textContent = text;
        p.imagePath   = imagePath;
        p.label       = label;
        p.timestamp   = System.currentTimeMillis();
        return p;
    }

    public long   getId()          { return id; }
    public int    getType()        { return type; }
    public String getTextContent() { return textContent; }
    public String getImagePath()   { return imagePath; }
    public String getLabel()       { return label; }
    public long   getTimestamp()   { return timestamp; }
    public int    getOrder()       { return order; }

    public void setId(long id)            { this.id = id; }
    public void setType(int type)         { this.type = type; }
    public void setTextContent(String t)  { this.textContent = t; }
    public void setImagePath(String p)    { this.imagePath = p; }
    public void setLabel(String label)    { this.label = label; }
    public void setTimestamp(long ts)     { this.timestamp = ts; }
    public void setOrder(int order)       { this.order = order; }

    public boolean isTextOnly()  { return type == TYPE_TEXT; }
    public boolean isImageOnly() { return type == TYPE_IMAGE; }
    public boolean isCombo()     { return type == TYPE_TEXT_IMAGE; }
    public boolean hasText()     { return textContent != null && !textContent.isEmpty(); }
    public boolean hasImage()    { return imagePath != null && !imagePath.isEmpty(); }

    public String getTypeLabel() {
        switch (type) {
            case TYPE_TEXT:       return "📝 Text";
            case TYPE_IMAGE:      return "🖼 Image";
            case TYPE_TEXT_IMAGE: return "📝🖼 Text+Image";
            default:              return "Pin";
        }
    }

    public String getDisplayLabel() {
        if (label != null && !label.isEmpty()) return label;
        if (hasText()) {
            String t = textContent.trim();
            return t.length() > 35 ? t.substring(0, 35) + "…" : t;
        }
        return "Image Pin";
    }

    public String getContent() {
        if (hasText()) return textContent;
        return imagePath;
    }
}
