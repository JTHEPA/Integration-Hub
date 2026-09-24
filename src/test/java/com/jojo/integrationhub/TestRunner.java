package com.jojo.integrationhub;

import java.lang.reflect.Method;
import java.util.List;

/**
 * A deliberately tiny test runner: no JUnit/TestNG dependency, so the
 * whole project (including tests) still builds with nothing but the JDK.
 *
 * Convention: any public no-arg method named "testXxx" on a registered
 * class is treated as a test case. A failed assertion (AssertionError,
 * thrown by {@link Assert}) is caught and reported; everything else is
 * reported as an error rather than silently killing the run.
 *
 * For a project of any real size, swap this for JUnit 5 + Maven Surefire.
 * It is hand-rolled here purely so the whole repo builds offline.
 */
public final class TestRunner {

    public static void main(String[] args) throws Exception {
        List<Class<?>> testClasses = List.of(
                EventBusTest.class,
                CircuitBreakerTest.class,
                InventoryRepositoryTest.class
        );

        int passed = 0;
        int failed = 0;

        for (Class<?> testClass : testClasses) {
            Object instance = testClass.getDeclaredConstructor().newInstance();
            for (Method method : testClass.getMethods()) {
                if (!method.getName().startsWith("test") || method.getParameterCount() != 0) {
                    continue;
                }
                String label = testClass.getSimpleName() + "." + method.getName();
                try {
                    method.invoke(instance);
                    System.out.println("PASS  " + label);
                    passed++;
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.out.println("FAIL  " + label + " -> " + cause);
                    failed++;
                }
            }
        }

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
