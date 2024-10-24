package mg.itu.prom16.utils;

import java.lang.reflect.Method;

public class Mapping {
    String className;
    String methodName;
    public Mapping(String key, String value) {
        this.className = key;
        this.methodName = value;
    }

    public String getClassName() {
        return className;
    }
    public String getMethodName() {
        return methodName;
    }
    public void setClassName(String className) {
        this.className = className;
    }public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public Object invoke() throws Exception {
        Object o = null;
        
        Class<?> clazz = Class.forName(this.className);
        Object ins = clazz.getConstructor().newInstance();

        Method m = clazz.getMethod(this.methodName, null);

        o = m.invoke(ins, null);

        return o;
    }


}
 