package mg.itu.prom16;
import mg.itu.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.prom16.utils.*;
import mg.itu.prom16.classes.*;
import mg.itu.prom16.annotation.*;

public class FrontController extends HttpServlet{
    private List<String> listControllers;
    protected HashMap<String,Mapping> urlMapping = new HashMap<String,Mapping>();

    @Override
    public void init() throws ServletException {
        super.init();


        ServletContext context = getServletContext();
        String packageName = context.getInitParameter("Controllers");

        List<String> controllers = listControllers;
        controllers = new ArrayList<>(); 

        HashMap<String, Mapping> urls = urlMapping;
        urls = new HashMap<>(); 
        
        try {

            List<Class<?>> allClasses = this.findClasses(packageName);

            for (Class<?> classe : allClasses) {
                if(classe.isAnnotationPresent(Controller.class)) {
                    controllers.add(classe.getName());
                    
                    Method[] allMethods = classe.getMethods();
                    for (Method m : allMethods) {
                        if (m.isAnnotationPresent(Get.class)) {
                            Get mGetAnnotation = (Get) m.getAnnotation(Get.class);
                            System.out.println(m.getName()+"jngngngngnngng");
                            urls.put(mGetAnnotation.url(), new Mapping(classe.getName(), m.getName()));
                        }
                    }
                }
            }
            // setting the values of the attributes
            this.listControllers = controllers;
            this.urlMapping = urls;
        } catch (Exception e) {
            
        }
    }

    public List<Class<?>> findClasses(String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();

        String path = "WEB-INF/classes/" + packageName.replace(".", "/");
        String realPath = getServletContext().getRealPath(path);

        File directory = new File(realPath);
        File[] files = directory.listFiles();

        for(File f : files) {
            // filtering class files
            if(f.isFile() && f.getName().endsWith(".class")) {
                String className = packageName + "." + f.getName().split(".class")[0];
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }
    // public void init() throws ServletException {
    //     super.init();
    //     scan();
    // }

    // private void scan(){
    //     String pack = this.getInitParameter("controllerPackage");
    //     try {
    //         List<Class<?>> ls = getClassesInPackage(pack);
    //         hashMap = initializeHashMap(ls);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         // ls = new ArrayList<>();
    //         hashMap = new HashMap<>();
    //     }
    // }

    private List<Class<?>> getClassesInPackage(String packageName) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');

        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File directory = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
                if (directory.exists() && directory.isDirectory()) {
                    File[] files = directory.listFiles();
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(Controller.class)) {
                                classes.add(clazz);
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }

    HashMap<String, Mapping> initializeHashMap(List<Class<?>> ls){
        HashMap<String, Mapping> map = new HashMap<>();
        for (Class<?> class1 : ls) {
            Method[] methods = class1.getDeclaredMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(Get.class)) {
                    Mapping mapping = new Mapping(class1.getSimpleName(), m.getName());

                    Get annotation = m.getAnnotation(Get.class);
                    map.put(annotation.url(), mapping);
                }
            }
        }

        return map;
    }

    String extract(String uri) {
        String[] segments = uri.split("/");
        if (segments.length > 1) {
            return String.join("/", java.util.Arrays.copyOfRange(segments, 2, segments.length));
        }
        return "";
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");

        PrintWriter out = resp.getWriter();

        // getting the URL requested by the client
        String requestedURL = req.getRequestURL().toString();
        String[] partedReq = requestedURL.split("/");
        String urlToSearch = partedReq[partedReq.length - 1];
        
        // searching for that URL inside of our HashMap
        if(urlMapping.containsKey(urlToSearch)) {
            Mapping m = urlMapping.get(urlToSearch);
            try {
                Object result = m.invoke();
                if (result instanceof String){
                    out.println(result);
                } else if (result instanceof ModelView){
                    ModelView view = (ModelView)result; 
                    req.setAttribute("attribut", view.getData());
                    RequestDispatcher dispatcher = req.getRequestDispatcher(view.getUrl());
                    dispatcher.forward(req, resp);
                }
            } catch (Exception e) {
                out.println(e.getMessage());
            }
            
        } else {
            out.println("La methode n'existe pas " + urlToSearch );
        }

        out.flush();
        out.close();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
       processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
       processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
