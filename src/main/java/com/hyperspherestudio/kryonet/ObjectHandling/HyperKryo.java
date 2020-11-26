package com.hyperspherestudio.kryonet.ObjectHandling;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import static com.esotericsoftware.kryo.util.Util.className;
//Author Johnathan Bizzano
public class HyperKryo extends Kryo {

    public HyperKryo() {
        setInstantiatorStrategy(new HyperStrategy());
    }

    @Override
    public void writeClassAndObject(Output output, Object o) {
        super.writeClassAndObject(output, o);
    }

    @Override
    public Object readClassAndObject(Input input) {
        return super.readClassAndObject(input);
    }
} class HyperStrategy implements org.objenesis.strategy.InstantiatorStrategy {
    private InstantiatorStrategy fallbackStrategy;
    private Objenesis objenesis = new ObjenesisStd();

    public void setFallbackInstantiatorStrategy (final InstantiatorStrategy fallbackStrategy) {
        this.fallbackStrategy = fallbackStrategy;
    }

    public InstantiatorStrategy getFallbackInstantiatorStrategy () {
        return fallbackStrategy;
    }

    public ObjectInstantiator newInstantiatorOf (final Class type) {
        if (!Util.isAndroid) {
            Class enclosingType = type.getEnclosingClass();
            boolean isNonStaticMemberClass = enclosingType != null && type.isMemberClass()
                    && !Modifier.isStatic(type.getModifiers());
            if (!isNonStaticMemberClass) {
                try {
                    final ConstructorAccess access = ConstructorAccess.get(type);
                    return () -> {
                        try {
                            return access.newInstance();
                        } catch (Exception ex) {
                            throw new KryoException("Error constructing instance of class: " + className(type), ex);
                        }
                    };
                } catch (Exception ignored) {
                }
            }
        }
        // Reflection.
        try {
            Constructor ctor;
            try {
                ctor = type.getConstructor((Class[])null);
            } catch (Exception ex) {
                ctor = type.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
            }
            final Constructor constructor = ctor;
            return () -> {
                try {
                    return constructor.newInstance();
                } catch (Exception ex) {
                    throw new KryoException("Error constructing instance of class: " + className(type), ex);
                }
            };
        } catch (Exception ignored) {
        }
        if (fallbackStrategy == null) {
            return () -> objenesis.getInstantiatorOf(type).newInstance();
        }
        return fallbackStrategy.newInstantiatorOf(type);
    }
}
