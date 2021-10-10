package ru.igla.duocamera.ui.toastcompat;

import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * @author drakeet
 */
public interface BadTokenListener {
    void onBadTokenCaught(@NonNull Toast toast);
}
