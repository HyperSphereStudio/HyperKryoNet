package com.hyperspherestudio.kryonet.JVMManager;

import com.esotericsoftware.kryonet.Connection;

import java.io.Serializable;
import java.util.function.Consumer;

public class SharedVariable<T> {

    public T t;
    public String variableName;
    public transient JVMConnection jvmConnection;
    public boolean destroyed;
    public Consumer<T> onSet;

    public SharedVariable() {

    }

    public SharedVariable(Consumer<T> onSetListener, JVMConnection jvmConnection, String variableName) {
        this.variableName = variableName;
        this.jvmConnection = jvmConnection;
        this.onSet = onSetListener;
        jvmConnection.sharedVariables.put(variableName, this);
    }


    public void setLocal(T t, boolean triggerSet) {
        this.t = t;
        if (triggerSet) {
            onSet.accept(t);
        }
    }

    public void set(T t) {
        setLocal(t, false);
        jvmConnection.sendToAll(new JVMConnectionPacket(new JVMVariableRequest<>(variableName, t)));
    }

    public boolean checkDestroyed() {
        return destroyed;
    }


    public T getLocal() {
        return t;
    }

    public void getAsnyc() {

        if (!jvmConnection.serverMode)
            jvmConnection.sendPacket(new JVMConnectionPacket(jvmConnection.getServerConnectionID(), new JVMVariableRequest<T>(variableName)));
        else {
            onSet.accept(t);
        }
    }


    public void destroy() {
        jvmConnection.sendToAll(new JVMConnectionPacket(new JVMVariableRequest<T>(variableName, true)));
        destroyed = true;
        jvmConnection.sharedVariables.remove(variableName);
    }


}

class JVMVariableRequest<T> implements Serializable {

    public boolean SET, DESTROY;
    public T t;
    public String variableName;

    public JVMVariableRequest() {

    }

    public void dealWith(Connection connection, JVMConnection jvmConnection) {
        if (DESTROY) {
            if (jvmConnection.sharedVariables.containsKey(variableName)) {
                jvmConnection.sharedVariables.get(variableName).destroyed = true;
                jvmConnection.sharedVariables.remove(variableName);
            }
        } else if (SET) {
            jvmConnection.sharedVariables.get(variableName).setLocal(t, true);
        } else {
            connection.sendTCP(new JVMConnectionPacket(new JVMVariableRequest<>(variableName, jvmConnection.sharedVariables.get(variableName).t)));
        }
    }

    public JVMVariableRequest(String variableName) {
        this.variableName = variableName;
    }

    public JVMVariableRequest(String variableName, T t) {
        this(variableName);
        this.t = t;
        this.SET = true;
    }

    public JVMVariableRequest(String variableName, boolean destroy) {
        this(variableName);
        this.DESTROY = destroy;
    }

}
