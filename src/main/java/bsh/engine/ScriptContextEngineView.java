//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package bsh.engine;

import javax.script.ScriptContext;
import java.util.*;

public class ScriptContextEngineView implements Map<String, Object> {
    ScriptContext context;

    public ScriptContextEngineView(ScriptContext context) {
        this.context = context;
    }

    public int size() {
        return this.totalKeySet().size();
    }

    public boolean isEmpty() {
        return this.totalKeySet().size() == 0;
    }

    public boolean containsKey(Object key) {
        return this.context.getAttribute((String) key) != null;
    }

    public boolean containsValue(Object value) {
        Set values = this.totalValueSet();
        return values.contains(value);
    }

    public Object get(Object key) {
        return this.context.getAttribute((String) key);
    }

    public Object put(String key, Object value) {
        Object oldValue = this.context.getAttribute(key, 100);
        this.context.setAttribute(key, value, 100);
        return oldValue;
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        this.context.getBindings(100).putAll(t);
    }

    public Object remove(Object okey) {
        String key = (String) okey;
        Object oldValue = this.context.getAttribute(key, 100);
        this.context.removeAttribute(key, 100);
        return oldValue;
    }

    public void clear() {
        this.context.getBindings(100).clear();
    }

    public Set keySet() {
        return this.totalKeySet();
    }

    public Collection values() {
        return this.totalValueSet();
    }

    public Set<Entry<String, Object>> entrySet() {
        throw new Error("unimplemented");
    }

    private Set totalKeySet() {
        Set keys = new HashSet();
        List<Integer> scopes = this.context.getScopes();
        Iterator var3 = scopes.iterator();

        while (var3.hasNext()) {
            int i = ((Integer) var3.next()).intValue();
            keys.addAll(this.context.getBindings(i).keySet());
        }

        return Collections.unmodifiableSet(keys);
    }

    private Set totalValueSet() {
        Set values = new HashSet();
        List<Integer> scopes = this.context.getScopes();
        Iterator var3 = scopes.iterator();

        while (var3.hasNext()) {
            int i = ((Integer) var3.next()).intValue();
            values.addAll(this.context.getBindings(i).values());
        }

        return Collections.unmodifiableSet(values);
    }
}
