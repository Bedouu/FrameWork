package mg.itu.prom16;
import mg.itu.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.naming.directory.InvalidAttributesException;

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
                            if(urls.containsKey(mGetAnnotation.url())){
                                throw new InvalidAttributesException("The url "+mGetAnnotation.url()+" is duplicated.");
                            }
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
        // Gerer les erreurs 
        if(!directory.exists()){
            try {
                throw new InvalidAttributesException("The package "+packageName+" does not exist.");
            } catch (InvalidAttributesException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if(files.length <= 0){
            try {
                throw new InvalidAttributesException("The package "+packageName+" is empty.");
            } catch (InvalidAttributesException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
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

    // HashMap<String, Mapping> initializeHashMap(List<Class<?>> ls){
    //     HashMap<String, Mapping> map = new HashMap<>();
    //     for (Class<?> class1 : ls) {
    //         Method[] methods = class1.getDeclaredMethods();
    //         for (Method m : methods) {
    //             if (m.isAnnotationPresent(Get.class)) {
    //                 Mapping mapping = new Mapping(class1.getSimpleName(), m.getName());

    //                 Get annotation = m.getAnnotation(Get.class);
    //                 map.put(annotation.url(), mapping);
    //             }
    //         }
    //     }

    //     return map;
    // }

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
        try {
            // getting the URL requested
            String requestedURL = req.getRequestURL().toString();
            String[] partedReq = requestedURL.split("/");
            String urlToSearch = partedReq[partedReq.length - 1];  
            System.out.println(requestedURL);  

            // Finding the url dans le map
            if(urlMapping.containsKey(urlToSearch)) {
                Mapping m = urlMapping.get(urlToSearch);
                Parameter[] params = null;
                String[] args = null;

                if(m.getParameters() != null) {
                    System.out.println(m.getParameters());

                    // Retrieve the parameters of the method from the request first 
                    params = m.getParameters();
                    args = new String[params.length];
                    int i = 0;
                    for (Parameter param : params) {
                        String paramString = req.getParameter(param.getName());
                        if(paramString != null){
                            args[i] = req.getParameter(param.getName());
                        } else {
                            String paramName = param.getAnnotation(Param.class).name();
                            args[i] = req.getParameter(paramName);
                        }
                    }
                }
                
                // Invoking the method 
                Object result = m.invoke(args);
                if (result instanceof String){
                    out.println(result);
                } else if (result instanceof ModelView){
                    ModelView view = (ModelView)result; 
                    req.setAttribute("attribut", view.getData());

                    RequestDispatcher dispatcher = req.getRequestDispatcher(view.getUrl());
                    dispatcher.forward(req, resp);
                }

                
            } else {
                out.println("No method matching '" + urlToSearch + "' to call");
            }
            
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            out.println(e.getMessage()+"   exccc");
        }
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
