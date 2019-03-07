package com.mapbox.mapboxsdk.testapp.activity;

import android.support.annotation.UiThread;
import android.support.test.rule.ActivityTestRule;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.testapp.R;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import timber.log.Timber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Base class for all Activity test hooking into an existing Activity that will load style.
 */
public abstract class BaseTest implements MapView.OnDidFinishLoadingStyleListener {

  private static final int WAIT_TIMEOUT = 30; //seconds
  private static final int MAX_TIMEOUT_FAILURE = 3;
  private static int timeoutCount;

  @Rule
  public ActivityTestRule rule = new ActivityTestRule<>(getActivityClass());

  @Rule
  public TestName testName = new TestName();

  protected MapboxMap mapboxMap;
  protected MapView mapView;
  private final CountDownLatch latch = new CountDownLatch(1);

  @Before
  public void beforeTest() {
    initialiseMap();
    holdTestRunnerForStyleLoad();
  }

  private void initialiseMap() {
    try {
      rule.runOnUiThread(() -> {
        mapView = rule.getActivity().findViewById(R.id.mapView);
        mapView.addOnDidFinishLoadingStyleListener(this);
        mapView.getMapAsync(this::initMap);
      });
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
  }

  private void holdTestRunnerForStyleLoad() {
    boolean interrupted;
    try {
      interrupted = latch.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
    } catch (InterruptedException ignore) {
      interrupted = true;
    }

    if (!interrupted) {
      Timber.e("Timeout occurred for %s", testName.getMethodName());

      // update global timeout counter
      timeoutCount++;
      if (timeoutCount > MAX_TIMEOUT_FAILURE) {
        // Too many timeouts, stop execution test runner
        Timber.e("Too many timeouts occurred. Crashing Test-Suite.");
        System.exit(-1);
      }
    }
  }

  @Override
  public void onDidFinishLoadingStyle() {
    latch.countDown();
  }

  @UiThread
  protected void initMap(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    Style style = mapboxMap.getStyle();
    if (style != null && style.isFullyLoaded()) {
      Timber.e("onDidFinishLoadingStyle with initMap");
      onDidFinishLoadingStyle();
    }
  }

  protected void validateTestSetup() {
    if (!Mapbox.isConnected()) {
      Timber.e("Not connected to the internet while running test");
    }
    assertNotNull("MapView isn't initialised", mapView);
    assertNotNull("MapboxMap isn't initialised", mapboxMap);
    assertNotNull("Style isn't initialised", mapboxMap.getStyle());
    assertTrue("Style isn't fully loaded", mapboxMap.getStyle().isFullyLoaded());
  }

  protected abstract Class getActivityClass();

  @After
  public void afterTest() {
    mapView.removeOnDidFinishLoadingStyleListener(this);
  }
}

