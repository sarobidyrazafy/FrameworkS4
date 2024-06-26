package mg.prom16;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class Mapping {
    private String className;
    private Method method;

    // Constructeur pour initialiser le nom de la classe et la méthode
    public Mapping(String className, Method method) {
        this.className = className;
        this.method = method;
    }

    // Getter pour le nom de la classe
    public String getClassName() {
        return className;
    }

    // Getter pour la méthode
    public Method getMethod() {
        return method;
    }

    // Représentation sous forme de chaîne pour faciliter le débogage
    @Override
    public String toString() {
        return "Mapping{" +
                "className='" + className + '\'' +
                ", methodName='" + method.getName() + '\'' +
                '}';
    }

    // Méthode pour obtenir une représentation textuelle de la méthode (nom et types de paramètres)
    public String method_to_string() {
        StringBuilder methodString = new StringBuilder();
        methodString.append(method.getName()).append("(");

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            methodString.append(parameters[i].getType().getSimpleName());
            if (i < parameters.length - 1) {
                methodString.append(", ");
            }
        }

        methodString.append(")");
        return methodString.toString();
    }
}
