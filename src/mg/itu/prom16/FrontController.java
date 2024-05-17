package mg.itu.prom16;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.annotation.AnnotationController;

public class FrontController extends HttpServlet{
    boolean checked = false;
    List<Class<?>> ls;

    public void init() throws ServletException {
        super.init();
        scan();
    }

    private void scan(){
        if (!checked) {
            String pack = this.getInitParameter("controllerPackage");
            try {
                ls = getClassesInPackage(pack);
                checked = true;
            } catch (Exception e) {
                e.printStackTrace();
                ls = new ArrayList<>();
            }
        }
    }

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
                            if (clazz.isAnnotationPresent(AnnotationController.class)) {
                                classes.add(clazz);
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        try (PrintWriter out = response.getWriter()) {
            // out.println("URL: "+request.getRequestURL());
            out.println("Liste des Controllers:");
            if (ls.isEmpty()) {
                out.println("La liste est vide.");
            }
            for (int i = 0; i < ls.size(); i++) {
                out.println((i+1)+": "+ls.get(i));
            }
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
