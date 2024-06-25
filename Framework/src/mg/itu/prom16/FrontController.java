package mg.itu.prom16;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.net.URLDecoder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.annotation.AnnotationController;
import mg.itu.annotation.FormParametre;
import mg.itu.annotation.Get;
import mg.itu.annotation.Parametre;
import mg.itu.annotation.RequestBody;

public class FrontController extends HttpServlet {
    Map<String, Mapping> urlMappings = new HashMap<>();
    boolean checked = false;
    ArrayList<Class<?>> list;

    public void init() throws ServletException {
        try {
            scanner();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Failed to load controllers", e);
        }
    }

    private void scanner() throws ServletException{
        if (!checked) {
            String pack = getServletConfig().getInitParameter("ControllerPackage");
            if(pack == null){
                throw new ServletException("The parameter name ControllerPackage doesn't exist");
            }
            else{
                try {
                    list = getClassesInSpecificPackage(pack);
                    getListController(pack);
                    checked = true;
                } catch (Exception e) {
                    list = new ArrayList<>();
                    throw new ServletException(e.getMessage());
                }
            }
        }
    }

    private void getListController(String package_name) throws Exception {
        String path = "WEB-INF/classes/" + package_name.replace(".", "/");
        path = getServletContext().getRealPath(path);
        File file = new File(path);

        list.clear();
        
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File uniquefile : file.listFiles()) {
                    if (uniquefile.isFile() && uniquefile.getName().endsWith(".class")) {
                        String className = package_name + "." + uniquefile.getName().replace(".class", "");
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(AnnotationController.class)) {
                            list.add(clazz);
    
                            for (Method method : clazz.getMethods()) {
                                if (method.isAnnotationPresent(Get.class)) {
                                    Mapping mapping = new Mapping(clazz.getName(), method);
                                    String key = method.getAnnotation(Get.class).value();
                                    if (urlMappings.containsKey(key)) {
                                        throw new Exception("The method "+urlMappings.get(key).getMethod()+" have already the url "+key+", so you can't affect this url with the method "+mapping.getMethod());
                                    }
                                    urlMappings.put(key, mapping);
                                }
                            }
                        }
                    }
                }
            }
        }
        else{
            throw new Exception("The package "+package_name+" doesn't exist");
        }
    }
    
    private ArrayList<Class<?>> getClassesInSpecificPackage(String packageName) throws IOException, ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<>();
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

    private Object getValueInMethod(HttpServletRequest request, Mapping map) throws Exception{
        Object returnValue = null;
        try {
            Class<?> clazz = Class.forName(map.getClassName());
            map.getMethod().setAccessible(true);

            Parameter[] methodParams = map.getMethod().getParameters();
            Object[] args = new Object[methodParams.length];

            Enumeration<String> params = request.getParameterNames();
            Map<String, String> paramMap = new HashMap<>();

            while (params.hasMoreElements()) {
                String paramName = params.nextElement();
                paramMap.put(paramName, request.getParameter(paramName));
            }
            for (int i = 0; i < methodParams.length; i++) {
                if (methodParams[i].isAnnotationPresent(RequestBody.class)) {
                    Class<?> paramType = methodParams[i].getType();
                    Object paramObject = paramType.getDeclaredConstructor().newInstance();
                    for (Field field : paramType.getDeclaredFields()) {
                        String paramName = field.isAnnotationPresent(FormParametre.class) ? field.getAnnotation(FormParametre.class).value() : field.getName();
                        if (paramMap.containsKey(paramName)) {
                            field.setAccessible(true);
                            field.set(paramObject, paramMap.get(paramName));
                        }
                    }
                    args[i] = paramObject;
                }
                //sprint6
                else if (methodParams[i].isAnnotationPresent(Parametre.class)) {
                    String paramName = methodParams[i].getAnnotation(Parametre.class).name();
                    String paramValue = paramMap.get(paramName);
                    args[i] = paramValue;
                } else {
                    if (paramMap.containsKey(methodParams[i].getName())) {
                        args[i] = paramMap.get(methodParams[i].getName());
                    } else {
                        args[i] = null;
                    }
                }
            }
            
            Object instance = clazz.getDeclaredConstructor().newInstance();
            returnValue = map.getMethod().invoke(instance, args);
        } catch (Exception e) {
            throw e;
        }
        return returnValue;
    }



    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String url = request.getRequestURI().substring(request.getContextPath().length());
        try{
            PrintWriter out = response.getWriter();
            Mapping mapping = urlMappings.get(url);
            if (mapping != null) {
                Object ob = getValueInMethod(request, mapping);
                if (ob != null) {
                    if (ob instanceof String) {
                        out.println("The value returned by the method <b>" + mapping.getMethod().getName() + "</b> is: <b>" + ob+"<b>");
                    }
                    else if(ob instanceof ModelAndView mw){
                        for (String cle : mw.getData().keySet()) {
                            request.setAttribute(cle, mw.getData().get(cle));
                        }
                        RequestDispatcher dispacther = request.getRequestDispatcher(mw.getUrl());
                        dispacther.forward(request, response);
                    }
                    else{
                        throw new ServletException("Failed to return the value",new Exception("The value returned by the method <b>" + mapping.getMethod() + "</b> is not in the framework"));
                    }
                }
                else{
                    throw new ServletException("No value returned");
                }
                
            } else {
                throw new ServletException("Failed to get the method",new Exception("No Get method associated with the URL: <b>" + url+"</b>"));
            }
        }
        catch(Exception e){
            try(PrintWriter out = response.getWriter()){
                out.println(e);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            PrintWriter out = response.getWriter();
            out.println("Erreur: " + ex.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            PrintWriter out = response.getWriter();
            out.println("Erreur: " + ex.getMessage());
        }
    }
}
