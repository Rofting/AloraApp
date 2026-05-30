package com.alora.app.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class CareLogPage {
    @SerializedName("content")
    private List<CareLog> content;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("totalElements")
    private long totalElements;

    @SerializedName("last")
    private boolean last;

    public List<CareLog> getContent() {
        return content != null ? content : new ArrayList<>();
    }

    public int getTotalPages() { return totalPages; }
    public long getTotalElements() { return totalElements; }
    public boolean isLast() { return last; }
}
