package com.vanvinh2402.tiltescape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParser;

@RunWith(RobolectricTestRunner.class)
public final class NeonUiResourcesTest {
    private final Context context = RuntimeEnvironment.getApplication();
    private final Resources resources = context.getResources();

    @Test
    public void neonPaletteMatchesApprovedDesign() {
        assertColor("background", Color.rgb(9, 11, 22));
        assertColor("surface", Color.rgb(19, 24, 49));
        assertColor("surface_elevated", Color.rgb(25, 31, 61));
        assertColor("primary", Color.rgb(109, 93, 252));
        assertColor("primary_end", Color.rgb(61, 155, 255));
        assertColor("accent", Color.rgb(42, 217, 186));
        assertColor("on_background", Color.rgb(248, 250, 252));
        assertColor("text_secondary", Color.rgb(174, 184, 220));
        assertColor("score_accent", Color.rgb(255, 102, 122));
    }

    @Test
    public void neonControlsMeetMinimumTouchTarget() {
        assertDimension("touch_target_min", 48);
        assertDimension("control_height", 52);
        assertDimension("card_radius", 24);
    }

    @Test
    public void neonDrawablesAreLoadable() {
        assertDrawable("bg_app_gradient");
        assertDrawable("bg_neon_card");
        assertDrawable("bg_neon_input");
        assertDrawable("bg_button_primary");
        assertDrawable("bg_button_secondary");
        assertDrawable("bg_button_ghost");
        assertDrawable("bg_result_item");
    }

    @Test
    public void customButtonsExposeAKeyboardFocusState() throws Exception {
        assertFocusedSelectorItem("bg_button_primary");
        assertFocusedSelectorItem("bg_button_secondary");
        assertFocusedSelectorItem("bg_button_ghost");
    }

    private void assertColor(String name, int expected) {
        int id = resources.getIdentifier(name, "color", context.getPackageName());
        assertTrue("Missing color resource: " + name, id != 0);
        assertEquals(name, expected, resources.getColor(id, context.getTheme()));
    }

    private void assertDimension(String name, int expectedDp) {
        int id = resources.getIdentifier(name, "dimen", context.getPackageName());
        assertTrue("Missing dimension resource: " + name, id != 0);
        int expectedPx = Math.round(expectedDp * resources.getDisplayMetrics().density);
        assertEquals(name, expectedPx, resources.getDimensionPixelSize(id));
    }

    private void assertDrawable(String name) {
        int id = resources.getIdentifier(name, "drawable", context.getPackageName());
        assertTrue("Missing drawable resource: " + name, id != 0);
        Drawable drawable = resources.getDrawable(id, context.getTheme());
        assertNotNull(name, drawable);
    }

    private void assertFocusedSelectorItem(String name) throws Exception {
        int id = resources.getIdentifier(name, "drawable", context.getPackageName());
        assertTrue("Missing drawable resource: " + name, id != 0);
        boolean hasFocusedItem = false;
        try (XmlResourceParser parser = resources.getXml(id)) {
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event != XmlPullParser.START_TAG || !"item".equals(parser.getName())) {
                    continue;
                }
                for (int index = 0; index < parser.getAttributeCount(); index++) {
                    if ("state_focused".equals(parser.getAttributeName(index))
                            && parser.getAttributeBooleanValue(index, false)) {
                        hasFocusedItem = true;
                    }
                }
            }
        }
        assertTrue(name + " must provide a visible keyboard-focus state", hasFocusedItem);
    }
}
