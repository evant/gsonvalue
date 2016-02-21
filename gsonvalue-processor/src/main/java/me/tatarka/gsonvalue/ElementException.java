package me.tatarka.gsonvalue;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

class ElementException extends Exception {
    private final Element element;

    public ElementException(String message, Element element) {
        super(message);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    public void printMessage(Messager messager) {
        messager.printMessage(Diagnostic.Kind.ERROR, getMessage(), element);
    }
}
