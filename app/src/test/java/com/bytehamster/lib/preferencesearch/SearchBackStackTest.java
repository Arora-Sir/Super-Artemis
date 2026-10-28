package com.bytehamster.lib.preferencesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// NOTE: Settings once needed one extra Back press after every search result, because the search page stayed on the back stack (BUG-044).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SearchBackStackTest {
    private static final int CONTAINER_ID = 4711;

    public static class Host extends AppCompatActivity {
        @Override
        protected void onCreate(android.os.Bundle savedInstanceState) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat);
            super.onCreate(savedInstanceState);
            FrameLayout container = new FrameLayout(this);
            container.setId(CONTAINER_ID);
            setContentView(container);
        }
    }

    private static void showSearchPage(AppCompatActivity activity) {
        activity.getSupportFragmentManager().beginTransaction()
                .add(CONTAINER_ID, new Fragment(), SearchPreferenceFragment.TAG)
                .addToBackStack(SearchPreferenceFragment.TAG)
                .commit();
        activity.getSupportFragmentManager().executePendingTransactions();
    }

    @Test
    public void closingTheSearchPageLeavesNothingOnTheBackStack() {
        AppCompatActivity activity = Robolectric.buildActivity(Host.class).setup().get();
        FragmentManager fm = activity.getSupportFragmentManager();
        SearchPreferenceResult result = new SearchPreferenceResult("some_key", 0, null);

        for (int i = 0; i < 5; i++) {
            showSearchPage(activity);
            assertEquals(1, fm.getBackStackEntryCount());

            result.closeSearchPage(activity);
            fm.executePendingTransactions();

            assertEquals("Cycle " + i + " left a stale back stack entry", 0, fm.getBackStackEntryCount());
            assertNull(fm.findFragmentByTag(SearchPreferenceFragment.TAG));
        }
    }

    @Test
    public void closingWithoutASearchPageDoesNothing() {
        AppCompatActivity activity = Robolectric.buildActivity(Host.class).setup().get();
        new SearchPreferenceResult("some_key", 0, null).closeSearchPage(activity);
        assertEquals(0, activity.getSupportFragmentManager().getBackStackEntryCount());
    }
}
