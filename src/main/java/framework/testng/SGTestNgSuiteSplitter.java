package framework.testng;

import framework.testng.annotations.TestConfiguration;
import framework.utils.LogUtils;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestClass;
import org.testng.ITestContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author moran
 */
public class SGTestNgSuiteSplitter implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods,
                                           ITestContext context) {

        //bug in testng which calls listener twice!
        //http://groups.google.com/group/testng-users/browse_thread/thread/426d260797d07c27
        {
            if (true == Boolean.getBoolean("sgtest.IMethodInterceptor.called")) {
                return methods; //already called!
            }
            System.setProperty("sgtest.IMethodInterceptor.called", "true");
        }
        //end of bug


        int NUM_OF_SUITES = Integer.getInteger("sgtest.numOfSuites", 1);
        int SUITE_ID = Integer.getInteger("sgtest.suiteId", 0);
        int totalTests = context.getAllTestMethods().length;


        LogUtils.log("SGTestNgSuiteSplitter - sysproperty key=\"sgtest.numOfSuites\" value=" + NUM_OF_SUITES);
        LogUtils.log("SGTestNgSuiteSplitter - sysproperty key=\"sgtest.suiteId\" value=" + SUITE_ID);
        LogUtils.log("SGTestNgSuiteSplitter - total tests: " + totalTests);

        Map<Integer, List<IMethodInstance>> suites = new HashMap<Integer, List<IMethodInstance>>();
        int initialCapacity = Math.max(10, totalTests / NUM_OF_SUITES); //try and optimize or use default capacity
        for (int i = 0; i < NUM_OF_SUITES; ++i) {
            suites.put(i, new ArrayList<IMethodInstance>(initialCapacity));
        }

        int suiteIndex = -1;
        ITestClass testClass = null;
        for (IMethodInstance methodInstance : methods) {
            if (context.getExcludedMethods().contains(methodInstance.getMethod())) {
                continue; //excluded method
            }
            if (!doRunMethodOnVM(methodInstance.getMethod().getTestClass().getRealClass(), methodInstance)) {
                continue;
            }
            ITestClass methodTestClass = methodInstance.getMethod().getTestClass();


            //include all methods of a certain test
            if (!methodTestClass.equals(testClass)) {
                testClass = methodTestClass;
                suiteIndex = getNextBalancedSuiteIndex(suites);
            }

            List<IMethodInstance> methodsPerSuite = suites.get(suiteIndex);
            methodsPerSuite.add(methodInstance);
        }

        //log how many tests are in each suite
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, List<IMethodInstance>> entry : suites.entrySet()) {
            sb.append("\tsuiteId: ").append(entry.getKey()).append(" size: ").append(entry.getValue().size()).append("\n");
        }
        LogUtils.log("SGTestNgSuiteSplitter - tests in each suite: \n" + sb);


        List<IMethodInstance> listOfMethodsInRequestedSuite = suites.get(SUITE_ID);
        LogUtils.log("SGTestNgSuiteSplitter - tests in requested suite: " + listOfMethodsInRequestedSuite.size());

        SGTestNGMethodInterceptor orderInterceptor = new SGTestNGMethodInterceptor();
        List<IMethodInstance> orderedListOfMethodsInRequestedSuite = orderInterceptor.intercept(listOfMethodsInRequestedSuite, context);
        return orderedListOfMethodsInRequestedSuite;
    }

    private int getNextBalancedSuiteIndex(Map<Integer, List<IMethodInstance>> suites) {
        int sizeOfSmallestSuite = Integer.MAX_VALUE;
        int smallestSuiteIndex = 0;
        for (Map.Entry<Integer, List<IMethodInstance>> entry : suites.entrySet()) {
            int numberOfTestsInSuite = entry.getValue().size();
            if (numberOfTestsInSuite < sizeOfSmallestSuite) {
                sizeOfSmallestSuite = numberOfTestsInSuite;
                smallestSuiteIndex = entry.getKey();
            }
        }
        return smallestSuiteIndex;
    }

    public boolean doRunMethodOnVM(final Class<?> klass, IMethodInstance methodInstance) {
        final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(klass.getDeclaredMethods()));
        for (final Method method : allMethods) {
            if (methodInstance.getMethod().getMethodName().equals(method.getName())) {
                Annotation[] annotations = method.getDeclaredAnnotations();
                if(!isTestConfigurationAnnotated(annotations)){
                    return true;
                }
                for (Annotation anno : annotations) {
                    if (addTestAccordingVM(anno)) return true;
                }
            }
        }
        return false;
    }

    private boolean addTestAccordingVM(Annotation anno) {
        if (anno instanceof TestConfiguration) {
            for (TestConfiguration.VM vm : ((TestConfiguration) anno).os()) {
                if ((vm.compareTo(getOS()) == 0) ||
                        (vm.compareTo(TestConfiguration.VM.ALL) == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTestConfigurationAnnotated(Annotation[] annotations){
        boolean testConfigurationAnnotated = false;
        for (Annotation anno : annotations) {
            if(anno instanceof TestConfiguration){
                testConfigurationAnnotated = true;
            }
        }
        return testConfigurationAnnotated;
    }


    private String OS = System.getProperty("os.name").toLowerCase();

    public TestConfiguration.VM getOS() {
        if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
            return TestConfiguration.VM.UNIX;
        } else {
            if (OS.indexOf("win") >= 0) {
                return TestConfiguration.VM.WINDOWS;
            } else {
                return TestConfiguration.VM.MAC;
            }
        }
    }
}
