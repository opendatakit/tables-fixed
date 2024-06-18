package org.opendatakit.espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.ExportCSVActivity;
import org.opendatakit.tables.activities.ImportCSVActivity;
import org.opendatakit.tables.activities.MainActivity;

public class MainActivityTest {

    private final String appName = "default";

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    );

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivity = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        Intents.init();
        ActivityScenario.launch(MainActivity.class);

    }

    @After
    public void tearDown() {
        Intents.release();
    }


    @Test
    public void testMenuImport_whenClickedShouldLaunchImportActivity() {
        // Click on the Import menu item
        onView(withId(R.id.menu_table_manager_import)).perform(click());

        // Check if the Import activity is launched
        intended(hasComponent(ImportCSVActivity.class.getName()));
    }

    @Test
    public void testMenuExport_whenClickedShouldLaunchExportActivity() {
        // Click on the Export menu item
        onView(withId(R.id.menu_table_manager_export)).perform(click());

        // Check if the Export activity is launched
        intended(hasComponent(ExportCSVActivity.class.getName()));
    }

    @Test
    public void testToolBarVisibility() {
        onView(withId(R.id.toolbarMainActivity)).check(matches(isDisplayed()));
    }

    @Test
    public void testMenuItems() {
        // Open the overflow menu
        onView(ViewMatchers.withContentDescription("More options"))
                .perform(ViewActions.click());

        // Check that specific menu items are displayed
        onView(withText(R.string.about))
                .check(matches(isDisplayed()));

        onView(withText(R.string.preferences))
                .check(matches(isDisplayed()));
    }
}
