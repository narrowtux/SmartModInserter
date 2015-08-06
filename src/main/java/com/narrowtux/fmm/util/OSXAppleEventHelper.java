package com.narrowtux.fmm.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    taken from this wonderful stackoverflow thread:
    http://stackoverflow.com/questions/20074610/handle-custom-url-schemes-in-an-os-x-java-application
 */
class OpenURIEventInvocationHandler implements InvocationHandler {

    private OpenUriAppleEventHandler urlHandler;

    public OpenURIEventInvocationHandler(OpenUriAppleEventHandler urlHandler) {
        this.urlHandler = urlHandler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked"})
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("openURI")) {
            try {
                Class openURIEventClass = Class.forName("com.apple.eawt.AppEvent$OpenURIEvent");
                Method getURLMethod = openURIEventClass.getMethod("getURI");
                //arg[0] should be an instance of OpenURIEvent
                URI uri =  (URI)getURLMethod.invoke(args[0]);
                urlHandler.handleURI(uri);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
}

public class OSXAppleEventHelper {
    /**
     * Call only on OS X
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setOpenURIAppleEventHandler(OpenUriAppleEventHandler urlHandler) {
        try {
            Class applicationClass = Class.forName("com.apple.eawt.Application");
            Method getApplicationMethod = applicationClass.getDeclaredMethod("getApplication", (Class[])null);
            Object application = getApplicationMethod.invoke(null, (Object[])null);

            Class openURIHandlerClass = Class.forName("com.apple.eawt.OpenURIHandler", false, applicationClass.getClassLoader());
            Method setOpenURIHandlerMethod = applicationClass.getMethod("setOpenURIHandler", openURIHandlerClass);

            OpenURIEventInvocationHandler handler = new OpenURIEventInvocationHandler(urlHandler);
            Object openURIEvent = Proxy.newProxyInstance(openURIHandlerClass.getClassLoader(), new Class[] { openURIHandlerClass }, handler);
            setOpenURIHandlerMethod.invoke(application, openURIEvent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}