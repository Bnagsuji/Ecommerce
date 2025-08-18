package kr.hhplus.be.server.lock;

import java.lang.annotation.*;
import java.time.temporal.ChronoUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {


    String[] keys();

    long lease() default 5;

    ChronoUnit unit() default ChronoUnit.SECONDS;

    long waitFor() default 2;

    ChronoUnit waitUnit() default ChronoUnit.SECONDS;

    long pollMillis() default 100;

    String prefix();
}
