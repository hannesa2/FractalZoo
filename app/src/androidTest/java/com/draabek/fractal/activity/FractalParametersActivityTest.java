package com.draabek.fractal.activity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.allOf;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.draabek.fractal.R;
import com.draabek.fractal.fractal.FractalRegistry;

import junit.framework.Assert;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.NumberFormat;
import java.text.ParseException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FractalParametersActivityTest {

    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule = new IntentsTestRule<>(MainActivity.class);

    @Test
    public void fractalParametersActivityTest() throws ParseException {
        Intent intent = new Intent(FractalZooApplication.getContext(), FractalParametersActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        getInstrumentation().startActivitySync(intent);

        String parameterName = getText(allOf(childAtPosition(
                        allOf(withId(R.id.layout_parameters),
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0)),
                        1),
                isDisplayed()));

        String desiredValue = FractalRegistry.getInstance().getCurrent().getParameters().get(parameterName).toString();

        String parameterValue = getText(allOf(childAtPosition(
                        allOf(withId(R.id.layout_parameters),
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0)),
                        2),
                isDisplayed()));
        Assert.assertEquals(Float.parseFloat(desiredValue), NumberFormat.getInstance().parse(parameterValue).floatValue(), Float.MAX_VALUE);
        /*TODO make it work
        ViewInteraction button = onView(
                allOf(withText("OK"),
                        isDisplayed()));
        button.perform(click());
        getInstrumentation().waitForIdleSync();
        intended(hasComponent(MainActivity.class.getName()));
        */
    }

    String getText(final Matcher<View> matcher) {
        final String[] stringHolder = {null};
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView) view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

}