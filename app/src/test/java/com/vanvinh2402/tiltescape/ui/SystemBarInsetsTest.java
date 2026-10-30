package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class SystemBarInsetsTest {
    @Test
    public void repeatedInsetsReplaceRatherThanAccumulate() {
        View root = new View(RuntimeEnvironment.getApplication());
        root.setPadding(1, 2, 3, 4);
        SystemBarInsets.applyToPadding(root);
        dispatch(root, Insets.of(10, 20, 30, 40));
        assertPadding(root, 11, 22, 33, 44);
        dispatch(root, Insets.of(5, 6, 7, 8));
        assertPadding(root, 6, 8, 10, 12);
    }

    @Test
    @Config(sdk = 24)
    public void api24KeepsContentBelowSystemBars() {
        View root = new View(RuntimeEnvironment.getApplication());
        SystemBarInsets.applyToPadding(root);
        dispatch(root, Insets.of(0, 24, 0, 48));
        assertPadding(root, 0, 24, 0, 48);
    }

    @Test
    @Config(sdk = 35)
    public void api35KeepsContentBelowSystemBars() {
        View root = new View(RuntimeEnvironment.getApplication());
        SystemBarInsets.applyToPadding(root);
        dispatch(root, Insets.of(0, 30, 0, 50));
        assertPadding(root, 0, 30, 0, 50);
    }

    private static void dispatch(View view, Insets insets) {
        WindowInsetsCompat value = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), insets)
                .build();
        ViewCompat.dispatchApplyWindowInsets(view, value);
    }

    private static void assertPadding(View view, int left, int top, int right, int bottom) {
        assertEquals(left, view.getPaddingLeft());
        assertEquals(top, view.getPaddingTop());
        assertEquals(right, view.getPaddingRight());
        assertEquals(bottom, view.getPaddingBottom());
    }
}
