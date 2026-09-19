package com.app.rewardsplanet.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * SpinResponse — Backend is the SOLE authority for spin results.
 *
 * Accepts ALL common field-name variations the backend might send:
 *  remainingSpins / remaining_spins / spins_left / spinsLeft / spins_remaining
 *  segmentIndex   / segment_index   / slot / index
 *  totalSegments  / total_segments  / segments / segment_count
 *  wheelSegments  / wheel_segments  / segments_list / wheel
 */
public class SpinResponse {

    // ─────────────────────────────────────────────────────────────────────────
    // REWARD — coins won this spin
    // ─────────────────────────────────────────────────────────────────────────
    @SerializedName(value = "reward", alternate = {"coins", "amount", "prize"})
    public int reward;

    // ─────────────────────────────────────────────────────────────────────────
    // REMAINING SPINS — how many spins are left today
    //
    // Accepts BOTH camelCase and snake_case from the backend:
    //   "remainingSpins"   ← original backend field (most likely)
    //   "remaining_spins"  ← snake_case variant
    //   "spins_left"       ← alternative naming
    //   "spinsLeft"        ← alternative camelCase
    //   "spins_remaining"  ← another common variant
    //   "left"             ← minimal response
    // ─────────────────────────────────────────────────────────────────────────
    @SerializedName(value = "remainingSpins", alternate = {
            "remaining_spins",
            "spins_left",
            "spinsLeft",
            "spins_remaining",
            "spinsRemaining",
            "left"
    })
    public int remainingSpins = -1;   // -1 = field was absent from JSON

    // ─────────────────────────────────────────────────────────────────────────
    // SEGMENT INDEX — 0-based index of the winning wheel segment (clockwise from 12 o'clock).
    // -1 = not provided (old backend).
    // ─────────────────────────────────────────────────────────────────────────
    @SerializedName(value = "segmentIndex", alternate = {
            "segment_index",
            "slot",
            "index",
            "slotIndex",
            "slot_index",
            "wheelSlot"
    })
    public int segmentIndex = -1;

    // ─────────────────────────────────────────────────────────────────────────
    // TOTAL SEGMENTS — how many slices the wheel has.
    // 0 = not provided (client falls back to DEFAULT_SEGMENT_COUNT).
    // ─────────────────────────────────────────────────────────────────────────
    @SerializedName(value = "totalSegments", alternate = {
            "total_segments",
            "segments",
            "segment_count",
            "segmentCount",
            "totalSlots",
            "total_slots"
    })
    public int totalSegments = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // WHEEL SEGMENTS — ordered reward values for each slice [index 0..N-1].
    // Optional: used for client-side verification only.
    // ─────────────────────────────────────────────────────────────────────────
    @SerializedName(value = "wheelSegments", alternate = {
            "wheel_segments",
            "segments_list",
            "segmentsList",
            "wheel",
            "slots"
    })
    public List<Integer> wheelSegments;

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the correct remaining-spin count.
     *
     * Uses >= 0 check (NOT > 0) so that a genuine "0 spins left" from the backend
     * is preserved — previously > 0 would skip 0 and return wrong fallback.
     *
     * Returns -1 ONLY if the field was completely absent from the JSON response.
     */
    public int getEffectiveRemainingSpins() {
        // remainingSpins maps ALL known field names via @SerializedName alternate.
        // If the backend sent any of them, this value will be >= 0.
        if (remainingSpins >= 0) return remainingSpins;
        // Field was absent from JSON entirely — caller should use local fallback.
        return -1;
    }

    /**
     * Returns the 0-based segment index the wheel should stop at.
     * -1 = backend did not send it (old backend; caller derives angle from reward value).
     */
    public int getEffectiveSegmentIndex() {
        return segmentIndex; // already defaults to -1 if absent
    }

    /**
     * Returns the total number of wheel segments.
     * 0 = backend did not send it; caller should use DEFAULT_SEGMENT_COUNT.
     */
    public int getEffectiveTotalSegments() {
        if (totalSegments > 0) return totalSegments;
        if (wheelSegments != null && !wheelSegments.isEmpty()) return wheelSegments.size();
        return 0;
    }
}