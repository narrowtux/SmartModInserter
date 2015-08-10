package com.narrowtux.fmm.util;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableObjectValue;
import javafx.beans.value.WritableStringValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.function.Function;

/**
 * Created by tux on 10/08/15.
 */
public class Bindings {

    /**
     * Returns a new observable string which contains either the contents of ifTrue, or ifFalse, depending on the condition
     * @param condition
     * @param ifTrue
     * @param ifFalse
     * @return
     */
    public static ObservableStringValue decision(ObservableBooleanValue condition,
                                                 ObservableStringValue ifTrue,
                                                 ObservableStringValue ifFalse) {
        StringProperty ret = new SimpleStringProperty();
        condition.addListener((obs, ov, nv) -> {
            ret.set(nv ? ifTrue.get() : ifFalse.get());
        });
        ifTrue.addListener((obs, ov, nv) -> {
            if (condition.get()) {
                ret.set(nv);
            }
        });
        ifFalse.addListener((obs, ov, nv) -> {
            if (!condition.get()) {
                ret.set(nv);
            }
        });
        ret.set(condition.get() ? ifTrue.get() : ifFalse.get());

        return ret;
    }

    public static <O> StringExpression selection(ObservableValue<O> selection, Function<O, ObservableValue<String>> property) {
        return selection(selection, property, new SimpleStringProperty());
    }

    public static
    <T extends ObservableValue<V> & WritableObjectValue<V>, O, V, R extends ObservableValue<V>>
    R selection(ObservableValue<O> selection, Function<O, ObservableValue<V>> property, T instance) {
        try {
            selection.addListener((obs, ov, nv) -> {
                if (nv != null) {
                    ObservableValue<V> prop = property.apply(nv);
                    instance.set(prop.getValue());
                    prop.addListener(new ChangeListener<V>() {
                        @Override
                        public void changed(ObservableValue<? extends V> obs2, V ov2, V nv2) {
                            if (selection.getValue() == nv) {
                                instance.set(nv2);
                            } else {
                                obs2.removeListener(this);
                            }
                        }
                    });
                }
            });
            final O currentSelection = selection.getValue();
            if (currentSelection != null) {
                ObservableValue<V> prop = property.apply(selection.getValue());
                instance.set(prop.getValue());
                prop.addListener(new ChangeListener<V>() {
                    @Override
                    public void changed(ObservableValue<? extends V> obs, V ov, V nv) {
                        if (selection.getValue() == currentSelection) {
                            instance.set(nv);
                        } else {
                            obs.removeListener(this);
                        }
                        ;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return (R) instance;
    }

    public static IntegerExpression collectionSize(ObservableList list) {
        SimpleIntegerProperty ret = new SimpleIntegerProperty(list.size());
        list.addListener((ListChangeListener) change -> ret.set(list.size()));
        return ret;
    }
}
