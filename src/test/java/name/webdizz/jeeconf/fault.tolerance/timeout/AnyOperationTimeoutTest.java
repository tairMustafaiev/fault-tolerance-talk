package name.webdizz.jeeconf.fault.tolerance.timeout;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static name.webdizz.jeeconf.fault.tolerance.utils.TimeOutAssertion.assertTheCallTookBetween;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

public class AnyOperationTimeoutTest {

    private static final int DELAY_MS = 50;
    private static final int ENOUGH_MS = 500;

    private static final int NOT_ENOUGH_MS = 5;

    @Test
    public void shouldUseFuture() throws Exception {
        ExecutorService executorService = Executors.newCachedThreadPool();
        long start = System.nanoTime();
        Future<String> future = executorService.submit(() -> doSomeHeavyWeightOperation());
        String result = future.get(ENOUGH_MS, MILLISECONDS);
        assertTheCallTookBetween(start, DELAY_MS, ENOUGH_MS);
        assertThat(result, is("done"));
    }

    @Test
    public void shouldUseCallWithTimeout() throws Exception {
        TimeLimiter timeLimiter = new SimpleTimeLimiter();
        long start = System.nanoTime();
        String result = timeLimiter.callWithTimeout(
                () -> doSomeHeavyWeightOperation(), ENOUGH_MS, MILLISECONDS, true);
        assertEquals("done", result);
        assertTheCallTookBetween(start, DELAY_MS, ENOUGH_MS);
    }

    @Test
    public void shouldUseProxy() throws Exception {
        TimeLimiter timeLimiter = new SimpleTimeLimiter();
        long start = System.nanoTime();
        HeavyOperation target = () -> doSomeHeavyWeightOperation();
        HeavyOperation proxy = timeLimiter.newProxy(target, HeavyOperation.class, ENOUGH_MS, MILLISECONDS);
        assertEquals("done", proxy.doHeavyWeightOperation());
        assertTheCallTookBetween(start, DELAY_MS, ENOUGH_MS);
    }

    @Test(expected = UncheckedTimeoutException.class)
    public void shouldTriggerTimeout() throws Exception {
        new SimpleTimeLimiter().callWithTimeout(
                () -> doSomeHeavyWeightOperation(), NOT_ENOUGH_MS, MILLISECONDS, true);
    }

    private static String doSomeHeavyWeightOperation() throws InterruptedException {
        MILLISECONDS.sleep(DELAY_MS);
        return "done";
    }

}