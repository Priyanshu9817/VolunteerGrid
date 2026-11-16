package com.volunteergrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight test runner for executing JUnit/Mockito unit tests without requiring external Maven CLI.
 * Runs tests via reflection, executing @BeforeEach and @Test methods with full Mockito mock injection.
 */
public class StandaloneTestRunner {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   VolunteerGrid Test Suite – Standalone Execution");
        System.out.println("===============================================================\n");

        Class<?>[] testClasses = new Class<?>[] {
            com.volunteergrid.service.EventServiceTest.class,
            com.volunteergrid.service.AssignmentServiceTest.class,
            com.volunteergrid.service.AttendanceServiceTest.class
        };

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        long suiteStartTime = System.currentTimeMillis();

        for (Class<?> clazz : testClasses) {
            DisplayName classDisplayName = clazz.getAnnotation(DisplayName.class);
            String className = classDisplayName != null ? classDisplayName.value() : clazz.getSimpleName();
            System.out.println("---------------------------------------------------------------");
            System.out.println("Running: " + className + " (" + clazz.getSimpleName() + ")");
            System.out.println("---------------------------------------------------------------");

            List<Method> testMethods = new ArrayList<>();
            Method beforeEachMethod = null;

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(BeforeEach.class)) {
                    beforeEachMethod = method;
                }
                if (method.isAnnotationPresent(Test.class)) {
                    testMethods.add(method);
                }
            }

            for (Method testMethod : testMethods) {
                totalTests++;
                DisplayName testDisplayName = testMethod.getAnnotation(DisplayName.class);
                String testDesc = testDisplayName != null ? testDisplayName.value() : testMethod.getName();

                AutoCloseable mocks = null;
                long start = System.currentTimeMillis();
                try {
                    Object testInstance = clazz.getDeclaredConstructor().newInstance();
                    mocks = MockitoAnnotations.openMocks(testInstance);

                    if (beforeEachMethod != null) {
                        beforeEachMethod.setAccessible(true);
                        beforeEachMethod.invoke(testInstance);
                    }

                    testMethod.setAccessible(true);
                    testMethod.invoke(testInstance);

                    long duration = System.currentTimeMillis() - start;
                    System.out.printf("  [PASS] %-60s (%d ms)%n", testDesc, duration);
                    passedTests++;
                } catch (InvocationTargetException ite) {
                    long duration = System.currentTimeMillis() - start;
                    Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
                    System.out.printf("  [FAIL] %-60s (%d ms)%n", testDesc, duration);
                    System.out.println("         Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                    cause.printStackTrace(System.out);
                    failedTests++;
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - start;
                    System.out.printf("  [ERROR] %-60s (%d ms)%n", testDesc, duration);
                    System.out.println("          Setup error: " + e.getMessage());
                    e.printStackTrace(System.out);
                    failedTests++;
                } finally {
                    if (mocks != null) {
                        try {
                            mocks.close();
                        } catch (Exception ignored) {}
                    }
                }
            }
            System.out.println();
        }

        long totalDuration = System.currentTimeMillis() - suiteStartTime;
        System.out.println("===============================================================");
        System.out.println("   Test Execution Summary");
        System.out.println("===============================================================");
        System.out.printf("  Total Tests : %d%n", totalTests);
        System.out.printf("  Passed      : %d%n", passedTests);
        System.out.printf("  Failed      : %d%n", failedTests);
        System.out.printf("  Total Time  : %d ms%n", totalDuration);
        System.out.println("===============================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }
}
